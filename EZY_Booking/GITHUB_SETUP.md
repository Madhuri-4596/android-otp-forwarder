# 🚀 GitHub Automated APK Building Setup

Follow these steps to get your Android APK automatically built and available for download:

## Step 1: Create GitHub Repository

1. Go to [GitHub.com](https://github.com) and sign in
2. Click **"+"** → **"New repository"**
3. Repository name: `android-otp-forwarder` (or any name you prefer)
4. Make it **Public** (required for free GitHub Actions)
5. ✅ Check **"Add a README file"**
6. Click **"Create repository"**

## Step 2: Upload Your Code

### Option A: Using GitHub Web Interface (Easiest)

1. In your new repository, click **"uploading an existing file"**
2. Drag and drop ALL files from your project folder, OR
3. Click **"choose your files"** and select all project files
4. **Important**: Make sure to upload the `.github` folder with the workflow file
5. Add commit message: `Initial commit - Android OTP Forwarder v2.0`
6. Click **"Commit changes"**

### Option B: Using Git Commands

```bash
# In your project folder
git init
git add .
git commit -m "Initial commit - Android OTP Forwarder v2.0"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/android-otp-forwarder.git
git push -u origin main
```

## Step 3: Watch the Magic Happen ✨

1. Go to your repository → **Actions** tab
2. You'll see "Build Android APK" workflow running
3. Wait 3-5 minutes for build to complete
4. Once complete, you'll see:
   - ✅ Green checkmark = Success
   - ❌ Red X = Failed (check logs)

## Step 4: Download Your APK

### From Actions (Every Build)
1. Go to **Actions** tab
2. Click on the latest completed workflow
3. Scroll down to **Artifacts** section
4. Download:
   - `app-debug` - For testing
   - `app-release` - For production

### From Releases (Automatic)
1. Go to **Releases** section (right side of repository)
2. Click on latest release (auto-created)
3. Download APK files from **Assets**

## Step 5: Install & Test

1. **Download** either APK file
2. **Transfer to your Android device**
3. **Enable unknown sources** if prompted
4. **Install the APK**
5. **Grant all permissions**
6. **Configure your settings**
7. **Test with a real OTP SMS**

## 📱 What You Get

### Automatic Features
- ✅ **Build on every code change**
- ✅ **Automatic version numbering** (v1, v2, v3...)
- ✅ **Release notes** with changelog
- ✅ **Both debug and release APKs**
- ✅ **Download links** always available

### APK Features
- ✅ **Persistent background service**
- ✅ **Offline message queuing**
- ✅ **Android 5-14+ compatibility**
- ✅ **Auto-restart after reboot**
- ✅ **Battery optimization handling**

## 🔧 Customization

### Change App Name/Package
Edit `app/src/main/AndroidManifest.xml`:
```xml
<application android:label="Your App Name">
```

### Change Server URL
Edit `OTPForegroundService.java` line 28:
```java
private final String userOTPPostAPI = "https://your-server.com/api/otp";
```

### Update Version
Edit `app/build.gradle`:
```gradle
versionCode 3
versionName "3.0"
```

## 🆘 Troubleshooting

### Build Failed?
1. Check **Actions** → Click failed build → Read error logs
2. Common issues:
   - Missing files (ensure `.github/workflows/build-apk.yml` uploaded)
   - Syntax errors in code
   - Missing dependencies

### APK Won't Install?
1. Uninstall old version first
2. Enable "Install from unknown sources"
3. Check Android version compatibility
4. Clear package installer cache

### Need Updates?
1. Make changes to your code
2. Upload changed files to GitHub
3. New APK automatically builds
4. Download from Actions or Releases

## 🎉 Success!

Once set up, you'll have:
- **Automatic APK builds** on every code change
- **Always-available download links**
- **Version tracking** and release notes
- **Professional distribution** system

Your APK download links will look like:
- `https://github.com/YOUR_USERNAME/android-otp-forwarder/releases/latest`
- Direct APK: `https://github.com/YOUR_USERNAME/android-otp-forwarder/releases/download/v1/app-release.apk`

Perfect for sharing with team members or deploying to multiple devices!

---

**Next Steps**: After uploading, check the Actions tab in ~5 minutes for your first automatically-built APK! 🚀