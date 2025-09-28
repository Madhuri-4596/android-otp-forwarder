package com.otp.ezybooking;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private ProgressBar progressBar;
    private Button btnLogin;

    private String userAuthenticationToken = null;
    SharedPreferences sp;

    private final String userAuthTokenConst = "userAuthToken";
    private final String userEmailConst = "userEmail";
    private final String userPasswordConst = "userPassword";
    private final String userIdConst = "userId";

    private int delay = 24 * 60 * 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the view now
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sp = getSharedPreferences("ezybookinglogin",MODE_PRIVATE);

        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnLogin = (Button) findViewById(R.id.btn_login);

        processSharedPreferenceUserAuthentication();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString();
                final String password = inputPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                // Perform the HTTP GET request
                Log.i("LoginActivity","Authenticating the user with email : " + email);
                processUserAuthentication(email, password);

            }
        });
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
                    userAuthenticationToken =  result.getData().getToken();
                    if (userAuthenticationToken != null) {
                        Toast.makeText(getApplicationContext(), "User authentication success", Toast.LENGTH_SHORT).show();

                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(userAuthTokenConst, userAuthenticationToken);
                        editor.putString(userEmailConst, result.getData().getEmail());
                        editor.putString(userPasswordConst, password);
                        editor.putString(userIdConst, result.getData().getId());
                        editor.commit();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userAuthToken", userAuthenticationToken);
                        intent.putExtra("userEmail", result.getData().getEmail());
                        intent.putExtra("userId", result.getData().getId());
                        startActivity(intent);
                        finish();
                    }
                }

            };
        userAuthenticationPostTask.execute(email, password, url);
    }


    @Override
    public void onResume() {
        super.onResume();
        //processSharedPreferenceUserAuthentication();
    }

    @Override
    public void onStart() {
        super.onStart();
        //processSharedPreferenceUserAuthentication();
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
}