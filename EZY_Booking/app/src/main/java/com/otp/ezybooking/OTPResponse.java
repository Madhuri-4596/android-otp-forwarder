package com.otp.ezybooking;

import org.codehaus.jackson.annotate.JsonProperty;

public class OTPResponse {

    private String response;

    public OTPResponse() {}

    public OTPResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
