package com.otp.ezybooking;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainAppService extends Service implements SmsListener {

    private final String userOTPPostAPI = "https://www.ezybooking.in/OTP/apiotpauth/";
    SharedPreferences sp;

    private final String userEmailValue = "userEmailValue";
    private final String simNumber1Value = "simNumber1Value";
    private final String simNumber2Value = "simNumber2Value";

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MainAppService", "onStartCommand");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("MainAppService", "IBinder");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("MainAppService", "onCreate");
        new CheckActivityStatus().execute();
    }

    private class CheckActivityStatus extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.i("MainAppService", "doInBackground");
            while(true) {
                SmsReceiver.bindListener(MainAppService.this);
            }
        }
    }

    @Override
    public void messageReceived(String slot, String messageText) {
        Log.i("MainAppService", "Slot number received : "+ slot + " Message : "+ messageText);
        sp = getSharedPreferences("ezybookinglogin",MODE_PRIVATE);
        String userEmail = sp.getString(userEmailValue, null);
        String inputMobileNumber1 = sp.getString(simNumber1Value, null);
        String inputMobileNumber2 = sp.getString(simNumber2Value, null);
        String validMobileNumber = null;
        if ((inputMobileNumber1 == null || inputMobileNumber1.isEmpty()) && (inputMobileNumber2 == null || inputMobileNumber2.isEmpty())) {
            Toast.makeText(MainAppService.this, "Please enter Mobile Number1 or Mobile Number2", Toast.LENGTH_LONG).show();
            Log.i("MainAppService", "Mobile number not entered in the text boxes above");
        } else {
            if (slot.equalsIgnoreCase("0") || slot.equalsIgnoreCase("3")) {
                if (inputMobileNumber1 != null && !inputMobileNumber1.isEmpty()) {
                    validMobileNumber = inputMobileNumber1;
                } else {
                    Toast.makeText(getApplicationContext(), "Mesage received on SIM 1, Please enter mobile number 1", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (inputMobileNumber2 != null && !inputMobileNumber2.isEmpty()) {
                    validMobileNumber = inputMobileNumber2;
                } else {
                    Toast.makeText(getApplicationContext(), "Message received on SIM 2, Please enter mobile number 2", Toast.LENGTH_SHORT).show();
                }
            }
            if (validMobileNumber != null) {
                boolean otpProcessed = processOtpCode(userEmail, validMobileNumber, messageText);
            }
        }
    }

    private boolean processOtpCode(String validUserEmail, String validMobileNumber, String messageText) {
        String otpCode = null;
        boolean isSimOTP = false;
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
            otpCode = messageText.substring(endIndex+6);
        }

        if (isSimOTP) {
            if (validMobileNumber != null && otpCode != null) {
                boolean otpSaved = saveOTPInDb(validUserEmail, validMobileNumber, otpCode);
            }
        }
        if (isVehicleOTP) {
            if (vehicleNumber != null && otpCode != null) {
                boolean otpSaved = saveOTPInDb(validUserEmail, vehicleNumber, otpCode);
            }
        }
        Log.i("MainAppActivity", "called Handler after insert methods: " + new Date());
        return true;
    }

    private boolean saveOTPInDb(final String referenceUserEmail, final String referenceKey, final String otpCode) {
        Toast.makeText(getApplicationContext(), "inside : "+referenceUserEmail + " key : " + referenceKey, Toast.LENGTH_LONG).show();
        try {
            OTPRequest otpRequest = new OTPRequest(referenceUserEmail, referenceKey, otpCode);

            RestTemplate restTemplate = new RestTemplate();

            restTemplate.getMessageConverters()
                    .add(new MappingJacksonHttpMessageConverter());
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
            acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
            acceptableMediaTypes.add(MediaType.TEXT_PLAIN);
            requestHeaders.setAccept(acceptableMediaTypes);
            HttpEntity<OTPRequest> requestEntity = new HttpEntity<OTPRequest>(otpRequest, requestHeaders);
            ResponseEntity<OTPResponse> responseEntity =
                    restTemplate.postForEntity(userOTPPostAPI, requestEntity,
                            OTPResponse.class);
            Log.i("MainAppService", "inside service: "+responseEntity.getStatusCode());
            Toast.makeText(getApplicationContext(), "inside service: "+responseEntity.getStatusCode(), Toast.LENGTH_LONG).show();
            if(responseEntity.getStatusCode() == HttpStatus.CREATED || responseEntity.getStatusCode() == HttpStatus.OK) {
                if (responseEntity.getBody().getResponse().equalsIgnoreCase("Your Request Submitted Successfully")) {
                    Toast.makeText(getApplicationContext(), "OTP record created for referenceKey "
                            + referenceKey + " OTP : " + otpCode, Toast.LENGTH_LONG).show();
                } else if (responseEntity.getBody().getResponse().equalsIgnoreCase("Subscription not found for user")) {
                    Toast.makeText(getApplicationContext(), "Subscription package not avaiable for the user requested"
                            , Toast.LENGTH_LONG).show();
                } else if (responseEntity.getBody().getResponse().equalsIgnoreCase("Post limit exceeded")) {
                    Toast.makeText(getApplicationContext(), "Post limit exceeded for the day for the current subscription."
                            , Toast.LENGTH_LONG).show();
                }
            } else {
                throw new HttpServerErrorException(responseEntity.getStatusCode());
            }

            Log.i("MainAppActivity", "OTP record created for referenceKey : "
                    + referenceKey + " OTP : " + otpCode);
            return true;
        } catch (Exception e) {
            Log.i("MainAppActivity", "OTP new record insertion failed for referenceKey : "
                    + e);
            Toast.makeText(MainAppService.this, "OTP new record insertion failed for referenceKey : "
                    + referenceKey, Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
