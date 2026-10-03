payload = bytes.fromhex('f9d788ffffe1e588952eae112fae0c21e1f90affef00')

def reverse_bits(b):
    return int('{:08b}'.format(b)[::-1], 2)

# XOR 0x7d + Reverse: 21 55 af 41 41 39 19 af 17 ca cb 36 4a cb 8e 3a 39 21 ee 41 49 be
# 21 55 AF    LD HL, AF55
# 41          LD B, C
# 41          LD B, C
# 39          ADD HL, SP
# 19          ADD HL, DE
# AF          XOR A
# 17          RLA
# CA CB 36    JP Z, 36CB
# 4A          LD C, D
# CB 8E       RES 1, (HL)
# 3A 39 21    LD A, (2139)
# EE 41       XOR 41
# 49          LD C, C
# BE          CP (HL)

# Still not quite looking like a clean loader.
# Wait, Block 68 payload has 22 bytes. 
# It is preceded by a Pulse Sequence (Block 67) and a Pure Tone (Block 66).
# Block 66: Pure Tone: Len=2254, Count=244
# Block 67: Pulse Sequence: [3150, 3150, 1130, 1130, 1130, 1130, 1130, 1130, 1130, 1130, 565, 565, 1130, 1130]

# This is a very specific SpeedLock 1 pilot/sync pattern.
# SpeedLock 1 often has:
# - Pilot (2254T)
# - Sync (two pulses of 3150T each? No, usually shorter)
# - Data pulses (565T for 0, 1130T for 1)

# Let's look at the Pulse Sequence (Block 67) again.
# 3150, 3150, 1130, 1130, 1130, 1130, 1130, 1130, 1130, 1130, 565, 565, 1130, 1130
# 1130 is '1', 565 is '0'.
# 1130*8 = 0xFF
# 565*2 = 0x00 ? No.
# If 1130 is 1 and 565 is 0:
# 1,1,1,1, 1,1,1,1, 0,0, 1,1
# This looks like 0xFF (8 ones) followed by 0011... 
# This might be a lead-in for the 22-byte block.

# Wait, if the 22-byte block is "Pure Data", it means it HAS NO PILOT/SYNC of its own.
# The TZX author used Block 66 and 67 to manualy create the Pilot and Sync.
# Block 68 then provides the data bits.

# Knight Lore SpeedLock 1 loader:
# The 22-byte block is often the "key" or "seed" for the rest of the loader.
# But it also contains the actual decryption code sometimes.
