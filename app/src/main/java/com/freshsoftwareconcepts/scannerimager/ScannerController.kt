package com.freshsoftwareconcepts.scannerimager

import android.app.Activity
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKManager.EMDKListener
import com.symbol.emdk.EMDKManager.FEATURE_TYPE.BARCODE
import com.symbol.emdk.EMDKResults.STATUS_CODE
import com.symbol.emdk.barcode.*
import com.symbol.emdk.barcode.BarcodeManager.DeviceIdentifier
import com.symbol.emdk.barcode.Scanner.*
import com.symbol.emdk.barcode.StatusData.ScannerStates.*

class ScannerController : EMDKListener, StatusListener, DataListener {
    private var emdkManager: EMDKManager? = null
        set(value) {
            if (field == null) {
                field = value?.let { emdk ->
                    this.barcodeManager = emdk.getInstance(BARCODE) as BarcodeManager

                    return@let emdk
                } ?: run {
                    if (this.barcodeManager != null) this.barcodeManager = null
                    this.listener?.onStatus("Failed to initialize the EMDK manager.")

                    return@run null
                }
            } else if (value == null) {
                this.barcodeManager = null
                field?.release()
                field = null
            }
        }

    private var barcodeManager: BarcodeManager? = null
        set(value) {
            if (field == null) {
                field = value?.let { bm ->
                    this.scanner = bm.getDevice(DeviceIdentifier.DEFAULT)

                    return@let bm
                } ?: run {
                    if (this.scanner != null) this.scanner = null
                    this.listener?.onStatus("Failed to initialize the barcode manager.")

                    return@run null
                }
            } else if (value == null) {
                this.scanner = null
                field = null
            }
        }

    private var scanner: Scanner? = null
        private set(value) {
            if (field == null) { // Configurar el escaner
                field = value?.let { scn ->
                    try {
                        scn.addDataListener(this)
                        scn.addStatusListener(this)
                        scn.triggerType = TriggerType.HARD
                        scn.enable()

                        if (scn.isEnabled) {
                            val config: ScannerConfig = scn.config
                            config.decoderParams.code39.enabled = true
                            config.decoderParams.code128.enabled = true
                            //config.readerParams.readerSpecific.imagerSpecific.scanMode = ScannerConfig.ScanMode.MULTI_BARCODE
                            //config.multiBarcodeParams.barcodeCount = 5
                            scn.config = config

                            scn.read()

                            return@let scn
                        } else {
                            this.listener?.onStatus("Scanner is not enabled")
                        }
                    } catch (e: ScannerException) {
                        this.listener?.onStatus("${e.message}")
                    }

                    return@let null
                } ?: run {
                    this.listener?.onStatus("Failed to initialize the scanner device.")

                    return@run null
                }
            } else if (value == null) { // Liberar escaner
                field?.let { scn ->
                    try {
                        scn.cancelRead()
                        scn.disable()
                    } catch (e: Exception) {
                        this.listener?.onStatus("${e.message}")
                    }

                    try {
                        scn.removeDataListener(this)
                        scn.removeStatusListener(this)
                    } catch (e: Exception) {
                        this.listener?.onStatus("${e.message}")
                    }

                    try {
                        scn.release()
                    } catch (e: Exception) {
                        this.listener?.onStatus("${e.message}")
                    }

                    field = null
                }
            }
        }

    private var listener: ScannerListener? = null

    companion object {
        fun getInstance(activity: Activity): ScannerController? {
            try {
                return ScannerController(activity)
            } catch (e: Exception) {
                return null
            }
        }
    }

    private constructor(activity: Activity) {
        if (activity is ScannerListener) {
            this.listener = activity
        }

        val results = EMDKManager.getEMDKManager(activity.applicationContext, this)
        if (results.statusCode != STATUS_CODE.SUCCESS) {
            throw Exception("EMDKManager object request failed!")
        }
    }

    fun pause() {
        this.barcodeManager = null
        this.emdkManager?.release(BARCODE)
    }

    fun resume() {
        this.emdkManager?.let { emdkManager ->
            this.barcodeManager = emdkManager.getInstance(BARCODE) as BarcodeManager
        }
    }

    fun release() {
        this.emdkManager = null
    }

    override fun onOpened(emdkManager: EMDKManager?) {
        this.emdkManager = emdkManager
        this.listener?.onStatus("EMDK open success!")
    }

    override fun onClosed() {
        this.release()
        this.listener?.onStatus("EMDK closed unexpectedly! Please close and restart the application.")
    }

    override fun onStatus(statusData: StatusData?) {
        statusData?.let {
            if (it.state == StatusData.ScannerStates.IDLE && this.scanner != null && this.scanner!!.isEnabled) {
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

            when (statusData.state) {
                IDLE -> {
                    this.listener?.onStatus("${statusData.friendlyName} is enabled and idle...")
                }

                WAITING -> {
                    this.listener?.onStatus("Scanner is waiting for trigger press...")
                }

                SCANNING -> {
                    this.listener?.onStatus("Scanning...")
                }

                DISABLED -> {
                    this.listener?.onStatus("${statusData.getFriendlyName()} is disabled.")
                }

                else -> {
                    this.listener?.onStatus("An error has occurred.")
                }
            }

            return@let it
        }
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        scanDataCollection?.let {
            if (it.result == ScannerResults.SUCCESS) {
                for (scanData: ScanDataCollection.ScanData in it.scanData) {
                    if (!scanData.data.isNullOrEmpty()) {
                        this.listener?.onBarcodeScan(scanData.data.trim())
                    }
                }
            }
        }
    }
}