loader = bytes.fromhex('f9 d7 88 ff ff e1 e5 88 95 2e ae 11 2f ae 0c 21 e1 f9 0a ff ef 00')

# SpeedLock 1 decryption is often:
# 1. Take a byte
# 2. XOR with some seed
# 3. Rotate the seed
# 4. Repeat

def decrypt_sl1(data, seed):
    out = bytearray()
    for b in data:
        dec = (b ^ seed) & 0xFF
        out.append(dec)
        # update seed - this is a guess, SpeedLock has many variants
        seed = (seed + 1) & 0xFF 
    return out

for s in range(256):
    d = decrypt_sl1(loader, s)
    if d[0] in [0x21, 0x11, 0x01]:
        print(f"Seed {hex(s)}: {d.hex()}")

def decrypt_sl1_v2(data, seed):
    out = bytearray()
    for b in data:
        dec = (b ^ seed) & 0xFF
        out.append(dec)
        seed = (seed - 1) & 0xFF 
    return out

for s in range(256):
    d = decrypt_sl1_v2(loader, s)
    if d[0] in [0x21, 0x11, 0x01]:
        print(f"Seed V2 {hex(s)}: {d.hex()}")
