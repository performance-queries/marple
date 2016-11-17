// Autogenerates a P4 file from a given template, filling in the LRU implementation as specified by that
// template. Typical uses:
//  go run gen_p4.go -template=$P4TEMPLATE -output=$P4OUT < my_update_function.txt
//  go run gen_p4.go -template=$P4TEMPLATE -output=$P4OUT -update="val = val + 1;"
//
// Note that the provided function and template are NOT type-checked or compile-time error-free.
// The output P4 file must be checked with a compiler.
package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"os"
	"strings"
	"text/template"
)

var templateFile = flag.String("template", "p4_template.txt", "Location of the p4-16 template")
var outputFile = flag.String("output", "", "Location of the output (default = stdout).")
var keyWidth = flag.Uint("key-width", 16, "Width of each key, in bits")
var valWidth = flag.Uint("value-width", 16, "Width of each value, in bits")
var lruRows = flag.Uint("lru-rows", 1024, "Number of rows in the LRU")
var lruWays = flag.Uint("lru-ways", 4, "Number of ways in the LRU. Total LRU size = lru-rows * lru-ways")
var updateStr = flag.String("update", "", "The update function in string form. If not specified, it should be entered via stdin")

type LRUSegment struct {
	// Inclusive start and end indices within the bitvector representing the LRU row in the keys table.
	// KeyStart < KeyEnd, since unlike in P4, we're counting from the LSB.
	KeyStart, KeyEnd int
	// Likewise for values
	ValStart, ValEnd int
	// Whether we should evict this key if no match (true on the least-recent entry)
	Evict bool
}

func main() {
	flag.Parse()

	funcMap := template.FuncMap{
		"add": func(x, y int) int { return x + y },
	}
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
}
