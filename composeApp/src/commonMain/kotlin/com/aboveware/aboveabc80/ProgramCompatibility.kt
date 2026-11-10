package com.aboveware.aboveabc80

enum class ProgramModelHint {
    ABC800,
    ABC806,
    ABC800_806,
    UNKNOWN
}

enum class ProgramCompatibilityEvidence {
    EXTENSION_800,
    EXTENSION_806,
    BASIC_II_RECORD,
    HIGH_RESOLUTION_COMMANDS,
    INSUFFICIENT
}

data class ProgramCompatibilityHint(
    val model: ProgramModelHint,
    val evidence: ProgramCompatibilityEvidence
)

fun isProgramCompatibilityCandidate(fileName: String): Boolean =
    fileName.substringAfterLast('.', "").lowercase() in setOf("bas", "bac", "abs", "800", "806")

fun programCompatibilityHint(fileName: String, data: ByteArray? = null): ProgramCompatibilityHint {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    if (extension == "806" || extension == "800") {
        if (data == null || abc80ExecutableFormat(fileName, data, 253) == null) {
            return ProgramCompatibilityHint(ProgramModelHint.UNKNOWN, ProgramCompatibilityEvidence.INSUFFICIENT)
        }
        val model = if (extension == "806") ProgramModelHint.ABC806 else ProgramModelHint.ABC800
        val evidence = if (extension == "806") {
            ProgramCompatibilityEvidence.EXTENSION_806
        } else {
            ProgramCompatibilityEvidence.EXTENSION_800
        }
        return ProgramCompatibilityHint(model, evidence)
    }

    if (data != null && extension == "bac" && data.firstOrNull() == 0x8F.toByte()) {
        return ProgramCompatibilityHint(ProgramModelHint.ABC800_806, ProgramCompatibilityEvidence.BASIC_II_RECORD)
    }

    if (data != null) {
        val source = data.decodeToString(throwOnInvalidSequence = false).uppercase()
        if (highResolutionCommands.containsMatchIn(source)) {
            return ProgramCompatibilityHint(
                ProgramModelHint.ABC806,
                ProgramCompatibilityEvidence.HIGH_RESOLUTION_COMMANDS
            )
        }
    }

    return ProgramCompatibilityHint(ProgramModelHint.UNKNOWN, ProgramCompatibilityEvidence.INSUFFICIENT)
}

private val highResolutionCommands =
    Regex("""\b(FGCTL|FGPICTURE|FGPOINT|FGPAINT|FGFILL|FGLINE|ATTRIBUTE)\b""")
