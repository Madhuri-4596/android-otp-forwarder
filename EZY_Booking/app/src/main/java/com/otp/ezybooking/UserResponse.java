package com.otp.ezybooking;

public class UserResponse {

    private String success;
    private String statusCode;
    private String code;
    private String message;
    private UserResponseData data;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserResponseData getData() {
        return data;
    }

    public void setData(UserResponseData data) {
        this.data = data;
    }
}
