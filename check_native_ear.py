# Look for any EAR bit handling in the native code.
# Port 0xFE read/write usually handles it.

import os

files = [
    'composeApp/src/main/cpp/native-lib.cpp',
    'composeApp/src/main/cpp/z80main.cpp',
    'composeApp/src/main/cpp/z80.cpp',
    'composeApp/src/main/cpp/z80_ops.cpp'
]

for f in files:
    with open(f, 'r') as fd:
        content = fd.read()
        if '0xFE' in content or 'port' in content.lower():
            print(f"Found potential port handling in {f}")
            # Search for bit 6 of port 0xFE (EAR)
            if '6' in content or '0x40' in content or '64' in content:
                print(f"  Possible EAR bit (bit 6) mentioned in {f}")
