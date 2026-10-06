/*****************************************************************************

    Texas Instruments SN76477 emulator

    SN76477 pin layout. There is a corresponding interface variable with the
    same name. The only exception is noise_clock which must be programmatically
    set.  The other pins have programmatic equivalents as well.
    The name of the function is SN76477_<pinname>_w.
    All capacitor functions can also specify a fixed voltage on the cap.
    The name of this function is SN76477_<pinname>_voltage_w

                      +-------------------+
          envelope_1  | 1      | |      28|  envelope_2
                      | 2 GND   -       27|  mixer_c
         noise_clock  | 3               26|  mixer_a
     noise_clock_res  | 4               25|  mixer_b
    noise_filter_res  | 5               24|  one_shot_res
    noise_filter_cap  | 6               23|  one_shot_cap
           decay_res  | 7               22|  vco
    attack_decay_cap  | 8               21|  slf_cap
              enable o| 9               20|  slf_res
          attack_res  |10               19|  pitch_voltage
       amplitude_res  |11               18|  vco_res
        feedback_res  |12               17|  vco_cap
                      |13 OUTPUT        16|  vco_voltage
                      |14 Vcc   +5V OUT 15|
                      +-------------------+

    All resistor values in Ohms
    All capacitor values in Farads
    Use RES_K, RES_M and CAP_U, CAP_N, CAP_P macros in rescap.h to convert
    magnitudes, eg. 220k = RES_K(220), 47nF = CAP_N(47)

 *****************************************************************************/

#ifndef SN76477_SOUND_H
#define SN76477_SOUND_H

#include "stdio.h"
#include <assert.h>
#include <memory.h>
#include <limits.h>
#include <math.h>        /* for pow() */

#define RES_K(res) ((double)(res)*1e3)
#define RES_M(res) ((double)(res)*1e6)
#define CAP_U(cap) ((double)(cap)*1e-6)
#define CAP_N(cap) ((double)(cap)*1e-9)
#define CAP_P(cap) ((double)(cap)*1e-12)

typedef unsigned int UINT32;

constexpr int SN76477_OVERSAMPLING = 16;

void sn76477_reset();
void sn76477_write_port(int port);
void sn76477_render(short *buffer, int length);
#endif
