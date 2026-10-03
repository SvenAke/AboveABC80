import struct

path = r'C:\Users\svena\AndroidStudioProjects\aboveZXSpectrum\composeApp\src\python\tzxtools\tzxtools\KnightLLore.tzx'
with open(path, 'rb') as f:
    data = f.read()

pos = 10
block_idx = 0
while pos < len(data):
    block_id = data[pos]
    start_pos = pos
    
    length = 0
    if block_id == 0x10:
        length = 4 + struct.unpack('<H', data[pos+3:pos+5])[0]
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
    elif block_id == 0x2B:
        length = 5
    elif block_id == 0x30:
        length = 1 + data[pos+1]
    elif block_id == 0x32:
        length = 2 + struct.unpack('<H', data[pos+1:pos+3])[0]
    
    if block_idx in range(65, 70):
        print(f"Block {block_idx}: ID {hex(block_id)} at {pos}, Length {length}")
        if block_id == 0x14:
            header = data[pos+1:pos+11]
            zero = header[0] | (header[1] << 8)
            one = header[2] | (header[3] << 8)
            last_bits = header[4]
            pause = header[5] | (header[6] << 8)
            dlen = header[7] | (header[8] << 8) | (header[9] << 16)
            print(f"  Pure Data: Zero={zero}, One={one}, LastBits={last_bits}, Pause={pause}, Len={dlen}")
            payload = data[pos+11:pos+11+dlen]
            print(f"  Payload: {payload.hex()}")
        elif block_id == 0x13:
            count = data[pos+1]
            pulses = [data[pos+2+i*2] | (data[pos+3+i*2] << 8) for i in range(count)]
            print(f"  Pulse Sequence: {pulses}")
        elif block_id == 0x12:
            plen = data[pos+1] | (data[pos+2] << 8)
            pcount = data[pos+3] | (data[pos+4] << 8)
            print(f"  Pure Tone: Len={plen}, Count={pcount}")

    pos += 1 + length
    block_idx += 1
    if pos >= len(data): break
