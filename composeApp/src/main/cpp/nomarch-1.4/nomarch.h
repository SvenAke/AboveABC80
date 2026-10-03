//
// Created by svena on 2026-09-12.
//

#ifndef ABOVEABC80_NOMARCH_H
#define ABOVEABC80_NOMARCH_H
#include "readrle.h"
#include "readhuff.h"
#include "readlzw.h"
#ifdef _WIN32
#  include <io.h>
#  include <process.h>
#else

#  include <unistd.h>

#endif
#endif //ABOVEABC80_NOMARCH_H
