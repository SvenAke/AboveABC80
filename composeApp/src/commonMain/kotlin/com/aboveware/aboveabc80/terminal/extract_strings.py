import re

def extract(path, start, end):
    with open(path, 'rb') as f:
        data = f.read()[start:end]
    
    strings = []
    current = []
    for b in data:
        # Check if 7 bits are ASCII
        c_code = b & 0x7F
        if 32 <= c_code <= 126:
            current.append(chr(c_code))
            # Some formats use the high bit to indicate the end of a string
            # or some other attribute. But many use it as a separator.
            # However, looking at the data, it seems strings are simply 
            # sequences of printable ASCII (when masked).
        else:
            if len(current) >= 1:
                strings.append("".join(current))
            current = []
    if len(current) >= 1:
        strings.append("".join(current))
    return strings

path = 'C:/Users/svena/AndroidStudioProjects/aboveABC80/composeApp/src/commonMain/composeResources/files/23-054E7.bin'
en = extract(path, 566, 2600)
fr = extract(path, 2602, 4830)
de = extract(path, 4833, 7000)

for i in range(max(len(en), len(fr), len(de))):
    e = en[i] if i < len(en) else ""
    f = fr[i] if i < len(fr) else ""
    d = de[i] if i < len(de) else ""
    print(f"{i}|{e}|{f}|{d}")
