package com.shekhaliay.barcodescanner.qrgenrater.qr_code_maker

interface Formatter {
    fun format(value: CharSequence, index: Int): CharSequence
}