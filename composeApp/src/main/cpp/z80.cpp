#include "z80.h"
#include "z80_internals.h"
#include "z80_macros.h"
#include "native-lib.h"
#include <cstdio>
#include <cstdarg>

libspectrum_dword tStates;
libspectrum_dword event_next_event;
libspectrum_dword g_cpu_frequency;
libspectrum_dword g_t_states_per_frame;
/* Byte placed on the data bus by the interrupting device in IM 2 */
libspectrum_byte g_im2_vector = 0xff;

/* Whether a half carry occurred or not can be determined by looking at
   the 3rd bit of the two arguments and the result; these are hashed
   into this table in the form r12, where r is the 3rd bit of the
   result, 1 is the 3rd bit of the 1st argument and 2 is the
   third bit of the 2nd argument; the tables differ for add and subtract
   operations */
const libspectrum_byte halfcarry_add_table[] = {0, FLAG_H, FLAG_H, FLAG_H,
                                                0, 0, 0, FLAG_H};
const libspectrum_byte halfcarry_sub_table[] = {0, 0, FLAG_H, 0,
                                                FLAG_H, 0, FLAG_H, FLAG_H};

/* Similarly, overflow can be determined by looking at the 7th bits; again
   the hash into this table is r12 */
const libspectrum_byte overflow_add_table[] = {0, 0, 0, FLAG_V,
                                               FLAG_V, 0, 0, 0};
const libspectrum_byte overflow_sub_table[] = {0, FLAG_V, 0, 0,
                                               0, 0, FLAG_V, 0};

/* Some more tables; initialised in z80_init_tables() */

libspectrum_byte sz53_table[0x100];   /* The S, Z, 5 and 3 bits of the index */
libspectrum_byte parity_table[0x100]; /* The parity of the lookup value */
libspectrum_byte sz53p_table[0x100];  /* OR the above two tables together */

/* This is what everything acts on! */
processor z80;
libspectrum_byte current_opcode;
libspectrum_byte current_opcode2;

int z80_interrupt_event;
int z80_nmi_event;
int z80_nmos_iff2_event;

static void z80_init_tables(void);

static void z80_nmi(libspectrum_dword ts, int type, void *user_data);

extern libspectrum_byte last_Q;

void scf() {
    SCF();
}

void ccf() {
    CCF();
}

/* Set up the z80 emulation */
int z80_init(int cpu_frequency) {
    g_cpu_frequency = cpu_frequency;
    // Real Spectrum 48K timing: 69888 T-states per frame
    g_t_states_per_frame = 69888;
    logd("CPU frequency: %d", cpu_frequency);
    logd("T-states per frame: %d (Fixed for 48K accuracy)", g_t_states_per_frame);

    logd("Z80 struct offsets: AF: %d, I: %d, R: %d, PC: %d, size: %d",
         (int) ((char *) &z80.af - (char *) &z80),
         (int) ((char *) &z80.i - (char *) &z80),
         (int) ((char *) &z80.r - (char *) &z80),
         (int) ((char *) &z80.pc - (char *) &z80),
         (int) sizeof(processor));

    event_next_event = g_t_states_per_frame;
    z80_init_tables();

    // z80_interrupt_event = event_register(z80_interrupt_event_fn, "Retriggered
    // interrupt"); z80_nmi_event = event_register(z80_nmi, "Non-maskable
    // interrupt"); z80_nmos_iff2_event = event_register(NULL, "IFF2 update dummy
    // event");
    z80_debugger_variables_init();
    return 0;
}

/* Initialise the tables used to set flags */
static void z80_init_tables(void) {
    int i, j, k;
    libspectrum_byte parity;

    for (i = 0; i < 0x100; i++) {
        sz53_table[i] = (libspectrum_byte) (i & (FLAG_3 | FLAG_5 | FLAG_S));
        j = i;
        parity = 0;
        for (k = 0; k < 8; k++) {
            parity ^= (libspectrum_byte) (j & 1);
            j >>= 1;
        }
        parity_table[i] = (libspectrum_byte) (parity ? 0 : FLAG_P);
        sz53p_table[i] = (libspectrum_byte) (sz53_table[i] | parity_table[i]);
    }

    sz53_table[0] |= FLAG_Z;
    sz53p_table[0] |= FLAG_Z;
}

/* Reset the z80 */
void z80_reset(bool hard_reset) {
    TOTAL_CYCLES = 0;
    AF = AF_ = 0xffff;
    I = R = R7 = 0;
    PC = 0;
    SP = 0xffff;
    IFF1 = IFF2 = IM = 0;
    z80.halted = 0;
    z80.iff2_read = 0;
    Q = 0;

    if (hard_reset) {
        BC = DE = HL = 0;
        BC_ = DE_ = HL_ = 0;
        IX = IY = 0;
        z80.memptr.w = 0; /* TODO: confirm if this happens on soft reset */
    }

    z80.interrupts_enabled_at = (libspectrum_dword) -1;
}

/* Process a z80 maskable interrupt */
int z80_interrupt(void) {

    /* An interrupt will occur if IFF1 is set and the /INT line hasn't
       gone high again. On a Timex machine, we also need the SCLD's
       INTDISABLE to be clear */
    if (IFF1 /* &&
      tStates < machine_current->timings.interrupt_length &&
      !scld_last_dec.name.intdisable  */) {
        if (z80.iff2_read && !IS_CMOS) {
            /* We just executed LD A,I or LD A,R, causing IFF2 to be copied to the
               parity flag.  This occured whilst accepting an interrupt.  For NMOS
               Z80s only, clear the parity flag to reflect the fact that IFF2 would
               have actually been cleared before its value was transferred by LD A,I
               or LD A,R.  We cannot do this when emulating LD itself as we cannot
               tell whether the next instruction will be interrupted. */
            F &= ~FLAG_P;
        }

        /* If interrupts have just been enabled, don't accept the interrupt now,
           but check after the next instruction has been executed */
        if (tStates == z80.interrupts_enabled_at) {
            event_next_event = tStates + 1;
            return 0;
        }

        if (z80.halted) {
            PC++;
            z80.halted = 0;
        }

        IFF1 = IFF2 = 0;
        R = (R & 0x80) | ((R + 1) & 0x7f);

        add_time(7); /* Longer than usual M1 cycle */

        writeByte(--SP, PCH);
        writeByte(--SP, PCL);

        switch (IM) {
            case 0:
                /* We assume 0xff (RST 38) is on the data bus, as the Spectrum leaves
                   it pulled high when the end-of-frame interrupt is delivered.  Only
                   the first byte is provided directly to the Z80: all remaining bytes
                   of the instruction are fetched from memory using PC, which is
                   incremented as normal.  As RST 38 takes a single byte, we do not
                   emulate fetching of additional bytes. */
                PC = 0x0038;
                break;
            case 1:
                /* RST 38 */
                PC = 0x0038;
                break;
            case 2:
                /* We assume 0xff is on the data bus, as the Spectrum leaves it pulled
                   high when the end-of-frame interrupt is delivered.  Our interrupt
                   vector is therefore 0xff. */
            {
                libspectrum_word inttemp = (libspectrum_word) ((0x100 * I) + g_im2_vector);
                PCL = readByte(inttemp++);
                PCH = readByte(inttemp);
                break;
            }
            default:
                std::printf("Unknown interrupt mode %d/n", IM);
                // fuse_abort();
                break;
        }

        z80.memptr.w = PC;
        Q = 0;

        return 1; /* Accepted an interrupt */

    } else {

        return 0; /* Did not accept an interrupt */
    }
}

/* Process a z80 non-maskable interrupt */
static void z80_nmi(libspectrum_dword ts, int type, void *user_data) {
    /* TODO: this isn't ideal */
    /*   if( spectranet_available && spectranet_nmi_flipflop() )
        return;
     */
    if (z80.halted) {
        PC++;
        z80.halted = 0;
    }

    IFF1 = 0;
    R++;
    add_time(5);

    writeByte(--SP, PCH);
    writeByte(--SP, PCL);

    /* TODO: check whether any of these should occur before PC is pushed. */
    /*   if( machine_current->capabilities &
          LIBSPECTRUM_MACHINE_CAPABILITY_SCORP_MEMORY ) {
     */
    /* Page in ROM 2 */
    //   writeport_internal( 0x1ffd, machine_current->ram.last_byte2 | 0x02 );

    // } else if( beta_available ) {

    //   /* Page in TR-DOS ROM */
    //   beta_page();
    // } else if( spectranet_available ) {

    //   /* Page in spectranet */
    //   spectranet_nmi();
    // }

    Q = 0;
    PC = 0x0066;
}

/* Special peripheral processing for RETN */
void z80_retn(void) { abc80_cassette_reti(); }
