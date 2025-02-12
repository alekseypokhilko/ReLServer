package net.relserver;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import net.relserver.core.Settings;
import net.relserver.core.api.AppCatalog;
import net.relserver.core.app.DefaultAppCatalog;

public class ReLServerService extends Service {
    private final String LOG_TAG = "ReLServerService";
    public static ReLServer reLServer;
    public static Settings settings;
    public static AppCatalog appCatalog;
    public static PowerManager.WakeLock wl;
    public static WifiManager.WifiLock wifiLock;

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "ReLServerService onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "ReLServerService onStartCommand");
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        //todo fix service crashing
        //Service constantly destroying after 2 minutes and I didn't found any working solution
        //Objects were moved to static variables to prevent ReLServer from a GC collection
        //I'm not proud of this code but it works
        int action = intent.getIntExtra(Constants.ACTION, -1);
        if (Constants.START == action) {
            start(intent);
        }
        if (Constants.STOP == action) {
            stop();
            Intent res = new Intent().putExtra(Constants.RESULT, Constants.STOP);
            sendResponse(intent, res, Constants.STOP);
        }
        if (Constants.GET_INFO == action) {
            Intent res = new Intent()
                    .putExtra(Constants.RESULT, reLServer != null ? Constants.START : Constants.STOP);
            sendResponse(intent, res, Constants.GET_INFO);
        }
        return START_STICKY;
    }

    private void sendResponse(Intent intent, Intent res, int resultCode) {
        PendingIntent pi = intent.getParcelableExtra(Constants.PENDING_INTENT);
        if (pi != null) {
            try {
                pi.send(this, resultCode, res);
            } catch (PendingIntent.CanceledException ignored) {
            }
        }
    }

    private void start(Intent intent) {
        if (reLServer != null) {
            reLServer.stop();
        }
        creteSettings(intent);
        appCatalog = new DefaultAppCatalog(settings);

        boolean started = false;
        String errorMessage = "";
        try {
            reLServer = new ReLServer(settings, appCatalog);
            started = true;

        } catch (Exception e) {
            Log.d(LOG_TAG, "ERROR onStartCommand: " + e.getMessage());
            errorMessage = e.getMessage();
            reLServer = null;
            settings = null;
        }

        Intent res = new Intent()
                .putExtra(Constants.RESULT, started ? Constants.START : Constants.STOP)
                .putExtra(Constants.ERROR_MESSAGE, errorMessage);
        sendResponse(intent, res, started ? Constants.START : Constants.STOP);
    }

    private void creteSettings(Intent intent) {
        settings = new Settings();
        //===== debug
//        settings.set(Settings.log, "true");
//        settings.set(Settings.logPacket, "true");
        //=====

        settings.set(Settings.mode, intent.getStringExtra(Constants.SELECTED_MODE));

        String customAppId = intent.getStringExtra(Constants.CUSTOM_APP_ID);
        if (isNotBlank(customAppId)) {
            settings.set(Settings.appId, customAppId);
        } else {
            settings.set(Settings.appId, intent.getStringExtra(Constants.SELECTED_APP_ID));
        }

        String customPort = intent.getStringExtra(Constants.CUSTOM_PORT);
        if (isNotBlank(customPort)){
            settings.set(Settings.appPort, customPort);
        }

        String localServerIp = intent.getStringExtra(Constants.CUSTOM_LOCAL_SERVER_IP);
        if (isNotBlank(localServerIp)){
            settings.set(Settings.localServerIp, localServerIp);
        }

        String hubIp = intent.getStringExtra(Constants.CUSTOM_HUB);
        if (isNotBlank(hubIp)){
            settings.set(Settings.hubIp, hubIp);
        }
    }

    private void stop() {
        if (reLServer != null) {
            reLServer.stop();
        }
        reLServer = null;
//        appCatalog = null;
        settings = null;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "ReLServerService onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "ReLServerService onBind");
        return null;
    }

    public static boolean isNotBlank(String str) {
        return str != null && !str.isEmpty();
    }

    public static void init() {
        if (appCatalog == null) {
            appCatalog = new DefaultAppCatalog(new Settings());
        }
    }
}
