package com.aboveware.aboveabc80.terminal

expect val ABC80_LAYOUT_ID: Int
expect val ABC800_LAYOUT_ID: Int

internal val ComputerModel.keyboardLayoutId: Int
    get() = when (this) {
        ComputerModel.ABC80 -> ABC80_LAYOUT_ID
        ComputerModel.ABC800M, ComputerModel.ABC800C -> ABC800_LAYOUT_ID
    }
