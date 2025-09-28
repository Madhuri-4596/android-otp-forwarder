package com.otp.ezybooking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {

            // Check if service should be started (user has configured settings)
            SharedPreferences sp = context.getSharedPreferences("ezybookinglogin", Context.MODE_PRIVATE);
            String userEmail = sp.getString("userEmailValue", null);

            if (userEmail != null && !userEmail.isEmpty()) {
                Log.i(TAG, "Starting OTP service after boot/update");
                Intent serviceIntent = new Intent(context, OTPForegroundService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else {
                Log.i(TAG, "User email not configured, not starting service");
            }
        }
    }
}