package main

import (
    "bytes"
    "flag"
    "fmt"
    "os"
    "strings"
    "text/template"
)

const (
    finalTemplate          = "final_p4.tmpl"
    groupByActionsTemplate = "groupby_actions_hash.tmpl"
)

var tableSize = flag.Uint("table-size", 1024, "Number of rows in the table")

type GroupByActionData struct {
    StageName              string
    TableSize              int
    UpdateCode             string // Code for the aggregation function
    DefaultValueDefinition string // {0, 0, ... #(pieces of state stored in the aggregation function)}
    DefaultKeyDefinition   string // {0, 0, ... #(fields that are grouped by in the aggregation function)}
    KeyFields              []string // names of the groupby fields
    ValueFields            []string // names of the state variables
    KeySourceFields        []string // packet headers / metadata fields for the key
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

// Making up for go's lack of a map function
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
    data := &GroupByActionData{
        StageName: 		s.Name,
	TableSize:              int(*tableSize),
        UpdateCode:             indentBy(strings.TrimSpace(s.Code), "\t\t"),
        DefaultValueDefinition: "{" + strings.Join(mapWith(s.Registers, zeroFunc), ",") + "}",
        DefaultKeyDefinition:   "{" + strings.Join(mapWith(s.KeyFields, zeroFunc), ",") + "}",
        KeyFields:              s.KeyFields,
        ValueFields:            s.Registers,
        KeySourceFields:        s.KeySourceFields,
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
