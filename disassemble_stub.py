
def reverse_bits(b):
    return int('{:08b}'.format(b)[::-1], 2)

def decrypt(data, key=0x7d):
    decrypted = []
    for b in data:
        # Try: (b XOR key) then reverse
        d = reverse_bits(b ^ key)
        decrypted.append(d)
    return decrypted

stub = [
    0xf9, 0xd7, 0x88, 0xff, 0xff, 0xe1, 0xe5, 0x88,
    0x95, 0x2e, 0xae, 0x11, 0x2f, 0xae, 0x0c, 0x21,
    0xe1, 0xf9, 0x0a, 0xff, 0xef, 0x00
]

print("Method 1: (byte XOR 0x7D) -> Reverse Bits")
d1 = decrypt(stub)
print("Hex:", " ".join(["{:02x}".format(x) for x in d1]))

def decrypt2(data, key=0x7d):
    decrypted = []
    for b in data:
        # Try: reverse then XOR key
        d = reverse_bits(b) ^ key
        decrypted.append(d)
    return decrypted

print("\nMethod 2: Reverse Bits -> (byte XOR 0x7D)")
d2 = decrypt2(stub)
print("Hex:", " ".join(["{:02x}".format(x) for x in d2]))

# Speedlock 1 often uses simple XOR with 0x00 or something else if not 0x7D
# Let's try just XOR 0x7D without bit reversal
print("\nMethod 3: Byte XOR 0x7D")
d3 = [b ^ 0x7d for b in stub]
print("Hex:", " ".join(["{:02x}".format(x) for x in d3]))
