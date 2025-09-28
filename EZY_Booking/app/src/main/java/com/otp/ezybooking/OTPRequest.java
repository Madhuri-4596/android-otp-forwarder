package com.otp.ezybooking;

public class OTPRequest {

    private String otpReferenceKey;
    private String useremail;
    private String otpCode;

    public OTPRequest(String useremail, String otpReferenceKey, String otpCode) {
        this.otpReferenceKey = otpReferenceKey;
        this.useremail = useremail;
        this.otpCode = otpCode;
    }

    public String getOtpReferenceKey() {
        return otpReferenceKey;
    }

    public void setOtpReferenceKey(String otpReferenceKey) {
        this.otpReferenceKey = otpReferenceKey;
    }

    public String getUseremail() {
        return useremail;
    }

    public void setUseremail(String useremail) {
        this.useremail = useremail;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
