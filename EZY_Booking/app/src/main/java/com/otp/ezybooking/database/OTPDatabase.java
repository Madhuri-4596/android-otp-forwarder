package com.otp.ezybooking.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {OTPMessage.class}, version = 1, exportSchema = false)
public abstract class OTPDatabase extends RoomDatabase {

    private static volatile OTPDatabase INSTANCE;

    public abstract OTPMessageDao otpMessageDao();

    public static OTPDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (OTPDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    OTPDatabase.class, "otp_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}