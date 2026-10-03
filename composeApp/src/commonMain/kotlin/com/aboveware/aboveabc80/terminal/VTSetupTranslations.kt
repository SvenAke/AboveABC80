package com.aboveware.aboveabc80.terminal

object VTSetupTranslations {
    // Array order: [English, FR Short, DE Short, FR Long, DE Long]
    private val translations = mapOf(
        "Keyboard" to arrayOf("Keyboard", "Clavier", "Tastatur", "Clavier", "Tastatur"),
        "Transmit" to arrayOf("Transmit", "Emiss.=", "Senden", "Emission=", "Senden"),
        "Receive=" to arrayOf("Receive=", "Récep.=", "Empfang=", "Réception=", "Empfang="),
        "Speed=" to arrayOf("Speed=", "Vit.=", "Geschw.=", "Vitesse=", "Geschwindigkeit="),
        "Parity" to arrayOf("Parity", "parité", "Parität", "parité", "Parität"),
        "Print" to arrayOf("Print", "Imprim", "drucken", "Imprimante", "drucken"),
        "No Check" to arrayOf(
            "No Check",
            "sans vérif",
            "keine Pr.",
            "sans vérification",
            "keine Prüfung"
        ),
        "7 Bits" to arrayOf("7 Bits", "7 bits", "7 Bit", "7 bits", "7 Bit"),
        "8 Bits" to arrayOf("8 Bits", "8 bits", "8 Bit", "8 bits", "8 Bit"),
        "Set-Up" to arrayOf("Set-Up", "Réglage", "Setup", "Réglage", "Setup"),
        "Display" to arrayOf("Display", "Écran", "ANZEIGE", "Affichage", "ANZEIGE"),
        "General" to arrayOf("General", "Général", "ALLGEMEINES", "Général", "ALLGEMEINES"),
        "Printer" to arrayOf("Printer", "Imprim.", "DRUCKER", "Imprimante", "DRUCKER"),
        "On-Line" to arrayOf("On-Line", "En ligne", "Online", "En ligne", "Online"),
        "Local" to arrayOf("Local", "Local", "Lok", "Local", "Lokal"),
        "80" to arrayOf("80", "80", "80", "80", "80"),
        "132" to arrayOf("132", "132", "132", "132", "132"),
        "Directory" to arrayOf("Directory", "Répert.", "ÜBERSICHT", "Répertoire", "ÜBERSICHT"),
        "Wrap" to arrayOf("Wrap", "Retour", "umbruch", "Retour", "umbruch"),
        "Jump" to arrayOf("Jump", "rapide", "schnell", "rapide", "schnell"),
        "Smooth" to arrayOf("Smooth", "lent", "glatt", "lent", "glatt"),
        "Light" to arrayOf("Light", "Clair", "hell", "Clair", "hell"),
        "Dark" to arrayOf("Dark", "Sombre", "dunkel", "Sombre", "dunkel"),
        "Underline" to arrayOf("Underline", "Souligné", "Strich", "Souligné", "Strich"),
        "Style" to arrayOf("Style", "Style", "darst.", "Style", "darstellung"),
        "Lock" to arrayOf("Lock", "Verr.", "Sperre", "Verrouillage", "Sperre"),
        "Indicator" to arrayOf("Indicator", "Indicat.", "Indikator", "Indicateur", "Indikator"),
        "Unlock" to arrayOf("Unlock", "Déver.", "entsp.", "Déverrouillé", "entsperrt"),
        "Locked" to arrayOf("Locked", "Verr.", "gesp.", "Verrouillé", "gesperrt"),
        "7-bit" to arrayOf("7-bit", "7 bits", "7-bit", "7 bits", "7-bit"),
        "8-bit" to arrayOf("8-bit", "8 bits", "8-bit", "8 bits", "8-bit"),
        "Numeric" to arrayOf("Numeric", "num", "Num.", "numérique", "Numerisch"),
        "Application" to arrayOf("Application", "applic", "Anwend.", "application", "Anwendung"),
        "New Line" to arrayOf(
            "New Line",
            "Nelle Ligne",
            "n. Zeile",
            "Nouvelle Ligne",
            "neue Zeile"
        ),
        "UPSS" to arrayOf("UPSS", "UPSS", "Bevorz.", "UPSS", "Bevorzugt"),
        "Supplemental" to arrayOf(
            "Supplemental",
            "Supplém.",
            "Ergänz.",
            "Supplémentaire",
            "Ergänzung"
        ),
        "Latin-1" to arrayOf("Latin-1", "Latin-1", "ISO-MCS", "Latin-1", "ISO-MCS"),
        "RS-232" to arrayOf("RS-232", "RS-232", "RS-232", "RS-232", "RS-232"),
        "Modem" to arrayOf("Modem", "Modem", "modem", "Modem", "modem"),
        "60 ms" to arrayOf("60 ms", "60 ms", "60 ms", "60 ms", "60 ms"),
        "Unlimited" to arrayOf("Unlimited", "Illim.", "unbegr.", "Illimité", "unbegrenzt"),
        "Limited" to arrayOf("Limited", "Limité", "begr.", "Limité", "begrenzt"),
        "Full Page" to arrayOf("Full Page", "Plein p.", "V. Seite", "Plein page", "Volle Seite"),
        "Only" to arrayOf("Only", "Seul", "Nur", "Seul", "Nur"),
        "All" to arrayOf("All", "Tout", "Alle", "Tout", "Alle"),
        "Terminator = FF" to arrayOf(
            "Terminator = FF",
            "term. = FF",
            "Abschl. = FF",
            "terminateur = FF",
            "Abschluss = FF"
        ),
        "Caps" to arrayOf("Caps", "MAJ", "Caps", "MAJ", "Caps"),
        "Shift" to arrayOf("Shift", "Maj", "Shift", "Maj", "Shift"),
        "Auto Repeat" to arrayOf(
            "Auto Repeat",
            "Répét. Auto",
            "Typamat.",
            "Répétition Auto",
            "Typamatik"
        ),
        "Keyclick" to arrayOf("Keyclick", "Bruit t.", "klick", "Bruit touches", "klick"),
        "Margin Bell" to arrayOf(
            "Margin Bell",
            "Bip marge",
            "Randsign.",
            "Bip marge",
            "Randsignal"
        ),
        "Warning Bell" to arrayOf(
            "Warning Bell",
            "Bip avert.",
            "Warn-Gl.",
            "Bip avertissement",
            "Warn-Glocke"
        ),
        "Compose" to arrayOf("Compose", "composé", "Kombiz", "composé", "Kombiz"),
        "Delete" to arrayOf("Delete", "suppr", "Lösch.", "supprimer", "Löschen"),
        "Backspace" to arrayOf("Backspace", "Backspace", "Backspace", "Backspace", "Backspace"),
        "Tab" to arrayOf("Tab", "Tab", "TABUL.", "Tab", "TABULATOR"),
        "ENTER" to arrayOf("ENTER", "VALID.", "EING.", "VALIDER", "EINGABE"),
        "Ready" to arrayOf("Ready", "Prête", "bereit", "Prête", "bereit"),
        "Done" to arrayOf("Done", "Terminé", "Fertig", "Terminé", "Fertig"),
        "Set-Up Directory" to arrayOf(
            "Set-Up Directory",
            "Rép. Régl.",
            "Setup-Verz.",
            "Répertoire Réglage",
            "Setup-Verzeichnis"
        ),
        "Set-Up English" to arrayOf(
            "Set-Up English",
            "Réglage Angl.",
            "Setup Engl.",
            "Réglage Anglais",
            "Setup Englisch"
        ),
        "Set-Up Francais" to arrayOf(
            "Set-Up Français",
            "Réglage Franç.",
            "Setup Frz.",
            "Réglage Français",
            "Setup Französisch"
        ),
        "Set-Up Deutsch" to arrayOf(
            "Set-Up Deutsch",
            "Réglage Allem.",
            "Setup Deut.",
            "Réglage Allemand",
            "Setup Deutsch"
        ),
        "Answerback=" to arrayOf("Answerback=", "Réponse=", "Antwort=", "Réponse=", "Antwort="),
        "<Concealed>" to arrayOf(
            "<Concealed>",
            "<Masqué>",
            "<Verborg.>",
            "<Masqué>",
            "<Verborgen>"
        ),
        "Enter Answerback " to arrayOf(
            "Enter Answerback ",
            "Entrer Rép. ",
            "Antw. eing.",
            "Entrer Réponse ",
            "Antwort eingeben "
        ),
        "Press ENTER to change this field - Press Cursor Keys to move" to arrayOf(
            "Press ENTER to change this field - Press Cursor Keys to move",
            "ENTREE changer/Fleches déplac.",
            "EINGABE Ändern/Pfeile Bewegen",
            "Appuyez sur ENTREE pour changer ce champ",
            "Drücken Sie EINGABE um diesen, weiter mit den Pfeiltasten"
        ),
        "Auto" to arrayOf("Auto", "Auto", "Auto", "Auto", "Auto"),
        "Auto Print" to arrayOf(
            "Auto Print",
            "Impr. Auto",
            "Aut. Dr.",
            "Impress. Auto",
            "Autom. Druck"
        ),
        "%sAuto Answerback" to arrayOf(
            "%sAuto Answerback",
            "%sRép. Auto",
            "%sAut. Antw.",
            "%sRép. Auto",
            "%sAutom. Antwort"
        ),
        "%sAuto Repeat" to arrayOf(
            "%sAuto Repeat",
            "%sRépét. Auto",
            "%sTypamatik %s",
            "%sRépét. Auto",
            "%sTypamatik %s"
        ),
        "All Rights Preserved" to arrayOf(
            "All Rights Preserved",
            "T. droits rés.",
            "All. R. bew.",
            "Tous droits réservés",
            "Alle Rechte bewahrt"
        ),
        "Block" to arrayOf("Block", "Bloc", "Block", "Bloc", "Block"),
        "%sBreak" to arrayOf(
            "%sBreak",
            "%sInterr.",
            "%sUnterr.",
            "%sInterruption",
            "%sUnterbrechen"
        ),
        "%s Characters" to arrayOf(
            "%s Characters",
            "%s Caract.",
            "%s Zeich.",
            "%s Caractères",
            "%s Zeichen"
        ),
        "Clear All Tabs" to arrayOf(
            "Clear All Tabs",
            "Eff. Tabuls.",
            "Tabs lösch.",
            "Effac. Tabuls.",
            "Alle Tabs löschen"
        ),
        "Clear Comm" to arrayOf(
            "Clear Comm",
            "Eff. Comm",
            "Komm rücks.",
            "Effac. Comm",
            "Komm rücks."
        ),
        "Clear Display" to arrayOf(
            "Clear Display",
            "Eff. Écran",
            "Anz. lösch.",
            "Effac. Écran",
            "Anz. löschen"
        ),
        "%s Columns" to arrayOf("%s Columns", "%s Col.", "%s Spalt.", "%s Colonnes", "%s Spalten"),
        "Communications Set-Up" to arrayOf(
            "Communications Set-Up",
            "Rég. Comm.",
            "KOMMUNIK.",
            "Réglage Communications",
            "KOMMUNIKATION"
        ),
        "Display Set-Up" to arrayOf(
            "Display Set-Up",
            "Rég. Écran",
            "ANZEIGE",
            "Réglage Écran",
            "ANZEIGE"
        ),
        "Keyboard Set-Up" to arrayOf(
            "Keyboard Set-Up",
            "Rég. Clavier",
            "TASTATUR",
            "Réglage Clavier",
            "TASTATUR"
        ),
        "Printer Set-Up" to arrayOf(
            "Printer Set-Up",
            "Rég. Impr.",
            "DRUCKER",
            "Réglage Imprimante",
            "DRUCKER"
        ),
        "Tab Set-Up" to arrayOf(
            "Tab Set-Up",
            "Rég. Tab.",
            "TABULATOR",
            "Réglage Tabul.",
            "TABULATOR"
        ),
        "%s Cursor Keys" to arrayOf(
            "%s Cursor Keys",
            "%s T. Curs.",
            "%s Cur.tast.",
            "%s Touches Curseur",
            "%s Cursortasten"
        ),
        "%s Cursor Style" to arrayOf(
            "%s Cursor Style",
            "%s Style Cur.",
            "%s Cur.stil",
            "%s Style Curseur",
            "%s Cursorstil"
        ),
        "Dark Text, Light Screen" to arrayOf(
            "Dark Text, Light Screen",
            "T. Sombre/Écl. Clair",
            "Dunkl. T./Hell. B.",
            "Texte Sombre, Écran Clair",
            "Dunkler Text, Helles Bild"
        ),
        "Light Text, Dark Screen" to arrayOf(
            "Light Text, Dark Screen",
            "T. Clair/Écr. Sombre",
            "Hell. T./Dunkl. B.",
            "Texte Clair, Écran Sombre",
            "Heller Text, Dunkles Bild"
        ),
        "Disconnect, %s Delay" to arrayOf(
            "Disconnect, %s Delay",
            "Déconn., %s Délai",
            "Trenn., %s Verz.",
            "Déconn., %s Délai",
            "Trennen, %s Verzöger."
        ),
        "DSR Connected" to arrayOf(
            "DSR Connected",
            "DSR Conn.",
            "DSR verb.",
            "DSR Connecté",
            "DSR verbunden"
        ),
        "Exit" to arrayOf("Exit", "Quitter", "Ende", "Quitter", "Ende"),
        "Interpret" to arrayOf(
            "Interpret",
            "Interpr.",
            "Interpr.",
            "Interpréter",
            "Interpretieren"
        ),
        "%sKeyclick" to arrayOf(
            "%sKeyclick",
            "%sBruit t.",
            "%sKlick",
            "%sBruit touches",
            "%sTastenklick"
        ),
        "%s Keyboard" to arrayOf(
            "%s Keyboard",
            "%s Clav.",
            "%s Tast.",
            "%s Clavier",
            "%s Tastatur"
        ),
        "%s Keypad" to arrayOf(
            "%s Keypad",
            "%s Pavé n.",
            "%s Ziff.bl.",
            "%s Pavé num.",
            "%s Ziffernblock"
        ),
        "%sLocal Echo" to arrayOf(
            "%sLocal Echo",
            "%sÉcho Loc.",
            "%sLok. Echo",
            "%sÉcho Local",
            "%sLokales Echo"
        ),
        "%s Lock" to arrayOf("%s Lock", "%s Verr.", "%s Sperre", "%s Verrouillage", "%s Sperre"),
        "%sMargin Bell" to arrayOf(
            "%sMargin Bell",
            "%sBip marg.",
            "%sR.sign.",
            "%sBip marge",
            "%sRandsignal"
        ),
        "Print All Characters" to arrayOf(
            "All Characters",
            "t. car.",
            "Z. dr.",
            "tous caract.",
            "Zeichen drucken"
        ),
        "National and Line Drawing" to arrayOf(
            "National and Line Drawing",
            "Nat./Graph.",
            "Nat. & Gr.",
            "National et Graphique",
            "National & Grafik"
        ),
        "National Only" to arrayOf(
            "National Only",
            "Nat. uniq.",
            "Nur Nat.",
            "National uniq.",
            "Nur National"
        ),
        "Printer Mode Normal" to arrayOf(
            "Printer Mode Normal",
            "Mode Impr. Normal",
            "Drucker Normal",
            "Mode Imprimante Normal",
            "Druckermodus Normal"
        ),
        "Printer Mode Auto Print" to arrayOf(
            "Printer Mode Auto Print",
            "Mode Impr. Auto",
            "Drucker Auto",
            "Mode Imprimante Auto Print",
            "Druckermodus Auto Print"
        ),
        "Printer Mode Controller" to arrayOf(
            "Printer Mode Controller",
            "Mode Impr. Contr.",
            "Drucker Steuer",
            "Mode Imprimante Contrôleur",
            "Druckermodus Controller"
        ),
        "%sNew Line" to arrayOf(
            "%sNew Line",
            "%sN. Ligne",
            "%sN. Zeile",
            "%sNouvelle Ligne",
            "%sNeue Zeile"
        ),
        "UPSS %s" to arrayOf("UPSS %s", "UPSS %s", "UPSS %s", "UPSS %s", "UPSS %s"),
        "DEC Supplemental" to arrayOf(
            "DEC Supplemental",
            "DEC Suppl.",
            "DEC-Erg.",
            "DEC Supplémentaire",
            "DEC-Ergänzung"
        ),
        "ISO Latin-1" to arrayOf(
            "ISO Latin-1",
            "ISO Latin-1",
            "ISO Latin-1",
            "ISO Latin-1",
            "ISO Latin-1"
        ),
        "No " to arrayOf("No ", "Non ", "Nein ", "Non ", "Nein "),
        "No Auto" to arrayOf("No Auto", "P. d'Auto", "Kein Aut.", "Pas d'Auto", "Kein Auto"),
        "No Cursor" to arrayOf(
            "No Cursor",
            "P. de Cur.",
            "Kein Cur.",
            "Pas de Curseur",
            "Kein Cursor"
        ),
        "No Terminator" to arrayOf(
            "No Terminator",
            "P. de Term.",
            "Kein Abs.",
            "Pas de Terminaison",
            "Kein Abschluss"
        ),
        "None" to arrayOf("None", "Aucun", "Kein", "Aucun", "Keiner"),
        "Not " to arrayOf("Not ", "Pas ", "Nicht ", "Pas ", "Nicht "),
        "1 Stop Bit" to arrayOf(
            "1 Stop Bit",
            "1 B. arrêt",
            "1 Stoppb.",
            "1 Bit d'arrêt",
            "1 Stoppbit"
        ),
        "RS232, Data Leads Only" to arrayOf(
            "RS232, Data Leads Only", "RS232, n. Dat.", "RS232, " +
                    "Données uniq" +
                    ".", "RS232, nur Daten"
        ),
        "DEC-423, Data Leads Only" to arrayOf(
            "DEC-423, Data Leads Only", "DEC-423, n. Dat.",
            "DEC-423, Données uniq.", "DEC-423, nur Daten"
        ),
        "RS232, Modem Control" to arrayOf(
            "RS232, Modem Control", "RS232, Mod.S.", "RS232, Ctrl " +
                    "Modem", "RS232, Modem-Strg."
        ),
        "DEC-423, Modem Control" to arrayOf(
            "DEC-423, Modem Control", "DEC-423, Mod.S.",
            "DEC-423, Ctrl Modem", "DEC-423, Modem-Strg."
        ),
        "Print %s" to arrayOf("Print %s", "Impr. %s", "Dr. %s", "Impr. %s", "Drucke %s"),
        "Speed=%s" to arrayOf("Speed=%s", "Vit.=%s", "Geschw.=%s", "Vitesse=%s", "Geschw.=%s"),
        "%sPrinter To Host" to arrayOf(
            "%sPrinter To Host",
            "%sImpr./Hôte",
            "%sDr. an H.",
            "%sImpr. vers Hôte",
            "%sDrucker an Host"
        ),
        "Recall" to arrayOf("Recall", "Rappel", "Abruf", "Rappel", "Abrufen"),
        "Receive=%s" to arrayOf(
            "Receive=%s",
            "Récept.=%s",
            "Empf.=%s",
            "Réception=%s",
            "Empfang=%s"
        ),
        "Replace" to arrayOf("Replace", "Rempl.", "Ersetz.", "Remplacer", "Ersetzen"),
        "Reset Terminal" to arrayOf(
            "Reset Terminal",
            "Réinit. Term.",
            "T. rücks.",
            "Réinit. Terminal",
            "T. zurücks."
        ),
        "Scroll Region" to arrayOf(
            "Scroll Region",
            "Zone Déf.",
            "S.ber.",
            "Zone Défilement",
            "Scroll-Bereich"
        ),
        "Save" to arrayOf("Save", "Sauver", "Speich.", "Sauver", "Speichern"),
        "%s Scroll" to arrayOf("%s Scroll", "%s Déf.", "%s Scrol.", "%s Défilement", "%s Scrollen"),
        "Set 8 Column Tabs" to arrayOf(
            "Set 8 Column Tabs",
            "Tab. t. 8 c.",
            "8-Sp.Tab.",
            "Tabul. toutes 8 col.",
            "8-Spalten-Tabs"
        ),
        "Set-Up %s" to arrayOf("Set-Up %s", "Rég. %s", "Setup %s", "Réglage %s", "Setup %s"),
        "Smooth" to arrayOf("Smooth", "Doux", "Glatt", "Doux", "Weich"),
        "%s Keys" to arrayOf("%s Keys", "%s T.", "%s Tast.", "%s Touches", "%s Tasten"),
        "%s Transmit" to arrayOf(
            "%s Transmit",
            "%s Trans.",
            "%s Senden",
            "%s Transmission",
            "%s Senden"
        ),
        "To Directory" to arrayOf(
            "To Directory",
            "V. Rép.",
            "Z. Verz.",
            "Vers Répertoire",
            "Zum Verzeichnis"
        ),
        "To Next Set-Up" to arrayOf(
            "To Next Set-Up",
            "V. Rég. Suiv.",
            "Z. n. Set.",
            "Vers Réglage Suivant",
            "Zum nächsten Setup"
        ),
        "Transmit=%s" to arrayOf(
            "Transmit=%s",
            "Trans.=%s",
            "Senden=%s",
            "Transmission=%s",
            "Senden=%s"
        ),
        "2 Stop Bits" to arrayOf(
            "2 Stop Bits",
            "2 B. arrêt",
            "2 Stoppb.",
            "2 Bits d'arrêt",
            "2 Stoppbits"
        ),
        "User Defined Keys %s" to arrayOf(
            "User Defined Keys %s",
            "T. util. %s",
            "Ben.tast. %s",
            "Touches utilisateur %s",
            "Benutzertasten %s"
        ),
        "User Features %s" to arrayOf(
            "User Features %s",
            "Fonc. ut. %s",
            "Ben.fkt. %s",
            "Fonctions utilis. %s",
            "Benutzerfunkt. %s"
        ),
        "VT100 Mode" to arrayOf(
            "VT100 Mode",
            "Mode VT100",
            "VT100-M.",
            "Mode VT100",
            "VT100-Modus"
        ),
        "%s ID" to arrayOf("%s ID", "ID %s", "%s-ID", "ID %s", "%s-ID"),
        "VT300 Mode, 7-Bit Controls" to arrayOf(
            "VT300 Mode, 7-Bit Controls",
            "M. VT300, C. 7-B",
            "VT300, 7-B-S.",
            "Mode VT300, Ctrl 7-Bit",
            "VT300, 7-Bit-Strg."
        ),
        "VT300 Mode, 8-Bit Controls" to arrayOf(
            "VT300 Mode, 8-Bit Controls",
            "M. VT300, C. 8-B",
            "VT300, 8-B-S.",
            "Mode VT300, Ctrl 8-Bit",
            "VT300, 8-Bit-Strg."
        ),
        "VT52 Mode" to arrayOf("VT52 Mode", "Mode VT52", "VT52-M.", "Mode VT52", "VT52-Modus"),
        "%sWarning Bell" to arrayOf(
            "%sWarning Bell",
            "%sBip av.",
            "%sW-Gl.",
            "%sBip avert.",
            "%sWarn-Glocke"
        ),
        "%s Wrap" to arrayOf("%s Wrap", "%s Ret.", "%s Umbr.", "%s Retour", "%s Umbruch"),
        "%s XOFF" to arrayOf("%s XOFF", "%s XOFF", "%s XOFF", "%s XOFF", "%s XOFF"),
        "XOFF" to arrayOf("XOFF", "XOFF", "XOFF", "XOFF", "XOFF"),
        "XOFF at 64" to arrayOf(
            "XOFF at 64",
            "XOFF à 64",
            "XOFF bei 64",
            "XOFF à 64",
            "XOFF bei 64"
        ),
        "XOFF at 64" to arrayOf(
            "XOFF at 64",
            "XOFF à 64",
            "XOFF bei 64",
            "XOFF à 64",
            "XOFF bei 64"
        ),
        "XOFF at 128" to arrayOf(
            "XOFF at 128",
            "XOFF à 128",
            "XOFF bei 128",
            "XOFF à 128",
            "XOFF bei 128"
        ),
        "No XOFF" to arrayOf("No XOFF", "Pas de XOFF", "Kein XOFF", "Pas de XOFF", "Kein XOFF"),
        "No Status Display" to arrayOf(
            "No Status Display",
            "Pas aff. état",
            "K. Status",
            "Pas d'affichage état",
            "Keine Statusanzeige"
        ),
        "Host Writable" to arrayOf(
            "Host Writable",
            "Écr. Hôte",
            "Host-besch.",
            "Écrit. Hôte",
            "Host-beschreibbar"
        ),
        "%sCompose" to arrayOf("%sCompose", "%sComp.", "%sComp.", "%sComposer", "%sCompose"),
        ",, and .. Keys%s" to arrayOf(
            ",, and .. Keys%s",
            "T. ,, et ..%s",
            ",, u. .. T.%s",
            "Touches ,, et ..%s",
            ",, und .. Tasten%s"
        ),
        " Send ,< and .>" to arrayOf(
            " Send ,< and .>",
            " Env. ,< et .>",
            " Send. ,< u .>",
            " Envoi ,< et .>",
            " Senden ,< och .>"
        ),
        ",, and .. Keys Send ,< and .>" to arrayOf(
            ",, and .. Keys Send ,< and .>",
            "T. ,, et .. Env. ,< et .>",
            ",, u. .. Send. ,< u .>",
            "Touches ,, et .. Env. ,< et .>",
            ",, und .. Tasten Senden ,< och .>"
        ),
        "<> Key Sends `~" to arrayOf(
            "<> Key Sends `~",
            "T. <> Env. `~",
            "<> T. Send. `~",
            "Touche <> Envoie `~",
            "<> Taste Sendet `~"
        ),
        "<> Key" to arrayOf("<> Key", "T. <>", "<> T.", "Touche <>", "<> Taste"),
        "`~ Key Sends ESC" to arrayOf(
            "`~ Key Sends ESC",
            "T. `~ Env. ESC",
            "`~ T. Send. ESC",
            "Touche `~ Envoie ESC",
            "`~ Taste Sendet ESC"
        ),
        "`~ Key" to arrayOf("`~ Key", "T. `~", "`~ T.", "Touche `~", "`~ Taste"),
        "Copyright \u00A9 2026 AboveWare - All Rights Reserved" to arrayOf(
            "Copyright \u00A9 2026 AboveWare - All Rights Reserved",
            "Copyright \u00A9 2026 AboveWare - Tous droits réservés",
            "Copyright \u00A9 2026 AboveWare - Alle Rechte bewahrt",
            "Copyright \u00A9 2026 AboveWare - Tous droits réservés",
            "Copyright \u00A9 2026 AboveWare - Alle Rechte bewahrt"
        ),
        "Yes" to arrayOf("Yes", "Oui", "Ja", "Oui", "Ja"),
        "8 Bits, No Parity" to arrayOf(
            "8 Bits, No Parity",
            "8 b., s. parité",
            "8 Bit, k. P.",
            "8 bits, sans parité",
            "8 bits, keine Parität"
        ),
        "8 Bits, Even Parity" to arrayOf(
            "8 Bits, Even Parity",
            "8 b., par. paire",
            "8 Bit, g. P.",
            "8 bits, parité paire",
            "8 bits, gerade Parität"
        ),
        "8 Bits, Odd Parity" to arrayOf(
            "8 Bits, Odd Parity",
            "8 b., par. imp.",
            "8 Bit, u. P.",
            "8 bits, parité impaire",
            "8 bits, ungerade Parität"
        ),
        "7 Bits, No Parity" to arrayOf(
            "7 Bits, No Parity",
            "7 b., s. parité",
            "7 Bit, k. P.",
            "7 bits, sans parité",
            "7 bits, keine Parität"
        ),
        "7 Bits, Mark Parity" to arrayOf(
            "7 Bits, Mark Parity",
            "7 b., mark",
            "7 Bit, mark",
            "7 bits, mark",
            "7 bits, mark"
        ),
        "7 Bits, Space Parity" to arrayOf(
            "7 Bits, Space Parity",
            "7 b., space",
            "7 Bit, space",
            "7 bits, space",
            "7 bits, space"
        ),
        "7 Bits, Even Parity" to arrayOf(
            "7 Bits, Even Parity",
            "7 b., par. paire",
            "7 Bit, g. P.",
            "7 bits, parité paire",
            "7 bits, gerade Parität"
        ),
        "7 Bits, Odd Parity" to arrayOf(
            "7 Bits, Odd Parity",
            "7 b., par. imp.",
            "7 Bit, u. P.",
            "7 bits, parité impaire",
            "7 bits, ungerade Parität"
        ),
        "8 bits, no parity" to arrayOf(
            "8 bits, no parity",
            "8 b., s. parité",
            "8 Bit, k. P.",
            "8 bits, sans parité",
            "8 bits, keine Parität"
        ),
        "7 bits, even parity" to arrayOf(
            "7 bits, even parity",
            "7 b., par. paire",
            "7 Bit, g. P.",
            "7 bits, parité paire",
            "7 bits, gerade Parität"
        ),
        "7 bits, odd parity" to arrayOf(
            "7 bits, odd parity",
            "7 b., par. imp.",
            "7 Bit, u. P.",
            "7 bits, parité impaire",
            "7 bits, ungerade Parität"
        ),
        "8 bits" to arrayOf("8 bits", "8 bits", "8 bits", "8 bits", "8 bits"),
        "7 bits" to arrayOf("7 bits", "7 bits", "7 bits", "7 bits", "7 bits"),
        "7-bit" to arrayOf("7-bit", "7-bit", "7-bit", "7-bit", "7-bit"),
        "8-bit" to arrayOf("8-bit", "8-bit", "8-bit", "8-bit", "8-bit"),
        "Typewriter" to arrayOf(
            "Typewriter",
            "Mach. à écr.",
            "Schreibmasch.",
            "Machine à écrire",
            "Schreibmaschine"
        ),
        "Data Processing" to arrayOf(
            "Data Processing",
            "Informatique",
            "Datenverarb.",
            "Informatique",
            "Datenverarbeitung"
        ),
        "North American" to arrayOf(
            "North American",
            "Américain",
            "N.amerikan.",
            "Américain",
            "Nordamerikanisch"
        ),
        "British" to arrayOf("British", "Britannique", "Britisch", "Britannique", "Britisch"),
        "Flemish" to arrayOf("Flemish", "Flamand", "Flämisch", "Flamand", "Flämisch"),
        "Canadian (French)" to arrayOf(
            "Canadian (French)",
            "Canadien f",
            "Kanad. (Frz.)",
            "Canadien f",
            "Kanadisch (Frz.)"
        ),
        "Danish" to arrayOf("Danish", "Danois", "Dänisch", "Danois", "Dänisch"),
        "Finnish" to arrayOf("Finnish", "Finnois", "Finnisch", "Finnois", "Finnisch"),
        "German/Austrian" to arrayOf(
            "German/Austrian",
            "Allemand",
            "Deutsch",
            "Allemand",
            "Deutsch"
        ),
        "Dutch" to arrayOf("Dutch", "Hollandais", "Niederl.", "Hollandais", "Niederländisch"),
        "Italian" to arrayOf("Italian", "Italien", "Italienisch", "Italien", "Italienisch"),
        "Swiss (French)" to arrayOf(
            "Swiss (French)",
            "Suisse (fr.)",
            "Schw. (Frz.)",
            "Suisse (fr.)",
            "Schweiz (Franz.)"
        ),
        "Swiss (German)" to arrayOf(
            "Swiss (German)",
            "Suisse (al.)",
            "Schw. (Deut.)",
            "Suisse (al.)",
            "Schweiz (Deutsch)"
        ),
        "Swedish" to arrayOf("Swedish", "Suédois", "Schwedisch", "Suédois", "Schwedisch"),
        "Norwegian" to arrayOf("Norwegian", "Norvégien", "Norwegisch", "Norvégien", "Norwegisch"),
        "French/Belgian" to arrayOf(
            "French/Belgian",
            "Franç./belge",
            "Frz./Belg.",
            "Franç./belge",
            "Französisch/Belg."
        ),
        "Spanish" to arrayOf("Spanish", "Espagnol", "Spanisch", "Espagnol", "Spanisch"),
        "Portuguese" to arrayOf("Portuguese", "Portugais", "Portug.", "Portugais", "Portugiesisch"),
        "All" to arrayOf("All", "Tout", "Alle", "Tout", "Alle"),
        "4800" to arrayOf("4800", "4800", "4800", "4800", "4800"),
        "9600" to arrayOf("9600", "9600", "9600", "9600", "9600"),
        "19200" to arrayOf("19200", "19200", "19200", "19200", "19200"),
        "38400" to arrayOf("38400", "38400", "38400", "38400", "38400"),
        "Transmit" to arrayOf("Transmit", "Trans.", "Senden", "Transmission", "Senden")
    )

    fun translate(key: String, langIndex: Int): String {
        val entry = translations[key] ?: return key
        return entry.getOrElse(langIndex) { entry[0] }
    }
}
