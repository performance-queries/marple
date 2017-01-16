package main

import (
	//	"fmt"
	"io"
	"io/ioutil"
	"os"
	"strings"
)

type OpType int

const (
	Unknown OpType = iota
	Filter
	Project
	GroupBy
	Zip
)

const (
	stageDelimiter = "================================="
	codeDelimiter  = "--"
)

// Map from each name to the fully qualified metadata struct it belongs to.
// TODO: shouldn't be global.
var sourceMap = map[string]string{}

type Schema struct {
	CommonMeta []string
	QueryMeta  []string
	Stages     []*Stage
}

type Stage struct {
	Op        OpType
	Name      string
	Code      string
	KeyFields []string
	Registers []string
}

func generateSourceMap(s *Schema) {
	for _, f := range s.CommonMeta {
		parts := strings.Split(f, " ")
		name := strings.TrimRight(strings.TrimSpace(parts[1]), ";")
		sourceMap[name] = "meta.common_meta." + name
	}
	for _, q := range s.QueryMeta {
		parts := strings.Split(q, " ")
		name := strings.TrimRight(strings.TrimSpace(parts[1]), ";")
		sourceMap[name] = "meta.query_meta." + name
	}
	sourceMap["ingress_timestamp"] = "meta.intrinsic_meta.global_ingress_timestamp"
	sourceMap["enq_queue_size"] = "(bit<32>)meta.queueing_meta.enq_qdepth"
	sourceMap["deq_queue_size"] = "(bit<32>)meta.queueing_meta.enq_qdepth"
	sourceMap["queue_time"] = "meta.queueing_meta.deq_timedelta"
	sourceMap["srcip"] = "hdrs.ip.srcAddr"
	sourceMap["dstip"] = "hdrs.ip.dstAddr"
	sourceMap["srcport"] = "hdrs.tcp.isValid() ? (bit<32>)hdrs.tcp.srcPort : 0"
	sourceMap["dstport"] = "hdrs.tcp.isValid() ? (bit<32>)hdrs.tcp.dstPort : 0"
	sourceMap["proto"] = "(bit<32>)hdrs.ip.protocol"
	sourceMap["pktlen"] = "(bit<32>)hdrs.ip.totalLen"
	sourceMap["ingress_port"] = "(bit<32>)standard_meta.ingress_port"
	sourceMap["egress_port"] = "(bit<32>)standard_meta.egress_port"
}

func NewSchemaFromInput(input string) *Schema {
	var schemaFile io.Reader
	if len(input) == 0 {
		schemaFile = os.Stdin
	} else {
		s, err := os.Open(input)
		if err != nil {
			panic(err)
		}
		schemaFile = s
	}
	contents, err := ioutil.ReadAll(schemaFile)
	if err != nil {
		panic(err)
	}
	s := &Schema{}
	s.ParseFrom(string(contents))
	return s
}

func (s *Stage) ParseFrom(stageStr string) {
	if strings.TrimSpace(stageStr) == "" {
		return
	}
	parts := strings.Split(stageStr, codeDelimiter)
	if len(parts) != 2 {
		panic("Expected stage to have exactly one codeDelimiter")
	}
	parts[0] = strings.TrimSpace(parts[0])
	parts[1] = strings.TrimSpace(parts[1])
	params := strings.Split(parts[0], "\n")
	if len(params) > 5 {
		panic("Expected stage metadata to have at most 5 lines")
	}
	s.Code = parts[1]
	s.Name = params[0]
	if len(params) > 3 {
		kfs := strings.Split(strings.Trim(params[3], "[]"), ",")
		for _, kf := range kfs {
			if tr := strings.TrimSpace(kf); len(tr) > 0 {
				s.KeyFields = append(s.KeyFields, tr)
			}
		}
		rs := strings.Split(strings.Trim(params[4], "[]"), ",")
		for _, r := range rs {
			if tr := strings.TrimSpace(r); len(tr) > 0 {
				s.Registers = append(s.Registers, strings.TrimSpace(r))
			}
		}
	}
	switch params[2] {
	case "GROUPBY":
		s.Op = GroupBy
		// There is only an action string and control string.
	case "FILTER":
		s.Op = Filter
	case "PROJECT":
		s.Op = Project
	case "ZIP":
		s.Op = Zip
	}
}

func (s *Schema) ParseFrom(schema string) {
	parts := strings.Split(schema, stageDelimiter)
	if len(parts) < 5 {
		panic("Expected at least 4 parts in the compiler output")
	}
	// parts[0] is an empty line
	s.CommonMeta = strings.Split(strings.TrimSpace(parts[1]), "\n")
	s.QueryMeta = strings.Split(strings.TrimSpace(parts[2]), "\n")
	for i := 4; i < len(parts); i++ {
		if len(strings.TrimSpace(parts[i])) > 0 {
			stage := &Stage{}
			stage.ParseFrom(parts[i])
			s.Stages = append(s.Stages, stage)
		}
	}
	generateSourceMap(s)
}
