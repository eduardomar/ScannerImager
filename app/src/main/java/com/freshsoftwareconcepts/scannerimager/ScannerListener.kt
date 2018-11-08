package com.freshsoftwareconcepts.scannerimager

interface ScannerListener {
    fun onBarcodeScan(barcode: String)
    fun onStatus(message: String)
}