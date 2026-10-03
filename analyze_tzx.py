import os

path = r'C:\Users\svena\AndroidStudioProjects\aboveCPM\composeApp\src\python\tzxtools\tzxtools\KnightLLore.tzx'
with open(path, 'rb') as f:
    data = f.read()

pos = 10
block_idx = 0
while pos < len(data):
    block_id = data[pos]
    print(f"Block {block_idx}: ID {hex(block_id)} at {pos}", end="")
    
    length = 0
    if block_id == 0x10:
        length = 4 + (data[pos+3] | (data[pos+4] << 8))
    elif block_id == 0x11:
        length = 0x12 + (data[pos+0x10] | (data[pos+0x11] << 8) | (data[pos+0x12] << 16))
    elif block_id == 0x12:
        length = 4
    elif block_id == 0x13:
        length = 1 + data[pos+1]*2
    elif block_id == 0x14:
        length = 10 + (data[pos+8] | (data[pos+9] << 8) | (data[pos+10] << 16))
    elif block_id == 0x20:
        length = 2
    elif block_id == 0x21:
        length = 1 + data[pos+1]
    elif block_id == 0x22:
        length = 0
    elif block_id == 0x23:
        length = 2
    elif block_id == 0x24:
        length = 2
    elif block_id == 0x25:
        length = 0
    elif block_id == 0x30:
        length = 1 + data[pos+1]
    elif block_id == 0x32:
        length = 2 + (data[pos+1] | (data[pos+2] << 8))
    
    if block_id == 0x10:
        d = data[pos+5:pos+5+(data[pos+3] | (data[pos+4] << 8))]
        if len(d) == 19 and d[0] == 0:
            print(f" (Standard Header) '{d[2:12].decode('ascii', errors='ignore').strip()}'")
        else:
            print(f" (Standard Data) Len {len(d)}")
    elif block_id == 0x21:
        print(f" (Group Start) '{data[pos+2:pos+2+data[pos+1]].decode('ascii', errors='ignore')}'")
    else:
        print(f" (ID {hex(block_id)})")

    pos += 1 + length
    block_idx += 1
    if block_idx > 100: break
