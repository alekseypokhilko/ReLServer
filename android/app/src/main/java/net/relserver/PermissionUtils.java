package net.relserver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class PermissionUtils {
    public static boolean checkIfBatteryOptimizationsDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } else
            return true;
    }

    public static void requestDisableBatteryOptimizations(@NonNull Context context) {
        requestDisableBatteryOptimizations(context, -1);
    }

    @SuppressLint("BatteryLife")
    public static Error requestDisableBatteryOptimizations(@NonNull Context context, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));

        // Flag must not be passed for activity contexts, otherwise onActivityResult() will not be called with permission grant result.
        // Flag must be passed for non-activity contexts like services, otherwise "Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag" exception will be raised.
        if (!(context instanceof Activity))
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (requestCode >=0)
            return startActivityForResult(context, requestCode, intent);
        else
            return startActivity(context, intent);
    }

    public static Error startActivity(@NonNull Context context, @NonNull Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            return new Error(e.getMessage());
        }
        return null;
    }

    public static Error startActivityForResult(Context context, int requestCode, @NonNull Intent intent) {
        try {
            if (context == null) {
                return new Error();
            }

            if (context instanceof AppCompatActivity)
                ((AppCompatActivity) context).startActivityForResult(intent, requestCode);
            else if (context instanceof Activity)
                ((Activity) context).startActivityForResult(intent, requestCode);
            else {
                return new Error();
            }
        } catch (Exception e) {
            return new Error(e.getMessage());
        }
        return null;
    }
}
