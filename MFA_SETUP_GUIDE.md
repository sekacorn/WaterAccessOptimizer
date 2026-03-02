# Multi-Factor Authentication (MFA) Setup Guide

## Overview

Multi-Factor Authentication (MFA) adds an extra layer of security to your WaterAccessOptimizer account by requiring two forms of verification:
1. **Something you know**: Your password
2. **Something you have**: Your smartphone with an authenticator app

## Why Enable MFA?

[X]**Enhanced Security**: Protects your account even if your password is compromised
[X]**Required for Enterprise**: Mandatory for all enterprise user accounts
[X]**Compliance**: Meets data protection and security compliance requirements
[X]**Data Protection**: Secures sensitive water data and community information
[X]**Role Requirement**: Required for ADMIN, SUPER_ADMIN, and ENTERPRISE_ADMIN roles

## Supported MFA Methods

### 1. TOTP (Time-based One-Time Password) - **Recommended**
- Works with: Google Authenticator, Authy, Microsoft Authenticator, 1Password
- No internet required after setup
- Most secure and reliable

### 2. Backup Codes
- Single-use codes for emergency access
- Store securely offline
- 10 codes generated

### 3. Trusted Devices (Optional)
- Skip MFA on trusted devices for 30 days
- Useful for personal devices
- Can be revoked anytime

## Setup Instructions

### Step 1: Install an Authenticator App

Choose one of these apps for your smartphone:

**iOS:**
- Google Authenticator (Free)
- Authy (Free)
- Microsoft Authenticator (Free)
- 1Password (Paid)

**Android:**
- Google Authenticator (Free)
- Authy (Free)
- Microsoft Authenticator (Free)
- andOTP (Free, Open Source)

### Step 2: Enable MFA in WaterAccessOptimizer

1. **Login** to your account
2. Navigate to **Settings** → **Security** → **Two-Factor Authentication**
   - Direct link: http://localhost:3000/settings/mfa
3. Click **"Enable 2FA"**

### Step 3: Scan QR Code

1. A QR code will appear on screen
2. Open your authenticator app
3. Tap **"Add Account"** or **"+"**
4. Choose **"Scan QR Code"**
5. Point camera at the QR code

**Alternative - Manual Entry:**
If scanning doesn't work:
1. Choose **"Enter Setup Key"** in your app
2. Enter these details:
   - **Account**: Your email/username
   - **Key**: The secret key shown below the QR code
   - **Type**: Time-based
   - **Algorithm**: SHA-1
   - **Digits**: 6
   - **Period**: 30 seconds

### Step 4: Verify Setup

1. Your authenticator app will display a 6-digit code
2. Enter this code in the verification field
3. Click **"Verify and Enable"**
4. If successful, MFA is now enabled!

### Step 5: Save Backup Codes

 **CRITICAL**: Save your backup codes immediately!

1. You'll see 10 backup codes (e.g., `1234-5678`)
2. **Download** or **Copy** them
3. Store in a secure location:
   - Password manager (recommended)
   - Encrypted file
   - Physical paper in a safe
4. **DO NOT** take screenshots or store in email

Each backup code can only be used once.

## Using MFA

### Login Process with MFA

1. **Enter username and password** (as usual)
2. **Enter 6-digit code** from your authenticator app
   - Code changes every 30 seconds
   - Valid for ±30 seconds window
3. **Optional**: Check "Trust this device" to skip MFA for 30 days
4. Click **"Verify"**

### Using a Backup Code

If you don't have access to your authenticator app:

1. Click **"Use backup code instead"** on MFA screen
2. Enter one of your saved backup codes
3. Click **"Verify"**
4. This code is now **used** and won't work again

### Lost Access?

**If you lost your phone and don't have backup codes:**

1. Contact support: security@wateraccess.org
2. Provide:
   - Your username/email
   - Proof of identity (government ID)
   - Account details (creation date, recent activity)
3. Support will verify and disable MFA
4. You'll receive a reset link via email
5. **Set up MFA again immediately**

## Managing MFA

### Access MFA Settings

Navigate to: **Profile** → **Security Settings** → **Two-Factor Authentication**

Or: http://localhost:3000/settings/mfa

### Regenerate Backup Codes

If you've used several backup codes or want fresh ones:

1. Go to MFA Settings
2. Scroll to **"Backup Codes"** section
3. Click **"Regenerate Codes"**
4.  Old codes will be **invalidated**
5. Save new codes securely

### Trusted Devices

**Add Trusted Device:**
- Check "Trust this device" during login
- Device won't require MFA for 30 days

**View Trusted Devices:**
1. Go to MFA Settings
2. Scroll to **"Trusted Devices"** section
3. See all devices with:
   - Device name (browser + OS)
   - Last used date
   - IP address

**Remove Trusted Device:**
- Click **"Remove"** next to any device
- That device will require MFA on next login

### Disable MFA

 **Not recommended unless necessary**

1. Go to MFA Settings
2. Click **"Disable 2FA"**
3. Enter your **current password** for confirmation
4. MFA will be disabled

**Note**: Admins can require MFA for certain roles or enterprise accounts. You may not be able to disable it in those cases.

## Enterprise MFA Policies

### Enterprise Admin Controls

Enterprise administrators can:

**Enforce MFA**:
- Require MFA for all enterprise users
- Set grace period for compliance
- View MFA adoption rates

**Monitor MFA Status**:
- See which users have MFA enabled
- Track MFA verification failures
- Audit MFA-related security events

**Support Users**:
- Reset MFA for locked-out users
- Generate emergency access codes
- View MFA activity logs

### User Compliance

**If your enterprise requires MFA:**

1. You'll see a banner: "MFA Required - 7 days remaining"
2. Setup MFA before deadline
3. After deadline:
   - Account access limited
   - Cannot upload data or create visualizations
   - Can only access profile to enable MFA

## Security Best Practices

### DO [X]

- **Enable MFA immediately** for important accounts
- **Use TOTP** (authenticator apps) over SMS
- **Save backup codes** in a password manager
- **Use unique passwords** for your account
- **Review trusted devices** monthly
- **Enable on multiple devices** (backup phone/tablet)
- **Keep apps updated** (authenticator apps)

### DON'T 

- **Don't share** backup codes with anyone
- **Don't screenshot** QR codes or backup codes
- **Don't store codes** in email or cloud notes
- **Don't use same device** for password manager and authenticator (ideally)
- **Don't ignore** failed MFA attempt notifications
- **Don't disable MFA** unless absolutely necessary

## Troubleshooting

### Code Not Working

**Symptoms**: "Invalid code" error

**Solutions**:
1. **Check time sync**:
   - Authenticator apps rely on accurate device time
   - Enable "Automatic date & time" on phone
   - Wait 30 seconds for new code
2. **Try multiple codes**:
   - Codes change every 30 seconds
   - Try 2-3 consecutive codes
3. **Use backup code**:
   - If repeatedly failing, use backup code
   - Then regenerate MFA setup

### Lost Phone

**Immediate Steps**:
1. Use **backup codes** to login
2. Go to MFA Settings
3. **Remove old authenticator** setup
4. **Setup MFA on new device**
5. **Generate new backup codes**

**No Backup Codes?**
- Contact support immediately
- Have identification ready
- Expect 24-48 hour verification process

### Wrong QR Code Scanned

**If you scanned wrong QR code:**
1. Delete the entry from authenticator app
2. Start setup process again
3. Scan the correct QR code

### Multiple Accounts

**Using same authenticator app for multiple WaterAccessOptimizer accounts:**

Each account gets a separate entry:
- Work Account: `WaterAccessOptimizer (work@example.com)`
- Personal: `WaterAccessOptimizer (personal@example.com)`

Make sure to use the code for the correct account!

## API Usage with MFA

### Obtaining API Tokens

When MFA is enabled, API token requests require MFA verification:

```bash
# Step 1: Login (returns temporary token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "pass"}'

# Returns: { "requiresMfa": true, "tempToken": "..." }

# Step 2: Verify MFA
curl -X POST http://localhost:8080/api/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{
    "tempToken": "...",
    "code": "123456"
  }'

# Returns: { "token": "full-access-token" }
```

### Using Trusted Devices for API

Generate a device-specific API key:

```bash
curl -X POST http://localhost:8080/api/auth/api-keys \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"name": "CI/CD Pipeline", "expiresIn": 90}'
```

Store securely and use in automated scripts.

## Compliance & Auditing

### Audit Logs

All MFA-related events are logged:

- MFA setup initiated
- MFA enabled
- MFA disabled
- MFA verification success/failure
- Backup codes regenerated
- Trusted devices added/removed

**Admins can view**: Dashboard → Audit Logs → Filter by "MFA"

### Compliance Reports

Enterprise admins can export:
- MFA adoption rates
- Users without MFA
- Failed MFA attempts
- MFA status by department

**Export**: Admin Dashboard → Reports → Security Report

## Support

### Documentation
- User Guide: https://docs.wateraccess.org/mfa
- API Docs: https://docs.wateraccess.org/api/mfa

### Contact
- General Support: support@wateraccess.org
- Security Issues: security@wateraccess.org
- Enterprise Support: enterprise@wateraccess.org

### Emergency MFA Reset
24/7 hotline: +1-XXX-XXX-XXXX

---

**Last Updated**: January 2024
**Version**: 1.0.0

**Remember**: MFA is one of the most effective ways to protect your account. Enable it today! 🔒
