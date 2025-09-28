package com.otp.ezybooking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    //interface
    private static SmsListener mListener;
    private String slot = "-1";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (mListener != null) {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Log.i("SmsListener", "Message received");
                Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                int simSlot = intent.getIntExtra("simId", -1);
                //Log.i("SmsReceiver", "intent slot = " + intent.getExtras().get("slot"));
                if(intent.getExtras().get("slot") != null){
                    slot = intent.getExtras().get("slot").toString();
                } else {
                    slot = String.valueOf(capturedSimSlot(bundle));
                }
                //Toast.makeText(context, "slot number : " + slot, Toast.LENGTH_SHORT).show();
                Log.i("SmsReceiver", "SlotInfo : " + slot);
                SmsMessage[] msgs = null;
                if (bundle != null) {
                    //---retrieve the SMS message received---
                    try {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        msgs = new SmsMessage[pdus.length];
                        for (int i = 0; i < msgs.length; i++) {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            String msgBody = msgs[i].getMessageBody();
                            Log.i("SmsReceiver", "Message : " + msgBody);
                            mListener.messageReceived(slot, msgBody);
                        }
                    } catch (Exception e) {
                        Log.d("Exception caught : ", e.getMessage());
                    }
                }
            }
        }
    }

    /*below methods captures the sim slot number from bundle */

    public int capturedSimSlot(Bundle bundle){
        int slot = -1;
        try {
            if (bundle != null) {
                Set<String> keySet = bundle.keySet();
                for(String key:keySet){
                    switch (key){
                        case "slot":slot = bundle.getInt("slot", -1);
                            break;
                        case "simId":slot = bundle.getInt("simId", -1);
                            break;
                        case "simSlot":slot = bundle.getInt("simSlot", -1);
                            break;
                        case "slot_id":slot = bundle.getInt("slot_id", -1);
                            break;
                        case "simnum":slot = bundle.getInt("simnum", -1);
                            break;
                        case "slotId":slot = bundle.getInt("slotId", -1);
                            break;
                        case "slotIdx":slot = bundle.getInt("slotIdx", -1);
                            break;
                        case "phone":slot = bundle.getInt("phone", -1);
                            break;
                        case "sim":slot = bundle.getInt("sim", -1);
                            break;
                        /*case "subscription":slot = bundle.getInt("subscription", -1);
                            break;*/
                        default:
                            Log.i("bundle.keySet", "key=>"+key);
                    }
                }
                Log.d("slot", "slot=>"+slot);
            }
        }catch(Exception e){
            Log.i("Exception", "SlotInfo : " + e.getMessage());
        }
        return slot;
}

    public static void bindListener(SmsListener listener) {
        mListener = listener;
    }

    public static void unBindListener() {
        mListener = null;
    }
}
