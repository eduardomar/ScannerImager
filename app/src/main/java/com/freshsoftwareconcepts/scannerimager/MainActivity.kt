package com.freshsoftwareconcepts.scannerimager

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private var scanner: ScannerController? = null
    private val tvStatus: TextView by lazy { this.findViewById<TextView>(R.id.tvStatus) }
    private val lvBarcode: ListView by lazy { this.findViewById<ListView>(R.id.lvBarcode) }
    private val parentLayout: View by lazy { this.findViewById<View>(android.R.id.content) }
    private val lstBarcode: ArrayList<Map<String, String>> by lazy { ArrayList<Map<String, String>>() }

    private val isUnloadingChart: Regex by lazy { Regex("""^\d+\.\d+\.\d+\.[01]\.[aA]\.\d+$""") }
    private val isLoadingChart: Regex by lazy { Regex("""^\d+\.\d+\.\d+\.[01]\.[sS]\.\d+$""") }
    private val isPalletTagOld: Regex by lazy { Regex("""^(\d{2,})$""") }
    private val isPalletTag: Regex by lazy { Regex("""^\d+\.\d+\.\d+$""") }
    private val isRack: Regex by lazy { Regex("""^\d{1,3}\.\d{1,3}\.[a-zA-Z]$""") }

    fun setAdapter() {
        this.lvBarcode.adapter = SimpleAdapter(this, this.lstBarcode, android.R.layout.simple_list_item_2, arrayOf("barcode", "type"), intArrayOf(android.R.id.text1, android.R.id.text2))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            this.setContentView(R.layout.activity_main)

            this.scanner = ScannerController(this)
            /*
        this.lstBarcode.add(mapOf("barcode" to "1.25.68.1.A.37365", "type" to "Unloading chart"))
        this.lstBarcode.add(mapOf("barcode" to "1.25.68.1.S.72734", "type" to "Loading chart"))
        this.lstBarcode.add(mapOf("barcode" to "00100341", "type" to "Pallet tag old"))
        this.lstBarcode.add(mapOf("barcode" to "02.503932.1", "type" to "Pallet tag"))
        this.lstBarcode.add(mapOf("barcode" to "1.1.A", "type" to "Rack"))
        */
            this.setAdapter()
        } catch (e: Exception) {
            Toast.makeText(this, "10 ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        this.scanner?.release()
    }

    override fun onPause() {
        super.onPause()

        this.scanner?.pause()
    }

    override fun onResume() {
        super.onResume()

        this.scanner?.resume()
    }
}