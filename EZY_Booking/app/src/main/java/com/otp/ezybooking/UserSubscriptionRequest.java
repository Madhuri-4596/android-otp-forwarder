package com.otp.ezybooking;

public class UserSubscriptionRequest {
    String userEmail;

    public UserSubscriptionRequest(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
