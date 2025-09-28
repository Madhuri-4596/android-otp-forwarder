package com.otp.ezybooking;

public class ACFData {

    private String otp_reference;
    private String otp_email;
    private String otp;
    private String is_active;
    private String title;
    private String startdate;

    public String getStartdate() {
        return startdate;
    }

    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }

    public ACFData(){}

    public ACFData(String is_active) {
        this.is_active = is_active;
    }

    public ACFData(String otp_reference, String otp_email, String otp, String is_active, String title, String startdate) {
        this.otp_reference = otp_reference;
        this.otp_email = otp_email;
        this.otp = otp;
        this.is_active = is_active;
        this.title = title;
        this.startdate = startdate;
    }

    public String getOtp_reference() {
        return otp_reference;
    }

    public void setOtp_reference(String otp_reference) {
        this.otp_reference = otp_reference;
    }

    public String getOtp_email() {
        return otp_email;
    }

    public void setOtp_email(String otp_email) {
        this.otp_email = otp_email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getIs_active() {
        return is_active;
    }

    public void setIs_active(String is_active) {
        this.is_active = is_active;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
