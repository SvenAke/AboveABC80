package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.*
import com.aboveware.aboveabc80.core.cpu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalUnsignedTypes::class)
class Abc80Error {

    companion object {
        const val DISK_FULL = 0xA9
        private const val ERROR_HANDLER_ADDRESS = 0x0695
    }

    private val errorMap: Map<Int, StringResource> = mapOf(
        0xA8 to Res.string.error0xA8,
        DISK_FULL to Res.string.error0xA9,
        0xAA to Res.string.error0xAA,
        0xAB to Res.string.error0xAB,
        0xAC to Res.string.error0xAC,
        0xAD to Res.string.error0xAD,
        0xAE to Res.string.error0xAE,
        0xAF to Res.string.error0xAF,
        0x98 to Res.string.error0x98,
        0x99 to Res.string.error0x99,
        0x9A to Res.string.error0x9A,
        0x9B to Res.string.error0x9B,
        0x9C to Res.string.error0x9C,
        0x9D to Res.string.error0x9D,
        0x9E to Res.string.error0x9E,
        0x9F to Res.string.error0x9F,
        0x88 to Res.string.error0x88,
        0x89 to Res.string.error0x89,
        0x8A to Res.string.error0x8A,
        0x8B to Res.string.error0x8B,
        0x8C to Res.string.error0x8C,
        0x8D to Res.string.error0x8D,
        0x8E to Res.string.error0x8E,
        0x8F to Res.string.error0x8F,
        0xB0 to Res.string.error0xB0,
        0xB1 to Res.string.error0xB1,
        0xB2 to Res.string.error0xB2,
        0xB3 to Res.string.error0xB3,
        0xB4 to Res.string.error0xB4,
        0xB5 to Res.string.error0xB5,
        0xB6 to Res.string.error0xB6,
        0xB7 to Res.string.error0xB7,
        0xA0 to Res.string.error0xA0,
        0xA1 to Res.string.error0xA1,
        0xA2 to Res.string.error0xA2,
        0xA3 to Res.string.error0xA3,
        0xA4 to Res.string.error0xA4,
        0xA5 to Res.string.error0xA5,
        0xA6 to Res.string.error0xA6,
        0xA7 to Res.string.error0xA7,
        0x90 to Res.string.error0x90,
        0x91 to Res.string.error0x91,
        0x92 to Res.string.error0x92,
        0x93 to Res.string.error0x93,
        0x94 to Res.string.error0x94,
        0x95 to Res.string.error0x95,
        0x96 to Res.string.error0x96,
        0x97 to Res.string.error0x97,
        0x80 to Res.string.error0x80,
        0x81 to Res.string.error0x81,
        0x82 to Res.string.error0x82,
        0x83 to Res.string.error0x83,
        0x84 to Res.string.error0x84,
        0x85 to Res.string.error0x85,
        0x86 to Res.string.error0x86,
        0x87 to Res.string.error0x87,
        0xB8 to Res.string.error0xB8,
        0xB9 to Res.string.error0xB9,
        0xBA to Res.string.error0xBA,
        0xBB to Res.string.error0xBB,
        0xBC to Res.string.error0xBC,
        0xBD to Res.string.error0xBD,
        0xBE to Res.string.error0xBE,
        0xBF to Res.string.error0xBF
    )

    fun onError(error: Int, function: (error: String) -> Any) {
        NativeLib.getObject().addMemoryReadWatcher(
            ERROR_HANDLER_ADDRESS,
            object : NativeLib.MemoryReadWatcher {
                override fun onRead(address: Int): Boolean {
                    if (error != cpu().a) return false
                    CoroutineScope(Dispatchers.Default).launch {
                        function(errorMap[error]?.let { getString(it) } ?: "ERR $error")
                    }
                    return true
                }
            })
    }
}