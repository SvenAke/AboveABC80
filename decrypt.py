import os

def decrypt_block135():
    with open('block135.bin', 'rb') as f:
        data = bytearray(f.read())
    
    # SpeedLock 1 decryption of the screen/payload block
    # Based on the 22-byte loader: F9 D7 88 FF FF E1 E5 88 95 2E AE 11 2F AE 0C 21 E1 F9 0A FF EF 00
    # Wait, the 22-byte block is at 0x5B00.
    # Disassembly of F9D788FFFFE1E588952EAE112FAE0C21E1F90AFFEF00
    # F9: JP (HL) or something? No, F9 is LD SP,HL
    # D7: RST 10H
    # 88: ADC A, B
    # This doesn't look like code.
    
    # Let's try a different approach. SpeedLock 1 often has a decryption loop.
    # Block 135 starts with a lot of FF bytes. This is likely the screen (6912 bytes).
    # Standard screen attributes start at offset 6144.
    
    print("Block 135 first 64 bytes:", data[:64].hex())
    print("Block 135 length:", len(data))

decrypt_block135()
