path = 'C:/Users/svena/AndroidStudioProjects/aboveABC80/composeApp/src/commonMain/composeResources/files/23-054E7.bin'
with open(path, 'rb') as f:
    data = f.read()

keys = [
    "Keyboard", "Transmit", "Receive=", "Speed=", "Parity", "Print", "No Check",
    "7 Bits", "8 Bits", "Set-Up", "Display", "Auto Answerback", "Characters",
    "Concealed", "XOFF", "Disconnect", "Status", "Bell", "Even", "Odd",
    "Line", "Scroll", "Clear", "Directory", "80 Columns", "132 Columns",
    "Wrap", "Jump", "Smooth", "Light", "Dark", "Underline", "Style", "Lock",
    "Indicator", "Writable", "Unlock", "Locked", "7-bit", "8-bit", "Numeric",
    "Application", "New Line", "UPSS", "Supplemental", "Latin-1", "110", "150",
    "192", "128", "Mark", "Space", "Echo", "RS-232", "Modem", "60 ms",
    "Unlimited", "Limited", "Full Page", "Only", "All", "Terminator = FF",
    "Caps", "Shift", "Auto Repeat", "Keyclick", "Margin Bell", "Warning Bell",
    "Compose", "Delete", "Backspace", "Tab", "ENTER", "Ready"
]

def find_all(s):
    res = []
    # Search in EN (566-2600), FR (2602-4830), DE (4833-7000)
    for start, end in [(566, 2600), (2602, 4830), (4833, 7000)]:
        # Mask high bit for comparison
        part = data[start:end]
        s_bytes = s.encode('ascii')
        pos = -1
        for i in range(len(part) - len(s_bytes) + 1):
            match = True
            for j in range(len(s_bytes)):
                if (part[i+j] & 0x7F) != s_bytes[j]:
                    match = False
                    break
            if match:
                pos = start + i
                break
        res.append([pos, len(s)] if pos != -1 else [-1, 0])
    return res

print("val romMappings = mapOf(")
for k in keys:
    mappings = find_all(k)
    print(f'    "{k}" to arrayOf(intArrayOf({mappings[0][0]}, {mappings[0][1]}), intArrayOf({mappings[1][0]}, {mappings[1][1]}), intArrayOf({mappings[2][0]}, {mappings[2][1]})),')
print(")")
