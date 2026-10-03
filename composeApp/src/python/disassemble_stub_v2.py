
import struct

def reverse_bits(b):
    return int('{:08b}'.format(b)[::-1], 2)

stub = [
    0xf9, 0xd7, 0x88, 0xff, 0xff, 0xe1, 0xe5, 0x88,
    0x95, 0x2e, 0xae, 0x11, 0x2f, 0xae, 0x0c, 0x21,
    0xe1, 0xf9, 0x0a, 0xff, 0xef, 0x00
]

# Method 1 was: (byte XOR 0x7D) -> Reverse Bits
# 21 55 af 41 41 39 19 af 17 ca cb 36 4a cb 8e 3a 39 21 ee 41 49 be
# LD HL, $AF55
# LD B, C ? No...

# Let's try Method 4: Byte XOR 0xFF (NOT)
print("\nMethod 4: Byte XOR 0xFF")
d4 = [b ^ 0xff for b in stub]
print("Hex:", " ".join(["{:02x}".format(x) for x in d4]))

# Method 5: Byte XOR 0xAA (alternating bits)
print("\nMethod 5: Byte XOR 0xAA")
d5 = [b ^ 0xaa for b in stub]
print("Hex:", " ".join(["{:02x}".format(x) for x in d5]))

# Method 6: RRA (Rotate Right Accumulator) - bitwise
def rra(b):
    return ((b & 1) << 7) | (b >> 1)

print("\nMethod 6: RRA (once)")
d6 = [rra(b) for b in stub]
print("Hex:", " ".join(["{:02x}".format(x) for x in d6]))

# Method 7: RLA (Rotate Left Accumulator)
def rla(b):
    return ((b << 1) & 0xFF) | (b >> 7)

print("\nMethod 7: RLA (once)")
d7 = [rla(b) for b in stub]
print("Hex:", " ".join(["{:02x}".format(x) for x in d7]))
