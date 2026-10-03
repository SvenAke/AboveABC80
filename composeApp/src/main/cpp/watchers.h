#ifndef WATCHERS_H
#define WATCHERS_H

#include <stdbool.h>
#include "z80.h"

#ifdef __cplusplus
extern "C" {
#endif

bool check_and_run_read_watcher(int address);
void check_and_run_write_watcher(int address, libspectrum_byte new_value);

#ifdef __cplusplus
}
#endif

#endif // WATCHERS_H
