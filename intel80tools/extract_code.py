import re
import os

def fix_hex(val):
    if val.endswith('H'):
        v = val[:-1]
        if v and v[0] in "ABCDEF":
            return "0" + val
    return val

def fix_name(name):
    if name == '$':
        return '$'
    if name == '@STACK$ORIGIN':
        return 'STACKORG'
        
    res = name.replace('$', '').replace('@', 'Q').replace('?', 'P')
    if name.startswith('@') and len(name) > 1 and name[1].isdigit():
        res = name.replace('$', '')
    
    return res

def fix_statement(stmt):
    def repl(m):
        s = m.group(0)
        if s == '$':
            return '$'
        return fix_name(s)
    
    return re.sub(r'[\?\@\w\$]+', repl, stmt)

def extract(infile, outfile):
    if not os.path.exists(infile):
        return

    with open(infile, 'r') as f:
        lines = f.readlines()
    
    asm_code = []
    equ_defs = []
    labels_in_code = set()
    
    label_re = re.compile(r'^\s*([0-9A-F]{4})\s+(\?[\w\$\?]+|@\d+):$')
    code_re = re.compile(r'^\s*([0-9A-F]{4})\s+([0-9A-F]{2,})\s+(.*)$')
    equ_re = re.compile(r'^\s*(\?[\w\$\?]+)\s+EQU\s+([0-9A-F]+H)$')
    
    special_mappings = {
        "PMON1": "0005H",
        "PMON2": "0005H",
        "PBOOT": "0000H",
    }

    # First pass: find labels
    for line in lines:
        line = line.strip()
        m = label_re.match(line)
        if m:
            labels_in_code.add(fix_name(m.group(2)))

    unknown_symbols = set()
    in_symbol_table = False
    seen_symbols = set()

    # Manual definition for STACKORG
    equ_defs.append(f"{'STACKORG':<32} EQU     0FFFFH\n")
    seen_symbols.add("STACKORG")

    for line in lines:
        line = line.strip()
        
        if "; SYMBOL TABLE" in line:
            in_symbol_table = True
            continue
        
        if "END OF PL/M-80 COMPILATION" in line:
            in_symbol_table = False
            continue

        if in_symbol_table:
            m = equ_re.match(line)
            if m:
                raw_name = m.group(1)
                name = fix_name(raw_name)
                if name in labels_in_code or name in seen_symbols:
                    continue
                
                val = fix_hex(m.group(2))
                if name in special_mappings and (val == "0H" or val == "0000H"):
                    val = special_mappings[name]
                
                equ_defs.append(f"{name:<32} SET     {val}\n")
                seen_symbols.add(name)
            continue

        m = code_re.match(line)
        if m:
            stmt = m.group(3).strip()
            if stmt:
                # Track symbols
                syms = re.findall(r'[\?@][\w\$\?]+', stmt)
                for s in syms:
                    unknown_symbols.add(fix_name(s))
                
                stmt = fix_statement(stmt)
                asm_code.append(f"        {stmt}\n")
        else:
            m = label_re.match(line)
            if m:
                asm_code.append(f"{fix_name(m.group(2))}:\n")
    
    for s in sorted(list(unknown_symbols)):
        if s not in seen_symbols and s not in labels_in_code:
            equ_defs.append(f"{s:<32} SET     0H ; Undefined\n")

    with open(outfile, 'wb') as f:
        f.write(b"        ORG 0100H\r\n")
        for line in equ_defs:
            f.write(line.replace('\n', '\r\n').encode('ascii', errors='ignore'))
        f.write(b"\r\n; --- CODE ---\r\n\r\n")
        for line in asm_code:
            f.write(line.replace('\n', '\r\n').encode('ascii', errors='ignore'))
        f.write(b"        END\r\n\x1a")

if __name__ == "__main__":
    extract('ed.lst', 'edgen.asm')
