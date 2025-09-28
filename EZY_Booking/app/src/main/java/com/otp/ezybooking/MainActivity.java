package com.otp.ezybooking;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_CONTACTS;

public class MainActivity extends AppCompatActivity implements SmsListener {

    private static final String TAG = "MainActivity";
    private Button btnEditSave, btnsignOut;
    private EditText simNumber1Textbox, simNumber2Textbox;
    private TextView subscriptionTextView, permissionTextView;

    private String userAuthToken = null;
    private String userEmail = null;
    private String userId = null;
    private boolean isPaidUser = false;

    //private final String userSubscriptionAPI = "https://ezybooking.in/wp-json/pmpro/v1/get_membership_level_for_user?user_id=";
    private final String userSubscriptionLevelAPI = "https://www.ezybooking.in/OTP/getsubscriptionlevel/";
    private final String userOTPPostAPI = "https://www.ezybooking.in/OTP/apiotpauth/";

    private static final int PERMISSION_REQUEST_CODE = 200;

    SharedPreferences sp;

    private final String userAuthTokenConst = "userAuthToken";
    private final String userEmailConst = "userEmail";
    private final String userPasswordConst = "userPassword";
    private final String userIdConst = "userId";

    private final String editSaveValue = "editSaveValue";
    private final String simNumber1Value = "simNumber1Value";
    private final String simNumber2Value = "simNumber2Value";

    private Handler handler = new Handler();
    private Runnable runnable;
    private int delay = 24 * 60 * 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        sp = getSharedPreferences("ezybookinglogin",MODE_PRIVATE);

        Intent intent = getIntent();
        userAuthToken = intent.getStringExtra("userAuthToken");
        userEmail = intent.getStringExtra("userEmail");
        userId = intent.getStringExtra("userId");

        simNumber1Textbox = (EditText) findViewById(R.id.simnumber1);
        simNumber2Textbox = (EditText) findViewById(R.id.simnumber2);

        subscriptionTextView = (TextView) findViewById(R.id.subscriptionTextView);
        permissionTextView = (TextView) findViewById(R.id.permissionTextView);

        btnEditSave = (Button) findViewById(R.id.btn_edit_save);
        btnsignOut = (Button) findViewById(R.id.signout_button);

        boolean permissionsAccepted = checkPermissions();
        Log.i("MainActivity", "Permissions status :" + permissionsAccepted);
        if (!permissionsAccepted) {
            requestPermissions();
        } else {
            permissionTextView.setTextColor(getResources().getColor(R.color.colorGreen));
            permissionTextView.setText("Permissions Granted, You can use the application");
        }
        validateUserSubscription();

        /*
        simNumber1Textbox.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(simNumber1Value, simNumber1Textbox.getText().toString().trim());
                editor.commit();
            }
        });

        simNumber2Textbox.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(simNumber2Value, simNumber2Textbox.getText().toString().trim());
                editor.commit();
            }
        });
        */

        btnsignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        btnEditSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String existingText = btnEditSave.getText().toString();
                processOtpStatusValues(existingText);
            }
        });

        scheduleEveryDay();
    }

    private void processOtpStatusValues(String existingText) {
        Log.i("MainActivity", "existingText : "+ existingText);
        SharedPreferences.Editor editor = sp.edit();
        if (existingText.equalsIgnoreCase("EDIT")) {
            btnEditSave.setText(getString(R.string.btn_save));
            simNumber1Textbox.setEnabled(true);
            simNumber2Textbox.setEnabled(true);
            editor.putString(editSaveValue, "SAVE");
        } else if (existingText.equalsIgnoreCase("SAVE")) {
            btnEditSave.setText(getString(R.string.btn_edit));
            simNumber1Textbox.setEnabled(false);
            simNumber2Textbox.setEnabled(false);
            editor.putString(editSaveValue, "EDIT");
            editor.putString(simNumber1Value, simNumber1Textbox.getText().toString().trim());
            editor.putString(simNumber2Value, simNumber2Textbox.getText().toString().trim());
        }
        editor.commit();
    }

    @Override
    public void messageReceived(String slot, String messageText) {
        Log.i("MainActivity", "Slot number received : "+ slot + " Message : "+ messageText);

        String validMobileNumber = null;
        String inputMobileNumber1 = simNumber1Textbox.getText().toString().trim();
        String inputMobileNumber2 = simNumber2Textbox.getText().toString().trim();
        if ((inputMobileNumber1 == null || inputMobileNumber1.isEmpty()) && (inputMobileNumber2 == null || inputMobileNumber2.isEmpty())) {
            Toast.makeText(MainActivity.this, "Please enter Mobile Number1 or Mobile Number2", Toast.LENGTH_LONG).show();
            Log.i("MainActivity", "Mobile number not entered in the text boxes above");
        } else {
            if (slot.equalsIgnoreCase("0")) {
                if (inputMobileNumber1 != null && !inputMobileNumber1.isEmpty()) {
                    validMobileNumber = inputMobileNumber1;
                } else {
                    Toast.makeText(MainActivity.this, "Mesage received on SIM 1, Please enter mobile number 1", Toast.LENGTH_SHORT).show();
                }
            } else if (slot.equalsIgnoreCase("1")) {
                if (inputMobileNumber2 != null && !inputMobileNumber2.isEmpty()) {
                    validMobileNumber = inputMobileNumber2;
                } else {
                    Toast.makeText(MainActivity.this, "Message received on SIM 2, Please enter mobile number 2", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.i("MainActivity", "Could not find slot number from the message");
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
        Log.i("MainActivity", "called Handler after insert methods: " + new Date());
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
                            requestHeaders.set("Authorization", "Bearer " + userAuthToken);
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
                            Log.i("MainActivity", "OTP record created in Database successfully : " + new Date());

                            if (result.equalsIgnoreCase("Your Request Submitted Successfully")) {
                                Toast.makeText(getApplicationContext(), "OTP record created for referenceKey "
                                        + referenceKey + " OTP : " + otpCode, Toast.LENGTH_LONG).show();
                            } else if (result.equalsIgnoreCase("Active subscription is not found")) {
                                Toast.makeText(getApplicationContext(), "Please purchase subscription package to proceed."
                                        , Toast.LENGTH_LONG).show();
                            } else if (result.equalsIgnoreCase("User not found")) {
                                Toast.makeText(getApplicationContext(), "User not found with the given user email"
                                        , Toast.LENGTH_LONG).show();
                            } else if (result.equalsIgnoreCase("Post limit exceeded")) {
                                Toast.makeText(getApplicationContext(), "Post limit exceeded for the day for the current subscription."
                                        , Toast.LENGTH_LONG).show();
                            }

                        }
                    };
            userOTPPostTask.execute(userEmail, referenceKey, otpCode);

            Log.i("MainActivity", "OTP record created for referenceKey : "
                    + referenceKey + " OTP : " + otpCode);
            return true;
        } catch (Exception e) {
            Log.i("MainActivity", "OTP new record insertion failed for referenceKey : "
                    + referenceKey);
            Toast.makeText(MainActivity.this, "OTP new record insertion failed for referenceKey : "
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
                            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(params[0]);

                            RestTemplate restTemplate = new RestTemplate();

                            restTemplate.getMessageConverters()
                                    .add(new MappingJacksonHttpMessageConverter());
                            HttpHeaders requestHeaders = new HttpHeaders();
                            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
                            List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
                            acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
                            acceptableMediaTypes.add(MediaType.TEXT_PLAIN);
                            requestHeaders.setAccept(acceptableMediaTypes);
                            requestHeaders.set("Authorization", "Bearer " + userAuthToken);
                            HttpEntity<SubscriptionRequest> requestEntity = new HttpEntity<SubscriptionRequest>(subscriptionRequest, requestHeaders);
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
                                //Toast.makeText(MainActivity.this, "User subscription is active, starting to read SMS", Toast.LENGTH_SHORT).show();
                                Log.i("MainActivity", "User subscription is active, starting to read SMS");
                                SmsReceiver.bindListener(MainActivity.this);
                            } else {
                                subscriptionTextView.setTextColor(getResources().getColor(R.color.colorRed));
                                subscriptionTextView.setText("Subscription is Expired");
                                //Toast.makeText(MainActivity.this, "User subscription is inactive stopping to read SMS", Toast.LENGTH_SHORT).show();
                                Log.i("MainActivity", "User subscription is inactive stopping to read SMS");
                                SmsReceiver.unBindListener();
                            }
                        }
                    };
            userSubscriptionGetTask.execute(userId);
        } catch (Exception e) {
            Log.i("MainActivity", "Unable to read the user data : " + e.getMessage());
            Toast.makeText(MainActivity.this, "Unable to read the user data",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //processSharedPreferenceUserAuthentication();
        extractDefaultValuesFromSharedPreferences();
        if (userEmail != null && userId != null && userAuthToken != null) {
            validateUserSubscription();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    //sign out method
    public void signOut() {
        SmsReceiver.unBindListener();

        SharedPreferences.Editor editor = sp.edit();
        editor.remove(userAuthTokenConst);
        editor.remove(userEmailConst);
        editor.remove(userPasswordConst);
        editor.remove(userIdConst);
        editor.commit();

        Intent intent = getIntent();
        intent.removeExtra("userAuthToken");
        intent.removeExtra("userEmail");
        intent.removeExtra("userId");
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }

    @Override
    public void onStart() {
        super.onStart();
        //processSharedPreferenceUserAuthentication();
        extractDefaultValuesFromSharedPreferences();
        if (userEmail != null && userId != null && userAuthToken != null) {
            validateUserSubscription();
            if (isPaidUser) {
                SmsReceiver.bindListener(MainActivity.this);
            }
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
                        Toast.makeText(MainActivity.this, "Permissions Granted, You can use the application",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        permissionTextView.setTextColor(getResources().getColor(R.color.colorRed));
                        permissionTextView.setText("Permissions Denied, You can not use the application.");
                        Toast.makeText(MainActivity.this, "Permissions Denied, You can not use the application.",
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
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void processSharedPreferenceUserAuthentication() {
        Log.i("LoginActivity", "loggedin shared preference");
        String savedUserAuthToken = sp.getString(userAuthTokenConst, null);
        String savedUserEmail = sp.getString(userEmailConst, null);
        String savedUserPassword = sp.getString(userPasswordConst, null);
        String savedUserId = sp.getString(userIdConst, null);

        if (savedUserEmail != null && savedUserEmail != "" && savedUserPassword != null && savedUserPassword != "") {
            Log.i("LoginActivity", "inside saved preference user details fetch");
            processUserAuthentication(savedUserEmail, savedUserPassword);
        } else {
            Log.i("LoginActivity", "not saved preference user details fetch");
        }
    }

    private void processUserAuthentication(final String email, final String password) {
        String url = "https://ezybooking.in/wp-json/jwt-auth/v1/token";

        AsyncTask<String, Void, UserResponse> userAuthenticationPostTask =
                new AsyncTask<String, Void, UserResponse>() {
                    @Override
                    protected UserResponse doInBackground(String... params) {
                        UserRequest userRequest = new UserRequest();
                        userRequest.setUsername(params[0]);
                        userRequest.setPassword(params[1]);

                        RestTemplate restTemplate = new RestTemplate();

                        String url = params[2];

                        restTemplate.getMessageConverters()
                                .add(new MappingJacksonHttpMessageConverter());
                        HttpHeaders requestHeaders = new HttpHeaders();
                        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
                        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
                        acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
                        acceptableMediaTypes.add(MediaType.TEXT_PLAIN);
                        requestHeaders.setAccept(acceptableMediaTypes);

                        HttpEntity<UserRequest> requestEntity =
                                new HttpEntity<UserRequest>(userRequest,requestHeaders);
                        ResponseEntity<UserResponse> responseEntity =
                                restTemplate.postForEntity(url, requestEntity,
                                        UserResponse.class);
                        UserResponse userResponse = null;
                        if(responseEntity.getStatusCode() != HttpStatus.OK) {
                            throw new HttpServerErrorException(
                                    responseEntity.getStatusCode());
                        } else {
                            try {
                                userResponse = (UserResponse) responseEntity.getBody();
                            } catch (Exception e) {
                                Log.i("LoginActivity", "Error reading data from the user authentication");
                            }
                        }
                        return userResponse;
                    }

                    @Override
                    protected void onPostExecute(UserResponse result) {
                        if (result.getData().getToken() != null) {
                            Toast.makeText(getApplicationContext(), "User authentication success", Toast.LENGTH_SHORT).show();

                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString(userAuthTokenConst, result.getData().getToken());
                            editor.putString(userEmailConst, result.getData().getEmail());
                            editor.putString(userPasswordConst, password);
                            editor.putString(userIdConst, result.getData().getId());
                            editor.commit();
                        }
                    }

                };
        userAuthenticationPostTask.execute(email, password, url);
    }

    private void extractDefaultValuesFromSharedPreferences() {
        String editSaveVal = sp.getString(editSaveValue, null);
        String spMobile1Number = sp.getString(simNumber1Value, null);
        String spMobile2Number = sp.getString(simNumber2Value, null);

        Log.i("MainActivity", "editSaveVal : "+ editSaveVal);
        if (editSaveVal != null && editSaveVal != "") {
            if (editSaveVal.equalsIgnoreCase("EDIT")) {
                simNumber1Textbox.setEnabled(false);
                simNumber2Textbox.setEnabled(false);
            } else if (editSaveVal.equalsIgnoreCase("SAVE")) {
                simNumber1Textbox.setEnabled(true);
                simNumber2Textbox.setEnabled(true);
            }
            btnEditSave.setText(editSaveVal);
        }
        if (spMobile1Number != null && spMobile1Number != "") {
            simNumber1Textbox.setText(spMobile1Number);
        }
        if (spMobile2Number != null && spMobile2Number != "") {
            simNumber2Textbox.setText(spMobile2Number);
        }
    }

    public void scheduleEveryDay() {
        final Handler myHandler = new Handler();
        myHandler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                //Do something after 20 seconds
                //call the method which is schedule to call after 20 sec
                Log.i("MainActivity", "Scheduler called for 24 hours : " + new Date());
                processSharedPreferenceUserAuthentication();
                myHandler.postDelayed(runnable, delay);
            }
        }, delay);
    }
}