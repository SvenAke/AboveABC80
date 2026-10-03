/* z80_ops.c: Process the next opcode
   Copyright (c) 1999-2005 Philip Kendall, Witold Filipczyk
   Copyright (c) 2015 Stuart Brady
   Copyright (c) 2015 Gergely Szasz
   Copyright (c) 2015 Sergio Baldoví

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

   Author contact information:

   E-mail: philip-fuse@shadowmagic.org.uk

*/

#include <stdio.h>
#include "z80.h"
#include "z80_macros.h"
#include "native-lib.h"

#ifdef __GNUC__
#else /* #ifdef __GNUC__ */

                                                                                                                        #define CHECK(label, condition) if (condition) {
#define END_CHECK }

#endif /* #ifdef __GNUC__ */

libspectrum_byte last_Q;

extern void onZ80MainLoop(libspectrum_dword tStates);

extern void onTState(libspectrum_dword tStates, libspectrum_word pc);

extern void add_time(libspectrum_dword time);

void z80_do_opcodes(void) {
    libspectrum_byte opcode = 0x00;
    static libspectrum_word previous_pc = 0;
    static libspectrum_byte previous_opcode = 0;
    static int reported_text_entry = 0;
    static int reported_text_pc = 0;

    while (tStates < event_next_event) {
        if (g_frozen) {
            break;
        }

        libspectrum_dword tState = tStates;

        // M1 Cycle: Opcode Fetch (T1-T2) + Refresh (T3-T4)
        contend_read(PC, 4);

        libspectrum_word fetched_pc = PC;
        opcode = readByteInternal(PC++);
        current_opcode = opcode;

        if (fetched_pc == 0x3EFC && !reported_text_pc) {
            logd("WINSTALL entered text data at PC=%04X; previous PC=%04X opcode=%02X, "
                 "SP=%04X AF=%04X BC=%04X DE=%04X HL=%04X",
                 fetched_pc, previous_pc, previous_opcode, SP & 0xFFFF,
                 AF & 0xFFFF, BC & 0xFFFF, DE & 0xFFFF, HL & 0xFFFF);
            reported_text_pc = 1;
        }
        if (fetched_pc == 0x3E99 && !reported_text_entry) {
            logd("WINSTALL entered text code at PC=%04X; previous PC=%04X opcode=%02X, "
                 "SP=%04X AF=%04X BC=%04X DE=%04X HL=%04X",
                 fetched_pc, previous_pc, previous_opcode, SP & 0xFFFF,
                 AF & 0xFFFF, BC & 0xFFFF, DE & 0xFFFF, HL & 0xFFFF);
            reported_text_entry = 1;
        }
        previous_pc = fetched_pc;
        previous_opcode = opcode;

        if (!z80.halted) {
            R = (R & 0x80) | ((R + 1) & 0x7f);
        }
        last_Q = Q;
        Q = 0;

        switch (opcode) {

            case 0x00: /* NOP */
                break;
            case 0x01: /* LD BC,nnnn */
                C = readByte(PC++);
                B = readByte(PC++);
                break;
            case 0x02: /* LD (BC),A */
                z80.memptr.b.l = BC + 1;
                z80.memptr.b.h = A;
                writeByte(BC, A);
                break;
            case 0x03: /* INC BC */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                BC++;
                break;
            case 0x04: /* INC B */
            INC(B);
                break;
            case 0x05: /* DEC B */
            DEC(B);
                break;
            case 0x06: /* LD B,nn */
                B = readByte(PC++);
                break;
            case 0x07: /* RLCA */
                A = (A << 1) | (A >> 7);
                F = (F & (FLAG_P | FLAG_Z | FLAG_S)) | (A & (FLAG_C | FLAG_3 | FLAG_5));
                Q = F;
                break;
            case 0x08: /* EX AF,AF' */
                if (PC == 0x04d1 || PC == 0x0077) {
                }

                {
                    libspectrum_word wordtemp = AF;
                    AF = AF_;
                    AF_ = wordtemp;
                }
                break;
            case 0x09: /* ADD HL,BC */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                ADD16(HL, BC);
                break;
            case 0x0a: /* LD A,(BC) */
                z80.memptr.w = BC + 1;
                A = readByte(BC);
                break;
            case 0x0b: /* DEC BC */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                BC--;
                break;
            case 0x0c: /* INC C */
            INC(C);
                break;
            case 0x0d: /* DEC C */
            DEC(C);
                break;
            case 0x0e: /* LD C,nn */
                C = readByte(PC++);
                break;
            case 0x0f: /* RRCA */
                F = (F & (FLAG_P | FLAG_Z | FLAG_S)) | (A & FLAG_C);
                A = (A >> 1) | (A << 7);
                F |= (A & (FLAG_3 | FLAG_5));
                Q = F;
                break;
            case 0x10: /* DJNZ offset */
                contend_read_no_mreq(IR, 1);
                B--;
                if (B) {
                    JR();
                } else {
                    contend_read(PC, 3);
                    PC++;
                }
                break;
            case 0x11: /* LD DE,nnnn */
                E = readByte(PC++);
                D = readByte(PC++);
                break;
            case 0x12: /* LD (DE),A */
                z80.memptr.b.l = DE + 1;
                z80.memptr.b.h = A;
                writeByte(DE, A);
                break;
            case 0x13: /* INC DE */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                DE++;
                break;
            case 0x14: /* INC D */
            INC(D);
                break;
            case 0x15: /* DEC D */
            DEC(D);
                break;
            case 0x16: /* LD D,nn */
                D = readByte(PC++);
                break;
            case 0x17: /* RLA */
            {
                libspectrum_byte bytetemp = A;
                A = (A << 1) | (F & FLAG_C);
                F = (F & (FLAG_P | FLAG_Z | FLAG_S)) | (A & (FLAG_3 | FLAG_5)) |
                    (bytetemp >> 7);
                Q = F;
            }
                break;
            case 0x18: /* JR offset */
            JR();
                break;
            case 0x19: /* ADD HL,DE */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                ADD16(HL, DE);
                break;
            case 0x1a: /* LD A,(DE) */
                z80.memptr.w = DE + 1;
                A = readByte(DE);
                break;
            case 0x1b: /* DEC DE */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                DE--;
                break;
            case 0x1c: /* INC E */
            INC(E);
                break;
            case 0x1d: /* DEC E */
            DEC(E);
                break;
            case 0x1e: /* LD E,nn */
                E = readByte(PC++);
                break;
            case 0x1f: /* RRA */
            {
                libspectrum_byte bytetemp = A;
                A = (A >> 1) | (F << 7);
                F = (F & (FLAG_P | FLAG_Z | FLAG_S)) | (A & (FLAG_3 | FLAG_5)) |
                    (bytetemp & FLAG_C);
                Q = F;
            }
                break;
            case 0x20: /* JR NZ,offset */
                if (!(F & FLAG_Z)) {
                    JR();
                } else {
                    contend_read(PC, 3);
                    PC++;
                }
                break;
            case 0x21: /* LD HL,nnnn */
                L = readByte(PC++);
                H = readByte(PC++);
                break;
            case 0x22: /* LD (nnnn),HL */
            LD16_NNRR(L, H);
                break;
            case 0x23: /* INC HL */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                HL++;
                break;
            case 0x24: /* INC H */
            INC(H);
                break;
            case 0x25: /* DEC H */
            DEC(H);
                break;
            case 0x26: /* LD H,nn */
                H = readByte(PC++);
                break;
            case 0x27: /* DAA */
            {
                libspectrum_byte add = 0, carry = (F & FLAG_C);
                if ((F & FLAG_H) || ((A & 0x0f) > 9))
                    add = 6;
                if (carry || (A > 0x99))
                    add |= 0x60;
                if (A > 0x99)
                    carry = FLAG_C;
                if (F & FLAG_N) {
                    SUB(add);
                } else {
                    ADD(add);
                }
                F = (F & ~(FLAG_C | FLAG_P)) | carry | parity_table[A];
                Q = F;
            }
                break;
            case 0x28: /* JR Z,offset */
                if (F & FLAG_Z) {
                    JR();
                } else {
                    contend_read(PC, 3);
                    PC++;
                }
                break;
            case 0x29: /* ADD HL,HL */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                ADD16(HL, HL);
                break;
            case 0x2a: /* LD HL,(nnnn) */
            LD16_RRNN(L, H);
                break;
            case 0x2b: /* DEC HL */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                HL--;
                break;
            case 0x2c: /* INC L */
            INC(L);
                break;
            case 0x2d: /* DEC L */
            DEC(L);
                break;
            case 0x2e: /* LD L,nn */
                L = readByte(PC++);
                break;
            case 0x2f: /* CPL */
                A ^= 0xff;
                F = (F & (FLAG_C | FLAG_P | FLAG_Z | FLAG_S)) | (A & (FLAG_3 | FLAG_5)) |
                    (FLAG_N | FLAG_H);
                Q = F;
                break;
            case 0x30: /* JR NC,offset */
                if (!(F & FLAG_C)) {
                    JR();
                } else {
                    contend_read(PC, 3);
                    PC++;
                }
                break;
            case 0x31: /* LD SP,nnnn */
                SPL = readByte(PC++);
                SPH = readByte(PC++);
                break;
            case 0x32: /* LD (nnnn),A */
            {
                libspectrum_word wordtemp = readByte(PC++);
                wordtemp |= readByte(PC++) << 8;
                z80.memptr.b.l = wordtemp + 1;
                z80.memptr.b.h = A;
                writeByte(wordtemp, A);
            }
                break;
            case 0x33: /* INC SP */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                SP++;
                break;
            case 0x34: /* INC (HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                contend_read_no_mreq(HL, 1);
                INC(bytetemp);
                writeByte(HL, bytetemp);
            }
                break;
            case 0x35: /* DEC (HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                contend_read_no_mreq(HL, 1);
                DEC(bytetemp);
                writeByte(HL, bytetemp);
            }
                break;
            case 0x36: /* LD (HL),nn */
                writeByte(HL, readByte(PC++));
                break;
            case 0x37: /* SCF */
            SCF();
                break;
            case 0x38: /* JR C,offset */
                if (F & FLAG_C) {
                    JR();
                } else {
                    contend_read(PC, 3);
                    PC++;
                }
                break;
            case 0x39: /* ADD HL,SP */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                ADD16(HL, SP);
                break;
            case 0x3a: /* LD A,(nnnn) */
            {
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC++);
                A = readByte(z80.memptr.w++);
            }
                break;
            case 0x3b: /* DEC SP */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                SP--;
                break;
            case 0x3c: /* INC A */
            INC(A);
                break;
            case 0x3d: /* DEC A */
            DEC(A);
                break;
            case 0x3e: /* LD A,nn */
                A = readByte(PC++);
                break;
            case 0x3f: /* CCF */
            CCF();
                break;
            case 0x40: /* LD B,B */
                break;
            case 0x41: /* LD B,C */
                B = C;
                break;
            case 0x42: /* LD B,D */
                B = D;
                break;
            case 0x43: /* LD B,E */
                B = E;
                break;
            case 0x44: /* LD B,H */
                B = H;
                break;
            case 0x45: /* LD B,L */
                B = L;
                break;
            case 0x46: /* LD B,(HL) */
                B = readByte(HL);
                break;
            case 0x47: /* LD B,A */
                B = A;
                break;
            case 0x48: /* LD C,B */
                C = B;
                break;
            case 0x49: /* LD C,C */
                break;
            case 0x4a: /* LD C,D */
                C = D;
                break;
            case 0x4b: /* LD C,E */
                C = E;
                break;
            case 0x4c: /* LD C,H */
                C = H;
                break;
            case 0x4d: /* LD C,L */
                C = L;
                break;
            case 0x4e: /* LD C,(HL) */
                C = readByte(HL);
                break;
            case 0x4f: /* LD C,A */
                C = A;
                break;
            case 0x50: /* LD D,B */
                D = B;
                break;
            case 0x51: /* LD D,C */
                D = C;
                break;
            case 0x52: /* LD D,D */
                break;
            case 0x53: /* LD D,E */
                D = E;
                break;
            case 0x54: /* LD D,H */
                D = H;
                break;
            case 0x55: /* LD D,L */
                D = L;
                break;
            case 0x56: /* LD D,(HL) */
                D = readByte(HL);
                break;
            case 0x57: /* LD D,A */
                D = A;
                break;
            case 0x58: /* LD E,B */
                E = B;
                break;
            case 0x59: /* LD E,C */
                E = C;
                break;
            case 0x5a: /* LD E,D */
                E = D;
                break;
            case 0x5b: /* LD E,E */
                break;
            case 0x5c: /* LD E,H */
                E = H;
                break;
            case 0x5d: /* LD E,L */
                E = L;
                break;
            case 0x5e: /* LD E,(HL) */
                E = readByte(HL);
                break;
            case 0x5f: /* LD E,A */
                E = A;
                break;
            case 0x60: /* LD H,B */
                H = B;
                break;
            case 0x61: /* LD H,C */
                H = C;
                break;
            case 0x62: /* LD H,D */
                H = D;
                break;
            case 0x63: /* LD H,E */
                H = E;
                break;
            case 0x64: /* LD H,H */
                break;
            case 0x65: /* LD H,L */
                H = L;
                break;
            case 0x66: /* LD H,(HL) */
                H = readByte(HL);
                break;
            case 0x67: /* LD H,A */
                H = A;
                break;
            case 0x68: /* LD L,B */
                L = B;
                break;
            case 0x69: /* LD L,C */
                L = C;
                break;
            case 0x6a: /* LD L,D */
                L = D;
                break;
            case 0x6b: /* LD L,E */
                L = E;
                break;
            case 0x6c: /* LD L,H */
                L = H;
                break;
            case 0x6d: /* LD L,L */
                break;
            case 0x6e: /* LD L,(HL) */
                L = readByte(HL);
                break;
            case 0x6f: /* LD L,A */
                L = A;
                break;
            case 0x70: /* LD (HL),B */
                writeByte(HL, B);
                break;
            case 0x71: /* LD (HL),C */
                writeByte(HL, C);
                break;
            case 0x72: /* LD (HL),D */
                writeByte(HL, D);
                break;
            case 0x73: /* LD (HL),E */
                writeByte(HL, E);
                break;
            case 0x74: /* LD (HL),H */
                writeByte(HL, H);
                break;
            case 0x75: /* LD (HL),L */
                writeByte(HL, L);
                break;
            case 0x76: /* HALT */
                z80.halted = 1;
                // On real hardware, HALT does not repeatedly fetch opcodes.
                // It sits and does dummy refresh cycles which DON'T increment R.
                // We simulate this by staying on the same PC but not increasing R in future loops.
                PC--;
                break;
            case 0x77: /* LD (HL),A */
                writeByte(HL, A);
                break;
            case 0x78: /* LD A,B */
                A = B;
                break;
            case 0x79: /* LD A,C */
                A = C;
                break;
            case 0x7a: /* LD A,D */
                A = D;
                break;
            case 0x7b: /* LD A,E */
                A = E;
                break;
            case 0x7c: /* LD A,H */
                A = H;
                break;
            case 0x7d: /* LD A,L */
                A = L;
                break;
            case 0x7e: /* LD A,(HL) */
                A = readByte(HL);
                break;
            case 0x7f: /* LD A,A */
                break;
            case 0x80: /* ADD A,B */
            ADD(B);
                break;
            case 0x81: /* ADD A,C */
            ADD(C);
                break;
            case 0x82: /* ADD A,D */
            ADD(D);
                break;
            case 0x83: /* ADD A,E */
            ADD(E);
                break;
            case 0x84: /* ADD A,H */
            ADD(H);
                break;
            case 0x85: /* ADD A,L */
            ADD(L);
                break;
            case 0x86: /* ADD A,(HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                ADD(bytetemp);
            }
                break;
            case 0x87: /* ADD A,A */
            ADD(A);
                break;
            case 0x88: /* ADC A,B */
            ADC(B);
                break;
            case 0x89: /* ADC A,C */
            ADC(C);
                break;
            case 0x8a: /* ADC A,D */
            ADC(D);
                break;
            case 0x8b: /* ADC A,E */
            ADC(E);
                break;
            case 0x8c: /* ADC A,H */
            ADC(H);
                break;
            case 0x8d: /* ADC A,L */
            ADC(L);
                break;
            case 0x8e: /* ADC A,(HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                ADC(bytetemp);
            }
                break;
            case 0x8f: /* ADC A,A */
            ADC(A);
                break;
            case 0x90: /* SUB A,B */
            SUB(B);
                break;
            case 0x91: /* SUB A,C */
            SUB(C);
                break;
            case 0x92: /* SUB A,D */
            SUB(D);
                break;
            case 0x93: /* SUB A,E */
            SUB(E);
                break;
            case 0x94: /* SUB A,H */
            SUB(H);
                break;
            case 0x95: /* SUB A,L */
            SUB(L);
                break;
            case 0x96: /* SUB A,(HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                SUB(bytetemp);
            }
                break;
            case 0x97: /* SUB A,A */
            SUB(A);
                break;
            case 0x98: /* SBC A,B */
            SBC(B);
                break;
            case 0x99: /* SBC A,C */
            SBC(C);
                break;
            case 0x9a: /* SBC A,D */
            SBC(D);
                break;
            case 0x9b: /* SBC A,E */
            SBC(E);
                break;
            case 0x9c: /* SBC A,H */
            SBC(H);
                break;
            case 0x9d: /* SBC A,L */
            SBC(L);
                break;
            case 0x9e: /* SBC A,(HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                SBC(bytetemp);
            }
                break;
            case 0x9f: /* SBC A,A */
            SBC(A);
                break;
            case 0xa0: /* AND A,B */
            AND(B);
                break;
            case 0xa1: /* AND A,C */
            AND(C);
                break;
            case 0xa2: /* AND A,D */
            AND(D);
                break;
            case 0xa3: /* AND A,E */
            AND(E);
                break;
            case 0xa4: /* AND A,H */
            AND(H);
                break;
            case 0xa5: /* AND A,L */
            AND(L);
                break;
            case 0xa6: /* AND A,(HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                AND(bytetemp);
            }
                break;
            case 0xa7: /* AND A,A */
            AND(A);
                break;
            case 0xa8: /* XOR A,B */
            XOR(B);
                break;
            case 0xa9: /* XOR A,C */
            XOR(C);
                break;
            case 0xaa: /* XOR A,D */
            XOR(D);
                break;
            case 0xab: /* XOR A,E */
            XOR(E);
                break;
            case 0xac: /* XOR A,H */
            XOR(H);
                break;
            case 0xad: /* XOR A,L */
            XOR(L);
                break;
            case 0xae: /* XOR A,(HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                XOR(bytetemp);
            }
                break;
            case 0xaf: /* XOR A,A */
            XOR(A);
                break;
            case 0xb0: /* OR A,B */
            OR(B);
                break;
            case 0xb1: /* OR A,C */
            OR(C);
                break;
            case 0xb2: /* OR A,D */
            OR(D);
                break;
            case 0xb3: /* OR A,E */
            OR(E);
                break;
            case 0xb4: /* OR A,H */
            OR(H);
                break;
            case 0xb5: /* OR A,L */
            OR(L);
                break;
            case 0xb6: /* OR A,(HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                OR(bytetemp);
            }
                break;
            case 0xb7: /* OR A,A */
            OR(A);
                break;
            case 0xb8: /* CP B */
            CP(B);
                break;
            case 0xb9: /* CP C */
            CP(C);
                break;
            case 0xba: /* CP D */
            CP(D);
                break;
            case 0xbb: /* CP E */
            CP(E);
                break;
            case 0xbc: /* CP H */
            CP(H);
                break;
            case 0xbd: /* CP L */
            CP(L);
                break;
            case 0xbe: /* CP (HL) */
            {
                libspectrum_byte bytetemp = readByte(HL);
                CP(bytetemp);
            }
                break;
            case 0xbf: /* CP A */
            CP(A);
                break;
            case 0xc0: /* RET NZ */
                contend_read_no_mreq(IR, 1);
                if (PC == 0x056c || PC == 0x0112) {
                    //	if( tape_load_trap() == 0 ) break;
                }
                if (!(F & FLAG_Z)) {
                    RET();
                }
                break;
            case 0xc1: /* POP BC */
            POP16(C, B);
                break;
            case 0xc2: /* JP NZ,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_Z)) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xc3: /* JP nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (z80.memptr.w == 0) {
                    logd("Z80 JP 0000 at %04X, SP=%04X, AF=%04X, BC=%04X, DE=%04X, HL=%04X",
                         (PC - 2) & 0xFFFF, SP & 0xFFFF, AF & 0xFFFF,
                         BC & 0xFFFF, DE & 0xFFFF, HL & 0xFFFF);
                }
                JP();
                break;
            case 0xc4: /* CALL NZ,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_Z)) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xc5: /* PUSH BC */
                contend_read_no_mreq(IR, 1);
                PUSH16(C, B);
                break;
            case 0xc6: /* ADD A,nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                ADD(bytetemp);
            }
                break;
            case 0xc7: /* RST 00 */
                logd("Z80 RST 00 at %04X, next=%04X, SP=%04X, AF=%04X, BC=%04X, DE=%04X, HL=%04X",
                     (PC - 1) & 0xFFFF, PC & 0xFFFF, SP & 0xFFFF, AF & 0xFFFF,
                     BC & 0xFFFF, DE & 0xFFFF, HL & 0xFFFF);
                contend_read_no_mreq(IR, 1);
                RST(0x00);
                break;
            case 0xc8: /* RET Z */
                contend_read_no_mreq(IR, 1);
                if (F & FLAG_Z) {
                    RET();
                }
                break;
            case 0xc9: /* RET */
            RET();
                break;
            case 0xca: /* JP Z,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_Z) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xcb: /* shift CB */
            {
                libspectrum_byte opcode2;
                contend_read(PC, 4);
                opcode2 = readByteInternal(PC);
                PC++;
                if (!z80.halted) {
                    R = (R & 0x80) | ((R + 1) & 0x7f);
                }
#ifdef HAVE_ENOUGH_MEMORY
                switch (opcode2) {
/* z80_cb.c: Z80 CBxx opcodes
   Copyright (c) 1999-2003 Philip Kendall

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

   Author contact information:

   E-mail: philip-fuse@shadowmagic.org.uk

*/

/* NB: this file is autogenerated by './z80/z80.pl' from 'opcodes_cb.dat',
   and included in 'z80_ops.c' */

                    case 0x00: /* RLC B */
                    RLC(B);
                        break;
                    case 0x01: /* RLC C */
                    RLC(C);
                        break;
                    case 0x02: /* RLC D */
                    RLC(D);
                        break;
                    case 0x03: /* RLC E */
                    RLC(E);
                        break;
                    case 0x04: /* RLC H */
                    RLC(H);
                        break;
                    case 0x05: /* RLC L */
                    RLC(L);
                        break;
                    case 0x06: /* RLC (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        RLC(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x07: /* RLC A */
                    RLC(A);
                        break;
                    case 0x08: /* RRC B */
                    RRC(B);
                        break;
                    case 0x09: /* RRC C */
                    RRC(C);
                        break;
                    case 0x0a: /* RRC D */
                    RRC(D);
                        break;
                    case 0x0b: /* RRC E */
                    RRC(E);
                        break;
                    case 0x0c: /* RRC H */
                    RRC(H);
                        break;
                    case 0x0d: /* RRC L */
                    RRC(L);
                        break;
                    case 0x0e: /* RRC (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        RRC(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x0f: /* RRC A */
                    RRC(A);
                        break;
                    case 0x10: /* RL B */
                    RL(B);
                        break;
                    case 0x11: /* RL C */
                    RL(C);
                        break;
                    case 0x12: /* RL D */
                    RL(D);
                        break;
                    case 0x13: /* RL E */
                    RL(E);
                        break;
                    case 0x14: /* RL H */
                    RL(H);
                        break;
                    case 0x15: /* RL L */
                    RL(L);
                        break;
                    case 0x16: /* RL (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        RL(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x17: /* RL A */
                    RL(A);
                        break;
                    case 0x18: /* RR B */
                    RR(B);
                        break;
                    case 0x19: /* RR C */
                    RR(C);
                        break;
                    case 0x1a: /* RR D */
                    RR(D);
                        break;
                    case 0x1b: /* RR E */
                    RR(E);
                        break;
                    case 0x1c: /* RR H */
                    RR(H);
                        break;
                    case 0x1d: /* RR L */
                    RR(L);
                        break;
                    case 0x1e: /* RR (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        RR(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x1f: /* RR A */
                    RR(A);
                        break;
                    case 0x20: /* SLA B */
                    SLA(B);
                        break;
                    case 0x21: /* SLA C */
                    SLA(C);
                        break;
                    case 0x22: /* SLA D */
                    SLA(D);
                        break;
                    case 0x23: /* SLA E */
                    SLA(E);
                        break;
                    case 0x24: /* SLA H */
                    SLA(H);
                        break;
                    case 0x25: /* SLA L */
                    SLA(L);
                        break;
                    case 0x26: /* SLA (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        SLA(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x27: /* SLA A */
                    SLA(A);
                        break;
                    case 0x28: /* SRA B */
                    SRA(B);
                        break;
                    case 0x29: /* SRA C */
                    SRA(C);
                        break;
                    case 0x2a: /* SRA D */
                    SRA(D);
                        break;
                    case 0x2b: /* SRA E */
                    SRA(E);
                        break;
                    case 0x2c: /* SRA H */
                    SRA(H);
                        break;
                    case 0x2d: /* SRA L */
                    SRA(L);
                        break;
                    case 0x2e: /* SRA (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        SRA(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x2f: /* SRA A */
                    SRA(A);
                        break;
                    case 0x30: /* SLL B */
                    SLL(B);
                        break;
                    case 0x31: /* SLL C */
                    SLL(C);
                        break;
                    case 0x32: /* SLL D */
                    SLL(D);
                        break;
                    case 0x33: /* SLL E */
                    SLL(E);
                        break;
                    case 0x34: /* SLL H */
                    SLL(H);
                        break;
                    case 0x35: /* SLL L */
                    SLL(L);
                        break;
                    case 0x36: /* SLL (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        SLL(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x37: /* SLL A */
                    SLL(A);
                        break;
                    case 0x38: /* SRL B */
                    SRL(B);
                        break;
                    case 0x39: /* SRL C */
                    SRL(C);
                        break;
                    case 0x3a: /* SRL D */
                    SRL(D);
                        break;
                    case 0x3b: /* SRL E */
                    SRL(E);
                        break;
                    case 0x3c: /* SRL H */
                    SRL(H);
                        break;
                    case 0x3d: /* SRL L */
                    SRL(L);
                        break;
                    case 0x3e: /* SRL (HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        SRL(bytetemp);
                        writeByte(HL, bytetemp);
                    }
                        break;
                    case 0x3f: /* SRL A */
                    SRL(A);
                        break;
                    case 0x40: /* BIT 0,B */
                    BIT(0, B);
                        break;
                    case 0x41: /* BIT 0,C */
                    BIT(0, C);
                        break;
                    case 0x42: /* BIT 0,D */
                    BIT(0, D);
                        break;
                    case 0x43: /* BIT 0,E */
                    BIT(0, E);
                        break;
                    case 0x44: /* BIT 0,H */
                    BIT(0, H);
                        break;
                    case 0x45: /* BIT 0,L */
                    BIT(0, L);
                        break;
                    case 0x46: /* BIT 0,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(0, bytetemp);
                    }
                        break;
                    case 0x47: /* BIT 0,A */
                    BIT(0, A);
                        break;
                    case 0x48: /* BIT 1,B */
                    BIT(1, B);
                        break;
                    case 0x49: /* BIT 1,C */
                    BIT(1, C);
                        break;
                    case 0x4a: /* BIT 1,D */
                    BIT(1, D);
                        break;
                    case 0x4b: /* BIT 1,E */
                    BIT(1, E);
                        break;
                    case 0x4c: /* BIT 1,H */
                    BIT(1, H);
                        break;
                    case 0x4d: /* BIT 1,L */
                    BIT(1, L);
                        break;
                    case 0x4e: /* BIT 1,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(1, bytetemp);
                    }
                        break;
                    case 0x4f: /* BIT 1,A */
                    BIT(1, A);
                        break;
                    case 0x50: /* BIT 2,B */
                    BIT(2, B);
                        break;
                    case 0x51: /* BIT 2,C */
                    BIT(2, C);
                        break;
                    case 0x52: /* BIT 2,D */
                    BIT(2, D);
                        break;
                    case 0x53: /* BIT 2,E */
                    BIT(2, E);
                        break;
                    case 0x54: /* BIT 2,H */
                    BIT(2, H);
                        break;
                    case 0x55: /* BIT 2,L */
                    BIT(2, L);
                        break;
                    case 0x56: /* BIT 2,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(2, bytetemp);
                    }
                        break;
                    case 0x57: /* BIT 2,A */
                    BIT(2, A);
                        break;
                    case 0x58: /* BIT 3,B */
                    BIT(3, B);
                        break;
                    case 0x59: /* BIT 3,C */
                    BIT(3, C);
                        break;
                    case 0x5a: /* BIT 3,D */
                    BIT(3, D);
                        break;
                    case 0x5b: /* BIT 3,E */
                    BIT(3, E);
                        break;
                    case 0x5c: /* BIT 3,H */
                    BIT(3, H);
                        break;
                    case 0x5d: /* BIT 3,L */
                    BIT(3, L);
                        break;
                    case 0x5e: /* BIT 3,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(3, bytetemp);
                    }
                        break;
                    case 0x5f: /* BIT 3,A */
                    BIT(3, A);
                        break;
                    case 0x60: /* BIT 4,B */
                    BIT(4, B);
                        break;
                    case 0x61: /* BIT 4,C */
                    BIT(4, C);
                        break;
                    case 0x62: /* BIT 4,D */
                    BIT(4, D);
                        break;
                    case 0x63: /* BIT 4,E */
                    BIT(4, E);
                        break;
                    case 0x64: /* BIT 4,H */
                    BIT(4, H);
                        break;
                    case 0x65: /* BIT 4,L */
                    BIT(4, L);
                        break;
                    case 0x66: /* BIT 4,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(4, bytetemp);
                    }
                        break;
                    case 0x67: /* BIT 4,A */
                    BIT(4, A);
                        break;
                    case 0x68: /* BIT 5,B */
                    BIT(5, B);
                        break;
                    case 0x69: /* BIT 5,C */
                    BIT(5, C);
                        break;
                    case 0x6a: /* BIT 5,D */
                    BIT(5, D);
                        break;
                    case 0x6b: /* BIT 5,E */
                    BIT(5, E);
                        break;
                    case 0x6c: /* BIT 5,H */
                    BIT(5, H);
                        break;
                    case 0x6d: /* BIT 5,L */
                    BIT(5, L);
                        break;
                    case 0x6e: /* BIT 5,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(5, bytetemp);
                    }
                        break;
                    case 0x6f: /* BIT 5,A */
                    BIT(5, A);
                        break;
                    case 0x70: /* BIT 6,B */
                    BIT(6, B);
                        break;
                    case 0x71: /* BIT 6,C */
                    BIT(6, C);
                        break;
                    case 0x72: /* BIT 6,D */
                    BIT(6, D);
                        break;
                    case 0x73: /* BIT 6,E */
                    BIT(6, E);
                        break;
                    case 0x74: /* BIT 6,H */
                    BIT(6, H);
                        break;
                    case 0x75: /* BIT 6,L */
                    BIT(6, L);
                        break;
                    case 0x76: /* BIT 6,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(6, bytetemp);
                    }
                        break;
                    case 0x77: /* BIT 6,A */
                    BIT(6, A);
                        break;
                    case 0x78: /* BIT 7,B */
                    BIT(7, B);
                        break;
                    case 0x79: /* BIT 7,C */
                    BIT(7, C);
                        break;
                    case 0x7a: /* BIT 7,D */
                    BIT(7, D);
                        break;
                    case 0x7b: /* BIT 7,E */
                    BIT(7, E);
                        break;
                    case 0x7c: /* BIT 7,H */
                    BIT(7, H);
                        break;
                    case 0x7d: /* BIT 7,L */
                    BIT(7, L);
                        break;
                    case 0x7e: /* BIT 7,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        BIT_MEMPTR(7, bytetemp);
                    }
                        break;
                    case 0x7f: /* BIT 7,A */
                    BIT(7, A);
                        break;
                    case 0x80: /* RES 0,B */
                        B &= 0xfe;
                        break;
                    case 0x81: /* RES 0,C */
                        C &= 0xfe;
                        break;
                    case 0x82: /* RES 0,D */
                        D &= 0xfe;
                        break;
                    case 0x83: /* RES 0,E */
                        E &= 0xfe;
                        break;
                    case 0x84: /* RES 0,H */
                        H &= 0xfe;
                        break;
                    case 0x85: /* RES 0,L */
                        L &= 0xfe;
                        break;
                    case 0x86: /* RES 0,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0xfe);
                    }
                        break;
                    case 0x87: /* RES 0,A */
                        A &= 0xfe;
                        break;
                    case 0x88: /* RES 1,B */
                        B &= 0xfd;
                        break;
                    case 0x89: /* RES 1,C */
                        C &= 0xfd;
                        break;
                    case 0x8a: /* RES 1,D */
                        D &= 0xfd;
                        break;
                    case 0x8b: /* RES 1,E */
                        E &= 0xfd;
                        break;
                    case 0x8c: /* RES 1,H */
                        H &= 0xfd;
                        break;
                    case 0x8d: /* RES 1,L */
                        L &= 0xfd;
                        break;
                    case 0x8e: /* RES 1,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0xfd);
                    }
                        break;
                    case 0x8f: /* RES 1,A */
                        A &= 0xfd;
                        break;
                    case 0x90: /* RES 2,B */
                        B &= 0xfb;
                        break;
                    case 0x91: /* RES 2,C */
                        C &= 0xfb;
                        break;
                    case 0x92: /* RES 2,D */
                        D &= 0xfb;
                        break;
                    case 0x93: /* RES 2,E */
                        E &= 0xfb;
                        break;
                    case 0x94: /* RES 2,H */
                        H &= 0xfb;
                        break;
                    case 0x95: /* RES 2,L */
                        L &= 0xfb;
                        break;
                    case 0x96: /* RES 2,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0xfb);
                    }
                        break;
                    case 0x97: /* RES 2,A */
                        A &= 0xfb;
                        break;
                    case 0x98: /* RES 3,B */
                        B &= 0xf7;
                        break;
                    case 0x99: /* RES 3,C */
                        C &= 0xf7;
                        break;
                    case 0x9a: /* RES 3,D */
                        D &= 0xf7;
                        break;
                    case 0x9b: /* RES 3,E */
                        E &= 0xf7;
                        break;
                    case 0x9c: /* RES 3,H */
                        H &= 0xf7;
                        break;
                    case 0x9d: /* RES 3,L */
                        L &= 0xf7;
                        break;
                    case 0x9e: /* RES 3,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0xf7);
                    }
                        break;
                    case 0x9f: /* RES 3,A */
                        A &= 0xf7;
                        break;
                    case 0xa0: /* RES 4,B */
                        B &= 0xef;
                        break;
                    case 0xa1: /* RES 4,C */
                        C &= 0xef;
                        break;
                    case 0xa2: /* RES 4,D */
                        D &= 0xef;
                        break;
                    case 0xa3: /* RES 4,E */
                        E &= 0xef;
                        break;
                    case 0xa4: /* RES 4,H */
                        H &= 0xef;
                        break;
                    case 0xa5: /* RES 4,L */
                        L &= 0xef;
                        break;
                    case 0xa6: /* RES 4,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0xef);
                    }
                        break;
                    case 0xa7: /* RES 4,A */
                        A &= 0xef;
                        break;
                    case 0xa8: /* RES 5,B */
                        B &= 0xdf;
                        break;
                    case 0xa9: /* RES 5,C */
                        C &= 0xdf;
                        break;
                    case 0xaa: /* RES 5,D */
                        D &= 0xdf;
                        break;
                    case 0xab: /* RES 5,E */
                        E &= 0xdf;
                        break;
                    case 0xac: /* RES 5,H */
                        H &= 0xdf;
                        break;
                    case 0xad: /* RES 5,L */
                        L &= 0xdf;
                        break;
                    case 0xae: /* RES 5,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0xdf);
                    }
                        break;
                    case 0xaf: /* RES 5,A */
                        A &= 0xdf;
                        break;
                    case 0xb0: /* RES 6,B */
                        B &= 0xbf;
                        break;
                    case 0xb1: /* RES 6,C */
                        C &= 0xbf;
                        break;
                    case 0xb2: /* RES 6,D */
                        D &= 0xbf;
                        break;
                    case 0xb3: /* RES 6,E */
                        E &= 0xbf;
                        break;
                    case 0xb4: /* RES 6,H */
                        H &= 0xbf;
                        break;
                    case 0xb5: /* RES 6,L */
                        L &= 0xbf;
                        break;
                    case 0xb6: /* RES 6,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0xbf);
                    }
                        break;
                    case 0xb7: /* RES 6,A */
                        A &= 0xbf;
                        break;
                    case 0xb8: /* RES 7,B */
                        B &= 0x7f;
                        break;
                    case 0xb9: /* RES 7,C */
                        C &= 0x7f;
                        break;
                    case 0xba: /* RES 7,D */
                        D &= 0x7f;
                        break;
                    case 0xbb: /* RES 7,E */
                        E &= 0x7f;
                        break;
                    case 0xbc: /* RES 7,H */
                        H &= 0x7f;
                        break;
                    case 0xbd: /* RES 7,L */
                        L &= 0x7f;
                        break;
                    case 0xbe: /* RES 7,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp & 0x7f);
                    }
                        break;
                    case 0xbf: /* RES 7,A */
                        A &= 0x7f;
                        break;
                    case 0xc0: /* SET 0,B */
                        B |= 0x01;
                        break;
                    case 0xc1: /* SET 0,C */
                        C |= 0x01;
                        break;
                    case 0xc2: /* SET 0,D */
                        D |= 0x01;
                        break;
                    case 0xc3: /* SET 0,E */
                        E |= 0x01;
                        break;
                    case 0xc4: /* SET 0,H */
                        H |= 0x01;
                        break;
                    case 0xc5: /* SET 0,L */
                        L |= 0x01;
                        break;
                    case 0xc6: /* SET 0,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x01);
                    }
                        break;
                    case 0xc7: /* SET 0,A */
                        A |= 0x01;
                        break;
                    case 0xc8: /* SET 1,B */
                        B |= 0x02;
                        break;
                    case 0xc9: /* SET 1,C */
                        C |= 0x02;
                        break;
                    case 0xca: /* SET 1,D */
                        D |= 0x02;
                        break;
                    case 0xcb: /* SET 1,E */
                        E |= 0x02;
                        break;
                    case 0xcc: /* SET 1,H */
                        H |= 0x02;
                        break;
                    case 0xcd: /* SET 1,L */
                        L |= 0x02;
                        break;
                    case 0xce: /* SET 1,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x02);
                    }
                        break;
                    case 0xcf: /* SET 1,A */
                        A |= 0x02;
                        break;
                    case 0xd0: /* SET 2,B */
                        B |= 0x04;
                        break;
                    case 0xd1: /* SET 2,C */
                        C |= 0x04;
                        break;
                    case 0xd2: /* SET 2,D */
                        D |= 0x04;
                        break;
                    case 0xd3: /* SET 2,E */
                        E |= 0x04;
                        break;
                    case 0xd4: /* SET 2,H */
                        H |= 0x04;
                        break;
                    case 0xd5: /* SET 2,L */
                        L |= 0x04;
                        break;
                    case 0xd6: /* SET 2,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x04);
                    }
                        break;
                    case 0xd7: /* SET 2,A */
                        A |= 0x04;
                        break;
                    case 0xd8: /* SET 3,B */
                        B |= 0x08;
                        break;
                    case 0xd9: /* SET 3,C */
                        C |= 0x08;
                        break;
                    case 0xda: /* SET 3,D */
                        D |= 0x08;
                        break;
                    case 0xdb: /* SET 3,E */
                        E |= 0x08;
                        break;
                    case 0xdc: /* SET 3,H */
                        H |= 0x08;
                        break;
                    case 0xdd: /* SET 3,L */
                        L |= 0x08;
                        break;
                    case 0xde: /* SET 3,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x08);
                    }
                        break;
                    case 0xdf: /* SET 3,A */
                        A |= 0x08;
                        break;
                    case 0xe0: /* SET 4,B */
                        B |= 0x10;
                        break;
                    case 0xe1: /* SET 4,C */
                        C |= 0x10;
                        break;
                    case 0xe2: /* SET 4,D */
                        D |= 0x10;
                        break;
                    case 0xe3: /* SET 4,E */
                        E |= 0x10;
                        break;
                    case 0xe4: /* SET 4,H */
                        H |= 0x10;
                        break;
                    case 0xe5: /* SET 4,L */
                        L |= 0x10;
                        break;
                    case 0xe6: /* SET 4,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x10);
                    }
                        break;
                    case 0xe7: /* SET 4,A */
                        A |= 0x10;
                        break;
                    case 0xe8: /* SET 5,B */
                        B |= 0x20;
                        break;
                    case 0xe9: /* SET 5,C */
                        C |= 0x20;
                        break;
                    case 0xea: /* SET 5,D */
                        D |= 0x20;
                        break;
                    case 0xeb: /* SET 5,E */
                        E |= 0x20;
                        break;
                    case 0xec: /* SET 5,H */
                        H |= 0x20;
                        break;
                    case 0xed: /* SET 5,L */
                        L |= 0x20;
                        break;
                    case 0xee: /* SET 5,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x20);
                    }
                        break;
                    case 0xef: /* SET 5,A */
                        A |= 0x20;
                        break;
                    case 0xf0: /* SET 6,B */
                        B |= 0x40;
                        break;
                    case 0xf1: /* SET 6,C */
                        C |= 0x40;
                        break;
                    case 0xf2: /* SET 6,D */
                        D |= 0x40;
                        break;
                    case 0xf3: /* SET 6,E */
                        E |= 0x40;
                        break;
                    case 0xf4: /* SET 6,H */
                        H |= 0x40;
                        break;
                    case 0xf5: /* SET 6,L */
                        L |= 0x40;
                        break;
                    case 0xf6: /* SET 6,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x40);
                    }
                        break;
                    case 0xf7: /* SET 6,A */
                        A |= 0x40;
                        break;
                    case 0xf8: /* SET 7,B */
                        B |= 0x80;
                        break;
                    case 0xf9: /* SET 7,C */
                        C |= 0x80;
                        break;
                    case 0xfa: /* SET 7,D */
                        D |= 0x80;
                        break;
                    case 0xfb: /* SET 7,E */
                        E |= 0x80;
                        break;
                    case 0xfc: /* SET 7,H */
                        H |= 0x80;
                        break;
                    case 0xfd: /* SET 7,L */
                        L |= 0x80;
                        break;
                    case 0xfe: /* SET 7,(HL) */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, bytetemp | 0x80);
                    }
                        break;
                    case 0xff: /* SET 7,A */
                        A |= 0x80;
                        break;
                }
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                                                                                                                                        if (z80_cbxx(opcode2))
    goto end_opcode;
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
            }
                break;
            case 0xcc: /* CALL Z,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_Z) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xcd: /* CALL nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                CALL();
                break;
            case 0xce: /* ADC A,nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                ADC(bytetemp);
            }
                break;
            case 0xcf: /* RST 8 */
                contend_read_no_mreq(IR, 1);
                RST(0x08);
                break;
            case 0xd0: /* RET NC */
                contend_read_no_mreq(IR, 1);
                if (!(F & FLAG_C)) {
                    RET();
                }
                break;
            case 0xd1: /* POP DE */
            POP16(E, D);
                break;
            case 0xd2: /* JP NC,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_C)) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xd3: /* OUT (nn),A */
            {
                libspectrum_byte nn = readByte(PC++);
                libspectrum_word outtemp = nn | (A << 8);
                z80.memptr.b.h = A;
                z80.memptr.b.l = (nn + 1);
                writeport(outtemp, A);
            }
                break;
            case 0xd4: /* CALL NC,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_C)) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xd5: /* PUSH DE */
                contend_read_no_mreq(IR, 1);
                PUSH16(E, D);
                break;
            case 0xd6: /* SUB nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                SUB(bytetemp);
            }
                break;
            case 0xd7: /* RST 10 */
                contend_read_no_mreq(IR, 1);
                RST(0x10);
                break;
            case 0xd8: /* RET C */
                contend_read_no_mreq(IR, 1);
                if (F & FLAG_C) {
                    RET();
                }
                break;
            case 0xd9: /* EXX */
            {
                libspectrum_word wordtemp;
                wordtemp = BC;
                BC = BC_;
                BC_ = wordtemp;
                wordtemp = DE;
                DE = DE_;
                DE_ = wordtemp;
                wordtemp = HL;
                HL = HL_;
                HL_ = wordtemp;
            }
                break;
            case 0xda: /* JP C,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_C) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xdb: /* IN A,(nn) */
            {
                libspectrum_word intemp;
                intemp = readByte(PC++) + (A << 8);
                A = readport(intemp);
                /* TODO: is this correct if (nn) was 0xff? */
                z80.memptr.w = intemp + 1;
            }
                break;
            case 0xdc: /* CALL C,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_C) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xdd: /* shift DD */
            {
                libspectrum_byte opcode2;
                contend_read(PC, 4);
                opcode2 = readByteInternal(PC);
                PC++;
                if (!z80.halted) {
                    R = (R & 0x80) | ((R + 1) & 0x7f);
                }
#ifdef HAVE_ENOUGH_MEMORY
                switch (opcode2) {
#define REGISTER IX
#define REGISTERL IXL
#define REGISTERH IXH
/* z80_ddfd.c Z80 {DD,FD}xx opcodes
   Copyright (c) 1999-2003 Philip Kendall

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

   Author contact information:

   E-mail: philip-fuse@shadowmagic.org.uk

*/

/* NB: this file is autogenerated by './z80/z80.pl' from 'opcodes_ddfd.dat',
   and included in 'z80_ops.c' */

                    case 0x09: /* ADD REGISTER,BC */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, BC);
                        break;
                    case 0x19: /* ADD REGISTER,DE */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, DE);
                        break;
                    case 0x21: /* LD REGISTER,nnnn */
                        REGISTERL = readByte(PC++);
                        REGISTERH = readByte(PC++);
                        break;
                    case 0x22: /* LD (nnnn),REGISTER */
                    LD16_NNRR(REGISTERL, REGISTERH);
                        break;
                    case 0x23: /* INC REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        REGISTER++;
                        break;
                    case 0x24: /* INC REGISTERH */
                    INC(REGISTERH);
                        break;
                    case 0x25: /* DEC REGISTERH */
                    DEC(REGISTERH);
                        break;
                    case 0x26: /* LD REGISTERH,nn */
                        REGISTERH = readByte(PC++);
                        break;
                    case 0x29: /* ADD REGISTER,REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, REGISTER);
                        break;
                    case 0x2a: /* LD REGISTER,(nnnn) */
                    LD16_RRNN(REGISTERL, REGISTERH);
                        break;
                    case 0x2b: /* DEC REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        REGISTER--;
                        break;
                    case 0x2c: /* INC REGISTERL */
                    INC(REGISTERL);
                        break;
                    case 0x2d: /* DEC REGISTERL */
                    DEC(REGISTERL);
                        break;
                    case 0x2e: /* LD REGISTERL,nn */
                        REGISTERL = readByte(PC++);
                        break;
                    case 0x34: /* INC (REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        contend_read_no_mreq(z80.memptr.w, 1);
                        INC(bytetemp);
                        writeByte(z80.memptr.w, bytetemp);
                    }
                        break;
                    case 0x35: /* DEC (REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        contend_read_no_mreq(z80.memptr.w, 1);
                        DEC(bytetemp);
                        writeByte(z80.memptr.w, bytetemp);
                    }
                        break;
                    case 0x36: /* LD (REGISTER+dd),nn */
                    {
                        libspectrum_byte offset, value;
                        offset = readByte(PC++);
                        value = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, value);
                    }
                        break;
                    case 0x39: /* ADD REGISTER,SP */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, SP);
                        break;
                    case 0x44: /* LD B,REGISTERH */
                        B = REGISTERH;
                        break;
                    case 0x45: /* LD B,REGISTERL */
                        B = REGISTERL;
                        break;
                    case 0x46: /* LD B,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        B = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x4c: /* LD C,REGISTERH */
                        C = REGISTERH;
                        break;
                    case 0x4d: /* LD C,REGISTERL */
                        C = REGISTERL;
                        break;
                    case 0x4e: /* LD C,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        C = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x54: /* LD D,REGISTERH */
                        D = REGISTERH;
                        break;
                    case 0x55: /* LD D,REGISTERL */
                        D = REGISTERL;
                        break;
                    case 0x56: /* LD D,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        D = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x5c: /* LD E,REGISTERH */
                        E = REGISTERH;
                        break;
                    case 0x5d: /* LD E,REGISTERL */
                        E = REGISTERL;
                        break;
                    case 0x5e: /* LD E,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        E = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x60: /* LD REGISTERH,B */
                        REGISTERH = B;
                        break;
                    case 0x61: /* LD REGISTERH,C */
                        REGISTERH = C;
                        break;
                    case 0x62: /* LD REGISTERH,D */
                        REGISTERH = D;
                        break;
                    case 0x63: /* LD REGISTERH,E */
                        REGISTERH = E;
                        break;
                    case 0x64: /* LD REGISTERH,REGISTERH */
                        break;
                    case 0x65: /* LD REGISTERH,REGISTERL */
                        REGISTERH = REGISTERL;
                        break;
                    case 0x66: /* LD H,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        H = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x67: /* LD REGISTERH,A */
                        REGISTERH = A;
                        break;
                    case 0x68: /* LD REGISTERL,B */
                        REGISTERL = B;
                        break;
                    case 0x69: /* LD REGISTERL,C */
                        REGISTERL = C;
                        break;
                    case 0x6a: /* LD REGISTERL,D */
                        REGISTERL = D;
                        break;
                    case 0x6b: /* LD REGISTERL,E */
                        REGISTERL = E;
                        break;
                    case 0x6c: /* LD REGISTERL,REGISTERH */
                        REGISTERL = REGISTERH;
                        break;
                    case 0x6d: /* LD REGISTERL,REGISTERL */
                        break;
                    case 0x6e: /* LD L,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        L = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x6f: /* LD REGISTERL,A */
                        REGISTERL = A;
                        break;
                    case 0x70: /* LD (REGISTER+dd),B */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, B);
                    }
                        break;
                    case 0x71: /* LD (REGISTER+dd),C */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, C);
                    }
                        break;
                    case 0x72: /* LD (REGISTER+dd),D */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, D);
                    }
                        break;
                    case 0x73: /* LD (REGISTER+dd),E */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, E);
                    }
                        break;
                    case 0x74: /* LD (REGISTER+dd),H */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, H);
                    }
                        break;
                    case 0x75: /* LD (REGISTER+dd),L */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, L);
                    }
                        break;
                    case 0x77: /* LD (REGISTER+dd),A */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, A);
                    }
                        break;
                    case 0x7c: /* LD A,REGISTERH */
                        A = REGISTERH;
                        break;
                    case 0x7d: /* LD A,REGISTERL */
                        A = REGISTERL;
                        break;
                    case 0x7e: /* LD A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        A = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x84: /* ADD A,REGISTERH */
                    ADD(REGISTERH);
                        break;
                    case 0x85: /* ADD A,REGISTERL */
                    ADD(REGISTERL);
                        break;
                    case 0x86: /* ADD A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        ADD(bytetemp);
                    }
                        break;
                    case 0x8c: /* ADC A,REGISTERH */
                    ADC(REGISTERH);
                        break;
                    case 0x8d: /* ADC A,REGISTERL */
                    ADC(REGISTERL);
                        break;
                    case 0x8e: /* ADC A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        ADC(bytetemp);
                    }
                        break;
                    case 0x94: /* SUB A,REGISTERH */
                    SUB(REGISTERH);
                        break;
                    case 0x95: /* SUB A,REGISTERL */
                    SUB(REGISTERL);
                        break;
                    case 0x96: /* SUB A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        SUB(bytetemp);
                    }
                        break;
                    case 0x9c: /* SBC A,REGISTERH */
                    SBC(REGISTERH);
                        break;
                    case 0x9d: /* SBC A,REGISTERL */
                    SBC(REGISTERL);
                        break;
                    case 0x9e: /* SBC A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        SBC(bytetemp);
                    }
                        break;
                    case 0xa4: /* AND A,REGISTERH */
                    AND(REGISTERH);
                        break;
                    case 0xa5: /* AND A,REGISTERL */
                    AND(REGISTERL);
                        break;
                    case 0xa6: /* AND A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        AND(bytetemp);
                    }
                        break;
                    case 0xac: /* XOR A,REGISTERH */
                    XOR(REGISTERH);
                        break;
                    case 0xad: /* XOR A,REGISTERL */
                    XOR(REGISTERL);
                        break;
                    case 0xae: /* XOR A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        XOR(bytetemp);
                    }
                        break;
                    case 0xb4: /* OR A,REGISTERH */
                    OR(REGISTERH);
                        break;
                    case 0xb5: /* OR A,REGISTERL */
                    OR(REGISTERL);
                        break;
                    case 0xb6: /* OR A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        OR(bytetemp);
                    }
                        break;
                    case 0xbc: /* CP A,REGISTERH */
                    CP(REGISTERH);
                        break;
                    case 0xbd: /* CP A,REGISTERL */
                    CP(REGISTERL);
                        break;
                    case 0xbe: /* CP A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        CP(bytetemp);
                    }
                        break;
                    case 0xcb: /* shift DDFDCB */
                    {
                        libspectrum_byte opcode3;
                        // Byte 3: Displacement (3 cycles, no R increment)
                        libspectrum_byte displacement = readByte(PC++);

                        // Byte 4: Actual Opcode (4 cycles fetch, but NO R increment on DDCB/FDCB!)
                        contend_read(PC, 4);

                        opcode3 = readByteInternal(PC++);
                        // On real hardware, R is only incremented for the first two bytes (DD/FD and CB)
                        // The 4th byte is a "fake" fetch.

                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) displacement;

#ifdef HAVE_ENOUGH_MEMORY
                        switch (opcode3) {
/* z80_ddfdcb.c Z80 {DD,FD}CBxx opcodes
   Copyright (c) 1999-2003 Philip Kendall

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

   Author contact information:

   E-mail: philip-fuse@shadowmagic.org.uk

*/

/* NB: this file is autogenerated by './z80/z80.pl' from 'opcodes_ddfdcb.dat',
   and included in 'z80_ops.c' */

                            case 0x00: /* LD B,RLC (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x01: /* LD C,RLC (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x02: /* LD D,RLC (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x03: /* LD E,RLC (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x04: /* LD H,RLC (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x05: /* LD L,RLC (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x06: /* RLC (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x07: /* LD A,RLC (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x08: /* LD B,RRC (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x09: /* LD C,RRC (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x0a: /* LD D,RRC (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x0b: /* LD E,RRC (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x0c: /* LD H,RRC (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x0d: /* LD L,RRC (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x0e: /* RRC (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x0f: /* LD A,RRC (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x10: /* LD B,RL (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x11: /* LD C,RL (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x12: /* LD D,RL (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x13: /* LD E,RL (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x14: /* LD H,RL (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x15: /* LD L,RL (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x16: /* RL (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x17: /* LD A,RL (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x18: /* LD B,RR (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x19: /* LD C,RR (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x1a: /* LD D,RR (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x1b: /* LD E,RR (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x1c: /* LD H,RR (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x1d: /* LD L,RR (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x1e: /* RR (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x1f: /* LD A,RR (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x20: /* LD B,SLA (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x21: /* LD C,SLA (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x22: /* LD D,SLA (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x23: /* LD E,SLA (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x24: /* LD H,SLA (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x25: /* LD L,SLA (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x26: /* SLA (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x27: /* LD A,SLA (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x28: /* LD B,SRA (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x29: /* LD C,SRA (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x2a: /* LD D,SRA (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x2b: /* LD E,SRA (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x2c: /* LD H,SRA (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x2d: /* LD L,SRA (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x2e: /* SRA (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x2f: /* LD A,SRA (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x30: /* LD B,SLL (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x31: /* LD C,SLL (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x32: /* LD D,SLL (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x33: /* LD E,SLL (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x34: /* LD H,SLL (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x35: /* LD L,SLL (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x36: /* SLL (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x37: /* LD A,SLL (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x38: /* LD B,SRL (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x39: /* LD C,SRL (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x3a: /* LD D,SRL (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x3b: /* LD E,SRL (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x3c: /* LD H,SRL (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x3d: /* LD L,SRL (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x3e: /* SRL (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x3f: /* LD A,SRL (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x40:
                            case 0x41:
                            case 0x42:
                            case 0x43:
                            case 0x44:
                            case 0x45:
                            case 0x46:
                            case 0x47: /* BIT 0,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(0, bytetemp);
                            }
                                break;
                            case 0x48:
                            case 0x49:
                            case 0x4a:
                            case 0x4b:
                            case 0x4c:
                            case 0x4d:
                            case 0x4e:
                            case 0x4f: /* BIT 1,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(1, bytetemp);
                            }
                                break;
                            case 0x50:
                            case 0x51:
                            case 0x52:
                            case 0x53:
                            case 0x54:
                            case 0x55:
                            case 0x56:
                            case 0x57: /* BIT 2,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(2, bytetemp);
                            }
                                break;
                            case 0x58:
                            case 0x59:
                            case 0x5a:
                            case 0x5b:
                            case 0x5c:
                            case 0x5d:
                            case 0x5e:
                            case 0x5f: /* BIT 3,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(3, bytetemp);
                            }
                                break;
                            case 0x60:
                            case 0x61:
                            case 0x62:
                            case 0x63:
                            case 0x64:
                            case 0x65:
                            case 0x66:
                            case 0x67: /* BIT 4,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(4, bytetemp);
                            }
                                break;
                            case 0x68:
                            case 0x69:
                            case 0x6a:
                            case 0x6b:
                            case 0x6c:
                            case 0x6d:
                            case 0x6e:
                            case 0x6f: /* BIT 5,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(5, bytetemp);
                            }
                                break;
                            case 0x70:
                            case 0x71:
                            case 0x72:
                            case 0x73:
                            case 0x74:
                            case 0x75:
                            case 0x76:
                            case 0x77: /* BIT 6,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(6, bytetemp);
                            }
                                break;
                            case 0x78:
                            case 0x79:
                            case 0x7a:
                            case 0x7b:
                            case 0x7c:
                            case 0x7d:
                            case 0x7e:
                            case 0x7f: /* BIT 7,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(7, bytetemp);
                            }
                                break;
                            case 0x80: /* LD B,RES 0,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x81: /* LD C,RES 0,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x82: /* LD D,RES 0,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x83: /* LD E,RES 0,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x84: /* LD H,RES 0,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x85: /* LD L,RES 0,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x86: /* RES 0,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xfe);
                            }
                                break;
                            case 0x87: /* LD A,RES 0,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x88: /* LD B,RES 1,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x89: /* LD C,RES 1,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x8a: /* LD D,RES 1,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x8b: /* LD E,RES 1,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x8c: /* LD H,RES 1,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x8d: /* LD L,RES 1,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x8e: /* RES 1,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xfd);
                            }
                                break;
                            case 0x8f: /* LD A,RES 1,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x90: /* LD B,RES 2,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x91: /* LD C,RES 2,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x92: /* LD D,RES 2,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x93: /* LD E,RES 2,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x94: /* LD H,RES 2,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x95: /* LD L,RES 2,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x96: /* RES 2,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xfb);
                            }
                                break;
                            case 0x97: /* LD A,RES 2,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x98: /* LD B,RES 3,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x99: /* LD C,RES 3,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x9a: /* LD D,RES 3,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x9b: /* LD E,RES 3,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x9c: /* LD H,RES 3,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x9d: /* LD L,RES 3,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x9e: /* RES 3,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xf7);
                            }
                                break;
                            case 0x9f: /* LD A,RES 3,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xa0: /* LD B,RES 4,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xa1: /* LD C,RES 4,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xa2: /* LD D,RES 4,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xa3: /* LD E,RES 4,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xa4: /* LD H,RES 4,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xa5: /* LD L,RES 4,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xa6: /* RES 4,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xef);
                            }
                                break;
                            case 0xa7: /* LD A,RES 4,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xa8: /* LD B,RES 5,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xa9: /* LD C,RES 5,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xaa: /* LD D,RES 5,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xab: /* LD E,RES 5,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xac: /* LD H,RES 5,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xad: /* LD L,RES 5,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xae: /* RES 5,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xdf);
                            }
                                break;
                            case 0xaf: /* LD A,RES 5,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xb0: /* LD B,RES 6,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xb1: /* LD C,RES 6,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xb2: /* LD D,RES 6,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xb3: /* LD E,RES 6,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xb4: /* LD H,RES 6,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xb5: /* LD L,RES 6,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xb6: /* RES 6,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xbf);
                            }
                                break;
                            case 0xb7: /* LD A,RES 6,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xb8: /* LD B,RES 7,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xb9: /* LD C,RES 7,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xba: /* LD D,RES 7,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xbb: /* LD E,RES 7,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xbc: /* LD H,RES 7,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xbd: /* LD L,RES 7,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xbe: /* RES 7,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0x7f);
                            }
                                break;
                            case 0xbf: /* LD A,RES 7,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xc0: /* LD B,SET 0,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xc1: /* LD C,SET 0,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xc2: /* LD D,SET 0,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xc3: /* LD E,SET 0,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xc4: /* LD H,SET 0,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xc5: /* LD L,SET 0,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xc6: /* SET 0,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x01);
                            }
                                break;
                            case 0xc7: /* LD A,SET 0,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xc8: /* LD B,SET 1,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xc9: /* LD C,SET 1,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xca: /* LD D,SET 1,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xcb: /* LD E,SET 1,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xcc: /* LD H,SET 1,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xcd: /* LD L,SET 1,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xce: /* SET 1,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x02);
                            }
                                break;
                            case 0xcf: /* LD A,SET 1,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xd0: /* LD B,SET 2,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xd1: /* LD C,SET 2,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xd2: /* LD D,SET 2,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xd3: /* LD E,SET 2,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xd4: /* LD H,SET 2,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xd5: /* LD L,SET 2,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xd6: /* SET 2,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x04);
                            }
                                break;
                            case 0xd7: /* LD A,SET 2,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xd8: /* LD B,SET 3,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xd9: /* LD C,SET 3,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xda: /* LD D,SET 3,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xdb: /* LD E,SET 3,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xdc: /* LD H,SET 3,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xdd: /* LD L,SET 3,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xde: /* SET 3,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x08);
                            }
                                break;
                            case 0xdf: /* LD A,SET 3,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xe0: /* LD B,SET 4,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xe1: /* LD C,SET 4,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xe2: /* LD D,SET 4,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xe3: /* LD E,SET 4,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xe4: /* LD H,SET 4,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xe5: /* LD L,SET 4,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xe6: /* SET 4,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x10);
                            }
                                break;
                            case 0xe7: /* LD A,SET 4,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xe8: /* LD B,SET 5,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xe9: /* LD C,SET 5,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xea: /* LD D,SET 5,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xeb: /* LD E,SET 5,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xec: /* LD H,SET 5,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xed: /* LD L,SET 5,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xee: /* SET 5,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x20);
                            }
                                break;
                            case 0xef: /* LD A,SET 5,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xf0: /* LD B,SET 6,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xf1: /* LD C,SET 6,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xf2: /* LD D,SET 6,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xf3: /* LD E,SET 6,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xf4: /* LD H,SET 6,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xf5: /* LD L,SET 6,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xf6: /* SET 6,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x40);
                            }
                                break;
                            case 0xf7: /* LD A,SET 6,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xf8: /* LD B,SET 7,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xf9: /* LD C,SET 7,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xfa: /* LD D,SET 7,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xfb: /* LD E,SET 7,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xfc: /* LD H,SET 7,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xfd: /* LD L,SET 7,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xfe: /* SET 7,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x80);
                            }
                                break;
                            case 0xff: /* LD A,SET 7,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                        }
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                        z80_ddfdcbxx(opcode3);
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
                    }
                        break;
                    case 0xe1: /* POP REGISTER */
                    POP16(REGISTERL, REGISTERH);
                        break;
                    case 0xe3: /* EX (SP),REGISTER */
                    {
                        libspectrum_byte bytetempl, bytetemph;
                        bytetempl = readByte(SP);
                        bytetemph = readByte(SP + 1);
                        contend_read_no_mreq(SP + 1, 1);
                        writeByte(SP + 1, REGISTERH);
                        writeByte(SP, REGISTERL);
                        contend_write_no_mreq(SP, 1);
                        contend_write_no_mreq(SP, 1);
                        REGISTERL = z80.memptr.b.l = bytetempl;
                        REGISTERH = z80.memptr.b.h = bytetemph;
                    }
                        break;
                    case 0xe5: /* PUSH REGISTER */
                        contend_read_no_mreq(IR, 1);
                        PUSH16(REGISTERL, REGISTERH);
                        break;
                    case 0xe9:     /* JP REGISTER */
                        PC = REGISTER; /* NB: NOT INDIRECT! */
                        break;
                    case 0xf9: /* LD SP,REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        SP = REGISTER;
                        break;
                    default: /* Instruction did not involve H or L, so backtrack
            one instruction and parse again */
                        PC--;
                        R--;
                        opcode = opcode2;
#ifdef HAVE_ENOUGH_MEMORY
                        goto end_opcode;
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                        return 1;
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
#undef REGISTERH
#undef REGISTERL
#undef REGISTER
                }
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                                                                                                                                        if (z80_ddxx(opcode2))
    goto end_opcode;
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
            }
                break;
            case 0xde: /* SBC A,nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                SBC(bytetemp);
            }
                break;
            case 0xdf: /* RST 18 */
                contend_read_no_mreq(IR, 1);
                RST(0x18);
                break;
            case 0xe0: /* RET PO */
                contend_read_no_mreq(IR, 1);
                if (!(F & FLAG_P)) {
                    RET();
                }
                break;
            case 0xe1: /* POP HL */
            POP16(L, H);
                break;
            case 0xe2: /* JP PO,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_P)) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xe3: /* EX (SP),HL */
            {
                libspectrum_byte bytetempl, bytetemph;
                bytetempl = readByte(SP);
                bytetemph = readByte(SP + 1);
                contend_read_no_mreq(SP + 1, 1);
                writeByte(SP + 1, H);
                writeByte(SP, L);
                contend_write_no_mreq(SP, 1);
                contend_write_no_mreq(SP, 1);
                L = z80.memptr.b.l = bytetempl;
                H = z80.memptr.b.h = bytetemph;
            }
                break;
            case 0xe4: /* CALL PO,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_P)) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xe5: /* PUSH HL */
                contend_read_no_mreq(IR, 1);
                PUSH16(L, H);
                break;
            case 0xe6: /* AND nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                AND(bytetemp);
            }
                break;
            case 0xe7: /* RST 20 */
                contend_read_no_mreq(IR, 1);
                RST(0x20);
                break;
            case 0xe8: /* RET PE */
                contend_read_no_mreq(IR, 1);
                if (F & FLAG_P) {
                    RET();
                }
                break;
            case 0xe9: /* JP HL */
                PC = HL;   /* NB: NOT INDIRECT! */
                break;
            case 0xea: /* JP PE,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_P) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xeb: /* EX DE,HL */
            {
                libspectrum_word wordtemp = DE;
                DE = HL;
                HL = wordtemp;
            }
                break;
            case 0xec: /* CALL PE,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_P) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xed: /* shift ED */
            {
                contend_read(PC, 4);
                current_opcode2 = readByteInternal(PC);
                PC++;
                if (!z80.halted) {
                    R = (R & 0x80) | ((R + 1) & 0x7f);
                }
#ifdef HAVE_ENOUGH_MEMORY
                switch (current_opcode2) {
/* z80_ed.c: Z80 CBxx opcodes
   Copyright (c) 1999-2003 Philip Kendall

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

   Author contact information:

   E-mail: philip-fuse@shadowmagic.org.uk

*/

/* NB: this file is autogenerated by './z80/z80.pl' from 'opcodes_ed.dat',
   and included in 'z80_ops.c' */

                    case 0x40: /* IN B,(C) */
                    Z80_IN(B, BC);
                        break;
                    case 0x41: /* OUT (C),B */
                        writeport(BC, B);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x42: /* SBC HL,BC */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        SBC16(BC);
                        break;
                    case 0x43: /* LD (nnnn),BC */
                    LD16_NNRR(C, B);
                        break;
                    case 0x44:
                    case 0x4c:
                    case 0x54:
                    case 0x5c:
                    case 0x64:
                    case 0x6c:
                    case 0x74:
                    case 0x7c: /* NEG */
                    {
                        libspectrum_byte bytetemp = A;
                        A = 0;
                        SUB(bytetemp);
                    }
                        break;
                    case 0x45:
                    case 0x4d:
                    case 0x55:
                    case 0x5d:
                    case 0x65:
                    case 0x6d:
                    case 0x75:
                    case 0x7d: /* RETN */
                        IFF1 = IFF2;
                        RET();
                        z80_retn();
                        break;
                    case 0x46:
                    case 0x4e:
                    case 0x66:
                    case 0x6e: /* IM 0 */
                        IM = 0;
                        break;
                    case 0x47: /* LD I,A */
                        contend_read_no_mreq(IR, 1);
                        I = A;
                        break;
                    case 0x48: /* IN C,(C) */
                    Z80_IN(C, BC);
                        break;
                    case 0x49: /* OUT (C),C */
                        writeport(BC, C);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x4a: /* ADC HL,BC */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADC16(BC);
                        break;
                    case 0x4b: /* LD BC,(nnnn) */
                    LD16_RRNN(C, B);
                        break;
                    case 0x4f: /* LD R,A */
                        contend_read_no_mreq(IR, 1);
/* Keep the RZX instruction counter right */
// rzx_instructions_offset += ( R - A );
                        R = R7 = A;
                        break;
                    case 0x50: /* IN D,(C) */
                    Z80_IN(D, BC);
                        break;
                    case 0x51: /* OUT (C),D */
                        writeport(BC, D);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x52: /* SBC HL,DE */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        SBC16(DE);
                        break;
                    case 0x53: /* LD (nnnn),DE */
                    LD16_NNRR(E, D);
                        break;
                    case 0x56:
                    case 0x76: /* IM 1 */
                        IM = 1;
                        break;
                    case 0x57: /* LD A,I */
                        contend_read_no_mreq(IR, 1);
                        A = I;
                        F = (F & FLAG_C) | sz53_table[A] | (IFF2 ? FLAG_V : 0);
                        Q = F;
                        z80.iff2_read = 1;
// event_add(tStates, z80_nmos_iff2_event);
                        break;
                    case 0x58: /* IN E,(C) */
                    Z80_IN(E, BC);
                        break;
                    case 0x59: /* OUT (C),E */
                        writeport(BC, E);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x5a: /* ADC HL,DE */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADC16(DE);
                        break;
                    case 0x5b: /* LD DE,(nnnn) */
                    LD16_RRNN(E, D);
                        break;
                    case 0x5e:
                    case 0x7e: /* IM 2 */
                        IM = 2;
                        break;
                    case 0x5f: /* LD A,R */
                        contend_read_no_mreq(IR, 1);
                        A = (libspectrum_byte) ((R & 0x7f) | (R7 & 0x80));
                        F = (F & FLAG_C) | sz53_table[A] | (IFF2 ? FLAG_V : 0);
                        Q = F;
                        z80.iff2_read = 1;
                        break;
                    case 0x60: /* IN H,(C) */
                    Z80_IN(H, BC);
                        break;
                    case 0x61: /* OUT (C),H */
                        writeport(BC, H);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x62: /* SBC HL,HL */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        SBC16(HL);
                        break;
                    case 0x63: /* LD (nnnn),HL */
                    LD16_NNRR(L, H);
                        break;
                    case 0x67: /* RRD */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, (A << 4) | (bytetemp >> 4));
                        A = (A & 0xf0) | (bytetemp & 0x0f);
                        F = (F & FLAG_C) | sz53p_table[A];
                        Q = F;
                        z80.memptr.w = HL + 1;
                    }
                        break;
                    case 0x68: /* IN L,(C) */
                    Z80_IN(L, BC);
                        break;
                    case 0x69: /* OUT (C),L */
                        writeport(BC, L);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x6a: /* ADC HL,HL */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADC16(HL);
                        break;
                    case 0x6b: /* LD HL,(nnnn) */
                    LD16_RRNN(L, H);
                        break;
                    case 0x6f: /* RLD */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        writeByte(HL, (bytetemp << 4) | (A & 0x0f));
                        A = (A & 0xf0) | (bytetemp >> 4);
                        F = (F & FLAG_C) | sz53p_table[A];
                        Q = F;
                        z80.memptr.w = HL + 1;
                    }
                        break;
                    case 0x70: /* IN F,(C) */
                    {
                        libspectrum_byte bytetemp;
                        Z80_IN(bytetemp, BC);
                    }
                        break;
                    case 0x71: /* OUT (C),0 */
                        writeport(BC, IS_CMOS ? 0xff : 0);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x72: /* SBC HL,SP */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        SBC16(SP);
                        break;
                    case 0x73: /* LD (nnnn),SP */
                    LD16_NNRR(SPL, SPH);
                        break;
                    case 0x78: /* IN A,(C) */
                    Z80_IN(A, BC);
                        break;
                    case 0x79: /* OUT (C),A */
                        writeport(BC, A);
                        z80.memptr.w = BC + 1;
                        break;
                    case 0x7a: /* ADC HL,SP */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADC16(SP);
                        break;
                    case 0x7b: /* LD SP,(nnnn) */
                    LD16_RRNN(SPL, SPH);
                        break;
                    case 0xa0: /* LDI */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        BC--;
                        writeByte(DE, bytetemp);
                        contend_write_no_mreq(DE, 1);
                        contend_write_no_mreq(DE, 1);
                        DE++;
                        HL++;
                        bytetemp += A;
                        F = (F & (FLAG_C | FLAG_Z | FLAG_S)) | (BC ? FLAG_V : 0) |
                            (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                    }
                        break;
                    case 0xa1: /* CPI */
                    {
                        libspectrum_byte value = readByte(HL), bytetemp = A - value,
                                lookup = ((A & 0x08) >> 3) | (((value) & 0x08) >> 2) |
                                         ((bytetemp & 0x08) >> 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        HL++;
                        BC--;
                        F = (F & FLAG_C) | (BC ? (FLAG_V | FLAG_N) : FLAG_N) |
                            halfcarry_sub_table[lookup] | (bytetemp ? 0 : FLAG_Z) |
                            (bytetemp & FLAG_S);
                        if (F & FLAG_H)
                            bytetemp--;
                        F |= (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                        z80.memptr.w++;
                    }
                        break;
                    case 0xa2: /* INI */
                    {
                        libspectrum_byte initemp, initemp2;

                        contend_read_no_mreq(IR, 1);
                        initemp = readport(BC);
                        writeByte(HL, initemp);

                        z80.memptr.w = BC + 1;
                        B--;
                        HL++;
                        initemp2 = initemp + C + 1;
                        F = (initemp & 0x80 ? FLAG_N : 0) |
                            ((initemp2 < initemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(initemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;
                    }
                        break;
                    case 0xa3: /* OUTI */
                    {
                        libspectrum_byte outitemp, outitemp2;

                        contend_read_no_mreq(IR, 1);
                        outitemp = readByte(HL);
                        B--; /* This does happen first, despite what the specs say */
                        z80.memptr.w = BC + 1;
                        writeport(BC, outitemp);

                        HL++;
                        outitemp2 = outitemp + L;
                        F = (outitemp & 0x80 ? FLAG_N : 0) |
                            ((outitemp2 < outitemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(outitemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;
                    }
                        break;
                    case 0xa8: /* LDD */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        BC--;
                        writeByte(DE, bytetemp);
                        contend_write_no_mreq(DE, 1);
                        contend_write_no_mreq(DE, 1);
                        DE--;
                        HL--;
                        bytetemp += A;
                        F = (F & (FLAG_C | FLAG_Z | FLAG_S)) | (BC ? FLAG_V : 0) |
                            (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                    }
                        break;
                    case 0xa9: /* CPD */
                    {
                        libspectrum_byte value = readByte(HL), bytetemp = A - value,
                                lookup = ((A & 0x08) >> 3) | (((value) & 0x08) >> 2) |
                                         ((bytetemp & 0x08) >> 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        HL--;
                        BC--;
                        F = (F & FLAG_C) | (BC ? (FLAG_V | FLAG_N) : FLAG_N) |
                            halfcarry_sub_table[lookup] | (bytetemp ? 0 : FLAG_Z) |
                            (bytetemp & FLAG_S);
                        if (F & FLAG_H)
                            bytetemp--;
                        F |= (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                        z80.memptr.w--;
                    }
                        break;
                    case 0xaa: /* IND */
                    {
                        libspectrum_byte initemp, initemp2;

                        contend_read_no_mreq(IR, 1);
                        initemp = readport(BC);
                        writeByte(HL, initemp);

                        z80.memptr.w = BC - 1;
                        B--;
                        HL--;
                        initemp2 = initemp + C - 1;
                        F = (initemp & 0x80 ? FLAG_N : 0) |
                            ((initemp2 < initemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(initemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;
                    }
                        break;
                    case 0xab: /* OUTD */
                    {
                        libspectrum_byte outitemp, outitemp2;

                        contend_read_no_mreq(IR, 1);
                        outitemp = readByte(HL);
                        B--; /* This does happen first, despite what the specs say */
                        z80.memptr.w = BC - 1;
                        writeport(BC, outitemp);

                        HL--;
                        outitemp2 = outitemp + L;
                        F = (outitemp & 0x80 ? FLAG_N : 0) |
                            ((outitemp2 < outitemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(outitemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;
                    }
                        break;
                    case 0xb0: /* LDIR */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        writeByte(DE, bytetemp);
                        contend_write_no_mreq(DE, 1);
                        contend_write_no_mreq(DE, 1);
                        BC--;
                        bytetemp += A;
                        F = (F & (FLAG_C | FLAG_Z | FLAG_S)) | (BC ? FLAG_V : 0) |
                            (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                        if (BC) {
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            PC -= 2;
                            z80.memptr.w = PC + 1;
                        }
                        HL++;
                        DE++;
                    }
                        break;
                    case 0xb1: /* CPIR */
                    {
                        libspectrum_byte value = readByte(HL), bytetemp = A - value,
                                lookup = ((A & 0x08) >> 3) | (((value) & 0x08) >> 2) |
                                         ((bytetemp & 0x08) >> 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        BC--;
                        F = (F & FLAG_C) | (BC ? (FLAG_V | FLAG_N) : FLAG_N) |
                            halfcarry_sub_table[lookup] | (bytetemp ? 0 : FLAG_Z) |
                            (bytetemp & FLAG_S);
                        if (F & FLAG_H)
                            bytetemp--;
                        F |= (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                        if ((F & (FLAG_V | FLAG_Z)) == FLAG_V) {
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            PC -= 2;
                            z80.memptr.w = PC + 1;
                        } else {
                            z80.memptr.w++;
                        }
                        HL++;
                    }
                        break;
                    case 0xb2: /* INIR */
                    {
                        libspectrum_byte initemp, initemp2;

                        contend_read_no_mreq(IR, 1);
                        initemp = readport(BC);
                        writeByte(HL, initemp);

                        z80.memptr.w = BC + 1;
                        B--;
                        initemp2 = initemp + C + 1;
                        F = (initemp & 0x80 ? FLAG_N : 0) |
                            ((initemp2 < initemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(initemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;

                        if (B) {
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            PC -= 2;
                        }
                        HL++;
                    }
                        break;
                    case 0xb3: /* OTIR */
                    {
                        libspectrum_byte outitemp, outitemp2;

                        contend_read_no_mreq(IR, 1);
                        outitemp = readByte(HL);
                        B--; /* This does happen first, despite what the specs say */
                        z80.memptr.w = BC + 1;
                        writeport(BC, outitemp);

                        HL++;
                        outitemp2 = outitemp + L;
                        F = (outitemp & 0x80 ? FLAG_N : 0) |
                            ((outitemp2 < outitemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(outitemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;

                        if (B) {
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            PC -= 2;
                        }
                    }
                        break;
                    case 0xb8: /* LDDR */
                    {
                        libspectrum_byte bytetemp = readByte(HL);
                        writeByte(DE, bytetemp);
                        contend_write_no_mreq(DE, 1);
                        contend_write_no_mreq(DE, 1);
                        BC--;
                        bytetemp += A;
                        F = (F & (FLAG_C | FLAG_Z | FLAG_S)) | (BC ? FLAG_V : 0) |
                            (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                        if (BC) {
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            contend_write_no_mreq(DE, 1);
                            PC -= 2;
                            z80.memptr.w = PC + 1;
                        }
                        HL--;
                        DE--;
                    }
                        break;
                    case 0xb9: /* CPDR */
                    {
                        libspectrum_byte value = readByte(HL), bytetemp = A - value,
                                lookup = ((A & 0x08) >> 3) | (((value) & 0x08) >> 2) |
                                         ((bytetemp & 0x08) >> 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        contend_read_no_mreq(HL, 1);
                        BC--;
                        F = (F & FLAG_C) | (BC ? (FLAG_V | FLAG_N) : FLAG_N) |
                            halfcarry_sub_table[lookup] | (bytetemp ? 0 : FLAG_Z) |
                            (bytetemp & FLAG_S);
                        if (F & FLAG_H)
                            bytetemp--;
                        F |= (bytetemp & FLAG_3) | ((bytetemp & 0x02) ? FLAG_5 : 0);
                        Q = F;
                        if ((F & (FLAG_V | FLAG_Z)) == FLAG_V) {
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            contend_read_no_mreq(HL, 1);
                            PC -= 2;
                            z80.memptr.w = PC + 1;
                        } else {
                            z80.memptr.w--;
                        }
                        HL--;
                    }
                        break;
                    case 0xba: /* INDR */
                    {
                        libspectrum_byte initemp, initemp2;

                        contend_read_no_mreq(IR, 1);
                        initemp = readport(BC);
                        writeByte(HL, initemp);

                        z80.memptr.w = BC - 1;
                        B--;
                        initemp2 = initemp + C - 1;
                        F = (initemp & 0x80 ? FLAG_N : 0) |
                            ((initemp2 < initemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(initemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;

                        if (B) {
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            contend_write_no_mreq(HL, 1);
                            PC -= 2;
                        }
                        HL--;
                    }
                        break;
                    case 0xbb: /* OTDR */
                    {
                        libspectrum_byte outitemp, outitemp2;

                        contend_read_no_mreq(IR, 1);
                        outitemp = readByte(HL);
                        B--; /* This does happen first, despite what the specs say */
                        z80.memptr.w = BC - 1;
                        writeport(BC, outitemp);

                        HL--;
                        outitemp2 = outitemp + L;
                        F = (outitemp & 0x80 ? FLAG_N : 0) |
                            ((outitemp2 < outitemp) ? FLAG_H | FLAG_C : 0) |
                            (parity_table[(outitemp2 & 0x07) ^ B] ? FLAG_P : 0) | sz53_table[B];
                        Q = F;

                        if (B) {
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            contend_read_no_mreq(BC, 1);
                            PC -= 2;
                        }
                    }
                        break;
                    case 0xfb: /* slttrap */
/// slt_trap( HL, A );
                        break;
                    default: /* All other opcodes are NOPD */
                        break;
                }
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                                                                                                                                        if (z80_edxx(opcode2))
    goto end_opcode;
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
            }
                break;
            case 0xee: /* XOR A,nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                XOR(bytetemp);
            }
                break;
            case 0xef: /* RST 28 */
                contend_read_no_mreq(IR, 1);
                RST(0x28);
                break;
            case 0xf0: /* RET P */
                contend_read_no_mreq(IR, 1);
                if (!(F & FLAG_S)) {
                    RET();
                }
                break;
            case 0xf1: /* POP AF */
            POP16(F, A);
                break;
            case 0xf2: /* JP P,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_S)) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xf3: /* DI */
                IFF1 = IFF2 = 0;
                break;
            case 0xf4: /* CALL P,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (!(F & FLAG_S)) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xf5: /* PUSH AF */
                contend_read_no_mreq(IR, 1);
                PUSH16(F, A);
                break;
            case 0xf6: /* OR nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                OR(bytetemp);
            }
                break;
            case 0xf7: /* RST 30 */
                contend_read_no_mreq(IR, 1);
                RST(0x30);
                break;
            case 0xf8: /* RET M */
                contend_read_no_mreq(IR, 1);
                if (F & FLAG_S) {
                    RET();
                }
                break;
            case 0xf9: /* LD SP,HL */
                contend_read_no_mreq(IR, 1);
                contend_read_no_mreq(IR, 1);
                SP = HL;
                break;
            case 0xfa: /* JP M,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_S) {
                    JP();
                } else {
                    PC++;
                }
                break;
            case 0xfb: /* EI */
/* Interrupts are not accepted immediately after an EI, but are
   accepted after the next instruction */
                IFF1 = IFF2 = 1;
                z80.interrupts_enabled_at = tStates;
                break;
            case 0xfc: /* CALL M,nnnn */
                z80.memptr.b.l = readByte(PC++);
                z80.memptr.b.h = readByte(PC);
                if (F & FLAG_S) {
                    CALL();
                } else {
                    PC++;
                }
                break;
            case 0xfd: /* shift FD */
            {
                libspectrum_byte opcode2;
                contend_read(PC, 4);
                opcode2 = readByteInternal(PC);
                PC++;
                if (!z80.halted) {
                    R = (R & 0x80) | ((R + 1) & 0x7f);
                }
#ifdef HAVE_ENOUGH_MEMORY
                switch (opcode2) {
#define REGISTER IY
#define REGISTERL IYL
#define REGISTERH IYH
/* z80_ddfd.c Z80 {DD,FD}xx opcodes
   Copyright (c) 1999-2003 Philip Kendall

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

   Author contact information:

   E-mail: philip-fuse@shadowmagic.org.uk

*/

/* NB: this file is autogenerated by './z80/z80.pl' from 'opcodes_ddfd.dat',
   and included in 'z80_ops.c' */

                    case 0x09: /* ADD REGISTER,BC */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, BC);
                        break;
                    case 0x19: /* ADD REGISTER,DE */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, DE);
                        break;
                    case 0x21: /* LD REGISTER,nnnn */
                        REGISTERL = readByte(PC++);
                        REGISTERH = readByte(PC++);
                        break;
                    case 0x22: /* LD (nnnn),REGISTER */
                    LD16_NNRR(REGISTERL, REGISTERH);
                        break;
                    case 0x23: /* INC REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        REGISTER++;
                        break;
                    case 0x24: /* INC REGISTERH */
                    INC(REGISTERH);
                        break;
                    case 0x25: /* DEC REGISTERH */
                    DEC(REGISTERH);
                        break;
                    case 0x26: /* LD REGISTERH,nn */
                        REGISTERH = readByte(PC++);
                        break;
                    case 0x29: /* ADD REGISTER,REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, REGISTER);
                        break;
                    case 0x2a: /* LD REGISTER,(nnnn) */
                    LD16_RRNN(REGISTERL, REGISTERH);
                        break;
                    case 0x2b: /* DEC REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        REGISTER--;
                        break;
                    case 0x2c: /* INC REGISTERL */
                    INC(REGISTERL);
                        break;
                    case 0x2d: /* DEC REGISTERL */
                    DEC(REGISTERL);
                        break;
                    case 0x2e: /* LD REGISTERL,nn */
                        REGISTERL = readByte(PC++);
                        break;
                    case 0x34: /* INC (REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        contend_read_no_mreq(z80.memptr.w, 1);
                        INC(bytetemp);
                        writeByte(z80.memptr.w, bytetemp);
                    }
                        break;
                    case 0x35: /* DEC (REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        contend_read_no_mreq(z80.memptr.w, 1);
                        DEC(bytetemp);
                        writeByte(z80.memptr.w, bytetemp);
                    }
                        break;
                    case 0x36: /* LD (REGISTER+dd),nn */
                    {
                        libspectrum_byte offset, value;
                        offset = readByte(PC++);
                        value = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, value);
                    }
                        break;
                    case 0x39: /* ADD REGISTER,SP */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        ADD16(REGISTER, SP);
                        break;
                    case 0x44: /* LD B,REGISTERH */
                        B = REGISTERH;
                        break;
                    case 0x45: /* LD B,REGISTERL */
                        B = REGISTERL;
                        break;
                    case 0x46: /* LD B,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        B = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x4c: /* LD C,REGISTERH */
                        C = REGISTERH;
                        break;
                    case 0x4d: /* LD C,REGISTERL */
                        C = REGISTERL;
                        break;
                    case 0x4e: /* LD C,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        C = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x54: /* LD D,REGISTERH */
                        D = REGISTERH;
                        break;
                    case 0x55: /* LD D,REGISTERL */
                        D = REGISTERL;
                        break;
                    case 0x56: /* LD D,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        D = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x5c: /* LD E,REGISTERH */
                        E = REGISTERH;
                        break;
                    case 0x5d: /* LD E,REGISTERL */
                        E = REGISTERL;
                        break;
                    case 0x5e: /* LD E,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        E = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x60: /* LD REGISTERH,B */
                        REGISTERH = B;
                        break;
                    case 0x61: /* LD REGISTERH,C */
                        REGISTERH = C;
                        break;
                    case 0x62: /* LD REGISTERH,D */
                        REGISTERH = D;
                        break;
                    case 0x63: /* LD REGISTERH,E */
                        REGISTERH = E;
                        break;
                    case 0x64: /* LD REGISTERH,REGISTERH */
                        break;
                    case 0x65: /* LD REGISTERH,REGISTERL */
                        REGISTERH = REGISTERL;
                        break;
                    case 0x66: /* LD H,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        H = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x67: /* LD REGISTERH,A */
                        REGISTERH = A;
                        break;
                    case 0x68: /* LD REGISTERL,B */
                        REGISTERL = B;
                        break;
                    case 0x69: /* LD REGISTERL,C */
                        REGISTERL = C;
                        break;
                    case 0x6a: /* LD REGISTERL,D */
                        REGISTERL = D;
                        break;
                    case 0x6b: /* LD REGISTERL,E */
                        REGISTERL = E;
                        break;
                    case 0x6c: /* LD REGISTERL,REGISTERH */
                        REGISTERL = REGISTERH;
                        break;
                    case 0x6d: /* LD REGISTERL,REGISTERL */
                        break;
                    case 0x6e: /* LD L,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        L = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x6f: /* LD REGISTERL,A */
                        REGISTERL = A;
                        break;
                    case 0x70: /* LD (REGISTER+dd),B */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, B);
                    }
                        break;
                    case 0x71: /* LD (REGISTER+dd),C */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, C);
                    }
                        break;
                    case 0x72: /* LD (REGISTER+dd),D */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, D);
                    }
                        break;
                    case 0x73: /* LD (REGISTER+dd),E */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, E);
                    }
                        break;
                    case 0x74: /* LD (REGISTER+dd),H */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, H);
                    }
                        break;
                    case 0x75: /* LD (REGISTER+dd),L */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, L);
                    }
                        break;
                    case 0x77: /* LD (REGISTER+dd),A */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        writeByte(z80.memptr.w, A);
                    }
                        break;
                    case 0x7c: /* LD A,REGISTERH */
                        A = REGISTERH;
                        break;
                    case 0x7d: /* LD A,REGISTERL */
                        A = REGISTERL;
                        break;
                    case 0x7e: /* LD A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        A = readByte(z80.memptr.w);
                    }
                        break;
                    case 0x84: /* ADD A,REGISTERH */
                    ADD(REGISTERH);
                        break;
                    case 0x85: /* ADD A,REGISTERL */
                    ADD(REGISTERL);
                        break;
                    case 0x86: /* ADD A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        ADD(bytetemp);
                    }
                        break;
                    case 0x8c: /* ADC A,REGISTERH */
                    ADC(REGISTERH);
                        break;
                    case 0x8d: /* ADC A,REGISTERL */
                    ADC(REGISTERL);
                        break;
                    case 0x8e: /* ADC A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        ADC(bytetemp);
                    }
                        break;
                    case 0x94: /* SUB A,REGISTERH */
                    SUB(REGISTERH);
                        break;
                    case 0x95: /* SUB A,REGISTERL */
                    SUB(REGISTERL);
                        break;
                    case 0x96: /* SUB A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        SUB(bytetemp);
                    }
                        break;
                    case 0x9c: /* SBC A,REGISTERH */
                    SBC(REGISTERH);
                        break;
                    case 0x9d: /* SBC A,REGISTERL */
                    SBC(REGISTERL);
                        break;
                    case 0x9e: /* SBC A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        SBC(bytetemp);
                    }
                        break;
                    case 0xa4: /* AND A,REGISTERH */
                    AND(REGISTERH);
                        break;
                    case 0xa5: /* AND A,REGISTERL */
                    AND(REGISTERL);
                        break;
                    case 0xa6: /* AND A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        AND(bytetemp);
                    }
                        break;
                    case 0xac: /* XOR A,REGISTERH */
                    XOR(REGISTERH);
                        break;
                    case 0xad: /* XOR A,REGISTERL */
                    XOR(REGISTERL);
                        break;
                    case 0xae: /* XOR A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        XOR(bytetemp);
                    }
                        break;
                    case 0xb4: /* OR A,REGISTERH */
                    OR(REGISTERH);
                        break;
                    case 0xb5: /* OR A,REGISTERL */
                    OR(REGISTERL);
                        break;
                    case 0xb6: /* OR A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        OR(bytetemp);
                    }
                        break;
                    case 0xbc: /* CP A,REGISTERH */
                    CP(REGISTERH);
                        break;
                    case 0xbd: /* CP A,REGISTERL */
                    CP(REGISTERL);
                        break;
                    case 0xbe: /* CP A,(REGISTER+dd) */
                    {
                        libspectrum_byte offset, bytetemp;
                        offset = readByte(PC);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        contend_read_no_mreq(PC, 1);
                        PC++;
                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) offset;
                        bytetemp = readByte(z80.memptr.w);
                        CP(bytetemp);
                    }
                        break;
                    case 0xcb: /* shift DDFDCB */
                    {
                        libspectrum_byte opcode3;
                        // Byte 3: Displacement (3 cycles, no R increment)
                        libspectrum_byte displacement = readByte(PC++);

                        // Byte 4: Actual Opcode (4 cycles fetch, but NO R increment on DDCB/FDCB!)
                        contend_read(PC, 4);

                        opcode3 = readByteInternal(PC++);
                        // On real hardware, R is only incremented for the first two bytes (DD/FD and CB)
                        // The 4th byte is a "fake" fetch.

                        z80.memptr.w = REGISTER + (libspectrum_signed_byte) displacement;

#ifdef HAVE_ENOUGH_MEMORY
                        switch (opcode3) {
/* z80_ddfdcb.c Z80 {DD,FD}CBxx opcodes
   Copyright (c) 1999-2003 Philip Kendall

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

   Author contact information:

   E-mail: philip-fuse@shadowmagic.org.uk

*/

/* NB: this file is autogenerated by './z80/z80.pl' from 'opcodes_ddfdcb.dat',
   and included in 'z80_ops.c' */

                            case 0x00: /* LD B,RLC (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x01: /* LD C,RLC (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x02: /* LD D,RLC (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x03: /* LD E,RLC (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x04: /* LD H,RLC (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x05: /* LD L,RLC (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x06: /* RLC (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x07: /* LD A,RLC (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RLC(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x08: /* LD B,RRC (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x09: /* LD C,RRC (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x0a: /* LD D,RRC (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x0b: /* LD E,RRC (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x0c: /* LD H,RRC (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x0d: /* LD L,RRC (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x0e: /* RRC (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x0f: /* LD A,RRC (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RRC(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x10: /* LD B,RL (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x11: /* LD C,RL (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x12: /* LD D,RL (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x13: /* LD E,RL (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x14: /* LD H,RL (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x15: /* LD L,RL (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x16: /* RL (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x17: /* LD A,RL (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RL(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x18: /* LD B,RR (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x19: /* LD C,RR (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x1a: /* LD D,RR (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x1b: /* LD E,RR (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x1c: /* LD H,RR (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x1d: /* LD L,RR (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x1e: /* RR (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x1f: /* LD A,RR (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                RR(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x20: /* LD B,SLA (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x21: /* LD C,SLA (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x22: /* LD D,SLA (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x23: /* LD E,SLA (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x24: /* LD H,SLA (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x25: /* LD L,SLA (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x26: /* SLA (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x27: /* LD A,SLA (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLA(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x28: /* LD B,SRA (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x29: /* LD C,SRA (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x2a: /* LD D,SRA (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x2b: /* LD E,SRA (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x2c: /* LD H,SRA (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x2d: /* LD L,SRA (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x2e: /* SRA (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x2f: /* LD A,SRA (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRA(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x30: /* LD B,SLL (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x31: /* LD C,SLL (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x32: /* LD D,SLL (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x33: /* LD E,SLL (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x34: /* LD H,SLL (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x35: /* LD L,SLL (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x36: /* SLL (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x37: /* LD A,SLL (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SLL(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x38: /* LD B,SRL (REGISTER+dd) */
                                B = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(B);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x39: /* LD C,SRL (REGISTER+dd) */
                                C = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(C);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x3a: /* LD D,SRL (REGISTER+dd) */
                                D = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(D);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x3b: /* LD E,SRL (REGISTER+dd) */
                                E = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(E);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x3c: /* LD H,SRL (REGISTER+dd) */
                                H = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(H);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x3d: /* LD L,SRL (REGISTER+dd) */
                                L = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(L);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x3e: /* SRL (REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(bytetemp);
                                writeByte(z80.memptr.w, bytetemp);
                            }
                                break;
                            case 0x3f: /* LD A,SRL (REGISTER+dd) */
                                A = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                SRL(A);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x40:
                            case 0x41:
                            case 0x42:
                            case 0x43:
                            case 0x44:
                            case 0x45:
                            case 0x46:
                            case 0x47: /* BIT 0,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(0, bytetemp);
                            }
                                break;
                            case 0x48:
                            case 0x49:
                            case 0x4a:
                            case 0x4b:
                            case 0x4c:
                            case 0x4d:
                            case 0x4e:
                            case 0x4f: /* BIT 1,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(1, bytetemp);
                            }
                                break;
                            case 0x50:
                            case 0x51:
                            case 0x52:
                            case 0x53:
                            case 0x54:
                            case 0x55:
                            case 0x56:
                            case 0x57: /* BIT 2,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(2, bytetemp);
                            }
                                break;
                            case 0x58:
                            case 0x59:
                            case 0x5a:
                            case 0x5b:
                            case 0x5c:
                            case 0x5d:
                            case 0x5e:
                            case 0x5f: /* BIT 3,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(3, bytetemp);
                            }
                                break;
                            case 0x60:
                            case 0x61:
                            case 0x62:
                            case 0x63:
                            case 0x64:
                            case 0x65:
                            case 0x66:
                            case 0x67: /* BIT 4,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(4, bytetemp);
                            }
                                break;
                            case 0x68:
                            case 0x69:
                            case 0x6a:
                            case 0x6b:
                            case 0x6c:
                            case 0x6d:
                            case 0x6e:
                            case 0x6f: /* BIT 5,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(5, bytetemp);
                            }
                                break;
                            case 0x70:
                            case 0x71:
                            case 0x72:
                            case 0x73:
                            case 0x74:
                            case 0x75:
                            case 0x76:
                            case 0x77: /* BIT 6,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(6, bytetemp);
                            }
                                break;
                            case 0x78:
                            case 0x79:
                            case 0x7a:
                            case 0x7b:
                            case 0x7c:
                            case 0x7d:
                            case 0x7e:
                            case 0x7f: /* BIT 7,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                BIT_MEMPTR(7, bytetemp);
                            }
                                break;
                            case 0x80: /* LD B,RES 0,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x81: /* LD C,RES 0,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x82: /* LD D,RES 0,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x83: /* LD E,RES 0,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x84: /* LD H,RES 0,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x85: /* LD L,RES 0,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x86: /* RES 0,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xfe);
                            }
                                break;
                            case 0x87: /* LD A,RES 0,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xfe;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x88: /* LD B,RES 1,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x89: /* LD C,RES 1,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x8a: /* LD D,RES 1,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x8b: /* LD E,RES 1,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x8c: /* LD H,RES 1,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x8d: /* LD L,RES 1,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x8e: /* RES 1,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xfd);
                            }
                                break;
                            case 0x8f: /* LD A,RES 1,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xfd;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x90: /* LD B,RES 2,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x91: /* LD C,RES 2,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x92: /* LD D,RES 2,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x93: /* LD E,RES 2,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x94: /* LD H,RES 2,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x95: /* LD L,RES 2,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x96: /* RES 2,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xfb);
                            }
                                break;
                            case 0x97: /* LD A,RES 2,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xfb;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0x98: /* LD B,RES 3,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0x99: /* LD C,RES 3,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0x9a: /* LD D,RES 3,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0x9b: /* LD E,RES 3,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0x9c: /* LD H,RES 3,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0x9d: /* LD L,RES 3,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0x9e: /* RES 3,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xf7);
                            }
                                break;
                            case 0x9f: /* LD A,RES 3,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xf7;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xa0: /* LD B,RES 4,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xa1: /* LD C,RES 4,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xa2: /* LD D,RES 4,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xa3: /* LD E,RES 4,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xa4: /* LD H,RES 4,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xa5: /* LD L,RES 4,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xa6: /* RES 4,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xef);
                            }
                                break;
                            case 0xa7: /* LD A,RES 4,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xef;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xa8: /* LD B,RES 5,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xa9: /* LD C,RES 5,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xaa: /* LD D,RES 5,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xab: /* LD E,RES 5,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xac: /* LD H,RES 5,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xad: /* LD L,RES 5,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xae: /* RES 5,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xdf);
                            }
                                break;
                            case 0xaf: /* LD A,RES 5,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xdf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xb0: /* LD B,RES 6,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xb1: /* LD C,RES 6,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xb2: /* LD D,RES 6,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xb3: /* LD E,RES 6,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xb4: /* LD H,RES 6,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xb5: /* LD L,RES 6,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xb6: /* RES 6,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0xbf);
                            }
                                break;
                            case 0xb7: /* LD A,RES 6,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0xbf;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xb8: /* LD B,RES 7,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xb9: /* LD C,RES 7,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xba: /* LD D,RES 7,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xbb: /* LD E,RES 7,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xbc: /* LD H,RES 7,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xbd: /* LD L,RES 7,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xbe: /* RES 7,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp & 0x7f);
                            }
                                break;
                            case 0xbf: /* LD A,RES 7,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) & 0x7f;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xc0: /* LD B,SET 0,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xc1: /* LD C,SET 0,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xc2: /* LD D,SET 0,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xc3: /* LD E,SET 0,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xc4: /* LD H,SET 0,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xc5: /* LD L,SET 0,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xc6: /* SET 0,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x01);
                            }
                                break;
                            case 0xc7: /* LD A,SET 0,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x01;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xc8: /* LD B,SET 1,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xc9: /* LD C,SET 1,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xca: /* LD D,SET 1,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xcb: /* LD E,SET 1,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xcc: /* LD H,SET 1,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xcd: /* LD L,SET 1,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xce: /* SET 1,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x02);
                            }
                                break;
                            case 0xcf: /* LD A,SET 1,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x02;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xd0: /* LD B,SET 2,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xd1: /* LD C,SET 2,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xd2: /* LD D,SET 2,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xd3: /* LD E,SET 2,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xd4: /* LD H,SET 2,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xd5: /* LD L,SET 2,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xd6: /* SET 2,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x04);
                            }
                                break;
                            case 0xd7: /* LD A,SET 2,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x04;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xd8: /* LD B,SET 3,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xd9: /* LD C,SET 3,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xda: /* LD D,SET 3,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xdb: /* LD E,SET 3,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xdc: /* LD H,SET 3,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xdd: /* LD L,SET 3,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xde: /* SET 3,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x08);
                            }
                                break;
                            case 0xdf: /* LD A,SET 3,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x08;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xe0: /* LD B,SET 4,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xe1: /* LD C,SET 4,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xe2: /* LD D,SET 4,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xe3: /* LD E,SET 4,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xe4: /* LD H,SET 4,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xe5: /* LD L,SET 4,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xe6: /* SET 4,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x10);
                            }
                                break;
                            case 0xe7: /* LD A,SET 4,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x10;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xe8: /* LD B,SET 5,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xe9: /* LD C,SET 5,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xea: /* LD D,SET 5,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xeb: /* LD E,SET 5,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xec: /* LD H,SET 5,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xed: /* LD L,SET 5,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xee: /* SET 5,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x20);
                            }
                                break;
                            case 0xef: /* LD A,SET 5,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x20;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xf0: /* LD B,SET 6,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xf1: /* LD C,SET 6,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xf2: /* LD D,SET 6,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xf3: /* LD E,SET 6,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xf4: /* LD H,SET 6,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xf5: /* LD L,SET 6,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xf6: /* SET 6,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x40);
                            }
                                break;
                            case 0xf7: /* LD A,SET 6,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x40;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                            case 0xf8: /* LD B,SET 7,(REGISTER+dd) */
                                B = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, B);
                                break;
                            case 0xf9: /* LD C,SET 7,(REGISTER+dd) */
                                C = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, C);
                                break;
                            case 0xfa: /* LD D,SET 7,(REGISTER+dd) */
                                D = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, D);
                                break;
                            case 0xfb: /* LD E,SET 7,(REGISTER+dd) */
                                E = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, E);
                                break;
                            case 0xfc: /* LD H,SET 7,(REGISTER+dd) */
                                H = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, H);
                                break;
                            case 0xfd: /* LD L,SET 7,(REGISTER+dd) */
                                L = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, L);
                                break;
                            case 0xfe: /* SET 7,(REGISTER+dd) */
                            {
                                libspectrum_byte bytetemp;
                                bytetemp = readByte(z80.memptr.w);
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, bytetemp | 0x80);
                            }
                                break;
                            case 0xff: /* LD A,SET 7,(REGISTER+dd) */
                                A = readByte(z80.memptr.w) | 0x80;
                                contend_read_no_mreq(z80.memptr.w, 1);
                                writeByte(z80.memptr.w, A);
                                break;
                        }
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                        z80_ddfdcbxx(opcode3);
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
                    }
                        break;
                    case 0xe1: /* POP REGISTER */
                    POP16(REGISTERL, REGISTERH);
                        break;
                    case 0xe3: /* EX (SP),REGISTER */
                    {
                        libspectrum_byte bytetempl, bytetemph;
                        bytetempl = readByte(SP);
                        bytetemph = readByte(SP + 1);
                        contend_read_no_mreq(SP + 1, 1);
                        writeByte(SP + 1, REGISTERH);
                        writeByte(SP, REGISTERL);
                        contend_write_no_mreq(SP, 1);
                        contend_write_no_mreq(SP, 1);
                        REGISTERL = z80.memptr.b.l = bytetempl;
                        REGISTERH = z80.memptr.b.h = bytetemph;
                    }
                        break;
                    case 0xe5: /* PUSH REGISTER */
                        contend_read_no_mreq(IR, 1);
                        PUSH16(REGISTERL, REGISTERH);
                        break;
                    case 0xe9:     /* JP REGISTER */
                        PC = REGISTER; /* NB: NOT INDIRECT! */
                        break;
                    case 0xf9: /* LD SP,REGISTER */
                        contend_read_no_mreq(IR, 1);
                        contend_read_no_mreq(IR, 1);
                        SP = REGISTER;
                        break;
                    default: /* Instruction did not involve H or L, so backtrack
            one instruction and parse again */
                        PC--;
                        R--;
                        opcode = opcode2;
#ifdef HAVE_ENOUGH_MEMORY
                        goto end_opcode;
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                        return 1;
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
#undef REGISTERH
#undef REGISTERL
#undef REGISTER
                }
#else  /* #ifdef HAVE_ENOUGH_MEMORY */
                                                                                                                                        if (z80_fdxx(opcode2))
    goto end_opcode;
#endif /* #ifdef HAVE_ENOUGH_MEMORY */
            }
                break;
            case 0xfe: /* CP nn */
            {
                libspectrum_byte bytetemp = readByte(PC++);
                CP(bytetemp);
            }
                break;
            case 0xff: /* RST 38 */
                contend_read_no_mreq(IR, 1);
                RST(0x38);
                break;
        }
        end_opcode:
        z80.iff2_read = 0; // Standard behavior: reset after instruction

        if (g_single_step) {
            g_single_step = false;
            g_frozen = true;
            break;
        }
    }
}
