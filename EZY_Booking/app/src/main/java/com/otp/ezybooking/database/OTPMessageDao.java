package com.otp.ezybooking.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface OTPMessageDao {

    @Insert
    void insert(OTPMessage otpMessage);

    @Update
    void update(OTPMessage otpMessage);

    @Query("SELECT * FROM otp_messages WHERE isSent = 0 ORDER BY timestamp ASC")
    List<OTPMessage> getPendingMessages();

    @Query("DELETE FROM otp_messages WHERE isSent = 1 AND timestamp < :threshold")
    void deleteOldSentMessages(long threshold);

    @Query("SELECT COUNT(*) FROM otp_messages WHERE isSent = 0")
    int getPendingMessageCount();
}