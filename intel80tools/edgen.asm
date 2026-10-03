        NAME EDGEN
        ORG     0100H
        JMP     PEDCOMMAND
STACKORG                         EQU     0FFFFH
PMEMORY                          SET     0H
PED                              SET     0C0H
PEDJMP                           SET     0H
PEDADR                           SET     1H
PBDISK                           SET     0H
PMAXB                            SET     0H
PFCB                             SET     0H
PBUFF                            SET     0H
PMON1                            SET     0005H
PF                               SET     0H
PA                               SET     0H
PMON2                            SET     0005H
PBOOT                            SET     0000H
PCOPYRIGHT                       SET     3H
PMAX                             SET     0H
PMAXM                            SET     2H
PHMAX                            SET     4H
PRFCB                            SET     6H
PRBP                             SET     27H
PXFCB                            SET     28H
PXFCBE                           SET     34H
PXFCBM                           SET     36H
PXFCBR                           SET     48H
PXBUFF                           SET     49H
PXBP                             SET     0C9H
PXFERON                          SET     0CAH
PNBUF                            SET     0CBH
PBUFFLENGTH                      SET     0CCH
PSFCB                            SET     0H
PSBUFFADR                        SET     0CEH
PSBUFF                           SET     0H
PDFCB                            SET     0D0H
PDFUB                            SET     0DDH
PDBUFFADR                        SET     0F1H
PDBUFF                           SET     0H
PNSOURCE                         SET     0F3H
PNDEST                           SET     0F5H
PSDISK                           SET     0F7H
PDDISK                           SET     0F8H
PREADCHAR                        SET     9D5H
PPRINTSUPPRESS                   SET     0F9H
PPRINTCHAR                       SET     9DEH
PCHAR                            SET     0FAH
PCOLUMN                          SET     0FBH
PSCOLUMN                         SET     0FCH
PTCOLUMN                         SET     0FDH
PQCOLUMN                         SET     0FEH
PTTYCHAR                         SET     9F6H
PBACKSPACE                       SET     0A1BH
PPRINTABS                        SET     0A39H
PI                               SET     101H
PJ                               SET     102H
PGRAPHIC                         SET     0A7DH
PC                               SET     103H
PPRINTC                          SET     0AADH
PCRLF                            SET     0AD1H
PPRINTM                          SET     0ADCH
PPRINT                           SET     0AECH
PREAD                            SET     0AFEH
PDCNT                            SET     10BH
POPEN                            SET     0B0EH
PCLOSE                           SET     0B21H
PSEARCH                          SET     0B34H
PDELETE                          SET     0B47H
PDISKREAD                        SET     0B57H
PDISKWRITE                       SET     0B67H
PMAKE                            SET     0B77H
PRENAME                          SET     0B8AH
PMAXLEN                          SET     11CH
PCOMLEN                          SET     11DH
PCOMBUFF                         SET     11EH
PTCBP                            SET     19EH
PCBP                             SET     19FH
PREADCOM                         SET     0B9AH
PBREAKKEY                        SET     0BA6H
PCSELECT                         SET     0BC0H
PSELECT                          SET     0BC9H
PDISK                            SET     1A0H
PSETDMA                          SET     0BD9H
PREBOOT                          SET     0BE9H
PBASELINE                        SET     1A3H
PRELLINE                         SET     1A5H
PLINESET                         SET     1A7H
PABORT                           SET     0BFAH
PFERR                            SET     0C0FH
PSETTYPE                         SET     0C1CH
PSETUP                           SET     0C3BH
PXCLEAR                          SET     0D2AH
PSETXDMA                         SET     0D3FH
PFILLSOURCE                      SET     0D4DH
PZN                              SET     0DB5H
PGETSOURCE                       SET     0DBCH
PB                               SET     1ADH
PWRITEDEST                       SET     0DE7H
PN                               SET     1AFH
PPUTDEST                         SET     0E4CH
PPUTXFER                         SET     0E73H
PFINIS                           SET     0EA7H
PMOVEUP                          SET     0F18H
PMACRO                           SET     1B2H
PSCRATCH                         SET     232H
PWBP                             SET     296H
PWBE                             SET     297H
PWBJ                             SET     298H
PFLAG                            SET     299H
PMP                              SET     29AH
PMI                              SET     29BH
PXP                              SET     29CH
PMT                              SET     29DH
PSTART                           SET     206H
PRESTART                         SET     1A7H
POVERCOUNT                       SET     1C3H
POVERFLOW                        SET     1D9H
PRESET                           SET     1E1H
PBADCOM                          SET     1CEH
PINSERTING                       SET     29FH
PREADBUFF                        SET     2A0H
PPRINTNMAC                       SET     0F2DH
PTRANSLATE                       SET     2A2H
PUPPER                           SET     2A3H
PLOWERCASE                       SET     0F42H
PUCASE                           SET     0F5AH
PUTRAN                           SET     0F73H
PPRINTVALUE                      SET     0F8AH
PV                               SET     2A7H
PD                               SET     2A9H
PZERO                            SET     2AAH
PK                               SET     2ABH
PPRINTLINE                       SET     0FF5H
PPRINTBASE                       SET     102AH
PPRINTNMBASE                     SET     1033H
PREADC                           SET     1040H
PSETRDMA                         SET     1122H
PREADFILE                        SET     1130H
PDISTANCE                        SET     2AFH
PTDIST                           SET     2B1H
PDIRECTION                       SET     2B3H
PFRONT                           SET     2B5H
PBACK                            SET     2B7H
PFIRST                           SET     2B9H
PLAST                            SET     2BBH
PSETFF                           SET     1162H
PDISTZERO                        SET     1169H
PZERODIST                        SET     1176H
PDISTNZERO                       SET     117DH
PSETLIMITS                       SET     1191H
PL                               SET     2C1H
PM                               SET     2C3H
PMIDDLE                          SET     2C5H
PLOOPING                         SET     2C6H
PINCFRONT                        SET     127AH
PINCBACK                         SET     1282H
PDECFRONT                        SET     128AH
PDECBACK                         SET     1292H
PINCBASE                         SET     129AH
PMEMMOVE                         SET     12A2H
PMOVEFLAG                        SET     2C7H
PMOVER                           SET     132AH
PSETPTRS                         SET     1330H
PMOVELINES                       SET     1336H
PSETCLIMITS                      SET     133DH
PREADLINE                        SET     139CH
PCTRAN                           SET     13DCH
PWRITELINE                       SET     13F3H
PWRHALF                          SET     1425H
PWRITEOUT                        SET     144EH
PCLEARMEM                        SET     148EH
PTERMINATE                       SET     1495H
PINSERT                          SET     14B1H
PSCANNING                        SET     14DBH
PCOLLECT                         SET     14FCH
PSETSCR                          SET     152AH
PFIND                            SET     1547H
PPA                              SET     2CCH
PPB                              SET     2CDH
PMATCH                           SET     2D1H
PSETFIND                         SET     15D1H
PCHKFOUND                        SET     15E0H
PSETRFCB                         SET     15F1H
PPRINTREL                        SET     160CH
PTYPELINES                       SET     161AH
PSETLPP                          SET     16AEH
PSAVEDIST                        SET     16B5H
PRESTDIST                        SET     16BCH
PPAGE                            SET     16C3H
PWAIT                            SET     170EH
PSETFORWARD                      SET     1733H
PAPPHALF                         SET     173FH
PINSCRLF                         SET     1762H
PTESTCASE                        SET     1773H
PT                               SET     2D7H
PREADCTRAN                       SET     1797H
PSINGLECOM                       SET     17A6H
PSINGLERCOM                      SET     17CDH
PEDCOMMAND                       SET     0C3H
PDIGIT                           SET     180CH
PNUMBER                          SET     181BH
PRELDISTANCE                     SET     1847H
@1                               SET     0H ; Undefined
@10                              SET     0H ; Undefined
@100                             SET     0H ; Undefined
@101                             SET     0H ; Undefined
@102                             SET     0H ; Undefined
@103                             SET     0H ; Undefined
@104                             SET     0H ; Undefined
@105                             SET     0H ; Undefined
@106                             SET     0H ; Undefined
@107                             SET     0H ; Undefined
@108                             SET     0H ; Undefined
@109                             SET     0H ; Undefined
@11                              SET     0H ; Undefined
@110                             SET     0H ; Undefined
@111                             SET     0H ; Undefined
@112                             SET     0H ; Undefined
@113                             SET     0H ; Undefined
@114                             SET     0H ; Undefined
@115                             SET     0H ; Undefined
@116                             SET     0H ; Undefined
@117                             SET     0H ; Undefined
@118                             SET     0H ; Undefined
@119                             SET     0H ; Undefined
@12                              SET     0H ; Undefined
@120                             SET     0H ; Undefined
@121                             SET     0H ; Undefined
@122                             SET     0H ; Undefined
@123                             SET     0H ; Undefined
@124                             SET     0H ; Undefined
@125                             SET     0H ; Undefined
@126                             SET     0H ; Undefined
@127                             SET     0H ; Undefined
@128                             SET     0H ; Undefined
@129                             SET     0H ; Undefined
@13                              SET     0H ; Undefined
@130                             SET     0H ; Undefined
@131                             SET     0H ; Undefined
@132                             SET     0H ; Undefined
@133                             SET     0H ; Undefined
@134                             SET     0H ; Undefined
@135                             SET     0H ; Undefined
@136                             SET     0H ; Undefined
@137                             SET     0H ; Undefined
@138                             SET     0H ; Undefined
@139                             SET     0H ; Undefined
@14                              SET     0H ; Undefined
@140                             SET     0H ; Undefined
@141                             SET     0H ; Undefined
@142                             SET     0H ; Undefined
@143                             SET     0H ; Undefined
@144                             SET     0H ; Undefined
@145                             SET     0H ; Undefined
@147                             SET     0H ; Undefined
@148                             SET     0H ; Undefined
@149                             SET     0H ; Undefined
@15                              SET     0H ; Undefined
@150                             SET     0H ; Undefined
@151                             SET     0H ; Undefined
@152                             SET     0H ; Undefined
@153                             SET     0H ; Undefined
@154                             SET     0H ; Undefined
@155                             SET     0H ; Undefined
@156                             SET     0H ; Undefined
@157                             SET     0H ; Undefined
@158                             SET     0H ; Undefined
@159                             SET     0H ; Undefined
@16                              SET     0H ; Undefined
@160                             SET     0H ; Undefined
@161                             SET     0H ; Undefined
@162                             SET     0H ; Undefined
@163                             SET     0H ; Undefined
@164                             SET     0H ; Undefined
@165                             SET     0H ; Undefined
@166                             SET     0H ; Undefined
@167                             SET     0H ; Undefined
@168                             SET     0H ; Undefined
@169                             SET     0H ; Undefined
@17                              SET     0H ; Undefined
@170                             SET     0H ; Undefined
@171                             SET     0H ; Undefined
@172                             SET     0H ; Undefined
@173                             SET     0H ; Undefined
@174                             SET     0H ; Undefined
@175                             SET     0H ; Undefined
@176                             SET     0H ; Undefined
@177                             SET     0H ; Undefined
@178                             SET     0H ; Undefined
@179                             SET     0H ; Undefined
@18                              SET     0H ; Undefined
@180                             SET     0H ; Undefined
@181                             SET     0H ; Undefined
@182                             SET     0H ; Undefined
@183                             SET     0H ; Undefined
@184                             SET     0H ; Undefined
@185                             SET     0H ; Undefined
@186                             SET     0H ; Undefined
@187                             SET     0H ; Undefined
@188                             SET     0H ; Undefined
@189                             SET     0H ; Undefined
@19                              SET     0H ; Undefined
@190                             SET     0H ; Undefined
@191                             SET     0H ; Undefined
@192                             SET     0H ; Undefined
@193                             SET     0H ; Undefined
@194                             SET     0H ; Undefined
@195                             SET     0H ; Undefined
@196                             SET     0H ; Undefined
@197                             SET     0H ; Undefined
@198                             SET     0H ; Undefined
@199                             SET     0H ; Undefined
@2                               SET     0H ; Undefined
@20                              SET     0H ; Undefined
@200                             SET     0H ; Undefined
@201                             SET     0H ; Undefined
@202                             SET     0H ; Undefined
@203                             SET     0H ; Undefined
@204                             SET     0H ; Undefined
@205                             SET     0H ; Undefined
@206                             SET     0H ; Undefined
@207                             SET     0H ; Undefined
@208                             SET     0H ; Undefined
@209                             SET     0H ; Undefined
@21                              SET     0H ; Undefined
@210                             SET     0H ; Undefined
@211                             SET     0H ; Undefined
@212                             SET     0H ; Undefined
@213                             SET     0H ; Undefined
@214                             SET     0H ; Undefined
@215                             SET     0H ; Undefined
@216                             SET     0H ; Undefined
@217                             SET     0H ; Undefined
@218                             SET     0H ; Undefined
@219                             SET     0H ; Undefined
@22                              SET     0H ; Undefined
@220                             SET     0H ; Undefined
@221                             SET     0H ; Undefined
@222                             SET     0H ; Undefined
@223                             SET     0H ; Undefined
@224                             SET     0H ; Undefined
@225                             SET     0H ; Undefined
@227                             SET     0H ; Undefined
@228                             SET     0H ; Undefined
@23                              SET     0H ; Undefined
@230                             SET     0H ; Undefined
@231                             SET     0H ; Undefined
@233                             SET     0H ; Undefined
@234                             SET     0H ; Undefined
@235                             SET     0H ; Undefined
@236                             SET     0H ; Undefined
@237                             SET     0H ; Undefined
@238                             SET     0H ; Undefined
@239                             SET     0H ; Undefined
@24                              SET     0H ; Undefined
@240                             SET     0H ; Undefined
@241                             SET     0H ; Undefined
@242                             SET     0H ; Undefined
@243                             SET     0H ; Undefined
@244                             SET     0H ; Undefined
@245                             SET     0H ; Undefined
@247                             SET     0H ; Undefined
@249                             SET     0H ; Undefined
@25                              SET     0H ; Undefined
@250                             SET     0H ; Undefined
@251                             SET     0H ; Undefined
@252                             SET     0H ; Undefined
@253                             SET     0H ; Undefined
@254                             SET     0H ; Undefined
@255                             SET     0H ; Undefined
@256                             SET     0H ; Undefined
@257                             SET     0H ; Undefined
@258                             SET     0H ; Undefined
@259                             SET     0H ; Undefined
@26                              SET     0H ; Undefined
@260                             SET     0H ; Undefined
@261                             SET     0H ; Undefined
@262                             SET     0H ; Undefined
@263                             SET     0H ; Undefined
@264                             SET     0H ; Undefined
@266                             SET     0H ; Undefined
@267                             SET     0H ; Undefined
@268                             SET     0H ; Undefined
@27                              SET     0H ; Undefined
@270                             SET     0H ; Undefined
@271                             SET     0H ; Undefined
@272                             SET     0H ; Undefined
@273                             SET     0H ; Undefined
@274                             SET     0H ; Undefined
@275                             SET     0H ; Undefined
@276                             SET     0H ; Undefined
@277                             SET     0H ; Undefined
@278                             SET     0H ; Undefined
@279                             SET     0H ; Undefined
@28                              SET     0H ; Undefined
@280                             SET     0H ; Undefined
@281                             SET     0H ; Undefined
@282                             SET     0H ; Undefined
@283                             SET     0H ; Undefined
@284                             SET     0H ; Undefined
@285                             SET     0H ; Undefined
@286                             SET     0H ; Undefined
@287                             SET     0H ; Undefined
@288                             SET     0H ; Undefined
@289                             SET     0H ; Undefined
@29                              SET     0H ; Undefined
@290                             SET     0H ; Undefined
@291                             SET     0H ; Undefined
@292                             SET     0H ; Undefined
@293                             SET     0H ; Undefined
@294                             SET     0H ; Undefined
@295                             SET     0H ; Undefined
@296                             SET     0H ; Undefined
@297                             SET     0H ; Undefined
@298                             SET     0H ; Undefined
@299                             SET     0H ; Undefined
@3                               SET     0H ; Undefined
@30                              SET     0H ; Undefined
@300                             SET     0H ; Undefined
@301                             SET     0H ; Undefined
@302                             SET     0H ; Undefined
@303                             SET     0H ; Undefined
@304                             SET     0H ; Undefined
@305                             SET     0H ; Undefined
@306                             SET     0H ; Undefined
@307                             SET     0H ; Undefined
@308                             SET     0H ; Undefined
@309                             SET     0H ; Undefined
@31                              SET     0H ; Undefined
@310                             SET     0H ; Undefined
@311                             SET     0H ; Undefined
@32                              SET     0H ; Undefined
@33                              SET     0H ; Undefined
@34                              SET     0H ; Undefined
@35                              SET     0H ; Undefined
@36                              SET     0H ; Undefined
@37                              SET     0H ; Undefined
@38                              SET     0H ; Undefined
@39                              SET     0H ; Undefined
@4                               SET     0H ; Undefined
@40                              SET     0H ; Undefined
@41                              SET     0H ; Undefined
@42                              SET     0H ; Undefined
@43                              SET     0H ; Undefined
@44                              SET     0H ; Undefined
@45                              SET     0H ; Undefined
@46                              SET     0H ; Undefined
@47                              SET     0H ; Undefined
@48                              SET     0H ; Undefined
@49                              SET     0H ; Undefined
@5                               SET     0H ; Undefined
@50                              SET     0H ; Undefined
@51                              SET     0H ; Undefined
@52                              SET     0H ; Undefined
@53                              SET     0H ; Undefined
@54                              SET     0H ; Undefined
@55                              SET     0H ; Undefined
@56                              SET     0H ; Undefined
@57                              SET     0H ; Undefined
@58                              SET     0H ; Undefined
@59                              SET     0H ; Undefined
@6                               SET     0H ; Undefined
@60                              SET     0H ; Undefined
@61                              SET     0H ; Undefined
@62                              SET     0H ; Undefined
@63                              SET     0H ; Undefined
@64                              SET     0H ; Undefined
@65                              SET     0H ; Undefined
@66                              SET     0H ; Undefined
@67                              SET     0H ; Undefined
@68                              SET     0H ; Undefined
@69                              SET     0H ; Undefined
@7                               SET     0H ; Undefined
@70                              SET     0H ; Undefined
@71                              SET     0H ; Undefined
@72                              SET     0H ; Undefined
@73                              SET     0H ; Undefined
@74                              SET     0H ; Undefined
@75                              SET     0H ; Undefined
@76                              SET     0H ; Undefined
@77                              SET     0H ; Undefined
@78                              SET     0H ; Undefined
@79                              SET     0H ; Undefined
@8                               SET     0H ; Undefined
@80                              SET     0H ; Undefined
@81                              SET     0H ; Undefined
@82                              SET     0H ; Undefined
@83                              SET     0H ; Undefined
@84                              SET     0H ; Undefined
@85                              SET     0H ; Undefined
@86                              SET     0H ; Undefined
@87                              SET     0H ; Undefined
@88                              SET     0H ; Undefined
@89                              SET     0H ; Undefined
@9                               SET     0H ; Undefined
@90                              SET     0H ; Undefined
@91                              SET     0H ; Undefined
@92                              SET     0H ; Undefined
@93                              SET     0H ; Undefined
@94                              SET     0H ; Undefined
@95                              SET     0H ; Undefined
@96                              SET     0H ; Undefined
@97                              SET     0H ; Undefined
@98                              SET     0H ; Undefined
@99                              SET     0H ; Undefined
QP0017                           SET     0H ; Undefined
QP0029                           SET     0H ; Undefined
QP0030                           SET     0H ; Undefined
QP0087                           SET     0H ; Undefined
QP0091                           SET     0H ; Undefined
QP0093                           SET     0H ; Undefined
QP0094                           SET     0H ; Undefined
QP0095                           SET     0H ; Undefined
QP0098                           SET     0H ; Undefined
QP0100                           SET     0H ; Undefined
QP0101                           SET     0H ; Undefined
QP0102                           SET     0H ; Undefined
QP0103                           SET     0H ; Undefined
QP0105                           SET     0H ; Undefined
QSTACKORIGIN                     SET     0H ; Undefined

; --- CODE ---

        ORG     00AD5H
        LXI     D,0H
        ORG     00AD8H
        MVI     C,1H
        ORG     00ADAH
        CALL    PMON2
        ORG     00ADDH
        RET
        ORG     00ADEH
        LXI     H,PCHAR
        ORG     00AE1H
        MOV     M,C
        ORG     00AE2H
        LDA     PPRINTSUPPRESS
        ORG     00AE5H
        RAR
        ORG     00AE6H
        JNC     @1
        ORG     00AE9H
        RET
        ORG     00AEAH
        LHLD    PCHAR
        ORG     00AEDH
        MVI     H,0
        ORG     00AEFH
        XCHG
        ORG     00AF0H
        MVI     C,2H
        ORG     00AF2H
        CALL    PMON1
        ORG     00AF5H
        RET
        ORG     00AF6H
        LXI     H,PCHAR
        ORG     00AF9H
        MOV     M,C
        ORG     00AFAH
        LDA     PCHAR
        ORG     00AFDH
        CPI     20H
        ORG     00AFFH
        JC      @2
        ORG     00B02H
        LXI     H,PCOLUMN
        ORG     00B05H
        INR     M
        ORG     00B06H
        LDA     PCHAR
        ORG     00B09H
        CPI     0AH
        ORG     00B0BH
        JNZ     @3
        ORG     00B0EH
        LXI     H,PCOLUMN
        ORG     00B11H
        MVI     M,0H
        ORG     00B13H
        LHLD    PCHAR
        ORG     00B16H
        MOV     C,L
        ORG     00B17H
        CALL    PPRINTCHAR
        ORG     00B1AH
        RET
        ORG     00B1BH
        LDA     PCOLUMN
        ORG     00B1EH
        CPI     0H
        ORG     00B20H
        JNZ     @4
        ORG     00B23H
        RET
        ORG     00B24H
        MVI     C,8H
        ORG     00B26H
        CALL    PTTYCHAR
        ORG     00B29H
        MVI     C,20H
        ORG     00B2BH
        CALL    PTTYCHAR
        ORG     00B2EH
        MVI     C,8H
        ORG     00B30H
        CALL    PTTYCHAR
        ORG     00B33H
        LXI     H,PCOLUMN
        ORG     00B36H
        DCR     M
        ORG     00B37H
        DCR     M
        ORG     00B38H
        RET
        ORG     00B39H
        LXI     H,PCHAR
        ORG     00B3CH
        MOV     M,C
        ORG     00B3DH
        LDA     PCHAR
        ORG     00B40H
        SUI     9H
        ORG     00B42H
        SUI     1
        ORG     00B44H
        SBB     A
        ORG     00B45H
        PUSH    PSW     ; 1
        ORG     00B46H
        LDA     PCOLUMN
        ORG     00B49H
        ANI     7H
        ORG     00B4BH
        MOV     C,A
        ORG     00B4CH
        MVI     A,7H
        ORG     00B4EH
        SUB     C
        ORG     00B4FH
        POP     B       ; 1
        ORG     00B50H
        MOV     C,B
        ORG     00B51H
        ANA     C
        ORG     00B52H
        STA     PI
        ORG     00B55H
        MOV     A,C
        ORG     00B56H
        RAR
        ORG     00B57H
        JNC     @5
        ORG     00B5AH
        LXI     H,PCHAR
        ORG     00B5DH
        MVI     M,20H
        ORG     00B5FH
        LXI     H,PJ
        ORG     00B62H
        MVI     M,0H
        ORG     00B64H
        LDA     PI
        ORG     00B67H
        LXI     H,PJ
        ORG     00B6AH
        CMP     M
        ORG     00B6BH
        JC      @225
        ORG     00B6EH
        LHLD    PCHAR
        ORG     00B71H
        MOV     C,L
        ORG     00B72H
        CALL    PTTYCHAR
        ORG     00B75H
        LXI     H,PJ
        ORG     00B78H
        INR     M
        ORG     00B79H
        JNZ     @224
        ORG     00B7CH
        RET
        ORG     00B7DH
        LXI     H,PC
        ORG     00B80H
        MOV     M,C
        ORG     00B81H
        LDA     PC
        ORG     00B84H
        CPI     20H
        ORG     00B86H
        JC      @6
        ORG     00B89H
        MVI     A,1H
        ORG     00B8BH
        RET
        ORG     00B8CH
        LDA     PC
        ORG     00B8FH
        SUI     0DH
        ORG     00B91H
        SUI     1
        ORG     00B93H
        SBB     A
        ORG     00B94H
        PUSH    PSW     ; 1
        ORG     00B95H
        LDA     PC
        ORG     00B98H
        SUI     0AH
        ORG     00B9AH
        SUI     1
        ORG     00B9CH
        SBB     A
        ORG     00B9DH
        POP     B       ; 1
        ORG     00B9EH
        MOV     C,B
        ORG     00B9FH
        ORA     C
        ORG     00BA0H
        PUSH    PSW     ; 1
        ORG     00BA1H
        LDA     PC
        ORG     00BA4H
        SUI     9H
        ORG     00BA6H
        SUI     1
        ORG     00BA8H
        SBB     A
        ORG     00BA9H
        POP     B       ; 1
        ORG     00BAAH
        MOV     C,B
        ORG     00BABH
        ORA     C
        ORG     00BACH
        RET
        ORG     00BADH
        LXI     H,PC
        ORG     00BB0H
        MOV     M,C
        ORG     00BB1H
        LHLD    PC
        ORG     00BB4H
        MOV     C,L
        ORG     00BB5H
        CALL    PGRAPHIC
        ORG     00BB8H
        RAR
        ORG     00BB9H
        JC      @7
        ORG     00BBCH
        MVI     C,5EH
        ORG     00BBEH
        CALL    PPRINTABS
        ORG     00BC1H
        LDA     PC
        ORG     00BC4H
        ADI     40H
        ORG     00BC6H
        STA     PC
        ORG     00BC9H
        LHLD    PC
        ORG     00BCCH
        MOV     C,L
        ORG     00BCDH
        CALL    PPRINTABS
        ORG     00BD0H
        RET
        ORG     00BD1H
        MVI     C,0DH
        ORG     00BD3H
        CALL    PPRINTC
        ORG     00BD6H
        MVI     C,0AH
        ORG     00BD8H
        CALL    PPRINTC
        ORG     00BDBH
        RET
        ORG     00BDCH
        LXI     H,PA+1H
        ORG     00BDFH
        MOV     M,B
        ORG     00BE0H
        DCX     H
        ORG     00BE1H
        MOV     M,C
        ORG     00BE2H
        LHLD    PA
        ORG     00BE5H
        XCHG
        ORG     00BE6H
        MVI     C,9H
        ORG     00BE8H
        CALL    PMON1
        ORG     00BEBH
        RET
        ORG     00BECH
        LXI     H,PA+1H
        ORG     00BEFH
        MOV     M,B
        ORG     00BF0H
        DCX     H
        ORG     00BF1H
        MOV     M,C
        ORG     00BF2H
        CALL    PCRLF
        ORG     00BF5H
        LHLD    PA
        ORG     00BF8H
        MOV     B,H
        ORG     00BF9H
        MOV     C,L
        ORG     00BFAH
        CALL    PPRINTM
        ORG     00BFDH
        RET
        ORG     00BFEH
        LXI     H,PA+1H
        ORG     00C01H
        MOV     M,B
        ORG     00C02H
        DCX     H
        ORG     00C03H
        MOV     M,C
        ORG     00C04H
        LHLD    PA
        ORG     00C07H
        XCHG
        ORG     00C08H
        MVI     C,0AH
        ORG     00C0AH
        CALL    PMON1
        ORG     00C0DH
        RET
        ORG     00C0EH
        LXI     H,PFCB+1H
        ORG     00C11H
        MOV     M,B
        ORG     00C12H
        DCX     H
        ORG     00C13H
        MOV     M,C
        ORG     00C14H
        LHLD    PFCB
        ORG     00C17H
        XCHG
        ORG     00C18H
        MVI     C,0FH
        ORG     00C1AH
        CALL    PMON2
        ORG     00C1DH
        STA     PDCNT
        ORG     00C20H
        RET
        ORG     00C21H
        LXI     H,PFCB+1H
        ORG     00C24H
        MOV     M,B
        ORG     00C25H
        DCX     H
        ORG     00C26H
        MOV     M,C
        ORG     00C27H
        LHLD    PFCB
        ORG     00C2AH
        XCHG
        ORG     00C2BH
        MVI     C,10H
        ORG     00C2DH
        CALL    PMON2
        ORG     00C30H
        STA     PDCNT
        ORG     00C33H
        RET
        ORG     00C34H
        LXI     H,PFCB+1H
        ORG     00C37H
        MOV     M,B
        ORG     00C38H
        DCX     H
        ORG     00C39H
        MOV     M,C
        ORG     00C3AH
        LHLD    PFCB
        ORG     00C3DH
        XCHG
        ORG     00C3EH
        MVI     C,11H
        ORG     00C40H
        CALL    PMON2
        ORG     00C43H
        STA     PDCNT
        ORG     00C46H
        RET
        ORG     00C47H
        LXI     H,PFCB+1H
        ORG     00C4AH
        MOV     M,B
        ORG     00C4BH
        DCX     H
        ORG     00C4CH
        MOV     M,C
        ORG     00C4DH
        LHLD    PFCB
        ORG     00C50H
        XCHG
        ORG     00C51H
        MVI     C,13H
        ORG     00C53H
        CALL    PMON1
        ORG     00C56H
        RET
        ORG     00C57H
        LXI     H,PFCB+1H
        ORG     00C5AH
        MOV     M,B
        ORG     00C5BH
        DCX     H
        ORG     00C5CH
        MOV     M,C
        ORG     00C5DH
        LHLD    PFCB
        ORG     00C60H
        XCHG
        ORG     00C61H
        MVI     C,14H
        ORG     00C63H
        CALL    PMON2
        ORG     00C66H
        RET
        ORG     00C67H
        LXI     H,PFCB+1H
        ORG     00C6AH
        MOV     M,B
        ORG     00C6BH
        DCX     H
        ORG     00C6CH
        MOV     M,C
        ORG     00C6DH
        LHLD    PFCB
        ORG     00C70H
        XCHG
        ORG     00C71H
        MVI     C,15H
        ORG     00C73H
        CALL    PMON2
        ORG     00C76H
        RET
        ORG     00C77H
        LXI     H,PFCB+1H
        ORG     00C7AH
        MOV     M,B
        ORG     00C7BH
        DCX     H
        ORG     00C7CH
        MOV     M,C
        ORG     00C7DH
        LHLD    PFCB
        ORG     00C80H
        XCHG
        ORG     00C81H
        MVI     C,16H
        ORG     00C83H
        CALL    PMON2
        ORG     00C86H
        STA     PDCNT
        ORG     00C89H
        RET
        ORG     00C8AH
        LXI     H,PFCB+1H
        ORG     00C8DH
        MOV     M,B
        ORG     00C8EH
        DCX     H
        ORG     00C8FH
        MOV     M,C
        ORG     00C90H
        LHLD    PFCB
        ORG     00C93H
        XCHG
        ORG     00C94H
        MVI     C,17H
        ORG     00C96H
        CALL    PMON1
        ORG     00C99H
        RET
        ORG     00C9AH
        LXI     H,PMAXLEN
        ORG     00C9DH
        MVI     M,80H
        ORG     00C9FH
        LXI     B,PMAXLEN
        ORG     00CA2H
        CALL    PREAD
        ORG     00CA5H
        RET
        ORG     00CA6H
        LXI     D,0H
        ORG     00CA9H
        MVI     C,0BH
        ORG     00CABH
        CALL    PMON2
        ORG     00CAEH
        RAR
        ORG     00CAFH
        JNC     @8
        ORG     00CB2H
        LXI     D,0H
        ORG     00CB5H
        MVI     C,1H
        ORG     00CB7H
        CALL    PMON1
        ORG     00CBAH
        MVI     A,1H
        ORG     00CBCH
        RET
        ORG     00CBDH
        MVI     A,0H
        ORG     00CBFH
        RET
        ORG     00CC0H
        LXI     D,0H
        ORG     00CC3H
        MVI     C,19H
        ORG     00CC5H
        CALL    PMON2
        ORG     00CC8H
        RET
        ORG     00CC9H
        LXI     H,PDISK
        ORG     00CCCH
        MOV     M,C
        ORG     00CCDH
        LHLD    PDISK
        ORG     00CD0H
        MVI     H,0
        ORG     00CD2H
        XCHG
        ORG     00CD3H
        MVI     C,0EH
        ORG     00CD5H
        CALL    PMON1
        ORG     00CD8H
        RET
        ORG     00CD9H
        LXI     H,PA+1H
        ORG     00CDCH
        MOV     M,B
        ORG     00CDDH
        DCX     H
        ORG     00CDEH
        MOV     M,C
        ORG     00CDFH
        LHLD    PA
        ORG     00CE2H
        XCHG
        ORG     00CE3H
        MVI     C,1AH
        ORG     00CE5H
        CALL    PMON1
        ORG     00CE8H
        RET
        ORG     00CE9H
        LDA     PXFERON
        ORG     00CECH
        RAR
        ORG     00CEDH
        JNC     @9
        ORG     00CF0H
        LXI     B,PXFCB
        ORG     00CF3H
        CALL    PDELETE
        ORG     00CF6H
        CALL    PBOOT
        ORG     00CF9H
        RET
        ORG     00CFAH
        LXI     H,PA+1H
        ORG     00CFDH
        MOV     M,B
        ORG     00CFEH
        DCX     H
        ORG     00CFFH
        MOV     M,C
        ORG     00D00H
        LHLD    PA
        ORG     00D03H
        MOV     B,H
        ORG     00D04H
        MOV     C,L
        ORG     00D05H
        CALL    PPRINT
        ORG     00D08H
        CALL    PCRLF
        ORG     00D0BH
        CALL    PREBOOT
        ORG     00D0EH
        RET
        ORG     00D0FH
        LXI     B,PDFCB
        ORG     00D12H
        CALL    PCLOSE
        ORG     00D15H
        LXI     B,$-0BECH
        ORG     00D18H
        CALL    PABORT
        ORG     00D1BH
        RET
        ORG     00D1CH
        LXI     H,PA+1H
        ORG     00D1FH
        MOV     M,B
        ORG     00D20H
        DCX     H
        ORG     00D21H
        MOV     M,C
        ORG     00D22H
        MVI     L,3H
        ORG     00D24H
        PUSH    H       ; 1
        ORG     00D25H
        LHLD    PA
        ORG     00D28H
        MOV     B,H
        ORG     00D29H
        MOV     C,L
        ORG     00D2AH
        LXI     D,PDFCB+9H
        ORG     00D2DH
        POP     H       ; 1
        ORG     00D2EH
        INR     L
        ORG     00D2FH
        JMP     $+7H
        ORG     00D32H
        LDAX    B
        ORG     00D33H
        STAX    D
        ORG     00D34H
        INX     B
        ORG     00D35H
        INX     D
        ORG     00D36H
        DCR     L
        ORG     00D37H
        JNZ     $-5H
        ORG     00D3AH
        RET
        ORG     00D3BH
        LHLD    PBUFFLENGTH
        ORG     00D3EH
        SHLD    PNSOURCE
        ORG     00D41H
        LXI     H,0H
        ORG     00D44H
        SHLD    PNDEST
        ORG     00D47H
        MOV     A,L
        ORG     00D48H
        STA     PSFCB+0CH
        ORG     00D4BH
        STA     PSFCB+0EH
        ORG     00D4EH
        STA     PSFCB+20H
        ORG     00D51H
        MVI     L,21H
        ORG     00D53H
        LXI     D,PDFCB
        ORG     00D56H
        LXI     B,PFCB
        ORG     00D59H
        INR     L
        ORG     00D5AH
        JMP     $+7H
        ORG     00D5DH
        LDAX    B
        ORG     00D5EH
        STAX    D
        ORG     00D5FH
        INX     B
        ORG     00D60H
        INX     D
        ORG     00D61H
        DCR     L
        ORG     00D62H
        JNZ     $-5H
        ORG     00D65H
        LXI     H,PDDISK
        ORG     00D68H
        LDA     PSDISK
        ORG     00D6BH
        CMP     M
        ORG     00D6CH
        JZ      @10
        ORG     00D6FH
        LHLD    PDDISK
        ORG     00D72H
        MOV     C,L
        ORG     00D73H
        CALL    PSELECT
        ORG     00D76H
        LXI     B,PFCB
        ORG     00D79H
        CALL    PSEARCH
        ORG     00D7CH
        LDA     PDCNT
        ORG     00D7FH
        CPI     0FFH
        ORG     00D81H
        JZ      @11
        ORG     00D84H
        LXI     B,$-0C44H
        ORG     00D87H
        CALL    PABORT
        ORG     00D8AH
        LHLD    PSDISK
        ORG     00D8DH
        MOV     C,L
        ORG     00D8EH
        CALL    PSELECT
        ORG     00D91H
        LXI     B,PFCB
        ORG     00D94H
        CALL    POPEN
        ORG     00D97H
        LDA     PDCNT
        ORG     00D9AH
        CPI     0FFH
        ORG     00D9CH
        JNZ     @12
        ORG     00D9FH
        LXI     B,PFCB
        ORG     00DA2H
        CALL    PMAKE
        ORG     00DA5H
        LDA     PDCNT
        ORG     00DA8H
        CPI     0FFH
        ORG     00DAAH
        JNZ     @13
        ORG     00DADH
        CALL    PFERR
        ORG     00DB0H
        LXI     B,$-0C5AH
        ORG     00DB3H
        CALL    PPRINT
        ORG     00DB6H
        CALL    PCRLF
        ORG     00DB9H
        JMP     @14
        ORG     00DBCH
        LDA     PFCB+9H
        ORG     00DBFH
        RLC
        ORG     00DC0H
        RAR
        ORG     00DC1H
        JNC     @15
        ORG     00DC4H
        LXI     B,$-0C65H
        ORG     00DC7H
        CALL    PPRINT
        ORG     00DCAH
        CALL    PCRLF
        ORG     00DCDH
        JMP     @16
        ORG     00DD0H
        LDA     PFCB+0AH
        ORG     00DD3H
        RLC
        ORG     00DD4H
        RAR
        ORG     00DD5H
        JNC     @17
        ORG     00DD8H
        LXI     B,$-0C61H
        ORG     00DDBH
        CALL    PABORT
        ORG     00DDEH
        LXI     B,$-0C4AH
        ORG     00DE1H
        CALL    PSETTYPE
        ORG     00DE4H
        LXI     B,PDFCB
        ORG     00DE7H
        CALL    PDELETE
        ORG     00DEAH
        LXI     H,PDDISK
        ORG     00DEDH
        LDA     PSDISK
        ORG     00DF0H
        CMP     M
        ORG     00DF1H
        JZ      @18
        ORG     00DF4H
        LHLD    PDDISK
        ORG     00DF7H
        MOV     C,L
        ORG     00DF8H
        CALL    PSELECT
        ORG     00DFBH
        LXI     B,PDFCB
        ORG     00DFEH
        CALL    PDELETE
        ORG     00E01H
        LXI     B,$-0C6AH
        ORG     00E04H
        CALL    PSETTYPE
        ORG     00E07H
        LXI     B,PDFCB
        ORG     00E0AH
        CALL    PDELETE
        ORG     00E0DH
        LXI     B,PDFCB
        ORG     00E10H
        CALL    PMAKE
        ORG     00E13H
        LXI     H,PDFCB+20H
        ORG     00E16H
        MVI     M,0H
        ORG     00E18H
        LDA     PDCNT
        ORG     00E1BH
        CPI     0FFH
        ORG     00E1DH
        JNZ     @19
        ORG     00E20H
        CALL    PFERR
        ORG     00E23H
        LXI     H,1H
        ORG     00E26H
        SHLD    PBASELINE
        ORG     00E29H
        RET
        ORG     00E2AH
        LXI     H,PXFERON
        ORG     00E2DH
        MVI     M,0H
        ORG     00E2FH
        LXI     H,PXFCBE
        ORG     00E32H
        MVI     M,0H
        ORG     00E34H
        LXI     H,PXFCBR
        ORG     00E37H
        MVI     M,0H
        ORG     00E39H
        LXI     H,PXBP
        ORG     00E3CH
        MVI     M,0H
        ORG     00E3EH
        RET
        ORG     00E3FH
        LHLD    PSDISK
        ORG     00E42H
        MOV     C,L
        ORG     00E43H
        CALL    PSELECT
        ORG     00E46H
        LXI     B,PXBUFF
        ORG     00E49H
        CALL    PSETDMA
        ORG     00E4CH
        RET
        ORG     00EB5H
        LXI     H,0H
        ORG     00EB8H
        SHLD    PNSOURCE
        ORG     00EBBH
        RET
        ORG     00E4DH
        CALL    PZN
        ORG     00E50H
        LHLD    PSDISK
        ORG     00E53H
        MOV     C,L
        ORG     00E54H
        CALL    PSELECT
        ORG     00E57H
        LXI     H,PI
        ORG     00E5AH
        MVI     M,0H
        ORG     00E5CH
        LDA     PNBUF
        ORG     00E5FH
        LXI     H,PI
        ORG     00E62H
        CMP     M
        ORG     00E63H
        JC      @228
        ORG     00E66H
        LHLD    PNSOURCE
        ORG     00E69H
        XCHG
        ORG     00E6AH
        LHLD    PSBUFFADR
        ORG     00E6DH
        DAD     D
        ORG     00E6EH
        MOV     B,H
        ORG     00E6FH
        MOV     C,L
        ORG     00E70H
        CALL    PSETDMA
        ORG     00E73H
        LXI     B,PFCB
        ORG     00E76H
        CALL    PDISKREAD
        ORG     00E79H
        STA     PDCNT
        ORG     00E7CH
        CPI     0H
        ORG     00E7EH
        JZ      @20
        ORG     00E81H
        MVI     A,1H
        ORG     00E83H
        LXI     H,PDCNT
        ORG     00E86H
        CMP     M
        ORG     00E87H
        JNC     @21
        ORG     00E8AH
        CALL    PFERR
        ORG     00E8DH
        LHLD    PNSOURCE
        ORG     00E90H
        XCHG
        ORG     00E91H
        LHLD    PSBUFFADR
        ORG     00E94H
        DAD     D
        ORG     00E95H
        MVI     M,1AH
        ORG     00E97H
        LDA     PNBUF
        ORG     00E9AH
        STA     PI
        ORG     00E9DH
        JMP     @22
        ORG     00EA0H
        LXI     D,80H
        ORG     00EA3H
        LHLD    PNSOURCE
        ORG     00EA6H
        DAD     D
        ORG     00EA7H
        SHLD    PNSOURCE
        ORG     00EAAH
        LXI     H,PI
        ORG     00EADH
        INR     M
        ORG     00EAEH
        JNZ     @227
        ORG     00EB1H
        CALL    PZN
        ORG     00EB4H
        RET
        ORG     00EBCH
        LXI     B,PBUFFLENGTH
        ORG     00EBFH
        LXI     D,PNSOURCE
        ORG     00EC2H
        CALL    QP0098
        ORG     00EC5H
        JC      @23
        ORG     00EC8H
        CALL    PFILLSOURCE
        ORG     00ECBH
        LHLD    PNSOURCE
        ORG     00ECEH
        XCHG
        ORG     00ECFH
        LHLD    PSBUFFADR
        ORG     00ED2H
        DAD     D
        ORG     00ED3H
        MOV     A,M
        ORG     00ED4H
        STA     PB
        ORG     00ED7H
        CPI     1AH
        ORG     00ED9H
        JZ      @24
        ORG     00EDCH
        LHLD    PNSOURCE
        ORG     00EDFH
        INX     H
        ORG     00EE0H
        SHLD    PNSOURCE
        ORG     00EE3H
        LDA     PB
        ORG     00EE6H
        RET
        ORG     00F45H
        LXI     H,0H
        ORG     00F48H
        SHLD    PNDEST
        ORG     00F4BH
        RET
        ORG     00EE7H
        LHLD    PDDISK
        ORG     00EEAH
        MOV     C,L
        ORG     00EEBH
        CALL    PSELECT
        ORG     00EEEH
        MVI     C,7H
        ORG     00EF0H
        LXI     H,PNDEST
        ORG     00EF3H
        CALL    QP0091
        ORG     00EF6H
        DCX     H
        ORG     00EF7H
        XCHG
        ORG     00EF8H
        LXI     H,PN
        ORG     00EFBH
        MOV     M,E
        ORG     00EFCH
        MOV     A,E
        ORG     00EFDH
        CPI     0FFH
        ORG     00EFFH
        JNZ     @25
        ORG     00F02H
        RET
        ORG     00F03H
        CALL    PZN
        ORG     00F06H
        LXI     H,PI
        ORG     00F09H
        MVI     M,0H
        ORG     00F0BH
        LDA     PN
        ORG     00F0EH
        LXI     H,PI
        ORG     00F11H
        CMP     M
        ORG     00F12H
        JC      @231
        ORG     00F15H
        LHLD    PNDEST
        ORG     00F18H
        XCHG
        ORG     00F19H
        LHLD    PDBUFFADR
        ORG     00F1CH
        DAD     D
        ORG     00F1DH
        MOV     B,H
        ORG     00F1EH
        MOV     C,L
        ORG     00F1FH
        CALL    PSETDMA
        ORG     00F22H
        LXI     B,PDFCB
        ORG     00F25H
        CALL    PDISKWRITE
        ORG     00F28H
        CPI     0H
        ORG     00F2AH
        JZ      @26
        ORG     00F2DH
        CALL    PFERR
        ORG     00F30H
        LXI     D,80H
        ORG     00F33H
        LHLD    PNDEST
        ORG     00F36H
        DAD     D
        ORG     00F37H
        SHLD    PNDEST
        ORG     00F3AH
        LXI     H,PI
        ORG     00F3DH
        INR     M
        ORG     00F3EH
        JNZ     @230
        ORG     00F41H
        CALL    PZN
        ORG     00F44H
        RET
        ORG     00F4CH
        LXI     H,PB
        ORG     00F4FH
        MOV     M,C
        ORG     00F50H
        LXI     B,PBUFFLENGTH
        ORG     00F53H
        LXI     D,PNDEST
        ORG     00F56H
        CALL    QP0098
        ORG     00F59H
        JC      @27
        ORG     00F5CH
        CALL    PWRITEDEST
        ORG     00F5FH
        LHLD    PNDEST
        ORG     00F62H
        XCHG
        ORG     00F63H
        LHLD    PDBUFFADR
        ORG     00F66H
        DAD     D
        ORG     00F67H
        LDA     PB
        ORG     00F6AH
        MOV     M,A
        ORG     00F6BH
        LHLD    PNDEST
        ORG     00F6EH
        INX     H
        ORG     00F6FH
        SHLD    PNDEST
        ORG     00F72H
        RET
        ORG     00F73H
        LXI     H,PC
        ORG     00F76H
        MOV     M,C
        ORG     00F77H
        LDA     PXBP
        ORG     00F7AH
        CPI     80H
        ORG     00F7CH
        JC      @28
        ORG     00F7FH
        CALL    PSETXDMA
        ORG     00F82H
        LXI     B,PXFCB
        ORG     00F85H
        CALL    PDISKWRITE
        ORG     00F88H
        CPI     0H
        ORG     00F8AH
        JZ      @29
        ORG     00F8DH
        CALL    PFERR
        ORG     00F90H
        LXI     H,PXBP
        ORG     00F93H
        MVI     M,0H
        ORG     00F95H
        LHLD    PXBP
        ORG     00F98H
        MVI     H,0
        ORG     00F9AH
        LXI     B,PXBUFF
        ORG     00F9DH
        DAD     B
        ORG     00F9EH
        LDA     PC
        ORG     00FA1H
        MOV     M,A
        ORG     00FA2H
        LXI     H,PXBP
        ORG     00FA5H
        INR     M
        ORG     00FA6H
        RET
        ORG     01018H
        MVI     L,10H
        ORG     0101AH
        LXI     D,PDFCB+10H
        ORG     0101DH
        LXI     B,PDFCB
        ORG     01020H
        INR     L
        ORG     01021H
        JMP     $+7H
        ORG     01024H
        LDAX    B
        ORG     01025H
        STAX    D
        ORG     01026H
        INX     B
        ORG     01027H
        INX     D
        ORG     01028H
        DCR     L
        ORG     01029H
        JNZ     $-5H
        ORG     0102CH
        RET
        ORG     00FA7H
        LXI     H,PDFUB
        ORG     00FAAH
        MVI     M,0H
        ORG     00FACH
        LHLD    PNDEST
        ORG     00FAFH
        MOV     A,L
        ORG     00FB0H
        ANI     7FH
        ORG     00FB2H
        CPI     0H
        ORG     00FB4H
        JZ      @234
        ORG     00FB7H
        LXI     H,PDFUB
        ORG     00FBAH
        INR     M
        ORG     00FBBH
        MVI     C,1AH
        ORG     00FBDH
        CALL    PPUTDEST
        ORG     00FC0H
        JMP     @233
        ORG     00FC3H
        CALL    PWRITEDEST
        ORG     00FC6H
        LXI     B,PDFCB
        ORG     00FC9H
        CALL    PCLOSE
        ORG     00FCCH
        LDA     PDCNT
        ORG     00FCFH
        CPI     0FFH
        ORG     00FD1H
        JNZ     @30
        ORG     00FD4H
        CALL    PFERR
        ORG     00FD7H
        LXI     B,$-0E3DH
        ORG     00FDAH
        CALL    PSETTYPE
        ORG     00FDDH
        CALL    PMOVEUP
        ORG     00FE0H
        LHLD    PSDISK
        ORG     00FE3H
        MOV     C,L
        ORG     00FE4H
        CALL    PSELECT
        ORG     00FE7H
        MVI     L,10H
        ORG     00FE9H
        LXI     D,PDFCB
        ORG     00FECH
        LXI     B,PFCB
        ORG     00FEFH
        INR     L
        ORG     00FF0H
        JMP     $+7H
        ORG     00FF3H
        LDAX    B
        ORG     00FF4H
        STAX    D
        ORG     00FF5H
        INX     B
        ORG     00FF6H
        INX     D
        ORG     00FF7H
        DCR     L
        ORG     00FF8H
        JNZ     $-5H
        ORG     00FFBH
        LXI     B,PDFCB
        ORG     00FFEH
        CALL    PRENAME
        ORG     01001H
        CALL    PMOVEUP
        ORG     01004H
        LXI     B,$-0E67H
        ORG     01007H
        CALL    PSETTYPE
        ORG     0100AH
        LHLD    PDDISK
        ORG     0100DH
        MOV     C,L
        ORG     0100EH
        CALL    PSELECT
        ORG     01011H
        LXI     B,PDFCB
        ORG     01014H
        CALL    PRENAME
        ORG     01017H
        RET
        ORG     0102DH
        LXI     H,PCHAR
        ORG     01030H
        MOV     M,C
        ORG     01031H
        LDA     PMP
        ORG     01034H
        CPI     0H
        ORG     01036H
        JZ      @31
        ORG     01039H
        RET
        ORG     0103AH
        LHLD    PCHAR
        ORG     0103DH
        MOV     C,L
        ORG     0103EH
        CALL    PPRINTC
        ORG     01041H
        RET
        ORG     01042H
        LXI     H,PC
        ORG     01045H
        MOV     M,C
        ORG     01046H
        LDA     PC
        ORG     01049H
        SUI     61H
        ORG     0104BH
        SBB     A
        ORG     0104CH
        CMA
        ORG     0104DH
        PUSH    PSW     ; 1
        ORG     0104EH
        MVI     A,7AH
        ORG     01050H
        LXI     H,PC
        ORG     01053H
        SUB     M
        ORG     01054H
        SBB     A
        ORG     01055H
        CMA
        ORG     01056H
        POP     B       ; 1
        ORG     01057H
        MOV     C,B
        ORG     01058H
        ANA     C
        ORG     01059H
        RET
        ORG     0105AH
        LXI     H,PC
        ORG     0105DH
        MOV     M,C
        ORG     0105EH
        LHLD    PC
        ORG     01061H
        MOV     C,L
        ORG     01062H
        CALL    PLOWERCASE
        ORG     01065H
        RAR
        ORG     01066H
        JNC     @32
        ORG     01069H
        LDA     PC
        ORG     0106CH
        ANI     5FH
        ORG     0106EH
        RET
        ORG     0106FH
        LDA     PC
        ORG     01072H
        RET
        ORG     01073H
        LXI     H,PC
        ORG     01076H
        MOV     M,C
        ORG     01077H
        LDA     PTRANSLATE
        ORG     0107AH
        RAR
        ORG     0107BH
        JNC     @33
        ORG     0107EH
        LHLD    PC
        ORG     01081H
        MOV     C,L
        ORG     01082H
        CALL    PUCASE
        ORG     01085H
        RET
        ORG     01086H
        LDA     PC
        ORG     01089H
        RET
        ORG     0108AH
        LXI     H,PV+1H
        ORG     0108DH
        MOV     M,B
        ORG     0108EH
        DCX     H
        ORG     0108FH
        MOV     M,C
        ORG     01090H
        LXI     H,2710H
        ORG     01093H
        SHLD    PK
        ORG     01096H
        LXI     H,PZERO
        ORG     01099H
        MVI     M,0H
        ORG     0109BH
        MVI     A,0H
        ORG     0109DH
        LXI     D,PK
        ORG     010A0H
        CALL    QP0101
        ORG     010A3H
        ORA     L
        ORG     010A4H
        JZ      @236
        ORG     010A7H
        LHLD    PV
        ORG     010AAH
        XCHG
        ORG     010ABH
        LHLD    PK
        ORG     010AEH
        CALL    QP0029
        ORG     010B1H
        MOV     A,E
        ORG     010B2H
        STA     PD
        ORG     010B5H
        LHLD    PV
        ORG     010B8H
        XCHG
        ORG     010B9H
        CALL    QP0030
        ORG     010BCH
        SHLD    PV
        ORG     010BFH
        MOV     D,B
        ORG     010C0H
        MOV     E,C
        ORG     010C1H
        LXI     H,0AH
        ORG     010C4H
        CALL    QP0029
        ORG     010C7H
        XCHG
        ORG     010C8H
        SHLD    PK
        ORG     010CBH
        LDA     PD
        ORG     010CEH
        SUI     0H
        ORG     010D0H
        ADI     255
        ORG     010D2H
        SBB     A
        ORG     010D3H
        LXI     H,PZERO
        ORG     010D6H
        ORA     M
        ORG     010D7H
        RAR
        ORG     010D8H
        JNC     @34
        ORG     010DBH
        LXI     H,PZERO
        ORG     010DEH
        MVI     M,1H
        ORG     010E0H
        LDA     PD
        ORG     010E3H
        ADI     30H
        ORG     010E5H
        MOV     C,A
        ORG     010E6H
        CALL    PPRINTC
        ORG     010E9H
        JMP     @35
        ORG     010ECH
        MVI     C,20H
        ORG     010EEH
        CALL    PPRINTC
        ORG     010F1H
        JMP     @235
        ORG     010F4H
        RET
        ORG     010F5H
        LXI     H,PV+1H
        ORG     010F8H
        MOV     M,B
        ORG     010F9H
        DCX     H
        ORG     010FAH
        MOV     M,C
        ORG     010FBH
        LDA     PLINESET
        ORG     010FEH
        RAR
        ORG     010FFH
        JC      @36
        ORG     01102H
        RET
        ORG     01103H
        LHLD    PV
        ORG     01106H
        MOV     B,H
        ORG     01107H
        MOV     C,L
        ORG     01108H
        CALL    PPRINTVALUE
        ORG     0110BH
        MVI     C,3AH
        ORG     0110DH
        CALL    PPRINTC
        ORG     01110H
        MVI     C,20H
        ORG     01112H
        CALL    PPRINTC
        ORG     01115H
        LDA     PINSERTING
        ORG     01118H
        RAR
        ORG     01119H
        JNC     @37
        ORG     0111CH
        MVI     C,20H
        ORG     0111EH
        CALL    PPRINTC
        ORG     01121H
        JMP     @38
        ORG     01124H
        MVI     C,2AH
        ORG     01126H
        CALL    PPRINTC
        ORG     01129H
        RET
        ORG     0112AH
        LHLD    PBASELINE
        ORG     0112DH
        MOV     B,H
        ORG     0112EH
        MOV     C,L
        ORG     0112FH
        CALL    PPRINTLINE
        ORG     01132H
        RET
        ORG     01133H
        LDA     PMP
        ORG     01136H
        CPI     0H
        ORG     01138H
        JZ      @39
        ORG     0113BH
        RET
        ORG     0113CH
        CALL    PPRINTBASE
        ORG     0113FH
        RET
        ORG     01140H
        MVI     A,0H
        ORG     01142H
        LXI     H,PMP
        ORG     01145H
        CMP     M
        ORG     01146H
        JNC     @40
        ORG     01149H
        CALL    PBREAKKEY
        ORG     0114CH
        RAR
        ORG     0114DH
        JNC     @41
        ORG     01150H
        JMP     POVERCOUNT
        ORG     01153H
        LXI     H,PMP
        ORG     01156H
        LDA     PXP
        ORG     01159H
        CMP     M
        ORG     0115AH
        JC      @42
        ORG     0115DH
        MVI     A,0H
        ORG     0115FH
        LXI     D,PMT
        ORG     01162H
        CALL    QP0101
        ORG     01165H
        ORA     L
        ORG     01166H
        JZ      @43
        ORG     01169H
        LHLD    PMT
        ORG     0116CH
        DCX     H
        ORG     0116DH
        SHLD    PMT
        ORG     01170H
        MVI     A,0H
        ORG     01172H
        CALL    QP0094
        ORG     01175H
        ORA     L
        ORG     01176H
        JNZ     @44
        ORG     01179H
        JMP     POVERCOUNT
        ORG     0117CH
        LXI     H,PXP
        ORG     0117FH
        MVI     M,0H
        ORG     01181H
        LDA     PXP
        ORG     01184H
        INR     A
        ORG     01185H
        STA     PXP
        ORG     01188H
        DCR     A
        ORG     01189H
        MOV     C,A
        ORG     0118AH
        MVI     B,0
        ORG     0118CH
        LXI     H,PMACRO
        ORG     0118FH
        DAD     B
        ORG     01190H
        MOV     C,M
        ORG     01191H
        CALL    PUTRAN
        ORG     01194H
        RET
        ORG     01195H
        LDA     PINSERTING
        ORG     01198H
        RAR
        ORG     01199H
        JNC     @45
        ORG     0119CH
        CALL    PREADCHAR
        ORG     0119FH
        MOV     C,A
        ORG     011A0H
        CALL    PUTRAN
        ORG     011A3H
        RET
        ORG     011A4H
        LDA     PREADBUFF
        ORG     011A7H
        RAR
        ORG     011A8H
        JNC     @46
        ORG     011ABH
        LXI     H,PREADBUFF
        ORG     011AEH
        MVI     M,0H
        ORG     011B0H
        LDA     PCOLUMN
        ORG     011B3H
        SUI     0H
        ORG     011B5H
        SUI     1
        ORG     011B7H
        SBB     A
        ORG     011B8H
        LXI     H,PLINESET
        ORG     011BBH
        ANA     M
        ORG     011BCH
        RAR
        ORG     011BDH
        JNC     @47
        ORG     011C0H
        LXI     B,PMAXM
        ORG     011C3H
        LXI     D,PBACK
        ORG     011C6H
        CALL    QP0098
        ORG     011C9H
        JC      @48
        ORG     011CCH
        LXI     B,0H
        ORG     011CFH
        CALL    PPRINTLINE
        ORG     011D2H
        JMP     @49
        ORG     011D5H
        CALL    PPRINTBASE
        ORG     011D8H
        JMP     @50
        ORG     011DBH
        MVI     C,2AH
        ORG     011DDH
        CALL    PPRINTC
        ORG     011E0H
        CALL    PREADCOM
        ORG     011E3H
        LXI     H,PCBP
        ORG     011E6H
        MVI     M,0H
        ORG     011E8H
        MVI     C,0AH
        ORG     011EAH
        CALL    PPRINTC
        ORG     011EDH
        LXI     H,PCOLUMN
        ORG     011F0H
        MVI     M,0H
        ORG     011F2H
        LXI     H,PCOMLEN
        ORG     011F5H
        LDA     PCBP
        ORG     011F8H
        SUB     M
        ORG     011F9H
        SUI     1
        ORG     011FBH
        SBB     A
        ORG     011FCH
        STA     PREADBUFF
        ORG     011FFH
        RAR
        ORG     01200H
        JNC     @51
        ORG     01203H
        LHLD    PCBP
        ORG     01206H
        MVI     H,0
        ORG     01208H
        LXI     B,PCOMBUFF
        ORG     0120BH
        DAD     B
        ORG     0120CH
        MVI     M,0DH
        ORG     0120EH
        LDA     PCBP
        ORG     01211H
        INR     A
        ORG     01212H
        STA     PCBP
        ORG     01215H
        DCR     A
        ORG     01216H
        MOV     C,A
        ORG     01217H
        MVI     B,0
        ORG     01219H
        LXI     H,PCOMBUFF
        ORG     0121CH
        DAD     B
        ORG     0121DH
        MOV     C,M
        ORG     0121EH
        CALL    PUTRAN
        ORG     01221H
        RET
        ORG     01222H
        LHLD    PSDISK
        ORG     01225H
        MOV     C,L
        ORG     01226H
        CALL    PSELECT
        ORG     01229H
        LXI     B,PBUFF
        ORG     0122CH
        CALL    PSETDMA
        ORG     0122FH
        RET
        ORG     01230H
        LDA     PRBP
        ORG     01233H
        CPI     80H
        ORG     01235H
        JC      @52
        ORG     01238H
        CALL    PSETRDMA
        ORG     0123BH
        LXI     B,PRFCB
        ORG     0123EH
        CALL    PDISKREAD
        ORG     01241H
        CPI     0H
        ORG     01243H
        JZ      @53
        ORG     01246H
        MVI     A,1AH
        ORG     01248H
        RET
        ORG     01249H
        LXI     H,PRBP
        ORG     0124CH
        MVI     M,0H
        ORG     0124EH
        LDA     PRBP
        ORG     01251H
        INR     A
        ORG     01252H
        STA     PRBP
        ORG     01255H
        DCR     A
        ORG     01256H
        MOV     C,A
        ORG     01257H
        MVI     B,0
        ORG     01259H
        LXI     H,PBUFF
        ORG     0125CH
        DAD     B
        ORG     0125DH
        MOV     C,M
        ORG     0125EH
        CALL    PUTRAN
        ORG     01261H
        RET
        ORG     01262H
        LXI     H,0FFFFH
        ORG     01265H
        SHLD    PDISTANCE
        ORG     01268H
        RET
        ORG     01269H
        MVI     A,0H
        ORG     0126BH
        LXI     D,PDISTANCE
        ORG     0126EH
        CALL    QP0101
        ORG     01271H
        ORA     L
        ORG     01272H
        SUI     1
        ORG     01274H
        SBB     A
        ORG     01275H
        RET
        ORG     01276H
        LXI     H,0H
        ORG     01279H
        SHLD    PDISTANCE
        ORG     0127CH
        RET
        ORG     0127DH
        CALL    PDISTZERO
        ORG     01280H
        RAR
        ORG     01281H
        JC      @54
        ORG     01284H
        LHLD    PDISTANCE
        ORG     01287H
        DCX     H
        ORG     01288H
        SHLD    PDISTANCE
        ORG     0128BH
        MVI     A,1H
        ORG     0128DH
        RET
        ORG     0128EH
        MVI     A,0H
        ORG     01290H
        RET
        ORG     01291H
        LXI     H,1H
        ORG     01294H
        SHLD    PRELLINE
        ORG     01297H
        LDA     PDIRECTION
        ORG     0129AH
        CPI     0H
        ORG     0129CH
        JNZ     @55
        ORG     0129FH
        LHLD    PDISTANCE
        ORG     012A2H
        INX     H
        ORG     012A3H
        SHLD    PDISTANCE
        ORG     012A6H
        LHLD    PFRONT
        ORG     012A9H
        SHLD    PI
        ORG     012ACH
        LXI     H,0H
        ORG     012AFH
        SHLD    PL
        ORG     012B2H
        DCX     H
        ORG     012B3H
        SHLD    PK
        ORG     012B6H
        JMP     @56
        ORG     012B9H
        LHLD    PBACK
        ORG     012BCH
        SHLD    PI
        ORG     012BFH
        LHLD    PMAXM
        ORG     012C2H
        SHLD    PL
        ORG     012C5H
        LXI     H,1H
        ORG     012C8H
        SHLD    PK
        ORG     012CBH
        LXI     H,PLOOPING
        ORG     012CEH
        MVI     M,1H
        ORG     012D0H
        LDA     PLOOPING
        ORG     012D3H
        RAR
        ORG     012D4H
        JNC     @238
        ORG     012D7H
        LXI     B,PL
        ORG     012DAH
        LXI     D,PI
        ORG     012DDH
        CALL    QP0098
        ORG     012E0H
        ORA     L
        ORG     012E1H
        ADI     255
        ORG     012E3H
        SBB     A
        ORG     012E4H
        STA     PMIDDLE
        ORG     012E7H
        LHLD    PK
        ORG     012EAH
        DCX     D
        ORG     012EBH
        PUSH    PSW     ; 1
        ORG     012ECH
        CALL    QP0017
        ORG     012EFH
        SHLD    PM
        ORG     012F2H
        LXI     B,PMEMORY
        ORG     012F5H
        DAD     B
        ORG     012F6H
        MOV     A,M
        ORG     012F7H
        SUI     0AH
        ORG     012F9H
        ADI     255
        ORG     012FBH
        SBB     A
        ORG     012FCH
        POP     B       ; 1
        ORG     012FDH
        MOV     C,B
        ORG     012FEH
        ANA     C
        ORG     012FFH
        RAR
        ORG     01300H
        JNC     @240
        ORG     01303H
        LHLD    PM
        ORG     01306H
        SHLD    PI
        ORG     01309H
        JMP     @239
        ORG     0130CH
        LHLD    PRELLINE
        ORG     0130FH
        DCX     H
        ORG     01310H
        SHLD    PRELLINE
        ORG     01313H
        LHLD    PDISTANCE
        ORG     01316H
        DCX     H
        ORG     01317H
        SHLD    PDISTANCE
        ORG     0131AH
        MVI     A,0H
        ORG     0131CH
        CALL    QP0094
        ORG     0131FH
        ORA     L
        ORG     01320H
        ADI     255
        ORG     01322H
        SBB     A
        ORG     01323H
        STA     PLOOPING
        ORG     01326H
        LDA     PMIDDLE
        ORG     01329H
        RAR
        ORG     0132AH
        JC      @57
        ORG     0132DH
        LXI     H,PLOOPING
        ORG     01330H
        MVI     M,0H
        ORG     01332H
        LXI     B,PK
        ORG     01335H
        LXI     D,PI
        ORG     01338H
        CALL    QP0098
        ORG     0133BH
        XCHG
        ORG     0133CH
        DCX     H
        ORG     0133DH
        MOV     M,E
        ORG     0133EH
        INX     H
        ORG     0133FH
        MOV     M,D
        ORG     01340H
        JMP     @58
        ORG     01343H
        LDA     PLOOPING
        ORG     01346H
        RAR
        ORG     01347H
        JNC     @59
        ORG     0134AH
        LHLD    PM
        ORG     0134DH
        SHLD    PI
        ORG     01350H
        JMP     @237
        ORG     01353H
        LDA     PDIRECTION
        ORG     01356H
        CPI     0H
        ORG     01358H
        JNZ     @60
        ORG     0135BH
        LHLD    PI
        ORG     0135EH
        SHLD    PFIRST
        ORG     01361H
        LHLD    PFRONT
        ORG     01364H
        DCX     H
        ORG     01365H
        SHLD    PLAST
        ORG     01368H
        JMP     @61
        ORG     0136BH
        LHLD    PBACK
        ORG     0136EH
        INX     H
        ORG     0136FH
        SHLD    PFIRST
        ORG     01372H
        LHLD    PI
        ORG     01375H
        INX     H
        ORG     01376H
        SHLD    PLAST
        ORG     01379H
        RET
        ORG     0137AH
        LHLD    PFRONT
        ORG     0137DH
        INX     H
        ORG     0137EH
        SHLD    PFRONT
        ORG     01381H
        RET
        ORG     01382H
        LHLD    PBACK
        ORG     01385H
        INX     H
        ORG     01386H
        SHLD    PBACK
        ORG     01389H
        RET
        ORG     0138AH
        LHLD    PFRONT
        ORG     0138DH
        DCX     H
        ORG     0138EH
        SHLD    PFRONT
        ORG     01391H
        RET
        ORG     01392H
        LHLD    PBACK
        ORG     01395H
        DCX     H
        ORG     01396H
        SHLD    PBACK
        ORG     01399H
        RET
        ORG     0139AH
        LHLD    PBASELINE
        ORG     0139DH
        INX     H
        ORG     0139EH
        SHLD    PBASELINE
        ORG     013A1H
        RET
        ORG     013A2H
        LXI     H,PMOVEFLAG
        ORG     013A5H
        MOV     M,C
        ORG     013A6H
        LDA     PDIRECTION
        ORG     013A9H
        CPI     1H
        ORG     013ABH
        JNZ     @62
        ORG     013AEH
        LXI     B,PLAST
        ORG     013B1H
        LXI     D,PBACK
        ORG     013B4H
        CALL    QP0098
        ORG     013B7H
        JNC     @242
        ORG     013BAH
        CALL    PINCBACK
        ORG     013BDH
        LDA     PMOVEFLAG
        ORG     013C0H
        RAR
        ORG     013C1H
        JNC     @63
        ORG     013C4H
        LHLD    PBACK
        ORG     013C7H
        LXI     B,PMEMORY
        ORG     013CAH
        DAD     B
        ORG     013CBH
        MOV     A,M
        ORG     013CCH
        STA     PC
        ORG     013CFH
        CPI     0AH
        ORG     013D1H
        JNZ     @64
        ORG     013D4H
        CALL    PINCBASE
        ORG     013D7H
        LHLD    PFRONT
        ORG     013DAH
        LXI     B,PMEMORY
        ORG     013DDH
        DAD     B
        ORG     013DEH
        LDA     PC
        ORG     013E1H
        MOV     M,A
        ORG     013E2H
        CALL    PINCFRONT
        ORG     013E5H
        JMP     @241
        ORG     013E8H
        JMP     @65
        ORG     013EBH
        LXI     D,PFIRST
        ORG     013EEH
        LXI     B,PFRONT
        ORG     013F1H
        CALL    QP0098
        ORG     013F4H
        JNC     @244
        ORG     013F7H
        CALL    PDECFRONT
        ORG     013FAH
        LHLD    PFRONT
        ORG     013FDH
        LXI     B,PMEMORY
        ORG     01400H
        DAD     B
        ORG     01401H
        MOV     A,M
        ORG     01402H
        STA     PC
        ORG     01405H
        CPI     0AH
        ORG     01407H
        JNZ     @66
        ORG     0140AH
        LHLD    PBASELINE
        ORG     0140DH
        DCX     H
        ORG     0140EH
        SHLD    PBASELINE
        ORG     01411H
        LDA     PMOVEFLAG
        ORG     01414H
        RAR
        ORG     01415H
        JNC     @67
        ORG     01418H
        LHLD    PBACK
        ORG     0141BH
        LXI     B,PMEMORY
        ORG     0141EH
        DAD     B
        ORG     0141FH
        LDA     PC
        ORG     01422H
        MOV     M,A
        ORG     01423H
        CALL    PDECBACK
        ORG     01426H
        JMP     @243
        ORG     01429H
        RET
        ORG     0142AH
        MVI     C,1H
        ORG     0142CH
        CALL    PMEMMOVE
        ORG     0142FH
        RET
        ORG     01430H
        MVI     C,0H
        ORG     01432H
        CALL    PMEMMOVE
        ORG     01435H
        RET
        ORG     01436H
        CALL    PSETLIMITS
        ORG     01439H
        CALL    PMOVER
        ORG     0143CH
        RET
        ORG     0143DH
        LDA     PDIRECTION
        ORG     01440H
        CPI     0H
        ORG     01442H
        JNZ     @68
        ORG     01445H
        LHLD    PBACK
        ORG     01448H
        SHLD    PLAST
        ORG     0144BH
        LXI     D,PFRONT
        ORG     0144EH
        LXI     B,PDISTANCE
        ORG     01451H
        CALL    QP0098
        ORG     01454H
        JNC     @69
        ORG     01457H
        LXI     H,1H
        ORG     0145AH
        SHLD    PFIRST
        ORG     0145DH
        JMP     @70
        ORG     01460H
        LXI     B,PDISTANCE
        ORG     01463H
        LXI     D,PFRONT
        ORG     01466H
        CALL    QP0098
        ORG     01469H
        SHLD    PFIRST
        ORG     0146CH
        JMP     @71
        ORG     0146FH
        LHLD    PFRONT
        ORG     01472H
        SHLD    PFIRST
        ORG     01475H
        LXI     B,PBACK
        ORG     01478H
        LXI     D,PMAX
        ORG     0147BH
        CALL    QP0098
        ORG     0147EH
        LXI     D,PDISTANCE
        ORG     01481H
        CALL    QP0102
        ORG     01484H
        JC      @72
        ORG     01487H
        LHLD    PMAXM
        ORG     0148AH
        SHLD    PLAST
        ORG     0148DH
        JMP     @73
        ORG     01490H
        LHLD    PDISTANCE
        ORG     01493H
        XCHG
        ORG     01494H
        LHLD    PBACK
        ORG     01497H
        DAD     D
        ORG     01498H
        SHLD    PLAST
        ORG     0149BH
        RET
        ORG     014DCH
        LXI     H,PB
        ORG     014DFH
        MOV     M,C
        ORG     014E0H
        LDA     PUPPER
        ORG     014E3H
        RAR
        ORG     014E4H
        JNC     @74
        ORG     014E7H
        LHLD    PB
        ORG     014EAH
        MOV     C,L
        ORG     014EBH
        CALL    PUTRAN
        ORG     014EEH
        RET
        ORG     014EFH
        LDA     PB
        ORG     014F2H
        RET
        ORG     0149CH
        LXI     B,PBACK
        ORG     0149FH
        LXI     D,PFRONT
        ORG     014A2H
        CALL    QP0098
        ORG     014A5H
        JC      @75
        ORG     014A8H
        JMP     POVERFLOW
        ORG     014ABH
        CALL    PGETSOURCE
        ORG     014AEH
        MOV     C,A
        ORG     014AFH
        CALL    PCTRAN
        ORG     014B2H
        STA     PB
        ORG     014B5H
        CPI     1AH
        ORG     014B7H
        JNZ     @76
        ORG     014BAH
        CALL    PZERODIST
        ORG     014BDH
        RET
        ORG     014BEH
        LHLD    PFRONT
        ORG     014C1H
        LXI     B,PMEMORY
        ORG     014C4H
        DAD     B
        ORG     014C5H
        LDA     PB
        ORG     014C8H
        MOV     M,A
        ORG     014C9H
        CALL    PINCFRONT
        ORG     014CCH
        LDA     PB
        ORG     014CFH
        CPI     0AH
        ORG     014D1H
        JNZ     @77
        ORG     014D4H
        CALL    PINCBASE
        ORG     014D7H
        RET
        ORG     014D8H
        JMP     @245
        ORG     014DBH
        RET
        ORG     014F3H
        LXI     B,PMAXM
        ORG     014F6H
        LXI     D,PBACK
        ORG     014F9H
        CALL    QP0098
        ORG     014FCH
        JC      @78
        ORG     014FFH
        CALL    PZERODIST
        ORG     01502H
        RET
        ORG     01503H
        CALL    PINCBACK
        ORG     01506H
        LHLD    PBACK
        ORG     01509H
        LXI     B,PMEMORY
        ORG     0150CH
        DAD     B
        ORG     0150DH
        MOV     A,M
        ORG     0150EH
        STA     PB
        ORG     01511H
        MOV     C,A
        ORG     01512H
        CALL    PPUTDEST
        ORG     01515H
        LDA     PB
        ORG     01518H
        CPI     0AH
        ORG     0151AH
        JNZ     @79
        ORG     0151DH
        CALL    PINCBASE
        ORG     01520H
        RET
        ORG     01521H
        JMP     @247
        ORG     01524H
        RET
        ORG     01525H
        CALL    PSETFF
        ORG     01528H
        CALL    PDISTNZERO
        ORG     0152BH
        RAR
        ORG     0152CH
        JNC     @250
        ORG     0152FH
        LXI     B,PBACK
        ORG     01532H
        LXI     D,PMAXM
        ORG     01535H
        CALL    QP0098
        ORG     01538H
        LXI     D,PHMAX
        ORG     0153BH
        CALL    QP0102
        ORG     0153EH
        JC      @80
        ORG     01541H
        CALL    PZERODIST
        ORG     01544H
        JMP     @81
        ORG     01547H
        CALL    PWRITELINE
        ORG     0154AH
        JMP     @249
        ORG     0154DH
        RET
        ORG     0154EH
        LXI     H,PDIRECTION
        ORG     01551H
        MVI     M,0H
        ORG     01553H
        LXI     H,1H
        ORG     01556H
        SHLD    PFIRST
        ORG     01559H
        LHLD    PBACK
        ORG     0155CH
        SHLD    PLAST
        ORG     0155FH
        CALL    PMOVER
        ORG     01562H
        CALL    PDISTZERO
        ORG     01565H
        RAR
        ORG     01566H
        JNC     @82
        ORG     01569H
        CALL    PWRHALF
        ORG     0156CH
        CALL    PDISTNZERO
        ORG     0156FH
        RAR
        ORG     01570H
        JNC     @252
        ORG     01573H
        CALL    PWRITELINE
        ORG     01576H
        JMP     @251
        ORG     01579H
        LXI     B,PLAST
        ORG     0157CH
        LXI     D,PBACK
        ORG     0157FH
        CALL    QP0098
        ORG     01582H
        JNC     @83
        ORG     01585H
        LXI     H,PDIRECTION
        ORG     01588H
        MVI     M,1H
        ORG     0158AH
        CALL    PMOVER
        ORG     0158DH
        RET
        ORG     0158EH
        CALL    PSETFF
        ORG     01591H
        CALL    PWRITEOUT
        ORG     01594H
        RET
        ORG     01595H
        CALL    PCLEARMEM
        ORG     01598H
        CALL    PGETSOURCE
        ORG     0159BH
        STA     PCHAR
        ORG     0159EH
        CPI     1AH
        ORG     015A0H
        JZ      @254
        ORG     015A3H
        LHLD    PCHAR
        ORG     015A6H
        MOV     C,L
        ORG     015A7H
        CALL    PPUTDEST
        ORG     015AAH
        JMP     @253
        ORG     015ADH
        CALL    PFINIS
        ORG     015B0H
        RET
        ORG     015B1H
        LXI     B,PBACK
        ORG     015B4H
        LXI     D,PFRONT
        ORG     015B7H
        CALL    QP0098
        ORG     015BAH
        ORA     L
        ORG     015BBH
        JNZ     @84
        ORG     015BEH
        JMP     POVERFLOW
        ORG     015C1H
        LHLD    PFRONT
        ORG     015C4H
        LXI     B,PMEMORY
        ORG     015C7H
        DAD     B
        ORG     015C8H
        LDA     PCHAR
        ORG     015CBH
        MOV     M,A
        ORG     015CCH
        CALL    PINCFRONT
        ORG     015CFH
        LDA     PCHAR
        ORG     015D2H
        CPI     0AH
        ORG     015D4H
        JNZ     @85
        ORG     015D7H
        CALL    PINCBASE
        ORG     015DAH
        RET
        ORG     015DBH
        CALL    PREADC
        ORG     015DEH
        STA     PCHAR
        ORG     015E1H
        SUI     1AH
        ORG     015E3H
        SUI     1
        ORG     015E5H
        SBB     A
        ORG     015E6H
        PUSH    PSW     ; 1
        ORG     015E7H
        LDA     PCHAR
        ORG     015EAH
        SUI     0DH
        ORG     015ECH
        SUI     1
        ORG     015EEH
        SBB     A
        ORG     015EFH
        PUSH    PSW     ; 2
        ORG     015F0H
        LDA     PINSERTING
        ORG     015F3H
        CMA
        ORG     015F4H
        POP     B       ; 2
        ORG     015F5H
        MOV     C,B
        ORG     015F6H
        ANA     C
        ORG     015F7H
        POP     B       ; 1
        ORG     015F8H
        MOV     C,B
        ORG     015F9H
        ORA     C
        ORG     015FAH
        CMA
        ORG     015FBH
        RET
        ORG     0162AH
        LHLD    PWBE
        ORG     0162DH
        MVI     H,0
        ORG     0162FH
        LXI     B,PSCRATCH
        ORG     01632H
        DAD     B
        ORG     01633H
        LDA     PCHAR
        ORG     01636H
        MOV     M,A
        ORG     01637H
        LDA     PWBE
        ORG     0163AH
        INR     A
        ORG     0163BH
        STA     PWBE
        ORG     0163EH
        CPI     64H
        ORG     01640H
        JC      @86
        ORG     01643H
        JMP     POVERFLOW
        ORG     01646H
        RET
        ORG     015FCH
        CALL    PSCANNING
        ORG     015FFH
        RAR
        ORG     01600H
        JNC     @256
        ORG     01603H
        LDA     PCHAR
        ORG     01606H
        CPI     0CH
        ORG     01608H
        JNZ     @87
        ORG     0160BH
        LXI     H,PCHAR
        ORG     0160EH
        MVI     M,0DH
        ORG     01610H
        CALL    PSETSCR
        ORG     01613H
        LXI     H,PCHAR
        ORG     01616H
        MVI     M,0AH
        ORG     01618H
        LDA     PCHAR
        ORG     0161BH
        CPI     0H
        ORG     0161DH
        JNZ     @88
        ORG     01620H
        JMP     PBADCOM
        ORG     01623H
        CALL    PSETSCR
        ORG     01626H
        JMP     @255
        ORG     01629H
        RET
        ORG     01647H
        LXI     H,PPB
        ORG     0164AH
        MOV     M,E
        ORG     0164BH
        DCX     H
        ORG     0164CH
        MOV     M,C
        ORG     0164DH
        LHLD    PBACK
        ORG     01650H
        SHLD    PJ
        ORG     01653H
        LXI     H,PMATCH
        ORG     01656H
        MVI     M,0H
        ORG     01658H
        LDA     PMATCH
        ORG     0165BH
        CMA
        ORG     0165CH
        LXI     D,PJ
        ORG     0165FH
        LXI     B,PMAXM
        ORG     01662H
        PUSH    PSW     ; 1
        ORG     01663H
        CALL    QP0098
        ORG     01666H
        SBB     A
        ORG     01667H
        POP     B       ; 1
        ORG     01668H
        MOV     C,B
        ORG     01669H
        ANA     C
        ORG     0166AH
        RAR
        ORG     0166BH
        JNC     @258
        ORG     0166EH
        LHLD    PJ
        ORG     01671H
        INX     H
        ORG     01672H
        SHLD    PLAST
        ORG     01675H
        SHLD    PJ
        ORG     01678H
        LDA     PPA
        ORG     0167BH
        STA     PK
        ORG     0167EH
        LHLD    PK
        ORG     01681H
        MVI     H,0
        ORG     01683H
        LXI     B,PSCRATCH
        ORG     01686H
        DAD     B
        ORG     01687H
        PUSH    H       ; 1
        ORG     01688H
        LHLD    PLAST
        ORG     0168BH
        LXI     B,PMEMORY
        ORG     0168EH
        DAD     B
        ORG     0168FH
        POP     D       ; 1
        ORG     01690H
        LDAX    D
        ORG     01691H
        SUB     M
        ORG     01692H
        SUI     1
        ORG     01694H
        SBB     A
        ORG     01695H
        LXI     H,PPB
        ORG     01698H
        PUSH    PSW     ; 1
        ORG     01699H
        LDA     PK
        ORG     0169CH
        SUB     M
        ORG     0169DH
        SUI     1
        ORG     0169FH
        SBB     A
        ORG     016A0H
        STA     PMATCH
        ORG     016A3H
        CMA
        ORG     016A4H
        POP     B       ; 1
        ORG     016A5H
        MOV     C,B
        ORG     016A6H
        ANA     C
        ORG     016A7H
        RAR
        ORG     016A8H
        JNC     @260
        ORG     016ABH
        LXI     H,PK
        ORG     016AEH
        INR     M
        ORG     016AFH
        LHLD    PLAST
        ORG     016B2H
        INX     H
        ORG     016B3H
        SHLD    PLAST
        ORG     016B6H
        JMP     @259
        ORG     016B9H
        JMP     @257
        ORG     016BCH
        LDA     PMATCH
        ORG     016BFH
        RAR
        ORG     016C0H
        JNC     @89
        ORG     016C3H
        LHLD    PLAST
        ORG     016C6H
        DCX     H
        ORG     016C7H
        SHLD    PLAST
        ORG     016CAH
        CALL    PMOVER
        ORG     016CDH
        LDA     PMATCH
        ORG     016D0H
        RET
        ORG     016D1H
        LXI     H,PWBE
        ORG     016D4H
        MVI     M,0H
        ORG     016D6H
        CALL    PCOLLECT
        ORG     016D9H
        LDA     PWBE
        ORG     016DCH
        STA     PWBP
        ORG     016DFH
        RET
        ORG     016E0H
        LHLD    PWBP
        ORG     016E3H
        XCHG
        ORG     016E4H
        MVI     C,0H
        ORG     016E6H
        CALL    PFIND
        ORG     016E9H
        RAR
        ORG     016EAH
        JC      @90
        ORG     016EDH
        JMP     POVERCOUNT
        ORG     016F0H
        RET
        ORG     016F1H
        LHLD    PCHAR
        ORG     016F4H
        MOV     C,L
        ORG     016F5H
        CALL    PUCASE
        ORG     016F8H
        PUSH    PSW     ; 1
        ORG     016F9H
        LDA     PRBP
        ORG     016FCH
        INR     A
        ORG     016FDH
        STA     PRBP
        ORG     01700H
        DCR     A
        ORG     01701H
        MOV     C,A
        ORG     01702H
        MVI     B,0
        ORG     01704H
        LXI     H,PRFCB
        ORG     01707H
        DAD     B
        ORG     01708H
        POP     B       ; 1
        ORG     01709H
        MOV     C,B
        ORG     0170AH
        MOV     M,C
        ORG     0170BH
        RET
        ORG     0170CH
        LHLD    PRELLINE
        ORG     0170FH
        XCHG
        ORG     01710H
        LHLD    PBASELINE
        ORG     01713H
        DAD     D
        ORG     01714H
        MOV     B,H
        ORG     01715H
        MOV     C,L
        ORG     01716H
        CALL    PPRINTLINE
        ORG     01719H
        RET
        ORG     0171AH
        CALL    PSETLIMITS
        ORG     0171DH
        LXI     H,PINSERTING
        ORG     01720H
        MVI     M,1H
        ORG     01722H
        LDA     PDIRECTION
        ORG     01725H
        CPI     1H
        ORG     01727H
        JNZ     @91
        ORG     0172AH
        LXI     H,0H
        ORG     0172DH
        SHLD    PRELLINE
        ORG     01730H
        LHLD    PFRONT
        ORG     01733H
        SHLD    PI
        ORG     01736H
        JMP     @92
        ORG     01739H
        LHLD    PFIRST
        ORG     0173CH
        SHLD    PI
        ORG     0173FH
        LHLD    PI
        ORG     01742H
        DCX     H
        ORG     01743H
        LXI     B,PMEMORY
        ORG     01746H
        DAD     B
        ORG     01747H
        MOV     A,M
        ORG     01748H
        STA     PC
        ORG     0174BH
        SUI     0AH
        ORG     0174DH
        SUI     1
        ORG     0174FH
        SBB     A
        ORG     01750H
        PUSH    PSW     ; 1
        ORG     01751H
        LDA     PCOLUMN
        ORG     01754H
        SUI     0H
        ORG     01756H
        ADI     255
        ORG     01758H
        SBB     A
        ORG     01759H
        POP     B       ; 1
        ORG     0175AH
        MOV     C,B
        ORG     0175BH
        ANA     C
        ORG     0175CH
        RAR
        ORG     0175DH
        JNC     @93
        ORG     01760H
        CALL    PCRLF
        ORG     01763H
        LHLD    PFIRST
        ORG     01766H
        SHLD    PI
        ORG     01769H
        LXI     D,PLAST
        ORG     0176CH
        LXI     B,PI
        ORG     0176FH
        CALL    QP0098
        ORG     01772H
        JC      @262
        ORG     01775H
        LDA     PC
        ORG     01778H
        CPI     0AH
        ORG     0177AH
        JNZ     @94
        ORG     0177DH
        CALL    PPRINTREL
        ORG     01780H
        LHLD    PRELLINE
        ORG     01783H
        INX     H
        ORG     01784H
        SHLD    PRELLINE
        ORG     01787H
        CALL    PBREAKKEY
        ORG     0178AH
        RAR
        ORG     0178BH
        JNC     @95
        ORG     0178EH
        JMP     POVERCOUNT
        ORG     01791H
        LHLD    PI
        ORG     01794H
        LXI     B,PMEMORY
        ORG     01797H
        DAD     B
        ORG     01798H
        MOV     A,M
        ORG     01799H
        STA     PC
        ORG     0179CH
        MOV     C,A
        ORG     0179DH
        CALL    PPRINTC
        ORG     017A0H
        LXI     D,1H
        ORG     017A3H
        LHLD    PI
        ORG     017A6H
        DAD     D
        ORG     017A7H
        SHLD    PI
        ORG     017AAH
        JNC     @261
        ORG     017ADH
        RET
        ORG     017AEH
        LXI     H,17H
        ORG     017B1H
        SHLD    PDISTANCE
        ORG     017B4H
        RET
        ORG     017B5H
        LHLD    PDISTANCE
        ORG     017B8H
        SHLD    PTDIST
        ORG     017BBH
        RET
        ORG     017BCH
        LHLD    PTDIST
        ORG     017BFH
        SHLD    PDISTANCE
        ORG     017C2H
        RET
        ORG     017C3H
        CALL    PSAVEDIST
        ORG     017C6H
        CALL    PSETLPP
        ORG     017C9H
        CALL    PMOVELINES
        ORG     017CCH
        LDA     PDIRECTION
        ORG     017CFH
        STA     PI
        ORG     017D2H
        LXI     H,PDIRECTION
        ORG     017D5H
        MVI     M,1H
        ORG     017D7H
        CALL    PSETLPP
        ORG     017DAH
        CALL    PTYPELINES
        ORG     017DDH
        LDA     PI
        ORG     017E0H
        STA     PDIRECTION
        ORG     017E3H
        LXI     B,PMAXM
        ORG     017E6H
        LXI     D,PLAST
        ORG     017E9H
        CALL    QP0098
        ORG     017ECH
        ORA     L
        ORG     017EDH
        SUI     1
        ORG     017EFH
        SBB     A
        ORG     017F0H
        PUSH    PSW     ; 1
        ORG     017F1H
        MVI     A,1H
        ORG     017F3H
        LXI     D,PFIRST
        ORG     017F6H
        CALL    QP0101
        ORG     017F9H
        ORA     L
        ORG     017FAH
        SUI     1
        ORG     017FCH
        SBB     A
        ORG     017FDH
        POP     B       ; 1
        ORG     017FEH
        MOV     C,B
        ORG     017FFH
        ORA     C
        ORG     01800H
        RAR
        ORG     01801H
        JNC     @96
        ORG     01804H
        CALL    PZERODIST
        ORG     01807H
        JMP     @97
        ORG     0180AH
        CALL    PRESTDIST
        ORG     0180DH
        RET
        ORG     0180EH
        LXI     H,PI
        ORG     01811H
        MVI     M,0H
        ORG     01813H
        MVI     A,13H
        ORG     01815H
        LXI     H,PI
        ORG     01818H
        CMP     M
        ORG     01819H
        JC      @264
        ORG     0181CH
        CALL    PBREAKKEY
        ORG     0181FH
        RAR
        ORG     01820H
        JNC     @98
        ORG     01823H
        JMP     PRESET
        ORG     01826H
        MVI     A,0FAH
        ORG     01828H
        CALL    QP0105
        ORG     0182BH
        LXI     H,PI
        ORG     0182EH
        INR     M
        ORG     0182FH
        JNZ     @263
        ORG     01832H
        RET
        ORG     01833H
        LXI     H,PDIRECTION
        ORG     01836H
        MVI     M,1H
        ORG     01838H
        LXI     H,1H
        ORG     0183BH
        SHLD    PDISTANCE
        ORG     0183EH
        RET
        ORG     0183FH
        CALL    PSETFF
        ORG     01842H
        CALL    PDISTNZERO
        ORG     01845H
        RAR
        ORG     01846H
        JNC     @267
        ORG     01849H
        LXI     B,PHMAX
        ORG     0184CH
        LXI     D,PFRONT
        ORG     0184FH
        CALL    QP0098
        ORG     01852H
        JC      @99
        ORG     01855H
        CALL    PZERODIST
        ORG     01858H
        JMP     @100
        ORG     0185BH
        CALL    PREADLINE
        ORG     0185EH
        JMP     @266
        ORG     01861H
        RET
        ORG     01862H
        LXI     H,PCHAR
        ORG     01865H
        MVI     M,0DH
        ORG     01867H
        CALL    PINSERT
        ORG     0186AH
        LXI     H,PCHAR
        ORG     0186DH
        MVI     M,0AH
        ORG     0186FH
        CALL    PINSERT
        ORG     01872H
        RET
        ORG     01873H
        LXI     H,PTRANSLATE
        ORG     01876H
        MVI     M,1H
        ORG     01878H
        LHLD    PCHAR
        ORG     0187BH
        MOV     C,L
        ORG     0187CH
        CALL    PLOWERCASE
        ORG     0187FH
        STA     PT
        ORG     01882H
        LHLD    PCHAR
        ORG     01885H
        MOV     C,L
        ORG     01886H
        CALL    PUTRAN
        ORG     01889H
        STA     PCHAR
        ORG     0188CH
        LDA     PT
        ORG     0188FH
        CMA
        ORG     01890H
        LXI     H,PUPPER
        ORG     01893H
        ORA     M
        ORG     01894H
        DCX     H
        ORG     01895H
        MOV     M,A
        ORG     01896H
        RET
        ORG     01897H
        LXI     H,PTRANSLATE
        ORG     0189AH
        MVI     M,0H
        ORG     0189CH
        CALL    PREADC
        ORG     0189FH
        STA     PCHAR
        ORG     018A2H
        CALL    PTESTCASE
        ORG     018A5H
        RET
        ORG     018A6H
        LXI     H,PC
        ORG     018A9H
        MOV     M,C
        ORG     018AAH
        LXI     H,PC
        ORG     018ADH
        LDA     PCHAR
        ORG     018B0H
        SUB     M
        ORG     018B1H
        SUI     1
        ORG     018B3H
        SBB     A
        ORG     018B4H
        PUSH    PSW     ; 1
        ORG     018B5H
        LDA     PCOMLEN
        ORG     018B8H
        SUI     1H
        ORG     018BAH
        SUI     1
        ORG     018BCH
        SBB     A
        ORG     018BDH
        POP     B       ; 1
        ORG     018BEH
        MOV     C,B
        ORG     018BFH
        ANA     C
        ORG     018C0H
        PUSH    PSW     ; 1
        ORG     018C1H
        LDA     PMP
        ORG     018C4H
        SUI     0H
        ORG     018C6H
        SUI     1
        ORG     018C8H
        SBB     A
        ORG     018C9H
        POP     B       ; 1
        ORG     018CAH
        MOV     C,B
        ORG     018CBH
        ANA     C
        ORG     018CCH
        RET
        ORG     018CDH
        LXI     H,PC
        ORG     018D0H
        MOV     M,C
        ORG     018D1H
        LHLD    PC
        ORG     018D4H
        MOV     C,L
        ORG     018D5H
        CALL    PSINGLECOM
        ORG     018D8H
        RAR
        ORG     018D9H
        JNC     @101
        ORG     018DCH
        CALL    PCRLF
        ORG     018DFH
        LHLD    PC
        ORG     018E2H
        MOV     C,L
        ORG     018E3H
        CALL    PPRINTCHAR
        ORG     018E6H
        LXI     D,$-1746H
        ORG     018E9H
        MVI     C,9H
        ORG     018EBH
        CALL    PMON1
        ORG     018EEH
        CALL    PREADCHAR
        ORG     018F1H
        MOV     C,A
        ORG     018F2H
        CALL    PUCASE
        ORG     018F5H
        STA     PC
        ORG     018F8H
        CALL    PCRLF
        ORG     018FBH
        LDA     PC
        ORG     018FEH
        CPI     59H
        ORG     01900H
        JZ      @102
        ORG     01903H
        JMP     PSTART
        ORG     01906H
        MVI     A,1H
        ORG     01908H
        RET
        ORG     01909H
        MVI     A,0H
        ORG     0190BH
        RET
        ORG     001C0H
        LXI     SP,QSTACKORIGIN
        ORG     001C3H
        LXI     B,PMEMORY
        ORG     001C6H
        LXI     D,PMAXB
        ORG     001C9H
        CALL    QP0100
        ORG     001CCH
        SHLD    PMAX
        ORG     001CFH
        MVI     C,0AH
        ORG     001D1H
        CALL    QP0093
        ORG     001D4H
        DCX     H
        ORG     001D5H
        XCHG
        ORG     001D6H
        LXI     H,PNBUF
        ORG     001D9H
        MOV     M,E
        ORG     001DAH
        INR     E
        ORG     001DBH
        MVI     D,0
        ORG     001DDH
        MVI     C,8H
        ORG     001DFH
        CALL    QP0087
        ORG     001E2H
        XCHG
        ORG     001E3H
        INX     H
        ORG     001E4H
        MOV     M,E
        ORG     001E5H
        INX     H
        ORG     001E6H
        MOV     M,D
        ORG     001E7H
        LXI     H,400H
        ORG     001EAH
        DAD     D
        ORG     001EBH
        LXI     D,PMAX
        ORG     001EEH
        CALL    QP0102
        ORG     001F1H
        JNC     @103
        ORG     001F4H
        LXI     B,$-4CH
        ORG     001F7H
        CALL    PPRINT
        ORG     001FAH
        CALL    PBOOT
        ORG     001FDH
        LXI     B,PBUFFLENGTH
        ORG     00200H
        LXI     D,PMAX
        ORG     00203H
        CALL    QP0098
        ORG     00206H
        DCX     H
        ORG     00207H
        XCHG
        ORG     00208H
        DCX     H
        ORG     00209H
        MOV     M,E
        ORG     0020AH
        INX     H
        ORG     0020BH
        MOV     M,D
        ORG     0020CH
        MVI     C,1H
        ORG     0020EH
        LXI     H,PBUFFLENGTH
        ORG     00211H
        CALL    QP0091
        ORG     00214H
        XCHG
        ORG     00215H
        DCX     H
        ORG     00216H
        MOV     M,E
        ORG     00217H
        INX     H
        ORG     00218H
        MOV     M,D
        ORG     00219H
        XCHG
        ORG     0021AH
        LXI     D,PMAXB
        ORG     0021DH
        CALL    QP0102
        ORG     00220H
        SHLD    PSBUFFADR
        ORG     00223H
        XCHG
        ORG     00224H
        LHLD    PBUFFLENGTH
        ORG     00227H
        CALL    QP0095
        ORG     0022AH
        SHLD    PDBUFFADR
        ORG     0022DH
        LHLD    PMAX
        ORG     00230H
        LXI     B,PMEMORY
        ORG     00233H
        DAD     B
        ORG     00234H
        MVI     M,0H
        ORG     00236H
        LHLD    PMAX
        ORG     00239H
        DCX     H
        ORG     0023AH
        SHLD    PMAXM
        ORG     0023DH
        MVI     C,1H
        ORG     0023FH
        CALL    QP0093
        ORG     00242H
        SHLD    PHMAX
        ORG     00245H
        LXI     H,PUPPER
        ORG     00248H
        MVI     M,0H
        ORG     0024AH
        LXI     H,PPRINTSUPPRESS
        ORG     0024DH
        MVI     M,0H
        ORG     0024FH
        LXI     H,PLINESET
        ORG     00252H
        MVI     M,1H
        ORG     00254H
        LDA     PFCB+1H
        ORG     00257H
        SUI     20H
        ORG     00259H
        SUI     1
        ORG     0025BH
        SBB     A
        ORG     0025CH
        PUSH    PSW     ; 1
        ORG     0025DH
        LDA     PFCB+11H
        ORG     00260H
        SUI     20H
        ORG     00262H
        ADI     255
        ORG     00264H
        SBB     A
        ORG     00265H
        POP     B       ; 1
        ORG     00266H
        MOV     C,B
        ORG     00267H
        ORA     C
        ORG     00268H
        RAR
        ORG     00269H
        JNC     @104
        ORG     0026CH
        CALL    PFERR
        ORG     0026FH
        LDA     PFCB
        ORG     00272H
        STA     PSDISK
        ORG     00275H
        CPI     0H
        ORG     00277H
        JNZ     @105
        ORG     0027AH
        CALL    PCSELECT
        ORG     0027DH
        STA     PSDISK
        ORG     00280H
        JMP     @106
        ORG     00283H
        LXI     H,PSDISK
        ORG     00286H
        DCR     M
        ORG     00287H
        LXI     H,PFCB
        ORG     0028AH
        MVI     M,0H
        ORG     0028CH
        LDA     PFCB+10H
        ORG     0028FH
        STA     PDDISK
        ORG     00292H
        CPI     0H
        ORG     00294H
        JNZ     @107
        ORG     00297H
        LDA     PSDISK
        ORG     0029AH
        STA     PDDISK
        ORG     0029DH
        JMP     @108
        ORG     002A0H
        LXI     H,PDDISK
        ORG     002A3H
        DCR     M
        ORG     002A4H
        CALL    PXCLEAR
        ORG     002A7H
        CALL    PSETUP
        ORG     002AAH
        LXI     H,PMEMORY
        ORG     002ADH
        MVI     M,0AH
        ORG     002AFH
        LXI     H,1H
        ORG     002B2H
        SHLD    PFRONT
        ORG     002B5H
        LHLD    PMAXM
        ORG     002B8H
        SHLD    PBACK
        ORG     002BBH
        LXI     H,PCOLUMN
        ORG     002BEH
        MVI     M,0H
        ORG     002C0H
        JMP     PSTART
        ORG     002C3H
        LXI     SP,QSTACKORIGIN
        ORG     002C6H
        LXI     H,PFLAG
        ORG     002C9H
        MVI     M,23H
        ORG     002CBH
        JMP     PRESET
        ORG     002CEH
        LXI     SP,QSTACKORIGIN
        ORG     002D1H
        LXI     H,PFLAG
        ORG     002D4H
        MVI     M,3FH
        ORG     002D6H
        JMP     PRESET
        ORG     002D9H
        LXI     SP,QSTACKORIGIN
        ORG     002DCH
        LXI     H,PFLAG
        ORG     002DFH
        MVI     M,3EH
        ORG     002E1H
        LXI     SP,QSTACKORIGIN
        ORG     002E4H
        LXI     H,PPRINTSUPPRESS
        ORG     002E7H
        MVI     M,0H
        ORG     002E9H
        LXI     B,$-137H
        ORG     002ECH
        CALL    PPRINT
        ORG     002EFH
        LHLD    PFLAG
        ORG     002F2H
        MOV     C,L
        ORG     002F3H
        CALL    PPRINTC
        ORG     002F6H
        LXI     B,$-13CH
        ORG     002F9H
        CALL    PPRINTM
        ORG     002FCH
        LHLD    PCHAR
        ORG     002FFH
        MOV     C,L
        ORG     00300H
        CALL    PPRINTC
        ORG     00303H
        CALL    PCRLF
        ORG     00306H
        LXI     SP,QSTACKORIGIN
        ORG     00309H
        LXI     H,PREADBUFF
        ORG     0030CH
        MVI     M,1H
        ORG     0030EH
        LXI     H,PMP
        ORG     00311H
        MVI     M,0H
        ORG     00313H
        LXI     H,PINSERTING
        ORG     00316H
        MVI     M,0H
        ORG     00318H
        CALL    PREADCTRAN
        ORG     0031BH
        LDA     PCBP
        ORG     0031EH
        STA     PMI
        ORG     00321H
        MVI     C,45H
        ORG     00323H
        CALL    PSINGLECOM
        ORG     00326H
        RAR
        ORG     00327H
        JNC     @109
        ORG     0032AH
        CALL    PTERMINATE
        ORG     0032DH
        LXI     H,PDDISK
        ORG     00330H
        LDA     PSDISK
        ORG     00333H
        CMP     M
        ORG     00334H
        JZ      @110
        ORG     00337H
        LDA     PBDISK
        ORG     0033AH
        ANI     0F0H
        ORG     0033CH
        PUSH    PSW     ; 1
        ORG     0033DH
        LDA     PDDISK
        ORG     00340H
        ANI     0FH
        ORG     00342H
        POP     B       ; 1
        ORG     00343H
        MOV     C,B
        ORG     00344H
        ORA     C
        ORG     00345H
        STA     PBDISK
        ORG     00348H
        CALL    PREBOOT
        ORG     0034BH
        JMP     @111
        ORG     0034EH
        MVI     C,48H
        ORG     00350H
        CALL    PSINGLECOM
        ORG     00353H
        RAR
        ORG     00354H
        JNC     @112
        ORG     00357H
        CALL    PTERMINATE
        ORG     0035AH
        LDA     PDDISK
        ORG     0035DH
        STA     PCHAR
        ORG     00360H
        LDA     PSDISK
        ORG     00363H
        STA     PDDISK
        ORG     00366H
        LDA     PCHAR
        ORG     00369H
        STA     PSDISK
        ORG     0036CH
        JMP     PRESTART
        ORG     0036FH
        JMP     @113
        ORG     00372H
        LDA     PCHAR
        ORG     00375H
        CPI     49H
        ORG     00377H
        JNZ     @114
        ORG     0037AH
        LXI     H,PCOMLEN
        ORG     0037DH
        LDA     PCBP
        ORG     00380H
        SUB     M
        ORG     00381H
        SUI     1
        ORG     00383H
        SBB     A
        ORG     00384H
        PUSH    PSW     ; 1
        ORG     00385H
        LDA     PMP
        ORG     00388H
        SUI     0H
        ORG     0038AH
        SUI     1
        ORG     0038CH
        SBB     A
        ORG     0038DH
        POP     B       ; 1
        ORG     0038EH
        MOV     C,B
        ORG     0038FH
        ANA     C
        ORG     00390H
        STA     PINSERTING
        ORG     00393H
        RAR
        ORG     00394H
        JNC     @115
        ORG     00397H
        CALL    PPRINTNMBASE
        ORG     0039AH
        LDA     PCOLUMN
        ORG     0039DH
        STA     PSCOLUMN
        ORG     003A0H
        CALL    PSCANNING
        ORG     003A3H
        RAR
        ORG     003A4H
        JNC     @271
        ORG     003A7H
        LDA     PCHAR
        ORG     003AAH
        CPI     0H
        ORG     003ACH
        JZ      @273
        ORG     003AFH
        LDA     PCHAR
        ORG     003B2H
        SUI     15H
        ORG     003B4H
        SUI     1
        ORG     003B6H
        SBB     A
        ORG     003B7H
        PUSH    PSW     ; 1
        ORG     003B8H
        LDA     PCHAR
        ORG     003BBH
        SUI     18H
        ORG     003BDH
        SUI     1
        ORG     003BFH
        SBB     A
        ORG     003C0H
        POP     B       ; 1
        ORG     003C1H
        MOV     C,B
        ORG     003C2H
        ORA     C
        ORG     003C3H
        PUSH    PSW     ; 1
        ORG     003C4H
        LDA     PCHAR
        ORG     003C7H
        SUI     12H
        ORG     003C9H
        SUI     1
        ORG     003CBH
        SBB     A
        ORG     003CCH
        POP     B       ; 1
        ORG     003CDH
        MOV     C,B
        ORG     003CEH
        ORA     C
        ORG     003CFH
        RAR
        ORG     003D0H
        JNC     @116
        ORG     003D3H
        LXI     H,0H
        ORG     003D6H
        SHLD    PDISTANCE
        ORG     003D9H
        MOV     A,L
        ORG     003DAH
        STA     PDIRECTION
        ORG     003DDH
        LDA     PCHAR
        ORG     003E0H
        CPI     12H
        ORG     003E2H
        JNZ     @117
        ORG     003E5H
        CALL    PCRLF
        ORG     003E8H
        CALL    PTYPELINES
        ORG     003EBH
        JMP     @118
        ORG     003EEH
        CALL    PSETLIMITS
        ORG     003F1H
        CALL    PSETPTRS
        ORG     003F4H
        LDA     PCHAR
        ORG     003F7H
        CPI     15H
        ORG     003F9H
        JNZ     @119
        ORG     003FCH
        CALL    PCRLF
        ORG     003FFH
        CALL    PPRINTNMBASE
        ORG     00402H
        JMP     @120
        ORG     00405H
        LDA     PSCOLUMN
        ORG     00408H
        LXI     H,PCOLUMN
        ORG     0040BH
        CMP     M
        ORG     0040CH
        JNC     @275
        ORG     0040FH
        CALL    PBACKSPACE
        ORG     00412H
        JMP     @274
        ORG     00415H
        JMP     @121
        ORG     00418H
        LDA     PCHAR
        ORG     0041BH
        CPI     8H
        ORG     0041DH
        JNZ     @122
        ORG     00420H
        LDA     PCOLUMN
        ORG     00423H
        STA     PTCOLUMN
        ORG     00426H
        MOV     C,A
        ORG     00427H
        MVI     A,0H
        ORG     00429H
        CMP     C
        ORG     0042AH
        JNC     @123
        ORG     0042DH
        MVI     C,20H
        ORG     0042FH
        CALL    PPRINTNMAC
        ORG     00432H
        MVI     A,1H
        ORG     00434H
        LXI     H,PFRONT
        ORG     00437H
        CALL    QP0103
        ORG     0043AH
        SBB     A
        ORG     0043BH
        PUSH    PSW     ; 1
        ORG     0043CH
        LDA     PSCOLUMN
        ORG     0043FH
        LXI     H,PTCOLUMN
        ORG     00442H
        SUB     M
        ORG     00443H
        SBB     A
        ORG     00444H
        POP     B       ; 1
        ORG     00445H
        MOV     C,B
        ORG     00446H
        ANA     C
        ORG     00447H
        RAR
        ORG     00448H
        JNC     @124
        ORG     0044BH
        LHLD    PFRONT
        ORG     0044EH
        DCX     H
        ORG     0044FH
        LXI     B,PMEMORY
        ORG     00452H
        DAD     B
        ORG     00453H
        MOV     A,M
        ORG     00454H
        CPI     0AH
        ORG     00456H
        JZ      @125
        ORG     00459H
        CALL    PDECFRONT
        ORG     0045CH
        LXI     H,PPRINTSUPPRESS
        ORG     0045FH
        MVI     M,1H
        ORG     00461H
        LXI     H,PCOLUMN
        ORG     00464H
        MVI     M,0H
        ORG     00466H
        LXI     H,0H
        ORG     00469H
        SHLD    PDISTANCE
        ORG     0046CH
        MOV     A,L
        ORG     0046DH
        STA     PDIRECTION
        ORG     00470H
        CALL    PTYPELINES
        ORG     00473H
        LXI     H,PPRINTSUPPRESS
        ORG     00476H
        MVI     M,0H
        ORG     00478H
        LDA     PCOLUMN
        ORG     0047BH
        STA     PQCOLUMN
        ORG     0047EH
        LXI     H,PSCOLUMN
        ORG     00481H
        CMP     M
        ORG     00482H
        JNC     @126
        ORG     00485H
        LDA     PSCOLUMN
        ORG     00488H
        STA     PQCOLUMN
        ORG     0048BH
        LDA     PTCOLUMN
        ORG     0048EH
        STA     PCOLUMN
        ORG     00491H
        LDA     PQCOLUMN
        ORG     00494H
        LXI     H,PCOLUMN
        ORG     00497H
        CMP     M
        ORG     00498H
        JNC     @277
        ORG     0049BH
        CALL    PBACKSPACE
        ORG     0049EH
        JMP     @276
        ORG     004A1H
        LDA     PCOLUMN
        ORG     004A4H
        STA     PTCOLUMN
        ORG     004A7H
        LXI     H,PCHAR
        ORG     004AAH
        MVI     M,0H
        ORG     004ACH
        LDA     PTCOLUMN
        ORG     004AFH
        STA     PCOLUMN
        ORG     004B2H
        JMP     @127
        ORG     004B5H
        LDA     PCHAR
        ORG     004B8H
        CPI     7FH
        ORG     004BAH
        JNZ     @128
        ORG     004BDH
        MVI     A,1H
        ORG     004BFH
        LXI     D,PFRONT
        ORG     004C2H
        CALL    QP0101
        ORG     004C5H
        ORA     L
        ORG     004C6H
        JNZ     @129
        ORG     004C9H
        JMP     PRESET
        ORG     004CCH
        CALL    PDECFRONT
        ORG     004CFH
        LHLD    PFRONT
        ORG     004D2H
        LXI     B,PMEMORY
        ORG     004D5H
        DAD     B
        ORG     004D6H
        MOV     A,M
        ORG     004D7H
        STA     PCHAR
        ORG     004DAH
        MOV     C,A
        ORG     004DBH
        CALL    PPRINTC
        ORG     004DEH
        LDA     PCHAR
        ORG     004E1H
        CPI     0AH
        ORG     004E3H
        JNZ     @130
        ORG     004E6H
        LHLD    PBASELINE
        ORG     004E9H
        DCX     H
        ORG     004EAH
        SHLD    PBASELINE
        ORG     004EDH
        LXI     H,PCHAR
        ORG     004F0H
        MVI     M,0H
        ORG     004F2H
        JMP     @131
        ORG     004F5H
        LHLD    PCHAR
        ORG     004F8H
        MOV     C,L
        ORG     004F9H
        CALL    PGRAPHIC
        ORG     004FCH
        RAR
        ORG     004FDH
        JC      @132
        ORG     00500H
        MVI     C,5EH
        ORG     00502H
        CALL    PPRINTNMAC
        ORG     00505H
        LDA     PCHAR
        ORG     00508H
        ADI     40H
        ORG     0050AH
        MOV     C,A
        ORG     0050BH
        CALL    PPRINTNMAC
        ORG     0050EH
        LDA     PCHAR
        ORG     00511H
        CPI     0CH
        ORG     00513H
        JNZ     @133
        ORG     00516H
        CALL    PINSCRLF
        ORG     00519H
        JMP     @134
        ORG     0051CH
        LDA     PMP
        ORG     0051FH
        CPI     0H
        ORG     00521H
        JNZ     @135
        ORG     00524H
        LDA     PCHAR
        ORG     00527H
        CPI     20H
        ORG     00529H
        JC      @136
        ORG     0052CH
        LXI     H,PCOLUMN
        ORG     0052FH
        INR     M
        ORG     00530H
        JMP     @137
        ORG     00533H
        LDA     PCHAR
        ORG     00536H
        CPI     9H
        ORG     00538H
        JNZ     @138
        ORG     0053BH
        LDA     PCOLUMN
        ORG     0053EH
        ANI     7H
        ORG     00540H
        MOV     C,A
        ORG     00541H
        MVI     A,8H
        ORG     00543H
        SUB     C
        ORG     00544H
        LXI     H,PCOLUMN
        ORG     00547H
        ADD     M
        ORG     00548H
        MOV     M,A
        ORG     00549H
        CALL    PINSERT
        ORG     0054CH
        LDA     PCHAR
        ORG     0054FH
        CPI     0AH
        ORG     00551H
        JNZ     @139
        ORG     00554H
        CALL    PPRINTNMBASE
        ORG     00557H
        LDA     PCHAR
        ORG     0055AH
        CPI     0DH
        ORG     0055CH
        JNZ     @140
        ORG     0055FH
        LXI     H,PCHAR
        ORG     00562H
        MVI     M,0AH
        ORG     00564H
        MVI     C,0AH
        ORG     00566H
        CALL    PPRINTNMAC
        ORG     00569H
        JMP     @141
        ORG     0056CH
        LXI     H,PCHAR
        ORG     0056FH
        MVI     M,0H
        ORG     00571H
        JMP     @272
        ORG     00574H
        JMP     @270
        ORG     00577H
        LDA     PCHAR
        ORG     0057AH
        CPI     1AH
        ORG     0057CH
        JZ      @142
        ORG     0057FH
        CALL    PINSCRLF
        ORG     00582H
        LDA     PLINESET
        ORG     00585H
        LXI     H,PINSERTING
        ORG     00588H
        ANA     M
        ORG     00589H
        RAR
        ORG     0058AH
        JNC     @143
        ORG     0058DH
        CALL    PCRLF
        ORG     00590H
        JMP     @144
        ORG     00593H
        MVI     C,4FH
        ORG     00595H
        CALL    PSINGLERCOM
        ORG     00598H
        RAR
        ORG     00599H
        JNC     @145
        ORG     0059CH
        JMP     PRESTART
        ORG     0059FH
        LDA     PCHAR
        ORG     005A2H
        CPI     52H
        ORG     005A4H
        JNZ     @147
        ORG     005A7H
        LXI     H,PRBP
        ORG     005AAH
        MVI     M,1H
        ORG     005ACH
        CALL    PSETRDMA
        ORG     005AFH
        CALL    PSCANNING
        ORG     005B2H
        RAR
        ORG     005B3H
        JNC     @279
        ORG     005B6H
        MVI     A,8H
        ORG     005B8H
        LXI     H,PRBP
        ORG     005BBH
        CMP     M
        ORG     005BCH
        JNC     @148
        ORG     005BFH
        JMP     POVERCOUNT
        ORG     005C2H
        CALL    PSETRFCB
        ORG     005C5H
        JMP     @278
        ORG     005C8H
        LXI     H,PCHAR
        ORG     005CBH
        MVI     M,20H
        ORG     005CDH
        LDA     PRBP
        ORG     005D0H
        SUI     1H
        ORG     005D2H
        SUI     1
        ORG     005D4H
        SBB     A
        ORG     005D5H
        STA     PFLAG
        ORG     005D8H
        RAR
        ORG     005D9H
        JNC     @149
        ORG     005DCH
        MVI     L,8H
        ORG     005DEH
        LXI     D,PRFCB+1H
        ORG     005E1H
        LXI     B,PXFCB+1H
        ORG     005E4H
        INR     L
        ORG     005E5H
        JMP     $+7H
        ORG     005E8H
        LDAX    B
        ORG     005E9H
        STAX    D
        ORG     005EAH
        INX     B
        ORG     005EBH
        INX     D
        ORG     005ECH
        DCR     L
        ORG     005EDH
        JNZ     $-5H
        ORG     005F0H
        LXI     B,PXFCB
        ORG     005F3H
        CALL    PCLOSE
        ORG     005F6H
        JMP     @150
        ORG     005F9H
        MVI     A,8H
        ORG     005FBH
        LXI     H,PRBP
        ORG     005FEH
        CMP     M
        ORG     005FFH
        JC      @281
        ORG     00602H
        CALL    PSETRFCB
        ORG     00605H
        JMP     @280
        ORG     00608H
        LXI     H,PRFCB+0CH
        ORG     0060BH
        MVI     M,0H
        ORG     0060DH
        LXI     H,PRFCB+20H
        ORG     00610H
        MVI     M,0H
        ORG     00612H
        LXI     B,PRFCB
        ORG     00615H
        CALL    POPEN
        ORG     00618H
        LXI     H,PRBP
        ORG     0061BH
        MVI     M,80H
        ORG     0061DH
        LDA     PDCNT
        ORG     00620H
        CPI     0FFH
        ORG     00622H
        JNZ     @151
        ORG     00625H
        LXI     H,PFLAG
        ORG     00628H
        MVI     M,4FH
        ORG     0062AH
        JMP     PRESET
        ORG     0062DH
        CALL    PREADFILE
        ORG     00630H
        STA     PCHAR
        ORG     00633H
        CPI     1AH
        ORG     00635H
        JZ      @283
        ORG     00638H
        CALL    PINSERT
        ORG     0063BH
        JMP     @282
        ORG     0063EH
        LXI     H,PI
        ORG     00641H
        MVI     M,0H
        ORG     00643H
        LDA     PFLAG
        ORG     00646H
        RAR
        ORG     00647H
        JNC     @152
        ORG     0064AH
        LXI     H,PXBP
        ORG     0064DH
        LDA     PI
        ORG     00650H
        CMP     M
        ORG     00651H
        JNC     @285
        ORG     00654H
        LHLD    PI
        ORG     00657H
        MVI     H,0
        ORG     00659H
        LXI     B,PXBUFF
        ORG     0065CH
        DAD     B
        ORG     0065DH
        MOV     A,M
        ORG     0065EH
        STA     PCHAR
        ORG     00661H
        LXI     H,PI
        ORG     00664H
        INR     M
        ORG     00665H
        CALL    PINSERT
        ORG     00668H
        JMP     @284
        ORG     0066BH
        JMP     @153
        ORG     0066EH
        MVI     C,51H
        ORG     00670H
        CALL    PSINGLERCOM
        ORG     00673H
        RAR
        ORG     00674H
        JNC     @154
        ORG     00677H
        LXI     B,PDFCB
        ORG     0067AH
        CALL    PDELETE
        ORG     0067DH
        CALL    PREBOOT
        ORG     00680H
        JMP     @155
        ORG     0190CH
        LDA     PCHAR
        ORG     0190FH
        SUI     30H
        ORG     01911H
        STA     PI
        ORG     01914H
        MOV     C,A
        ORG     01915H
        MVI     A,9H
        ORG     01917H
        SUB     C
        ORG     01918H
        SBB     A
        ORG     01919H
        CMA
        ORG     0191AH
        RET
        ORG     0191BH
        LXI     H,0H
        ORG     0191EH
        SHLD    PDISTANCE
        ORG     01921H
        CALL    PDIGIT
        ORG     01924H
        RAR
        ORG     01925H
        JNC     @287
        ORG     01928H
        LHLD    PDISTANCE
        ORG     0192BH
        DAD     H
        ORG     0192CH
        DAD     H
        ORG     0192DH
        DAD     H
        ORG     0192EH
        PUSH    H       ; 1
        ORG     0192FH
        LHLD    PDISTANCE
        ORG     01932H
        DAD     H
        ORG     01933H
        POP     B       ; 1
        ORG     01934H
        DAD     B
        ORG     01935H
        PUSH    H       ; 1
        ORG     01936H
        LHLD    PI
        ORG     01939H
        MVI     H,0
        ORG     0193BH
        POP     B       ; 1
        ORG     0193CH
        DAD     B
        ORG     0193DH
        SHLD    PDISTANCE
        ORG     01940H
        CALL    PREADCTRAN
        ORG     01943H
        JMP     @286
        ORG     01946H
        RET
        ORG     01947H
        LXI     D,PBASELINE
        ORG     0194AH
        LXI     B,PDISTANCE
        ORG     0194DH
        CALL    QP0098
        ORG     01950H
        JNC     @156
        ORG     01953H
        LXI     H,PDIRECTION
        ORG     01956H
        MVI     M,1H
        ORG     01958H
        LXI     B,PBASELINE
        ORG     0195BH
        LXI     D,PDISTANCE
        ORG     0195EH
        CALL    QP0098
        ORG     01961H
        XCHG
        ORG     01962H
        DCX     H
        ORG     01963H
        MOV     M,E
        ORG     01964H
        INX     H
        ORG     01965H
        MOV     M,D
        ORG     01966H
        JMP     @157
        ORG     01969H
        LXI     H,PDIRECTION
        ORG     0196CH
        MVI     M,0H
        ORG     0196EH
        LXI     B,PDISTANCE
        ORG     01971H
        LXI     D,PBASELINE
        ORG     01974H
        CALL    QP0098
        ORG     01977H
        SHLD    PDISTANCE
        ORG     0197AH
        RET
        ORG     00683H
        CALL    PSETFORWARD
        ORG     00686H
        LDA     PCHAR
        ORG     00689H
        CPI     2DH
        ORG     0068BH
        JNZ     @158
        ORG     0068EH
        CALL    PREADCTRAN
        ORG     00691H
        LXI     H,PDIRECTION
        ORG     00694H
        MVI     M,0H
        ORG     00696H
        LDA     PCHAR
        ORG     00699H
        CPI     23H
        ORG     0069BH
        JNZ     @159
        ORG     0069EH
        CALL    PSETFF
        ORG     006A1H
        CALL    PREADCTRAN
        ORG     006A4H
        JMP     @160
        ORG     006A7H
        CALL    PDIGIT
        ORG     006AAH
        RAR
        ORG     006ABH
        JNC     @161
        ORG     006AEH
        CALL    PNUMBER
        ORG     006B1H
        LDA     PCHAR
        ORG     006B4H
        CPI     3AH
        ORG     006B6H
        JNZ     @162
        ORG     006B9H
        LXI     H,PCHAR
        ORG     006BCH
        MVI     M,4CH
        ORG     006BEH
        CALL    PRELDISTANCE
        ORG     006C1H
        JMP     @163
        ORG     006C4H
        LDA     PCHAR
        ORG     006C7H
        CPI     3AH
        ORG     006C9H
        JNZ     @164
        ORG     006CCH
        CALL    PREADCTRAN
        ORG     006CFH
        CALL    PNUMBER
        ORG     006D2H
        CALL    PRELDISTANCE
        ORG     006D5H
        LDA     PDIRECTION
        ORG     006D8H
        CPI     1H
        ORG     006DAH
        JNZ     @165
        ORG     006DDH
        LHLD    PDISTANCE
        ORG     006E0H
        INX     H
        ORG     006E1H
        SHLD    PDISTANCE
        ORG     006E4H
        CALL    PDISTZERO
        ORG     006E7H
        RAR
        ORG     006E8H
        JNC     @166
        ORG     006EBH
        LXI     H,PDIRECTION
        ORG     006EEH
        MVI     M,0H
        ORG     006F0H
        LDA     PCHAR
        ORG     006F3H
        CPI     42H
        ORG     006F5H
        JNZ     @167
        ORG     006F8H
        LXI     H,PDIRECTION
        ORG     006FBH
        MVI     A,1H
        ORG     006FDH
        SUB     M
        ORG     006FEH
        MOV     M,A
        ORG     006FFH
        LXI     H,1H
        ORG     00702H
        SHLD    PFIRST
        ORG     00705H
        LHLD    PMAXM
        ORG     00708H
        SHLD    PLAST
        ORG     0070BH
        CALL    PMOVER
        ORG     0070EH
        JMP     @168
        ORG     00711H
        LDA     PCHAR
        ORG     00714H
        CPI     43H
        ORG     00716H
        JNZ     @169
        ORG     00719H
        CALL    PSETCLIMITS
        ORG     0071CH
        CALL    PMOVER
        ORG     0071FH
        JMP     @170
        ORG     00722H
        LDA     PCHAR
        ORG     00725H
        CPI     44H
        ORG     00727H
        JNZ     @171
        ORG     0072AH
        CALL    PSETCLIMITS
        ORG     0072DH
        CALL    PSETPTRS
        ORG     00730H
        JMP     @172
        ORG     00733H
        LDA     PCHAR
        ORG     00736H
        CPI     4BH
        ORG     00738H
        JNZ     @173
        ORG     0073BH
        CALL    PSETLIMITS
        ORG     0073EH
        CALL    PSETPTRS
        ORG     00741H
        JMP     @174
        ORG     00744H
        LDA     PCHAR
        ORG     00747H
        CPI     4CH
        ORG     00749H
        JNZ     @175
        ORG     0074CH
        CALL    PMOVELINES
        ORG     0074FH
        JMP     @176
        ORG     00752H
        LDA     PCHAR
        ORG     00755H
        CPI     50H
        ORG     00757H
        JNZ     @177
        ORG     0075AH
        CALL    PDISTZERO
        ORG     0075DH
        RAR
        ORG     0075EH
        JNC     @178
        ORG     00761H
        LXI     H,PDIRECTION
        ORG     00764H
        MVI     M,1H
        ORG     00766H
        CALL    PSETLPP
        ORG     00769H
        CALL    PTYPELINES
        ORG     0076CH
        JMP     @179
        ORG     0076FH
        CALL    PDISTNZERO
        ORG     00772H
        RAR
        ORG     00773H
        JNC     @289
        ORG     00776H
        CALL    PPAGE
        ORG     00779H
        CALL    PWAIT
        ORG     0077CH
        JMP     @288
        ORG     0077FH
        JMP     @180
        ORG     00782H
        LDA     PCHAR
        ORG     00785H
        CPI     54H
        ORG     00787H
        JNZ     @181
        ORG     0078AH
        CALL    PTYPELINES
        ORG     0078DH
        JMP     @182
        ORG     00790H
        LDA     PCHAR
        ORG     00793H
        CPI     55H
        ORG     00795H
        JNZ     @183
        ORG     00798H
        LDA     PDIRECTION
        ORG     0079BH
        SUI     1H
        ORG     0079DH
        SUI     1
        ORG     0079FH
        SBB     A
        ORG     007A0H
        STA     PUPPER
        ORG     007A3H
        JMP     @184
        ORG     007A6H
        LDA     PCHAR
        ORG     007A9H
        CPI     56H
        ORG     007ABH
        JNZ     @185
        ORG     007AEH
        CALL    PDISTZERO
        ORG     007B1H
        RAR
        ORG     007B2H
        JNC     @186
        ORG     007B5H
        LXI     B,PFRONT
        ORG     007B8H
        LXI     D,PBACK
        ORG     007BBH
        CALL    QP0098
        ORG     007BEH
        MOV     B,H
        ORG     007BFH
        MOV     C,L
        ORG     007C0H
        CALL    PPRINTVALUE
        ORG     007C3H
        MVI     C,2FH
        ORG     007C5H
        CALL    PPRINTC
        ORG     007C8H
        LHLD    PMAXM
        ORG     007CBH
        MOV     B,H
        ORG     007CCH
        MOV     C,L
        ORG     007CDH
        CALL    PPRINTVALUE
        ORG     007D0H
        CALL    PCRLF
        ORG     007D3H
        JMP     @187
        ORG     007D6H
        LDA     PDIRECTION
        ORG     007D9H
        SUI     1H
        ORG     007DBH
        SUI     1
        ORG     007DDH
        SBB     A
        ORG     007DEH
        STA     PLINESET
        ORG     007E1H
        JMP     @188
        ORG     007E4H
        LDA     PCHAR
        ORG     007E7H
        CPI     0DH
        ORG     007E9H
        JNZ     @189
        ORG     007ECH
        LDA     PMI
        ORG     007EFH
        SUI     1H
        ORG     007F1H
        SUI     1
        ORG     007F3H
        SBB     A
        ORG     007F4H
        PUSH    PSW     ; 1
        ORG     007F5H
        LDA     PMP
        ORG     007F8H
        SUI     0H
        ORG     007FAH
        SUI     1
        ORG     007FCH
        SBB     A
        ORG     007FDH
        POP     B       ; 1
        ORG     007FEH
        MOV     C,B
        ORG     007FFH
        ANA     C
        ORG     00800H
        RAR
        ORG     00801H
        JNC     @190
        ORG     00804H
        CALL    PMOVELINES
        ORG     00807H
        CALL    PSETFORWARD
        ORG     0080AH
        CALL    PTYPELINES
        ORG     0080DH
        JMP     @191
        ORG     00810H
        LDA     PDIRECTION
        ORG     00813H
        SUI     1H
        ORG     00815H
        SUI     1
        ORG     00817H
        SBB     A
        ORG     00818H
        PUSH    PSW     ; 1
        ORG     00819H
        CALL    PDISTZERO
        ORG     0081CH
        POP     B       ; 1
        ORG     0081DH
        MOV     C,B
        ORG     0081EH
        ORA     C
        ORG     0081FH
        RAR
        ORG     00820H
        JNC     @192
        ORG     00823H
        LDA     PCHAR
        ORG     00826H
        CPI     41H
        ORG     00828H
        JNZ     @193
        ORG     0082BH
        LXI     H,PDIRECTION
        ORG     0082EH
        MVI     M,1H
        ORG     00830H
        LHLD    PFRONT
        ORG     00833H
        SHLD    PFIRST
        ORG     00836H
        LHLD    PMAXM
        ORG     00839H
        SHLD    PLAST
        ORG     0083CH
        CALL    PMOVER
        ORG     0083FH
        CALL    PDISTZERO
        ORG     00842H
        RAR
        ORG     00843H
        JNC     @194
        ORG     00846H
        CALL    PAPPHALF
        ORG     00849H
        CALL    PDISTNZERO
        ORG     0084CH
        RAR
        ORG     0084DH
        JNC     @291
        ORG     00850H
        CALL    PREADLINE
        ORG     00853H
        JMP     @290
        ORG     00856H
        LXI     H,PDIRECTION
        ORG     00859H
        MVI     M,0H
        ORG     0085BH
        CALL    PMOVER
        ORG     0085EH
        JMP     @195
        ORG     00861H
        LDA     PCHAR
        ORG     00864H
        CPI     46H
        ORG     00866H
        JNZ     @196
        ORG     00869H
        CALL    PSETFIND
        ORG     0086CH
        CALL    PDISTNZERO
        ORG     0086FH
        RAR
        ORG     00870H
        JNC     @293
        ORG     00873H
        CALL    PCHKFOUND
        ORG     00876H
        JMP     @292
        ORG     00879H
        JMP     @197
        ORG     0087CH
        LDA     PCHAR
        ORG     0087FH
        CPI     4AH
        ORG     00881H
        JNZ     @198
        ORG     00884H
        CALL    PSETFIND
        ORG     00887H
        CALL    PCOLLECT
        ORG     0088AH
        LDA     PWBE
        ORG     0088DH
        STA     PWBJ
        ORG     00890H
        CALL    PCOLLECT
        ORG     00893H
        CALL    PDISTNZERO
        ORG     00896H
        RAR
        ORG     00897H
        JNC     @295
        ORG     0089AH
        CALL    PCHKFOUND
        ORG     0089DH
        LDA     PWBP
        ORG     008A0H
        DCR     A
        ORG     008A1H
        STA     PMI
        ORG     008A4H
        LDA     PMI
        ORG     008A7H
        INR     A
        ORG     008A8H
        STA     PMI
        ORG     008ABH
        LXI     H,PWBJ
        ORG     008AEH
        CMP     M
        ORG     008AFH
        JNC     @297
        ORG     008B2H
        LHLD    PMI
        ORG     008B5H
        MVI     H,0
        ORG     008B7H
        LXI     B,PSCRATCH
        ORG     008BAH
        DAD     B
        ORG     008BBH
        MOV     A,M
        ORG     008BCH
        STA     PCHAR
        ORG     008BFH
        CALL    PINSERT
        ORG     008C2H
        JMP     @296
        ORG     008C5H
        LHLD    PFRONT
        ORG     008C8H
        SHLD    PT
        ORG     008CBH
        LHLD    PWBJ
        ORG     008CEH
        MOV     C,L
        ORG     008CFH
        LHLD    PWBE
        ORG     008D2H
        XCHG
        ORG     008D3H
        CALL    PFIND
        ORG     008D6H
        RAR
        ORG     008D7H
        JC      @199
        ORG     008DAH
        JMP     POVERCOUNT
        ORG     008DDH
        LXI     H,PWBJ
        ORG     008E0H
        LDA     PWBE
        ORG     008E3H
        SUB     M
        ORG     008E4H
        LXI     D,PFRONT
        ORG     008E7H
        CALL    QP0101
        ORG     008EAH
        SHLD    PFIRST
        ORG     008EDH
        LXI     H,PDIRECTION
        ORG     008F0H
        MVI     M,0H
        ORG     008F2H
        CALL    PMOVER
        ORG     008F5H
        LHLD    PT
        ORG     008F8H
        SHLD    PFRONT
        ORG     008FBH
        JMP     @294
        ORG     008FEH
        JMP     @200
        ORG     00901H
        LDA     PCHAR
        ORG     00904H
        SUI     4DH
        ORG     00906H
        SUI     1
        ORG     00908H
        SBB     A
        ORG     00909H
        PUSH    PSW     ; 1
        ORG     0090AH
        LDA     PMP
        ORG     0090DH
        SUI     0H
        ORG     0090FH
        SUI     1
        ORG     00911H
        SBB     A
        ORG     00912H
        POP     B       ; 1
        ORG     00913H
        MOV     C,B
        ORG     00914H
        ANA     C
        ORG     00915H
        RAR
        ORG     00916H
        JNC     @201
        ORG     00919H
        LXI     H,PXP
        ORG     0091CH
        MVI     M,0FFH
        ORG     0091EH
        MVI     A,1H
        ORG     00920H
        LXI     D,PDISTANCE
        ORG     00923H
        CALL    QP0101
        ORG     00926H
        ORA     L
        ORG     00927H
        JNZ     @202
        ORG     0092AH
        CALL    PZERODIST
        ORG     0092DH
        CALL    PREADC
        ORG     00930H
        PUSH    PSW     ; 1
        ORG     00931H
        LDA     PXP
        ORG     00934H
        INR     A
        ORG     00935H
        STA     PXP
        ORG     00938H
        MOV     C,A
        ORG     00939H
        MVI     B,0
        ORG     0093BH
        LXI     H,PMACRO
        ORG     0093EH
        DAD     B
        ORG     0093FH
        POP     B       ; 1
        ORG     00940H
        MOV     C,B
        ORG     00941H
        MOV     M,C
        ORG     00942H
        MOV     A,C
        ORG     00943H
        CPI     0DH
        ORG     00945H
        JZ      @299
        ORG     00948H
        JMP     @298
        ORG     0094BH
        LDA     PXP
        ORG     0094EH
        STA     PMP
        ORG     00951H
        LXI     H,PXP
        ORG     00954H
        MVI     M,0H
        ORG     00956H
        PUSH    H       ; 1
        ORG     00957H
        LHLD    PDISTANCE
        ORG     0095AH
        XCHG
        ORG     0095BH
        POP     H       ; 1
        ORG     0095CH
        INX     H
        ORG     0095DH
        MOV     M,E
        ORG     0095EH
        INX     H
        ORG     0095FH
        MOV     M,D
        ORG     00960H
        JMP     @203
        ORG     00963H
        LDA     PCHAR
        ORG     00966H
        CPI     4EH
        ORG     00968H
        JNZ     @204
        ORG     0096BH
        CALL    PSETFIND
        ORG     0096EH
        CALL    PDISTNZERO
        ORG     00971H
        RAR
        ORG     00972H
        JNC     @301
        ORG     00975H
        LHLD    PWBP
        ORG     00978H
        XCHG
        ORG     00979H
        MVI     C,0H
        ORG     0097BH
        CALL    PFIND
        ORG     0097EH
        RAR
        ORG     0097FH
        JC      @303
        ORG     00982H
        CALL    PBREAKKEY
        ORG     00985H
        RAR
        ORG     00986H
        JNC     @205
        ORG     00989H
        JMP     PRESET
        ORG     0098CH
        CALL    PSAVEDIST
        ORG     0098FH
        CALL    PCLEARMEM
        ORG     00992H
        CALL    PAPPHALF
        ORG     00995H
        LXI     H,PDIRECTION
        ORG     00998H
        MVI     M,0H
        ORG     0099AH
        LXI     H,1H
        ORG     0099DH
        SHLD    PFIRST
        ORG     009A0H
        CALL    PMOVER
        ORG     009A3H
        CALL    PRESTDIST
        ORG     009A6H
        LXI     H,PDIRECTION
        ORG     009A9H
        MVI     M,1H
        ORG     009ABH
        LXI     B,PMAXM
        ORG     009AEH
        LXI     D,PBACK
        ORG     009B1H
        CALL    QP0098
        ORG     009B4H
        JC      @206
        ORG     009B7H
        JMP     POVERCOUNT
        ORG     009BAH
        JMP     @302
        ORG     009BDH
        JMP     @300
        ORG     009C0H
        JMP     @207
        ORG     009C3H
        LDA     PCHAR
        ORG     009C6H
        CPI     53H
        ORG     009C8H
        JNZ     @208
        ORG     009CBH
        CALL    PSETFIND
        ORG     009CEH
        CALL    PCOLLECT
        ORG     009D1H
        CALL    PDISTNZERO
        ORG     009D4H
        RAR
        ORG     009D5H
        JNC     @305
        ORG     009D8H
        CALL    PCHKFOUND
        ORG     009DBH
        LDA     PWBP
        ORG     009DEH
        STA     PMI
        ORG     009E1H
        LXI     D,PFRONT
        ORG     009E4H
        CALL    QP0101
        ORG     009E7H
        XCHG
        ORG     009E8H
        DCX     H
        ORG     009E9H
        MOV     M,E
        ORG     009EAH
        INX     H
        ORG     009EBH
        MOV     M,D
        ORG     009ECH
        LXI     H,PWBE
        ORG     009EFH
        LDA     PMI
        ORG     009F2H
        CMP     M
        ORG     009F3H
        JNC     @307
        ORG     009F6H
        LHLD    PMI
        ORG     009F9H
        MVI     H,0
        ORG     009FBH
        LXI     B,PSCRATCH
        ORG     009FEH
        DAD     B
        ORG     009FFH
        MOV     A,M
        ORG     00A00H
        STA     PCHAR
        ORG     00A03H
        LXI     H,PMI
        ORG     00A06H
        INR     M
        ORG     00A07H
        CALL    PINSERT
        ORG     00A0AH
        JMP     @306
        ORG     00A0DH
        JMP     @304
        ORG     00A10H
        JMP     @209
        ORG     00A13H
        LDA     PCHAR
        ORG     00A16H
        CPI     57H
        ORG     00A18H
        JNZ     @210
        ORG     00A1BH
        CALL    PWRITEOUT
        ORG     00A1EH
        JMP     @211
        ORG     00A21H
        LDA     PCHAR
        ORG     00A24H
        CPI     58H
        ORG     00A26H
        JNZ     @212
        ORG     00A29H
        CALL    PSETXDMA
        ORG     00A2CH
        CALL    PDISTZERO
        ORG     00A2FH
        RAR
        ORG     00A30H
        JNC     @213
        ORG     00A33H
        CALL    PXCLEAR
        ORG     00A36H
        LXI     B,PXFCB
        ORG     00A39H
        CALL    PDELETE
        ORG     00A3CH
        JMP     @214
        ORG     00A3FH
        LDA     PXFERON
        ORG     00A42H
        RAR
        ORG     00A43H
        JC      @215
        ORG     00A46H
        CALL    PXCLEAR
        ORG     00A49H
        LXI     H,PXFERON
        ORG     00A4CH
        MVI     M,1H
        ORG     00A4EH
        LXI     B,PXFCB
        ORG     00A51H
        CALL    PDELETE
        ORG     00A54H
        LXI     B,PXFCB
        ORG     00A57H
        CALL    PMAKE
        ORG     00A5AH
        LDA     PDCNT
        ORG     00A5DH
        CPI     0FFH
        ORG     00A5FH
        JNZ     @216
        ORG     00A62H
        CALL    PFERR
        ORG     00A65H
        CALL    PSETLIMITS
        ORG     00A68H
        LHLD    PFIRST
        ORG     00A6BH
        SHLD    PI
        ORG     00A6EH
        LXI     D,PLAST
        ORG     00A71H
        LXI     B,PI
        ORG     00A74H
        CALL    QP0098
        ORG     00A77H
        JC      @309
        ORG     00A7AH
        LHLD    PI
        ORG     00A7DH
        LXI     B,PMEMORY
        ORG     00A80H
        DAD     B
        ORG     00A81H
        MOV     C,M
        ORG     00A82H
        CALL    PPUTXFER
        ORG     00A85H
        LXI     D,1H
        ORG     00A88H
        LHLD    PI
        ORG     00A8BH
        DAD     D
        ORG     00A8CH
        SHLD    PI
        ORG     00A8FH
        JNC     @308
        ORG     00A92H
        JMP     @217
        ORG     00A95H
        LDA     PCHAR
        ORG     00A98H
        CPI     5AH
        ORG     00A9AH
        JNZ     @218
        ORG     00A9DH
        CALL    PDISTZERO
        ORG     00AA0H
        RAR
        ORG     00AA1H
        JNC     @219
        ORG     00AA4H
        CALL    PREADCHAR
        ORG     00AA7H
        CPI     1AH
        ORG     00AA9H
        JNZ     @220
        ORG     00AACH
        JMP     PRESET
        ORG     00AAFH
        CALL    PDISTNZERO
        ORG     00AB2H
        RAR
        ORG     00AB3H
        JNC     @311
        ORG     00AB6H
        CALL    PWAIT
        ORG     00AB9H
        JMP     @310
        ORG     00ABCH
        JMP     @221
        ORG     00ABFH
        LDA     PCHAR
        ORG     00AC2H
        CPI     0H
        ORG     00AC4H
        JZ      @222
        ORG     00AC7H
        JMP     PBADCOM
        ORG     00ACAH
        JMP     @223
        ORG     00ACDH
        JMP     PBADCOM
        ORG     00AD0H
        JMP     @268
        ORG     00AD3H
        EI
        ORG     00AD4H
        HLT
        END
