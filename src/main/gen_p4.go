// Autogenerates a P4 file from a given template, filling in the LRU implementation as specified by that
// template. Typical uses:
//  go run gen_p4.go -template=$P4TEMPLATE -output=$P4OUT < my_update_function.txt
//  go run gen_p4.go -template=$P4TEMPLATE -output=$P4OUT -update="val = val + 1;"
//
// Note that the provided function and template are NOT type-checked or compile-time error-free.
// The output P4 file must be checked with a compiler.
// TODO: add default value.
package main

import (
	//"bufio"
	"bytes"
	"flag"
	"fmt"
	//"io"
	"os"
	"strings"
	"text/template"
)

const (
	finalTemplate          = "../p4/final_p4.tmpl"
	groupByActionsTemplate = "../p4/groupby_actions.tmpl"
)

var outputFile = flag.String("output", "", "Location of the output (default = stdout).")
var keyWidth = flag.Uint("key-width", 16, "Width of each key, in bits")
var valWidth = flag.Uint("value-width", 16, "Width of each value, in bits")
var lruRows = flag.Uint("lru-rows", 1024, "Number of rows in the LRU")
var lruWays = flag.Uint("lru-ways", 4, "Number of ways in the LRU. Total LRU size = lru-rows * lru-ways")
var compilerOut = flag.String("compiler-output", "", "The compiler output in string form. If not specified, will read from stdin.")
var funcMap = template.FuncMap{
	"add":   func(x, y int) int { return x + y },
	"sub":   func(x, y int) int { return x - y },
	"slice": func(x []string, a, b int) []string { return x[a:b] },
	"rev": func(x []string) []string {
		y := []string{}
		for i := range x {
			y = append(y, x[len(x)-i-1])
		}
		return y
	},
	"rep": func(x string, y int) string {
		r := []string{}
		for i := 0; i < y; i++ {
			r = append(r, x)
		}
		return strings.Join(r, ",")
	},
}

type LRUSegment struct {
	// Inclusive start and end indices within the bitvector representing the LRU row in the keys table.
	// KeyStart < KeyEnd, since unlike in P4, we're counting from the LSB.
	KeyStart, KeyEnd int
	// Likewise for values
	ValStart, ValEnd int
	// Whether we should evict this key if no match (true on the least-recent entry)
	Evict bool
}

type GroupByActionData struct {
	UpdateFn               string
	EqualsFn               string
	ZeroFn                 string
	LRUFn                  string
	KeyName                string
	ValueName              string
	RowKeyName             string
	RowValueName           string
	KeyRegisterName        string
	ValueRegisterName      string
	TableSize              int
	LruWays                int
	RowFieldNameList       []string
	UpdateCode             string
	DefaultValueName       string
	DefaultValueDefinition string
	DefaultKeyName         string
	DefaultKeyDefinition   string
	FieldNames             []string
	KeySourceFields        []string
}

type TemplateData struct {
	TemplateName string
	CommonMeta   []string
	QueryMeta    []string
	Stages       []*StageData
}

type StageData struct {
	Structs []string
	Actions string
	Control string
}

// Maps sources that we need to use in each key to the place they come from,
// i.e. standard metadata or metadata.
func mapToSources(fields []string) []string {
	// Map from each field to the fully qualified expression used to retrieve it from metadata.
	sources := []string{}
	for _, f := range fields {
		src, ok := sourceMap[strings.TrimSpace(f)]
		if !ok {
			panic("Unable to find key field " + f + " in metadata struct")
		}
		sources = append(sources, src)
	}
	return sources
}

func genGroupByAction(s *Stage) string {
	ways := int(*lruWays)
	defaultVal := []string{}
	for i := 0; i < len(s.Registers); i++ {
		defaultVal = append(defaultVal, "0")
	}
	defaultKey := []string{}
	for i := 0; i < len(s.KeyFields); i++ {
		defaultKey = append(defaultKey, "0")
	}
	data := &GroupByActionData{
		UpdateFn:               "update_" + s.Name,
		EqualsFn:               "equals_" + s.Name,
		ZeroFn:                 "isZero_" + s.Name,
		LRUFn:                  "groupby_" + s.Name,
		KeyName:                "Key_" + s.Name,
		ValueName:              "Value_" + s.Name,
		RowKeyName:             "RowKey_" + s.Name,
		RowValueName:           "RowValue_" + s.Name,
		KeyRegisterName:        "KeyReg_" + s.Name,
		ValueRegisterName:      "ValueReg_" + s.Name,
		TableSize:              int(*lruRows),
		LruWays:                ways,
		RowFieldNameList:       []string{"first", "second", "third", "fourth"},
		UpdateCode:             indentBy(strings.TrimSpace(s.Code), "\t\t"),
		DefaultValueName:       "defaultVal_" + s.Name,
		DefaultValueDefinition: "{" + strings.Join(defaultVal, ",") + "}",
		DefaultKeyName:         "defaultKey_" + s.Name,
		DefaultKeyDefinition:   "{" + strings.Join(defaultKey, ",") + "}",
		FieldNames:             s.KeyFields,
		KeySourceFields:        mapToSources(s.KeyFields),
	}
	t, err := template.New("groupby" + s.Name).Funcs(funcMap).ParseFiles(groupByActionsTemplate)
	if err != nil {
		panic(err)
	}
	ts := t.Templates()
	if len(ts) != 1 {
		panic("More than 1 defined template")
	}
	var out bytes.Buffer
	err = ts[0].Execute(&out, data)
	if err != nil {
		panic(err)
	}
	return out.String()
}

func genStructFields(fieldType string, fieldNames []string) string {
	out := []string{}
	for _, name := range fieldNames {
		out = append(out, fmt.Sprintf("\t%s %s;", fieldType, name))
	}
	return strings.Join(out, "\n")
}

func genStructsForGroupBy(s *Stage) []string {
	keyName := "Key_" + s.Name
	valName := "Value_" + s.Name
	// Generate the Key struct
	keyStruct := fmt.Sprintf("struct %s {\n%s\n}", keyName, genStructFields("bit<32>", s.KeyFields))
	valStruct := fmt.Sprintf("struct %s {\n%s\n}", valName, genStructFields("bit<32>", s.Registers))
	//rowKeyStruct := fmt.Sprintf("struct Row%s {\n%s\n}", keyName, genStructFields(keyName, []string{"first", "second", "third", "fourth"}))
	//rowValStruct := fmt.Sprintf("struct Row%s {\n%s\n}", valName, genStructFields(valName, []string{"first", "second", "third", "fourth"}))
	return []string{keyStruct, valStruct}
}

// Prefix every line in 'body' with 'space'.
// Intended to be used with tabs to pretty print the code.
func indentBy(body, space string) string {
	bodyParts := strings.Split(body, "\n")
	for i, p := range bodyParts {
		bodyParts[i] = space + p
	}
	return strings.Join(bodyParts, "\n")
}

func genBasicAction(name, code string) string {
	return fmt.Sprintf("\taction %s() {\n%s\n\t}\n", name, indentBy(code, "\t\t"))
}

func (s *Stage) ToData() *StageData {
	sd := &StageData{}
	keyName, valName := "Key_"+s.Name, "Value_"+s.Name
	evKey, evVal := "evictedKey_"+s.Name, "evictedValue_"+s.Name
	switch s.Op {
	case Project, Filter:
		updateFn := "update_" + s.Name
		sd.Actions = genBasicAction(updateFn, s.Code)
		sd.Control = indentBy(updateFn+"();", "\t\t")
	case GroupBy:
		sd.Structs = genStructsForGroupBy(s)
		sd.Actions = genGroupByAction(s)
		cntrl := fmt.Sprintf("%s %s;\n%s %s;\n%s(%s, %s);", keyName, evKey, valName, evVal, "groupby_"+s.Name, evKey, evVal)
		sd.Control = indentBy(cntrl, "\t\t")
	default:
		panic("Stage type not supported")
	}
	return sd
}

func main() {
	flag.Parse()
	s := NewSchemaFromInput(*compilerOut)
	data := &TemplateData{
		TemplateName: "final_p4.tmpl",
		CommonMeta:   s.CommonMeta,
		QueryMeta:    s.QueryMeta,
	}
	for _, stage := range s.Stages {
		data.Stages = append(data.Stages, stage.ToData())
	}

	t, err := template.New("final").Funcs(funcMap).ParseFiles(finalTemplate)
	if err != nil {
		panic(err)
	}
	ts := t.Templates()
	if len(ts) != 1 {
		panic("More than 1 defined template")
	}
	err = ts[0].Execute(os.Stdout, data)
	if err != nil {
		panic(err)
	}
}

/*
func main() {
	flag.Parse()

	t, err := template.New("p4").Funcs(funcMap).ParseFiles(*templateFile)
	if err != nil {
		panic(err)
	}

	keySize := int(*keyWidth)
	valSize := int(*valWidth)

	lruSegments := make([]LRUSegment, *lruWays)
	for i := 0; i < int(*lruWays); i++ {
		lruSegments[i].KeyStart = i * keySize
		lruSegments[i].KeyEnd = (i+1)*keySize - 1
		lruSegments[i].ValStart = i * valSize
		lruSegments[i].ValEnd = (i+1)*valSize - 1
	}
	lruSegments[*lruWays-1].Evict = true
	updateFunc := *updateStr
	if len(updateFunc) == 0 {
		br := bufio.NewReader(os.Stdin)
		s, err := br.ReadString('\n')
		for err == nil {
			updateFunc += s
			s, err = br.ReadString('\n')
		}
		// Add the remainder
		updateFunc += s
	}
	updateFunc += "\n"
	templateLoc := strings.Split(*templateFile, "/")

	data := struct {
		// Dimensions of the LRU.
		KeySize, ValSize, LruWays, LruRows, KeyRowSize, ValRowSize int
		// The update operation, as P4-16 code. Fills in the body of func(val, hdrs, meta, standard_meta) { .. }, modifying val in place.
		KeyMask, ValueMask, Update, TemplateName string
		// Dimensions of each entry in the LRU.
		Segments []LRUSegment
	}{
		KeySize:      keySize,
		ValSize:      valSize,
		LruWays:      int(*lruWays),
		LruRows:      int(*lruRows),
		KeyRowSize:   int(*lruWays) * keySize,
		ValRowSize:   int(*lruWays) * valSize,
		KeyMask:      fmt.Sprintf("0x%x", (1<<*keyWidth - 1)),
		ValueMask:    fmt.Sprintf("0x%x", (1<<*valWidth - 1)),
		Update:       updateFunc,
		Segments:     lruSegments,
		TemplateName: templateLoc[len(templateLoc)-1],
	}

	var outF io.Writer
	if len(*outputFile) > 0 {
		outF, err = os.Create(*outputFile)
		if err != nil {
			panic(err)
		}
	} else {
		outF = os.Stdout
	}
	ts := t.Templates()
	if len(ts) != 1 {
		panic("More than 1 defined template")
	}
	err = ts[0].Execute(outF, data)
	if err != nil {
		panic(err)
	}
}*/
