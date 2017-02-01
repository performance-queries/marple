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
	finalTemplate          = "final_p4.tmpl"
	groupByActionsTemplate = "groupby_actions_hash.tmpl"
)

var outputFile = flag.String("output", "", "Location of the output (default = stdout).")
var lruRows = flag.Uint("lru-rows", 1024, "Number of rows in the LRU")
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
	KeyRegisters           []string
	ValueRegisters         []string
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

func mapWith(s []string, f func(string) string) []string {
	n := []string{}
	for _, el := range s {
		n = append(n, f(el))
	}
	return n
}

func genGroupByAction(s *Stage) string {
	zeroFunc := func(s string) string {
		return "0"
	}
	regFunc := func(prefix string) func(string) string {
		return func(str string) string {
			return fmt.Sprintf("reg%s_%s_%s", prefix, s.Name, str)
		}
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
		RowFieldNameList:       []string{"first", "second", "third", "fourth"},
		UpdateCode:             indentBy(strings.TrimSpace(s.Code), "\t\t"),
		DefaultValueName:       "defaultVal_" + s.Name,
		DefaultValueDefinition: "{" + strings.Join(mapWith(s.Registers, zeroFunc), ",") + "}",
		DefaultKeyName:         "defaultKey_" + s.Name,
		DefaultKeyDefinition:   "{" + strings.Join(mapWith(s.KeyFields, zeroFunc), ",") + "}",
		KeyFields:              s.KeyFields,
		ValueFields:            s.Registers,
		KeySourceFields:        s.KeySourceFields,
		KeyRegisters:           mapWith(s.KeyFields, regFunc("K")),
		ValueRegisters:         mapWith(s.Registers, regFunc("V")),
	}
	t, err := template.New("groupby" + s.Name).ParseFiles(groupByActionsTemplate)
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
	s := NewSchemaFromInput()
	data := &TemplateData{
		TemplateName: "final_p4.tmpl",
		SwitchId:     1,
		CommonMeta:   s.CommonMeta,
		QueryMeta:    s.QueryMeta,
	}
	for _, stage := range s.Stages {
		data.Stages = append(data.Stages, stage.ToData())
	}

	t, err := template.New("final").ParseFiles(finalTemplate)
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
