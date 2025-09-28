package com.otp.ezybooking;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.SEND_SMS;

public class MainAppActivityBackup extends AppCompatActivity implements SmsListener {

    private static final String TAG = "MainAppActivity";
    private Button btnEditSave;
    private EditText userEmailTextbox, simNumber1Textbox, simNumber2Textbox;
    private TextView subscriptionTextView, permissionTextView;

    private boolean isPaidUser = false;

    private final String userSubscriptionLevelAPI = "https://www.ezybooking.in/OTP/getusersubscriptionlevel/";
    private final String userOTPPostAPI = "https://www.ezybooking.in/OTP/apiotpauth/";

    private static final int PERMISSION_REQUEST_CODE = 200;

    SharedPreferences sp;

    private final String editSaveValue = "editSaveValue";
    private final String userEmailValue = "userEmailValue";
    private final String simNumber1Value = "simNumber1Value";
    private final String simNumber2Value = "simNumber2Value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        sp = getSharedPreferences("ezybookinglogin",MODE_PRIVATE);

        userEmailTextbox = (EditText) findViewById(R.id.useremail);
        simNumber1Textbox = (EditText) findViewById(R.id.simnumber1);
        simNumber2Textbox = (EditText) findViewById(R.id.simnumber2);

        subscriptionTextView = (TextView) findViewById(R.id.subscriptionTextView);
        permissionTextView = (TextView) findViewById(R.id.permissionTextView);

        btnEditSave = (Button) findViewById(R.id.btn_edit_save);

        boolean permissionsAccepted = checkPermissions();
        Log.i("MainAppActivity", "Permissions status :" + permissionsAccepted);
        if (!permissionsAccepted) {
            requestPermissions();
        } else {
            permissionTextView.setTextColor(getResources().getColor(R.color.colorGreen));
            permissionTextView.setText("Permissions Granted, You can use the application");
        }

        btnEditSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String existingText = btnEditSave.getText().toString();
                processOtpStatusValues(existingText);
            }
        });
    }

    private void processOtpStatusValues(String existingText) {
        Log.i("MainAppActivity", "existingText : "+ existingText);
        SharedPreferences.Editor editor = sp.edit();
        if (existingText.equalsIgnoreCase("EDIT")) {
            btnEditSave.setText(getString(R.string.btn_save));
            userEmailTextbox.setEnabled(true);
            simNumber1Textbox.setEnabled(true);
            simNumber2Textbox.setEnabled(true);
            editor.putString(editSaveValue, "SAVE");
        } else if (existingText.equalsIgnoreCase("SAVE")) {
            btnEditSave.setText(getString(R.string.btn_edit));
            userEmailTextbox.setEnabled(false);
            simNumber1Textbox.setEnabled(false);
            simNumber2Textbox.setEnabled(false);
            editor.putString(editSaveValue, "EDIT");
            editor.putString(userEmailValue, userEmailTextbox.getText().toString().trim());
            editor.putString(simNumber1Value, simNumber1Textbox.getText().toString().trim());
            editor.putString(simNumber2Value, simNumber2Textbox.getText().toString().trim());
            Log.i("MainAppActivity", "userEmailTextbox.getText().toString() 1: " + userEmailTextbox.getText().toString());
            if (userEmailTextbox.getText().toString() != null && !userEmailTextbox.getText().toString().isEmpty()) {
                validateUserSubscription();
            }
        }
        editor.commit();
    }

    @Override
    public void messageReceived(String slot, String messageText) {
        Log.i("MainAppActivity", "Slot number received : "+ slot + " Message : "+ messageText);

        String validMobileNumber = null;
        String inputMobileNumber1 = simNumber1Textbox.getText().toString().trim();
        String inputMobileNumber2 = simNumber2Textbox.getText().toString().trim();
        if ((inputMobileNumber1 == null || inputMobileNumber1.isEmpty()) && (inputMobileNumber2 == null || inputMobileNumber2.isEmpty())) {
            Toast.makeText(MainAppActivityBackup.this, "Please enter Mobile Number1 or Mobile Number2", Toast.LENGTH_LONG).show();
            Log.i("MainAppActivity", "Mobile number not entered in the text boxes above");
        } else {
            if (slot.equalsIgnoreCase("0")) {
                if (inputMobileNumber1 != null && !inputMobileNumber1.isEmpty()) {
                    validMobileNumber = inputMobileNumber1;
                } else {
                    Toast.makeText(MainAppActivityBackup.this, "Mesage received on SIM 1, Please enter mobile number 1", Toast.LENGTH_SHORT).show();
                }
            } else if (slot.equalsIgnoreCase("1")) {
                if (inputMobileNumber2 != null && !inputMobileNumber2.isEmpty()) {
                    validMobileNumber = inputMobileNumber2;
                } else {
                    Toast.makeText(MainAppActivityBackup.this, "Message received on SIM 2, Please enter mobile number 2", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.i("MainAppActivity", "Could not find slot number from the message");
            }
            if (validMobileNumber != null) {
                boolean otpProcessed = processOtpCode(validMobileNumber, messageText);
            }
        }
    }

    private boolean processOtpCode(String validMobileNumber, String messageText) {
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
                boolean otpSaved = saveOTPInDb(validMobileNumber, otpCode);
            }
        }
        if (isVehicleOTP) {
            if (vehicleNumber != null && otpCode != null) {
                boolean otpSaved = saveOTPInDb(vehicleNumber, otpCode);
            }
        }
        Log.i("MainAppActivity", "called Handler after insert methods: " + new Date());
        return true;
    }

    private boolean saveOTPInDb(final String referenceKey, final String otpCode) {
        try {
            AsyncTask<String, Void, String> userOTPPostTask =
                    new AsyncTask<String, Void, String>() {
                        @Override
                        protected String doInBackground(String... params) {
                            OTPRequest otpRequest = new OTPRequest(params[0], params[1], params[2]);

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
                            if(responseEntity.getStatusCode() == HttpStatus.CREATED || responseEntity.getStatusCode() == HttpStatus.OK) {
                                return responseEntity.getBody().getResponse();
                            } else {
                                throw new HttpServerErrorException(responseEntity.getStatusCode());
                            }
                        }

                        @Override
                        protected void onPostExecute(final String result) {
                            Log.i("MainAppActivity", "OTP record created in Database successfully : " + new Date());

                            if (result.equalsIgnoreCase("Your Request Submitted Successfully")) {
                                Toast.makeText(getApplicationContext(), "OTP record created for referenceKey "
                                        + referenceKey + " OTP : " + otpCode, Toast.LENGTH_LONG).show();
                            } else if (result.equalsIgnoreCase("Subscription not found for user")) {
                                Toast.makeText(getApplicationContext(), "Subscription package not avaiable for the user requested"
                                        , Toast.LENGTH_LONG).show();
                            } else if (result.equalsIgnoreCase("Post limit exceeded")) {
                                Toast.makeText(getApplicationContext(), "Post limit exceeded for the day for the current subscription."
                                        , Toast.LENGTH_LONG).show();
                            }

                        }
                    };
            userOTPPostTask.execute(userEmailTextbox.getText().toString(), referenceKey, otpCode);

            Log.i("MainAppActivity", "OTP record created for referenceKey : "
                    + referenceKey + " OTP : " + otpCode);
            return true;
        } catch (Exception e) {
            Log.i("MainAppActivity", "OTP new record insertion failed for referenceKey : "
                    + referenceKey);
            Toast.makeText(MainAppActivityBackup.this, "OTP new record insertion failed for referenceKey : "
                    + referenceKey, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void validateUserSubscription() {
        try {
            @SuppressLint("StaticFieldLeak") AsyncTask<String, Void, String> userSubscriptionGetTask =
                    new AsyncTask<String, Void, String>() {
                        @Override
                        protected String doInBackground(String... params) {
                            UserSubscriptionRequest subscriptionRequest = new UserSubscriptionRequest(params[0]);

                            Log.i("MainAppActivity", "params[0] : " + params[0]);
                            RestTemplate restTemplate = new RestTemplate();

                            restTemplate.getMessageConverters()
                                    .add(new MappingJacksonHttpMessageConverter());
                            HttpHeaders requestHeaders = new HttpHeaders();
                            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
                            List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
                            acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
                            acceptableMediaTypes.add(MediaType.TEXT_PLAIN);
                            requestHeaders.setAccept(acceptableMediaTypes);
                            HttpEntity<UserSubscriptionRequest> requestEntity = new HttpEntity<>(subscriptionRequest, requestHeaders);
                            ResponseEntity<OTPResponse> responseEntity =
                                    restTemplate.postForEntity(userSubscriptionLevelAPI, requestEntity,
                                            OTPResponse.class);
                            if(responseEntity.getStatusCode() == HttpStatus.CREATED || responseEntity.getStatusCode() == HttpStatus.OK) {
                                return responseEntity.getBody().getResponse();
                            } else {
                                throw new HttpServerErrorException(responseEntity.getStatusCode());
                            }
                        }
                        @Override
                        protected void onPostExecute(String response) {
                            boolean result = false;
                            if (response.equalsIgnoreCase("true")) {
                                result = true;
                            } else {
                                result = false;
                            }
                            isPaidUser = result;
                            if (isPaidUser) {
                                subscriptionTextView.setTextColor(getResources().getColor(R.color.colorGreen));
                                subscriptionTextView.setText("Subscription is Active");
                                //Toast.makeText(MainAppActivity.this, "User subscription is active, starting to read SMS", Toast.LENGTH_SHORT).show();
                                Log.i("MainAppActivity", "User subscription is active, starting to read SMS");
                                SmsReceiver.bindListener(MainAppActivityBackup.this);
                            } else {
                                subscriptionTextView.setTextColor(getResources().getColor(R.color.colorRed));
                                subscriptionTextView.setText("Subscription is Expired");
                                //Toast.makeText(MainAppActivity.this, "User subscription is inactive stopping to read SMS", Toast.LENGTH_SHORT).show();
                                Log.i("MainAppActivity", "User subscription is inactive stopping to read SMS");
                                SmsReceiver.unBindListener();
                            }
                        }
                    };
            Log.i("MainAppActivity", "userEmailTextbox.getText().toString() 4: " + userEmailTextbox.getText().toString());
            userSubscriptionGetTask.execute(userEmailTextbox.getText().toString());
        } catch (Exception e) {
            Log.i("MainAppActivity", "Unable to read the user data : " + e.getMessage());
            Toast.makeText(MainAppActivityBackup.this, "Unable to read the user data",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        extractDefaultValuesFromSharedPreferences();
        Log.i("MainAppActivity", "userEmailTextbox.getText().toString() 2: " + userEmailTextbox.getText().toString());
        if (userEmailTextbox.getText().toString() != null && !userEmailTextbox.getText().toString().isEmpty()) {
            validateUserSubscription();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        extractDefaultValuesFromSharedPreferences();
        Log.i("MainAppActivity", "userEmailTextbox.getText().toString() 3: " + userEmailTextbox.getText().toString());
        if (userEmailTextbox.getText().toString() != null && !userEmailTextbox.getText().toString().isEmpty()) {
            validateUserSubscription();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    private boolean checkPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_STATE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECEIVE_SMS);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_SMS);
        int result3 = ContextCompat.checkSelfPermission(getApplicationContext(), SEND_SMS);
        int result4 = ContextCompat.checkSelfPermission(getApplicationContext(), GET_ACCOUNTS);
        int result5 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_CONTACTS);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
                && result2 == PackageManager.PERMISSION_GRANTED && result3 == PackageManager.PERMISSION_GRANTED
                && result4 == PackageManager.PERMISSION_GRANTED && result5 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{READ_PHONE_STATE, RECEIVE_SMS, READ_SMS, SEND_SMS, GET_ACCOUNTS, READ_CONTACTS}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                            && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED
                            && grantResults[4] == PackageManager.PERMISSION_GRANTED && grantResults[5] == PackageManager.PERMISSION_GRANTED) {
                        permissionTextView.setTextColor(getResources().getColor(R.color.colorGreen));
                        permissionTextView.setText("Permissions Granted, You can use the application");
                        Toast.makeText(MainAppActivityBackup.this, "Permissions Granted, You can use the application",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        permissionTextView.setTextColor(getResources().getColor(R.color.colorRed));
                        permissionTextView.setText("Permissions Denied, You can not use the application.");
                        Toast.makeText(MainAppActivityBackup.this, "Permissions Denied, You can not use the application.",
                                Toast.LENGTH_SHORT).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(RECEIVE_SMS) && shouldShowRequestPermissionRationale(READ_PHONE_STATE)
                                    && shouldShowRequestPermissionRationale(READ_SMS)  && shouldShowRequestPermissionRationale(GET_ACCOUNTS)
                                    && shouldShowRequestPermissionRationale(SEND_SMS)  && shouldShowRequestPermissionRationale(READ_CONTACTS)) {
                                showMessageOKCancel("You need to allow access to all the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{READ_PHONE_STATE, RECEIVE_SMS, READ_SMS, SEND_SMS, GET_ACCOUNTS, READ_CONTACTS},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }


                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainAppActivityBackup.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void extractDefaultValuesFromSharedPreferences() {
        String editSaveVal = sp.getString(editSaveValue, null);
        String userEmail = sp.getString(userEmailValue, null);
        String spMobile1Number = sp.getString(simNumber1Value, null);
        String spMobile2Number = sp.getString(simNumber2Value, null);

        Log.i("MainAppActivity", "editSaveVal : "+ editSaveVal);
        if (editSaveVal != null && editSaveVal != "") {
            if (editSaveVal.equalsIgnoreCase("EDIT")) {
                userEmailTextbox.setEnabled(false);
                simNumber1Textbox.setEnabled(false);
                simNumber2Textbox.setEnabled(false);
            } else if (editSaveVal.equalsIgnoreCase("SAVE")) {
                userEmailTextbox.setEnabled(true);
                simNumber1Textbox.setEnabled(true);
                simNumber2Textbox.setEnabled(true);
            }
            btnEditSave.setText(editSaveVal);
        }
        if (userEmail != null && userEmail != "") {
            userEmailTextbox.setText(userEmail);
        }
        if (spMobile1Number != null && spMobile1Number != "") {
            simNumber1Textbox.setText(spMobile1Number);
        }
        if (spMobile2Number != null && spMobile2Number != "") {
            simNumber2Textbox.setText(spMobile2Number);
        }
    }
}