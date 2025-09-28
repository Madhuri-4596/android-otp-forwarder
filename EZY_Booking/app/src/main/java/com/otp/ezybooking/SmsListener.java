package com.otp.ezybooking;

public interface SmsListener {
    public void messageReceived(String slot, String messageText);
}
