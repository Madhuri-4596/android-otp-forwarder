package com.otp.ezybooking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.otp.ezybooking.database.OTPDatabase;
import com.otp.ezybooking.database.OTPMessage;
import com.otp.ezybooking.database.OTPMessageDao;
import com.otp.ezybooking.utils.NetworkUtils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OTPForegroundService extends Service implements SmsListener {

    private static final String TAG = "OTPForegroundService";
    private static final String CHANNEL_ID = "OTP_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    private static final int RETRY_DELAY_MS = 30000; // 30 seconds
    private static final int MAX_RETRY_COUNT = 5;

    private final String userOTPPostAPI = "https://www.ezybooking.in/OTP/apiotpauth/";
    private SharedPreferences sp;
    private OTPDatabase database;
    private OTPMessageDao otpMessageDao;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Handler retryHandler;

    private final String userEmailValue = "userEmailValue";
    private final String simNumber1Value = "simNumber1Value";
    private final String simNumber2Value = "simNumber2Value";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");

        sp = getSharedPreferences("ezybookinglogin", MODE_PRIVATE);
        database = OTPDatabase.getDatabase(this);
        otpMessageDao = database.otpMessageDao();
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        retryHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        SmsReceiver.bindListener(this);
        startRetryMechanism();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service onStartCommand");
        startForegroundService();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
        SmsReceiver.unBindListener();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (retryHandler != null) {
            retryHandler.removeCallbacksAndMessages(null);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "OTP Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Background service for OTP forwarding");
            serviceChannel.setShowBadge(false);
            serviceChannel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainAppActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OTP Forwarding Active")
                .setContentText("Monitoring SMS messages in the background")
                .setSmallIcon(R.drawable.ezybooking)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void messageReceived(String slot, String messageText) {
        Log.i(TAG, "Message received - Slot: " + slot + ", Text: " + messageText);

        String userEmail = sp.getString(userEmailValue, null);
        String inputMobileNumber1 = sp.getString(simNumber1Value, null);
        String inputMobileNumber2 = sp.getString(simNumber2Value, null);
        String validMobileNumber = null;

        if ((inputMobileNumber1 == null || inputMobileNumber1.isEmpty()) &&
            (inputMobileNumber2 == null || inputMobileNumber2.isEmpty())) {
            Log.e(TAG, "No mobile numbers configured");
            return;
        }

        if (slot.equalsIgnoreCase("0") || slot.equalsIgnoreCase("3")) {
            if (inputMobileNumber1 != null && !inputMobileNumber1.isEmpty()) {
                validMobileNumber = inputMobileNumber1;
            }
        } else {
            if (inputMobileNumber2 != null && !inputMobileNumber2.isEmpty()) {
                validMobileNumber = inputMobileNumber2;
            }
        }

        if (validMobileNumber != null) {
            processOtpCode(userEmail, validMobileNumber, messageText);
        }
    }

    private void processOtpCode(String validUserEmail, String validMobileNumber, String messageText) {
        String otpCode = null;
        boolean isSimOTP = false;

        // Extract OTP from message text (same logic as original)
        if (messageText.contains("Your One Time Password is ") && messageText.contains(" for SSMMS Login")) {
            isSimOTP = true;
            int endIndex = messageText.indexOf(" for SSMMS Login");
            otpCode = messageText.substring(26, endIndex).trim();
        } else if (messageText.contains("Use OTP ") && messageText.contains(" for SSMMS Login")) {
            isSimOTP = true;
            int endIndex = messageText.indexOf(" for SSMMS Login");
            otpCode = messageText.substring(8, endIndex).trim();
        } else if (messageText.contains(" is your One Time Password for SSMMS Login")) {
            isSimOTP = true;
            int endIndex = messageText.indexOf(" is your One Time Password for SSMMS Login");
            otpCode = messageText.substring(0, endIndex).trim();
        }

        String vehicleNumber = null;
        boolean isVehicleOTP = false;
        if (messageText.contains("OTP for SSMMS Booking with Vehicle No : ")) {
            isVehicleOTP = true;
            messageText = messageText.substring(40);
            int endIndex = messageText.indexOf(" is ");
            vehicleNumber = messageText.substring(0, endIndex).trim();
            otpCode = messageText.substring(endIndex + 6);
        }

        if (isSimOTP && validMobileNumber != null && otpCode != null) {
            queueOTPMessage(validUserEmail, validMobileNumber, otpCode);
        }
        if (isVehicleOTP && vehicleNumber != null && otpCode != null) {
            queueOTPMessage(validUserEmail, vehicleNumber, otpCode);
        }
    }

    private void queueOTPMessage(String userEmail, String referenceKey, String otpCode) {
        executorService.execute(() -> {
            try {
                OTPMessage otpMessage = new OTPMessage(userEmail, referenceKey, otpCode);
                otpMessageDao.insert(otpMessage);
                Log.i(TAG, "OTP message queued: " + referenceKey + " - " + otpCode);

                // Try to send immediately if network is available
                if (NetworkUtils.isNetworkAvailable(this)) {
                    sendPendingMessages();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error queuing OTP message", e);
            }
        });
    }

    private void startRetryMechanism() {
        retryHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (NetworkUtils.isNetworkAvailable(OTPForegroundService.this)) {
                    sendPendingMessages();
                }
                retryHandler.postDelayed(this, RETRY_DELAY_MS);
            }
        }, RETRY_DELAY_MS);
    }

    private void sendPendingMessages() {
        executorService.execute(() -> {
            try {
                List<OTPMessage> pendingMessages = otpMessageDao.getPendingMessages();
                Log.i(TAG, "Found " + pendingMessages.size() + " pending messages");

                for (OTPMessage message : pendingMessages) {
                    if (message.retryCount < MAX_RETRY_COUNT) {
                        boolean success = sendOTPToServer(message);
                        if (success) {
                            message.isSent = true;
                            otpMessageDao.update(message);
                            Log.i(TAG, "Message sent successfully: " + message.referenceKey);
                        } else {
                            message.retryCount++;
                            otpMessageDao.update(message);
                            Log.w(TAG, "Failed to send message, retry count: " + message.retryCount);
                        }
                    } else {
                        Log.e(TAG, "Max retry count reached for message: " + message.referenceKey);
                        message.isSent = true; // Mark as sent to stop retrying
                        otpMessageDao.update(message);
                    }
                }

                // Clean up old sent messages (older than 24 hours)
                long threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                otpMessageDao.deleteOldSentMessages(threshold);

            } catch (Exception e) {
                Log.e(TAG, "Error sending pending messages", e);
            }
        });
    }

    private boolean sendOTPToServer(OTPMessage message) {
        try {
            OTPRequest otpRequest = new OTPRequest(message.userEmail, message.referenceKey, message.otpCode);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            List<MediaType> acceptableMediaTypes = new ArrayList<>();
            acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
            acceptableMediaTypes.add(MediaType.TEXT_PLAIN);
            requestHeaders.setAccept(acceptableMediaTypes);

            HttpEntity<OTPRequest> requestEntity = new HttpEntity<>(otpRequest, requestHeaders);
            ResponseEntity<OTPResponse> responseEntity = restTemplate.postForEntity(
                    userOTPPostAPI, requestEntity, OTPResponse.class);

            if (responseEntity.getStatusCode() == HttpStatus.CREATED ||
                responseEntity.getStatusCode() == HttpStatus.OK) {

                String response = responseEntity.getBody().getResponse();
                mainHandler.post(() -> {
                    if (response.equalsIgnoreCase("Your Request Submitted Successfully")) {
                        Log.i(TAG, "OTP sent successfully: " + message.referenceKey);
                    } else {
                        Log.w(TAG, "Server response: " + response);
                    }
                });
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending OTP to server: " + e.getMessage());
        }
        return false;
    }
}