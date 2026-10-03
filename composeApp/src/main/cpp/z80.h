#ifndef FUSE_Z80_H
#define FUSE_Z80_H

#include <stddef.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define HAVE_ENOUGH_MEMORY 1
#define DEBUG 1

typedef uint8_t libspectrum_byte;
typedef int8_t libspectrum_signed_byte;
typedef uint16_t libspectrum_word;
typedef int32_t libspectrum_signed_dword;
typedef uint32_t libspectrum_dword;

extern libspectrum_dword g_cpu_frequency;
extern libspectrum_dword g_t_states_per_frame;
extern libspectrum_dword tStates;
extern uint64_t g_last_interrupt_tstate;
extern libspectrum_dword event_next_event;

/* Union allowing a register pair to be accessed as bytes or as a word */
typedef union {
#ifdef WORDS_BIGENDIAN
    struct {
      libspectrum_byte h, l;
    } b;
#else
    struct {
        libspectrum_byte l, h;
    } b;
#endif
    libspectrum_word w;
} regpair;

#pragma pack(push, 1)
/* What's stored in the main processor */
typedef struct {
    regpair af, bc, de, hl;
    regpair af_, bc_, de_, hl_;
    regpair ix, iy;
    libspectrum_byte i;
    libspectrum_word r;  /* The low seven bits of the R register. 16 bits long
                          so it can also act as an RZX instruction counter */
    libspectrum_byte r7; /* The high bit of the R register */
    regpair sp, pc;
    regpair memptr; /* The hidden register */
    int iff2_read;
    libspectrum_byte iff1, iff2, im;
    int halted;

    uint64_t total_cycles;

    /* Presumably, internal register where Z80 assembles the new content of the
       F register, before moving it back to F. The behaviour is deterministic in
       Zilog Z8- and nondeterministic in NEC Z80.
       https://www.worldofspectrum.org/forums/discussion/41704/ */
    libspectrum_byte q;

    /* Interrupts were enabled at this time; do not accept any interrupts
       until tStates > this value */
    libspectrum_signed_dword interrupts_enabled_at;

} processor;
#pragma pack(pop)

libspectrum_byte readByte(libspectrum_word address);
libspectrum_byte readByteInternal(libspectrum_word address);

void writeByte(libspectrum_word address, libspectrum_byte b);
void writeByteInternal(libspectrum_word address, libspectrum_byte b);

void contend_read(libspectrum_word address, libspectrum_dword time);
void contend_read_no_mreq(libspectrum_word address, libspectrum_dword time);
void contend_write_no_mreq(libspectrum_word address, libspectrum_dword time);

libspectrum_byte readport(libspectrum_word port);
void writeport(libspectrum_word port, libspectrum_byte b);

void add_time(libspectrum_dword time);
void z80_retn(void);
void z80_register_startup(void);
int z80_init(int cpu_frequency);
void z80_reset(bool hard_reset);
int z80_interrupt(void);
void z80_do_opcodes(void);
void z80_enable_interrupts(void);
void scf(void);
void ccf(void);

extern processor z80;
extern libspectrum_byte current_opcode;
extern libspectrum_byte current_opcode2;
extern const libspectrum_byte halfcarry_add_table[];
extern const libspectrum_byte halfcarry_sub_table[];
extern const libspectrum_byte overflow_add_table[];
extern const libspectrum_byte overflow_sub_table[];
extern libspectrum_byte sz53_table[];
extern libspectrum_byte sz53p_table[];
extern libspectrum_byte parity_table[];

extern int z80_interrupt_event;
extern int z80_nmi_event;
extern int z80_nmos_iff2_event;

#ifdef __cplusplus
}
#endif

#endif /* #ifndef FUSE_Z80_H */
