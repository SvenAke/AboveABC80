#ifndef PATCH_H
#define PATCH_H

#include <stdbool.h>
#include "z80.h"

#ifdef __cplusplus
extern "C" {
#endif

bool get_patched_byte(libspectrum_word address, libspectrum_byte *value);

#ifdef __cplusplus
}
#endif

#endif // PATCH_H
