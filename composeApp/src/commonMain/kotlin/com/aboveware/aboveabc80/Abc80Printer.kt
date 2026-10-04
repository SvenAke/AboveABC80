package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.printer.VirtualPrinter

@Suppress("SpellCheckingInspection")
@ExperimentalUnsignedTypes
@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
class Abc80Printer : Abc80Bus.BusInterface {

    /**
    Med default nedan menas att värdet används om inget annat anges.
    Första bokstaven anger printertyp eller RGB-driver.
    Exempel: PR:V
    BOKSTAV                      FUNKTION
    P                            SP1
    U                            UART
    V (default)                  V24 simulerad UART
    C                            Centronics
    R                            RGB, se separat beskrivning

    Andra bokstaven anger paritetsbit.
    BOKSTAV                     FUNKTION
    'S' (default)               SPACE (ingen paritet)
    'M'                         MARK
    'E'                         JÄMNA
    'O'                         UDDA
    exempel: ?R:VS

    Tredje bokstaven anger antal NULLS som skall sändas efter <LF>~
    exempel: PR:VSA
    BOKSTAV                   FUNKTION
    A (default)               O NULLS
    B                         2 NULLS
    C                         4 NULLS
    z                         50 NULLS

    Fjärde positionen anger tecken/rad.
    SIFFRA                   FUNKTION
    l                        40 T/R
    2                        72 T/R
    3 (default)              80 T/R
    4                        120 T/R
    5                        132 T/R
    6                        158 T/R
    7                        255 T/R
    exempel: PR:VSA2

    Femte positionen anger antal rader att hoppa över vid sidslut
    (perforeringsskip) .
    Om O rader, st skickas ingen LF vid sidslut.
    exempel: PR:VSA26
    SIFFRA                            FUNKTION
    0 (default)                       O RADER
    ....9 (Default)                   9 RADER

    Sjätte positionen anger om man skall simulera forrnfeed, d v s
    att printern inte accepterar form-feed-tecknet, utan man får
    simulera med hjälp av upprepade radframmatningar. Man specificerar
    även om automatisk radframrnatning vid radslut önskas.
    exempel: PR:VSA36C
    BOKSTAV                    FUNKTION
    A                          NO AUTO LF + NO SIM
    B                          NO AUTO LF + SIM
    C (default)                AUTO LF + NO SIM
    D                          AUTO LF + S Il4

    Sjunde och åttonde positionerna anger rader/sida.
    OBS~ Om färre än 10 rader/sida önskas måste detta skrivas som
    två siffror meä en nolla i sjunde positionen. T ex PR:PSA20N07
    ger sju rader/sida. Default-värde är 46 rader/sida.
    Efter '.' i nionde positionen anges baudrate om V24-uttaget
    används.
    SIFFRA                          FUNKTION
    l                               110 BAUD
    2                               300 BAUD
    3                               600 BAUD
    4 (default)                     1200 BAUD
    5                               2400 BAUD
    6                               4800 BA.UD
    7                               9600 BAUD
    exempel: PR:VSA36C 70.4

    Baudrate har enbart betydelse för den simulerade UART:n på
    V24 uttaget och behöver ej heller specificeras utom vid
    V24 användning.
    Om enbart "PR:" specificeras så försöker promet gissa vilken
    printertyp det är. Detta går till enligt följande:
    Om det finns ett SPI-kort anslutet väljs typ 'P'.
    Om det finns ett UART-kort anslutet väljs typ 'U'.
    Om det finns ett Centronicsinterface anslutet väljs typ 'C'.
    Om inget kort finns anslutet väljs typ 'V', d v s simulerad UART
    på V24 uttaget.
    Därefter tas de åefault värden som redovisats tidiqare.
    Om det står något efter "PR:" så måste ALL1~ optioner specificeras,
    men detta behöver bara göras en gång vid varje påslagningstillfälIe
    av J'J3C-80 eftersom dessa värden sedan ersätter de inprogrammerade
    defaultvärdena vid senare användande av printer, d v s
    om man först skriver
    LIST PR:VSC12BIO
    och sedan skriver enbart
    LIST PR:
    så används de olian specificerade optionerna istället för de som
    finns i promet.
     */
    fun run() {
    }

    private fun print(data: UByte) {
        val code = data.toInt()
        Abc80Log.printer("$state 0x${code.toString(16).padStart(2, '0')}${if (code and 0x7F in 0x20..0x7E) " '${(code and 0x7F).toChar()}'" else ""}")
        when (state) {
            STATE.NORMAL -> normal(data)
            STATE.FF -> ff(data)
            STATE.FILE -> state = STATE.NORMAL
            STATE.CONSOLE -> console(data)
        }
    }

    private fun console(data: UByte) {}

    private fun ff(data: UByte) {
        when (data.toInt()) {
            0x00 -> end()
            in 0xa0..0xbf -> state = STATE.NORMAL // todo file!
            0xc0 -> state = STATE.CONSOLE
            else -> {
                output(data)
                state = STATE.NORMAL
            }
        }
    }

    private fun end() {
        state = STATE.NORMAL
        VirtualPrinter.instance.isVisible = true
    }

    private fun normal(data: UByte) {
        if (data == 0xffu.toUByte()) state = STATE.FF else output(data)
    }

    private fun output(data: UByte) {
        val printer = VirtualPrinter.instance
        printer.isVisible = true
        printer.printChar((data.toInt() and 0x7F).toChar())
    }

    override fun onWrite(port: UByte, data: UByte, status: UByte) {
        when (port) {
            0x00u.toUByte() -> print(data)
            0x04u.toUByte() -> reset()
        }
    }

    override fun onRead(port: UByte) =
        when (port.toInt()) {
            0 -> read()
            1 -> poll()
            else -> 0xffu.toUByte()
        }

    private fun poll(): UByte = if (readQueue.isEmpty()) 0x40.toUByte() else 0x00.toUByte()

    private fun read(): UByte = if (readQueue.isEmpty()) 0xff.toUByte() else readQueue.removeAt(readQueue.lastIndex)

    private enum class STATE {
        NORMAL,         /* Normal operation */
        FF,             /* 0xFF received */
        FILE,           /* File operation in progress */
        CONSOLE         /* Output to console */
    }

    private var readQueue = mutableListOf<UByte>()
    private var state = STATE.NORMAL
    private var init = false

    fun reset() {
        if (!init) {
            init = true
            state = STATE.NORMAL
        }
    }
}