package com.freshsoftwareconcepts.scannerimager

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity(), ScannerListener {
    private var scanner: ScannerController? = null
    private val tvStatus: TextView? by lazy { this.findViewById<TextView>(R.id.tvStatus) }
    private val etBarcode: EditText? by lazy { this.findViewById<EditText>(R.id.etBarcode) }
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
        super.onCreate(savedInstanceState)

        this.scanner = ScannerController.getInstance(this)
        this.setContentView(if (this.scanner == null) {
            R.layout.activity_main
        } else {
            R.layout.activity_scanner_main
        })

        with(this.etBarcode) {
            this?.setOnKeyListener { _, keyCode, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_REFRESH) && !this.text.isNullOrEmpty()) {
                    AsyncDataUpdate().execute(this.text.toString().trim())
                    return@setOnKeyListener true
                }

                return@setOnKeyListener false
            }

            this?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && !this.text.isNullOrEmpty()) {
                    AsyncDataUpdate().execute(this.text.toString().trim())
                    return@setOnEditorActionListener true
                }

                return@setOnEditorActionListener false
            }
        }

        this.setAdapter()
    }

    override fun onPause() {
        super.onPause()

        this.scanner?.pause()
    }

    override fun onResume() {
        super.onResume()

        this.scanner?.resume()
    }

    inner class AsyncDataUpdate : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg p0: String?): String {
            return p0[0]!!
        }

        override fun onPostExecute(result: String) {
            var type: String = if (this@MainActivity.isUnloadingChart.matches(result)) {
                "Unloading chart"
            } else if (this@MainActivity.isLoadingChart.matches(result)) {
                 "Loading chart"
            } else if (this@MainActivity.isPalletTagOld.matches(result)) {
                "Pallet tag old"
            } else if (this@MainActivity.isPalletTag.matches(result)) {
                "Pallet tag"
            } else if (this@MainActivity.isRack.matches(result)) {
                "Rack"
            } else {
                ""
            }

            if (!type.isNullOrEmpty()) {
                this@MainActivity.lstBarcode.add(0, mapOf("barcode" to result, "type" to type))
                this@MainActivity.setAdapter()
            }
        }
    }

    override fun onBarcodeScan(barcode: String) {
        AsyncDataUpdate().execute(barcode)
    }

    override fun onStatus(message: String) {
        object : AsyncTask<String, Void, String>() {
            override fun doInBackground(vararg p0: String?): String {
                return p0[0]!!
            }

            override fun onPostExecute(result: String?) {
                this@MainActivity.tvStatus?.text = "Status: ${result}"
            }
        }.execute(message)
    }
}