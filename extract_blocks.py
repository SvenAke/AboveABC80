import os

path = r'C:\Users\svena\AndroidStudioProjects\aboveZXSpectrum\composeApp\src\python\tzxtools\tzxtools\KnightLLore.tzx'
with open(path, 'rb') as f:
    data = f.read()

pos = 10
block_idx = 0
while pos < len(data):
    block_id = data[pos]
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
    else:
        print(f"Unknown block ID {hex(block_id)} at {pos}")
        break
    
    if block_idx in [68, 135, 202]:
        print(f"Block {block_idx}: ID {hex(block_id)} at {pos}, Length {length}")
        if block_id == 0x14:
            data_len = data[pos+8] | (data[pos+9] << 8) | (data[pos+10] << 16)
            payload = data[pos+11:pos+11+data_len]
            print(f"  Data Len: {data_len}, Data: {payload[:32].hex()}")
            if block_idx == 135:
                with open('block135.bin', 'wb') as bf: bf.write(payload)
        elif block_id == 0x11:
            data_len = data[pos+0x10] | (data[pos+0x11] << 8) | (data[pos+0x12] << 16)
            payload = data[pos+0x13:pos+0x13+data_len]
            print(f"  Turbo Data Len: {data_len}")

    pos += 1 + length
    block_idx += 1
