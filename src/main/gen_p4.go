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
	groupByActionsTemplate = "../p4/groupby_actions_hash.tmpl"
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
	KeyFields              []string
	ValueFields            []string
	KeySourceFields        []string
}

type TemplateData struct {
	TemplateName string
	SwitchId     int
	CommonMeta   []string
	QueryMeta    []string
	Stages       []*StageData
}

type StageData struct {
	Structs []string
	Actions string
	Control string
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
		KeyFields:              s.KeyFields,
		ValueFields:            s.Registers,
		KeySourceFields:        s.KeySourceFields,
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

// Prefix every line after the first in 'body' with 'space'.
// Intended to be used with tabs to pretty print the code.
func indentBy(body, space string) string {
	bodyParts := strings.Split(body, "\n")
	for i, p := range bodyParts {
		if i > 0 {
			bodyParts[i] = space + p
		}
	}
	return strings.Join(bodyParts, "\n")
}

func genBasicAction(name, code string) string {
	return fmt.Sprintf("\taction %s() {\n\t\t%s\n\t}\n", name, indentBy(code, "\t\t"))
}

func (s *Stage) ToData() *StageData {
	sd := &StageData{}
	keyName, valName := "Key_"+s.Name, "Value_"+s.Name
	evKey, evVal := "evictedKey_"+s.Name, "evictedValue_"+s.Name
	switch s.Op {
	case Project, Filter, Zip:
		updateFn := "update_" + s.Name
		sd.Actions = genBasicAction(updateFn, s.Code)
		sd.Control = "\t\t" + updateFn + "();"
	case GroupBy:
		sd.Structs = genStructsForGroupBy(s)
		sd.Actions = genGroupByAction(s)
		cntrl := fmt.Sprintf("\t\t%s %s;\n%s %s;\n%s(%s, %s);", keyName, evKey, valName, evVal, "groupby_"+s.Name, evKey, evVal)
		sd.Control = indentBy(cntrl, "\t\t")
	default:
		panic("Stage type " + s.Name + " not supported")
	}
	return sd
}

func main() {
	flag.Parse()
	s := NewSchemaFromInput(*compilerOut)
	data := &TemplateData{
		TemplateName: "final_p4.tmpl",
		SwitchId:     1,
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
