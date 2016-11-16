// Runs the CLI and queries all the registers.
// Sample usage:
//  ../behavioral-model/tools/runtime_CLI.py < CLI_commands.txt | go run read_registers.go --sub_key_size=8
// CLI_commands.txt contains a list of register_read commands.
// 
// TODO: put CLI_commands.txt into this script.

package main

import (
	"os/exec"
	"bufio"
	"os"
	"fmt"
	"time"
	"strings"
	"strconv"
	"bytes"
	"regexp"
	"flag"
)

const cliExec = "/home/vikramn/p4c/behavioral-model/tools/runtime_CLI.py"
const subPartSep = "."
var lineMatcher = regexp.MustCompile(`([A-Za-z]+)\[([0-9]+)\]=(.*)`)
// Registers report a 64-bit number, which is an entire LRU row. This is parsed into 'keySize'-sized chunks
// to obtain each key.
var keySize = flag.Uint("key-size", 16, "Size of each key, in bits")
// Likewise for values. 
var valSize = flag.Uint("value-size", 16, "Size of each value, in bits")
// When pretty printing, the key may not be meaningful as a `keySize'-bit number and may have meaningful subdivisions.
// For example, it may be an IP address, where each byte is treated as a separate number.
// In this case, set --sub-key-size to be the number of bits in each 'subkey', i.e. 8 in this case.
var subKeySize = flag.Uint("sub-key-size", 0, "Size of the key's subparts (default 0 if there are none)")

func parseValue(line string) (uint64, error) {
	p := strings.Split(strings.TrimSpace(line), "=")
	if len(p) < 2 {
		return 0, fmt.Errorf("No value provided.")
	}
	return strconv.ParseUint(strings.TrimSpace(p[1]), 10, 64)
}

func parse(line string, k, v map[int]uint64) {
	p := lineMatcher.FindStringSubmatch(strings.TrimSpace(line))
	if len(p) < 4 {
		return
	}
	u, err := strconv.ParseUint(strings.TrimSpace(p[3]), 10, 64)
	if err != nil {
		panic(err)
	}
	ix, err := strconv.Atoi(strings.TrimSpace(p[2]))
	if err != nil {
		panic(err)
	}
	if strings.TrimSpace(p[1]) == "kvKeys" {
		k[ix] = u
	} else if strings.TrimSpace(p[1]) == "kvValues" {
		v[ix] = u
	}
}

func splitIntoParts(u uint64, sz uint) []uint64 {
	parts := []uint64{}
	mask := uint64(1 << sz - 1)
	for u != 0 {
		parts = append(parts, u & mask)
		u = u >> sz
	}
	return parts
}

func formatBySubpart(u uint64) string {
	if *subKeySize <= 0 {
		return fmt.Sprintf("%d", u)
	}
	subparts := splitIntoParts(u, *subKeySize)
	subpartsStr := make([]string, len(subparts))
	for i, s := range subparts {
		subpartsStr[len(subparts)-1-i] = fmt.Sprintf("%d", s)
	}
	return strings.Join(subpartsStr, subPartSep)
}

func main() {
	flag.Parse()
	r := bufio.NewReader(os.Stdin)
	keys := map[int]uint64{}
	vals := map[int]uint64{}
	for {
		s, err := r.ReadString('\n')
		if err != nil {
			break
		}
		parse(s, keys, vals)
	}
	for k, ix := range keys {
		if ix == 0 {
			continue
		}
		keyParts := splitIntoParts(ix, *keySize)
		valParts := splitIntoParts(vals[k], *valSize)
		for i, kp := range keyParts {
			fmt.Printf("%s:\t%d\n", formatBySubpart(kp), valParts[i])
		}
	}
}

// Unused
func readKeysAndValues() map[uint64]uint64 {
	cmd := exec.Command(cliExec)
	inW, err := cmd.StdinPipe()
	if err != nil {
		panic(err)
	}
	in := bufio.NewWriter(inW)
	var out bytes.Buffer
	cmd.Stdout = &out //os.Stdout
	// Start non-blocking command.
	fmt.Println("Starting CLI...")
	if err := cmd.Start(); err != nil {
		panic(err)
	}
	time.Sleep(1 * time.Second)
	fmt.Println("Reading keys and values...")
	kvStore := map[uint64]uint64{}
	// Input queries and read output.
	for i := 0; i < 10; i++ {
		_, err = in.WriteString(fmt.Sprintf("register_read kvKeys %d\n", i))
		if err != nil {
			panic(err)
		}
		in.Flush()
		time.Sleep(50 * time.Millisecond)
		if _, err = in.WriteString(fmt.Sprintf("register_read kvValues %d\n", i)); err != nil {
			panic(err)
		}
		in.Flush()
		time.Sleep(50 * time.Millisecond)
	}
	cmd.Process.Kill()
	return kvStore
}

