package net.relserver

import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.PowerManager
import android.os.StrictMode
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "MainActivity"
    private lateinit var properties: Properties

    var connected = false;
    lateinit var appSpinner: Spinner


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        properties = Properties(applicationContext)
        ReLServerService.init()

        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.progressBar).visibility = View.GONE

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        findViewById<View>(R.id.ConnectButton).setOnClickListener(refreshButton())
        findViewById<View>(R.id.SettingsButton).setOnClickListener(this::onButtonShowPopupWindowClick)

        appSpinner = findViewById(R.id.application_spinner)
        appSpinner.adapter = getAppNameAdapter()

        val modeSpinner: Spinner = findViewById(R.id.mode_spinner)
        ArrayAdapter.createFromResource(this, R.array.modes, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                modeSpinner.adapter = adapter
            }

        val pendingIntent = createPendingResult(
            Constants.RELSERVER_REQUEST_CODE,
            Intent(
                this,
                ReLServerService::class.java
            ),
            0
        )
        val startIntent = Intent(this, ReLServerService::class.java)
            .putExtra(Constants.PENDING_INTENT, pendingIntent)
            .putExtra(Constants.ACTION, Constants.GET_INFO)
        startService(startIntent)

        properties.initProperties()
    }

    private fun getAppNameAdapter(): ArrayAdapter<String> {
        val appNameAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item,
                ReLServerService.appCatalog.appNamesWithStats
            )
        appNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return appNameAdapter
    }


    private fun refreshButton(): (v: View) -> Unit {
        val mainActivity = this
        return {
            val connectButton = findViewById<Button>(R.id.ConnectButton)
            if (!connected) {
                connectButton.text = getString(R.string.connecting)
            } else {
                connectButton.text = getString(R.string.disconnecting)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val appSpinner = findViewById<Spinner>(R.id.application_spinner)
                val modeSpinner = findViewById<Spinner>(R.id.mode_spinner)
                val selectedItemIdMode = modeSpinner.selectedItemId
                val selectedItemIdApp = appSpinner.selectedItemId

                if (connected) {
                    val pendingIntent = createPendingResult(
                        Constants.RELSERVER_REQUEST_CODE,
                        Intent(
                            mainActivity,
                            ReLServerService::class.java
                        ).putExtra(Constants.RESULT, 0),
                        0
                    )
                    val startIntent = Intent(mainActivity, ReLServerService::class.java)
                        .putExtra(Constants.PENDING_INTENT, pendingIntent)
                        .putExtra(Constants.ACTION, Constants.STOP)
                    startService(startIntent)
                    releaseWakeLock()
                } else {
                    val pm = getSystemService(POWER_SERVICE) as PowerManager
                    ReLServerService.wl =
                        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "relserver:running-wake")
                    ReLServerService.wl.acquire(60 * 60 * 1000L /*60 minutes*/)

                    // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
                    val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                    ReLServerService.wifiLock = wm.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "relserver:running-wifi"
                    )
                    ReLServerService.wifiLock.acquire()

                    if (!PermissionUtils.checkIfBatteryOptimizationsDisabled(mainActivity)) {
                        PermissionUtils.requestDisableBatteryOptimizations(mainActivity)
                    }

                    val pendingIntent = createPendingResult(
                        Constants.RELSERVER_REQUEST_CODE,
                        Intent(
                            mainActivity,
                            ReLServerService::class.java
                        ).putExtra(Constants.ERROR_MESSAGE, ""),
                        0
                    )

                    val customAppId = properties.getProperty(Constants.CUSTOM_APP_ID)
                    val startIntent = Intent(mainActivity, ReLServerService::class.java)
                        .putExtra(
                            Constants.SELECTED_MODE,
                            when (selectedItemIdMode) {
                                0L -> "CLIENT"
                                1L -> "SERVER"
                                2L -> "CLIENT_SERVER"
                                else -> "CLIENT"
                            }
                        )
                        .putExtra(
                            Constants.SELECTED_APP_ID,
                            if (ReLServerService.isNotBlank(customAppId))
                                properties.getProperty(Constants.CUSTOM_APP_ID)
                            else
                                ReLServerService.appCatalog.apps.get(selectedItemIdApp.toInt()).id
                        )
                        .putExtra(Constants.PENDING_INTENT, pendingIntent)
                        .putExtra(Constants.ACTION, Constants.START)

                    val customServerIp = properties.getProperty(Constants.CUSTOM_LOCAL_SERVER_IP)
                    if (ReLServerService.isNotBlank(customServerIp)) {
                        startIntent.putExtra(Constants.CUSTOM_LOCAL_SERVER_IP, customServerIp)
                    }

                    val customPort = properties.getProperty(Constants.CUSTOM_PORT)
                    if (ReLServerService.isNotBlank(customPort)) {
                        startIntent.putExtra(Constants.CUSTOM_PORT, customPort)
                    }

                    val customHubIp = properties.getProperty(Constants.CUSTOM_HUB)
                    if (ReLServerService.isNotBlank(customHubIp)) {
                        startIntent.putExtra(Constants.CUSTOM_HUB, customHubIp)
                    }
                    startService(startIntent)
                }
            }
        }
    }

    private fun releaseWakeLock() {
        if (ReLServerService.wl != null) {
            ReLServerService.wl.release()
        }
        if (ReLServerService.wifiLock != null) {
            ReLServerService.wifiLock.release()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(LOG_TAG, "requestCode = $requestCode, resultCode = $resultCode")

        val connectButton = findViewById<Button>(R.id.ConnectButton)
        if (resultCode == Constants.START) {
            when (requestCode) {
                Constants.RELSERVER_REQUEST_CODE -> {
                    connected = true
                    connectButton.text = getString(R.string.connected)
                }
            }
        }

        if (resultCode == Constants.STOP) {
            when (requestCode) {
                Constants.RELSERVER_REQUEST_CODE -> {
                    connected = false
                    connectButton.text = getString(R.string.connect)
                    val result = data?.getStringExtra(Constants.ERROR_MESSAGE)
                    if (!result.isNullOrEmpty()) {
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        if (resultCode == Constants.GET_INFO) {
            when (requestCode) {
                Constants.RELSERVER_REQUEST_CODE -> {
                    val result = data?.getIntExtra(Constants.RESULT, -1)
                    connected = result == 1
                    if (connected) {
                        connectButton.text = getString(R.string.connected)
                    } else {
                        connectButton.text = getString(R.string.connect)
                    }
                }
            }
        }

        val appSpinner = findViewById<Spinner>(R.id.application_spinner)
        val selectedItemIdApp = appSpinner.selectedItemId
        appSpinner.adapter = getAppNameAdapter()
        val customPort = properties.getProperty(Constants.CUSTOM_PORT)
        if (ReLServerService.isNotBlank(customPort)) {
            appSpinner.setSelection(appSpinner.count - 1, true)
        } else {
            appSpinner.setSelection(selectedItemIdApp.toInt(), true)
        }
    }

    private fun onButtonShowPopupWindowClick(view: View?) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_window, null)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true
        val popupWindow = PopupWindow(popupView, width, height, focusable)

        popupView.findViewById<TextView>(R.id.localServerIp).text =
            properties.getProperty(Constants.CUSTOM_LOCAL_SERVER_IP)
        popupView.findViewById<TextView>(R.id.customAppId).text =
            properties.getProperty(Constants.CUSTOM_APP_ID)
        popupView.findViewById<TextView>(R.id.customAppPort).text =
            properties.getProperty(Constants.CUSTOM_PORT)
        popupView.findViewById<TextView>(R.id.customHubIp).text =
            properties.getProperty(Constants.CUSTOM_HUB)

        popupWindow.showAtLocation(view, Gravity.TOP, 0, 0)
        popupView.visibility = View.VISIBLE

        popupView.findViewById<View>(R.id.SaveSettingsButton)
            .setOnClickListener { this.onSettingsSaveButtonClick(it, popupView) }
    }

    private fun onSettingsSaveButtonClick(view: View, popupView: View) {
        properties.saveProperty(
            Constants.CUSTOM_LOCAL_SERVER_IP,
            popupView.findViewById<TextView>(R.id.localServerIp).text.toString()
        )

        properties.saveProperty(
            Constants.CUSTOM_APP_ID,
            popupView.findViewById<TextView>(R.id.customAppId).text.toString()
        )

        properties.saveProperty(
            Constants.CUSTOM_PORT,
            popupView.findViewById<TextView>(R.id.customAppPort).text.toString()
        )

        properties.saveProperty(
            Constants.CUSTOM_HUB,
            popupView.findViewById<TextView>(R.id.customHubIp).text.toString()
        )

        popupView.visibility = View.GONE
    }
}
