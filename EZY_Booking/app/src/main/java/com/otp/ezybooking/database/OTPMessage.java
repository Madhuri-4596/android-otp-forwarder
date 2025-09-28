package com.otp.ezybooking.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "otp_messages")
public class OTPMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userEmail;
    public String referenceKey;
    public String otpCode;
    public long timestamp;
    public int retryCount;
    public boolean isSent;

    public OTPMessage(String userEmail, String referenceKey, String otpCode) {
        this.userEmail = userEmail;
        this.referenceKey = referenceKey;
        this.otpCode = otpCode;
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
        this.isSent = false;
    }
}