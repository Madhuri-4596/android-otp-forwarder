package com.otp.ezybooking;

public class SubscriptionRequest {
    String userId;

    public SubscriptionRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
