# Android OTP SMS Forwarder

![Build Status](https://github.com/yourusername/android-otp-forwarder/workflows/Build%20Android%20APK/badge.svg)

An Android application that automatically forwards OTP SMS messages to your Apache + MySQL server with robust background processing and offline queuing capabilities.

## 🚀 Features

- ✅ **Persistent Background Service** - Stays alive 24/7, won't be killed by system
- ✅ **Offline Message Queuing** - Stores messages locally when offline, sends when connected
- ✅ **Auto-restart After Reboot** - Automatically starts after device restart
- ✅ **Battery Optimization Handling** - Requests whitelist for maximum reliability
- ✅ **Android 5-14+ Compatibility** - Works on Android 5.0 through Android 14+
- ✅ **Smart Retry Mechanism** - Automatic retry with exponential backoff
- ✅ **Dual SIM Support** - Handles messages from both SIM slots

## 📱 Compatibility

- **Minimum Android Version**: Android 5.0 (API 21)
- **Target Android Version**: Android 14 (API 34)
- **Architecture**: ARM, ARM64, x86, x86_64

## 📥 Download

### Latest Release
Go to [Releases](../../releases/latest) and download:
- **app-debug.apk** - For testing and development
- **app-release.apk** - For production use (recommended)

### Auto-builds
Every commit triggers an automatic build. Check the [Actions](../../actions) tab for the latest builds.

## 🔧 Installation

1. **Download APK** from releases or actions
2. **Enable Unknown Sources**:
   - Android 8+: Settings → Apps → Special Access → Install Unknown Apps → Enable for your browser/file manager
   - Android 7-: Settings → Security → Unknown Sources → Enable
3. **Install APK** by tapping the downloaded file
4. **Grant Permissions** when prompted (SMS, Phone, Network access)
5. **Configure Settings**:
   - Enter your email address
   - Enter mobile number(s) for SIM 1/2
   - Allow battery optimization exception when prompted

## ⚙️ Configuration

### Server Setup
The app sends OTP data to your server endpoint:
- **Endpoint**: `https://www.ezybooking.in/OTP/apiotpauth/`
- **Method**: POST
- **Content-Type**: application/json

### Request Format
```json
{
  "userEmail": "your@email.com",
  "referenceKey": "mobile_number_or_vehicle_number",
  "otpCode": "123456"
}
```

### Supported OTP Formats
The app automatically detects and extracts OTP from messages containing:
- `"Your One Time Password is [CODE] for SSMMS Login"`
- `"Use OTP [CODE] for SSMMS Login"`
- `"[CODE] is your One Time Password for SSMMS Login"`
- `"OTP for SSMMS Booking with Vehicle No : [VEHICLE] is [CODE]"`

## 🔒 Permissions Required

- **RECEIVE_SMS** - Intercept incoming SMS messages
- **READ_SMS** - Read SMS content
- **READ_PHONE_STATE** - Detect SIM slot information
- **INTERNET** - Send data to server
- **ACCESS_NETWORK_STATE** - Monitor connectivity
- **FOREGROUND_SERVICE** - Run persistent background service
- **RECEIVE_BOOT_COMPLETED** - Auto-start after reboot

## 🏗️ Building from Source

### Prerequisites
- Android Studio with SDK API 34
- Java 11 or higher

### Local Build
```bash
git clone https://github.com/yourusername/android-otp-forwarder.git
cd android-otp-forwarder
./gradlew assembleDebug
```

### GitHub Actions Build
Every push to main/master branch automatically:
1. Builds debug and release APKs
2. Creates a new release with version number
3. Uploads APKs as downloadable assets

## 🛠️ Troubleshooting

### Common Issues

#### "App not installed" Error
- Uninstall any previous version first
- Clear package installer cache
- Ensure sufficient storage space

#### Service Not Running
- Check if battery optimization is disabled for the app
- Verify all permissions are granted
- Restart the app after granting permissions

#### Messages Not Sending
- Check internet connectivity
- Verify server endpoint is accessible
- Check app logs for error details

#### Background Service Killed
- Disable battery optimization for the app
- Add app to auto-start list (varies by manufacturer)
- Ensure "Background App Refresh" is enabled

### Debugging
- Enable USB debugging and check Android Studio logcat
- Look for logs with tag `OTPForegroundService`
- Check notification panel for service status

## 📊 Architecture

### Components
- **MainAppActivity** - Main UI and configuration
- **OTPForegroundService** - Persistent background service
- **SmsReceiver** - Intercepts incoming SMS
- **BootReceiver** - Handles device restart
- **OTPDatabase** - Local message queue (Room database)

### Data Flow
1. SMS received → SmsReceiver
2. OTP extracted → Queued in local database
3. Network available → Send to server
4. Success → Mark as sent | Failure → Retry (max 5 times)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is for personal/educational use. Ensure compliance with local laws regarding SMS interception and data forwarding.

## 🔧 Support

For issues and questions:
1. Check existing [Issues](../../issues)
2. Create a new issue with detailed description
3. Include device model, Android version, and logs if possible

---

**Note**: This app requires careful permission management and may need manufacturer-specific optimizations for some devices (Samsung, Xiaomi, Huawei, etc.) to ensure reliable background operation.