package com.freshsoftwareconcepts.scannerimager

import android.app.Activity
import android.widget.Toast
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKManager.EMDKListener
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*
import com.symbol.emdk.barcode.Scanner.DataListener
import com.symbol.emdk.barcode.Scanner.StatusListener

class ScannerController(val activity: Activity) : EMDKListener, StatusListener, DataListener {
    private var isContinuousMode: Boolean = false
    private var emdkManager: EMDKManager? = null
        get() = field
        set(value) {
            if (value == null) {
                field?.let {
                    it.release()
                }

                this.barcodeManager = null
            } else {
                this.barcodeManager = value.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
            }

            field = value
        }

    private var barcodeManager: BarcodeManager? = null
        get() = field
        set(value) {
            if (value == null) {
                field?.let {
                    //it.removeConnectionListener(this)
                }

                this.scanner = null
            } else {
                //value.addConnectionListener(this)
                this.scanner = value.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            }

            field = value
        }

    private var scanner: Scanner? = null
        get() = field
        set(value) {
            if (field == null && value != null) { // Configurar el escaner
                field = value.let { scn ->
                    try {
                        scn.addDataListener(this)
                        scn.addStatusListener(this)
                        scn.triggerType = Scanner.TriggerType.HARD
                        scn.enable()

                        if (scn.isEnabled) {
                            try {
                                val config: ScannerConfig = scn.config
                                config.decoderParams.ean8.enabled = true
                                config.decoderParams.ean13.enabled = true
                                config.decoderParams.code39.enabled = true
                                config.decoderParams.code128.enabled = true
                                scn.config = config
                            } catch (e: ScannerException) {
                                e.printStackTrace()
                            }

                            scn.read()
                            this.isContinuousMode = true
                            Toast.makeText(this.activity, "Press Hard Scan Button to start scanning...", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: ScannerException) {
                        e.printStackTrace()
                    }

                    return@let scn
                }
            } else if (field != null && value == null) { // Liberar escaner
                field?.let { scn ->
                    try {
                        this.isContinuousMode = false
                        scn.cancelRead()
                        scn.disable()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        scn.removeDataListener(this)
                        scn.removeStatusListener(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        scn.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                field = null
            }
        }

    init {
        try {
            val results = EMDKManager.getEMDKManager(this.activity.applicationContext, this)
            if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
                throw Exception("EMDKManager object request failed!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        this.barcodeManager = null
        this.emdkManager?.release(EMDKManager.FEATURE_TYPE.BARCODE)
    }

    fun resume() {
        this.emdkManager?.let { emdkManager ->
            this.barcodeManager = emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
        }
    }

    fun release() {
        this.emdkManager = null
    }

    override fun onOpened(emdkManager: EMDKManager?) {
        try {
            this.emdkManager = emdkManager
        } catch (e: Exception) {
            Toast.makeText(this.activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onClosed() {
        this.release()
    }

    override fun onStatus(statusData: StatusData?) {
        Toast.makeText(this.activity, "1. onStatus", Toast.LENGTH_LONG).show()

        statusData?.let {
            Toast.makeText(this.activity, "${it.friendlyName}:${it.state}", Toast.LENGTH_SHORT).show()

            if (it.state == StatusData.ScannerStates.IDLE && this.isContinuousMode) {
                try {
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    this.scanner?.read()
                } catch (e: ScannerException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        Toast.makeText(this.activity, "1. onData", Toast.LENGTH_LONG).show()
        scanDataCollection?.let {
            Toast.makeText(this.activity, "2. getData", Toast.LENGTH_LONG).show()

            if (it.result == ScannerResults.SUCCESS) {
                Toast.makeText(this.activity, "3. result is SUCCESS", Toast.LENGTH_LONG).show()
                Toast.makeText(this.activity, "4. result size-> ${it.scanData.size}", Toast.LENGTH_LONG).show()

                for (scanData: ScanDataCollection.ScanData in it.scanData) {
                    Toast.makeText(this.activity, "barcode: ${scanData.data.trim()}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}