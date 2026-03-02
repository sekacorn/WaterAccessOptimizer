# Single Sign-On (SSO) Setup Guide

**Complete guide for configuring enterprise SSO authentication in WaterAccessOptimizer**

## Table of Contents

1. [Overview](#overview)
2. [Supported Providers](#supported-providers)
3. [Prerequisites](#prerequisites)
4. [Configuration Steps](#configuration-steps)
5. [Provider-Specific Guides](#provider-specific-guides)
6. [Testing SSO](#testing-sso)
7. [Troubleshooting](#troubleshooting)
8. [Security Best Practices](#security-best-practices)

---

## Overview

WaterAccessOptimizer supports Single Sign-On (SSO) for enterprise accounts, allowing organizations to:

- **Centralize authentication** through existing Identity Providers (IdP)
- **Eliminate password management** for enterprise users
- **Auto-provision users** on first SSO login
- **Enforce corporate IT policies** and MFA requirements
- **Simplify onboarding/offboarding** processes
- **Maintain compliance** with security audit logs

### Supported Protocols

- **SAML 2.0** - Industry standard for enterprise SSO
- **OAuth 2.0 / OpenID Connect (OIDC)** - Modern authentication protocol

---

## Supported Providers

WaterAccessOptimizer has pre-configured templates for:

| Provider | Protocol | Common Use Case |
|----------|----------|-----------------|
| **Okta** | SAML 2.0 | Enterprise SSO platform |
| **Azure Active Directory** | SAML 2.0 | Microsoft 365 organizations |
| **Google Workspace** | SAML 2.0 | Google Workspace organizations |
| **OneLogin** | SAML 2.0 | Cloud identity management |
| **Auth0** | OIDC | Developer-friendly auth platform |
| **Keycloak** | OIDC | Open-source identity solution |
| **Custom** | SAML 2.0 / OIDC | Any SAML/OIDC compliant provider |

---

## Prerequisites

### For ENTERPRISE_ADMIN

1. **Enterprise Account** - Active WaterAccessOptimizer enterprise subscription
2. **Role** - ENTERPRISE_ADMIN or SUPER_ADMIN role
3. **Access to IdP** - Administrative access to configure SAML/OIDC apps

### For Identity Provider Setup

- **Redirect/Callback URL**: `https://your-domain.com/api/auth/sso/callback`
- **Entity ID/Audience**: `https://your-domain.com/api/auth/sso/metadata/{enterpriseId}`
- **Email domain control** - Verified ownership of email domains

---

## Configuration Steps

### Step 1: Access SSO Settings

1. Log in as **ENTERPRISE_ADMIN**
2. Navigate to **Settings** → **SSO Configuration**
3. URL: `https://your-domain.com/settings/sso`

### Step 2: Choose SSO Provider

1. Select your Identity Provider from the dropdown
2. Choose protocol (SAML 2.0 or OIDC)

### Step 3: Configure Provider Settings

#### For SAML 2.0:

```yaml
IdP Entity ID: http://www.okta.com/exk1234567890
IdP SSO URL: https://dev-123456.okta.com/app/wateraccess/sso/saml
IdP Certificate: -----BEGIN CERTIFICATE-----
                  MIIDpDCCAoygAwIBAgIGAXoq...
                  -----END CERTIFICATE-----
Single Logout URL: https://dev-123456.okta.com/app/wateraccess/slo/saml (optional)
```

#### For OAuth 2.0 / OIDC:

```yaml
Issuer URL: https://dev-123456.okta.com
Authorization Endpoint: https://dev-123456.okta.com/oauth2/v1/authorize
Client ID: 0oa1234567890abcdef
Client Secret: ••••••••••••••••••••••
Scopes: openid, email, profile
```

### Step 4: Configure Domain & User Settings

```yaml
Allowed Email Domains:
  - company.com
  - subsidiary.com

Force SSO: [✓] (disables password login for domain users)
Auto-provision Users: [✓] (creates accounts on first SSO login)
Default Role: USER
JIT Provisioning: [✓] (updates user data on each login)
Session Timeout: 480 minutes (8 hours)
```

### Step 5: Save Configuration

1. Click **"Save Configuration"**
2. Review success message
3. Click **"Test Configuration"** to verify setup

### Step 6: Configure Identity Provider

1. Download SAML metadata: `GET /api/auth/sso/metadata/{enterpriseId}`
2. Or manually configure:
   - **ACS URL**: `https://your-domain.com/api/auth/sso/callback`
   - **Entity ID**: `https://your-domain.com/api/auth/sso/metadata/{enterpriseId}`
   - **Name ID Format**: `urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress`

---

## Provider-Specific Guides

### Okta (SAML 2.0)

**1. Create SAML Application in Okta**

1. Log in to Okta Admin Console
2. Go to **Applications** → **Applications** → **Create App Integration**
3. Select **SAML 2.0**
4. **General Settings**:
   - App name: `WaterAccessOptimizer`
   - App logo: (optional)

**2. Configure SAML Settings**

- **Single sign on URL**: `https://your-domain.com/api/auth/sso/callback`
- **Audience URI (SP Entity ID)**: `https://your-domain.com/api/auth/sso/metadata/{enterpriseId}`
- **Name ID format**: `EmailAddress`
- **Application username**: `Email`

**3. Attribute Statements** (optional):

| Name | Value |
|------|-------|
| email | user.email |
| firstName | user.firstName |
| lastName | user.lastName |

**4. Assign Users/Groups**

1. Go to **Assignments** tab
2. Assign users or groups who should have access

**5. Copy Configuration to WaterAccessOptimizer**

1. Go to **Sign On** tab → **View SAML setup instructions**
2. Copy:
   - **Identity Provider Single Sign-On URL** → IdP SSO URL
   - **Identity Provider Issuer** → IdP Entity ID
   - **X.509 Certificate** → IdP Certificate

---

### Azure Active Directory (SAML 2.0)

**1. Register Application in Azure AD**

1. Log in to Azure Portal
2. Go to **Azure Active Directory** → **Enterprise Applications**
3. Click **New application** → **Create your own application**
4. Name: `WaterAccessOptimizer`, select **Integrate any other application**

**2. Configure Single Sign-On**

1. Go to **Single sign-on** → Select **SAML**
2. **Basic SAML Configuration**:
   - **Identifier (Entity ID)**: `https://your-domain.com/api/auth/sso/metadata/{enterpriseId}`
   - **Reply URL (ACS URL)**: `https://your-domain.com/api/auth/sso/callback`
   - **Sign on URL**: `https://your-domain.com`

**3. Attributes & Claims**

- **Unique User Identifier**: `user.mail`
- Additional claims:
  - `email`: user.mail
  - `firstName`: user.givenname
  - `lastName`: user.surname

**4. Copy SAML Certificates**

1. Download **Certificate (Base64)**
2. Copy **Login URL** → IdP SSO URL
3. Copy **Azure AD Identifier** → IdP Entity ID

**5. Assign Users**

1. Go to **Users and groups** → **Add user/group**

---

### Google Workspace (SAML 2.0)

**1. Add SAML App**

1. Log in to Google Admin Console
2. Go to **Apps** → **Web and mobile apps** → **Add App** → **Add custom SAML app**
3. App name: `WaterAccessOptimizer`

**2. Google IdP Information**

1. Copy:
   - **SSO URL** → IdP SSO URL
   - **Entity ID** → IdP Entity ID
   - **Certificate** → IdP Certificate

**3. Service Provider Details**

- **ACS URL**: `https://your-domain.com/api/auth/sso/callback`
- **Entity ID**: `https://your-domain.com/api/auth/sso/metadata/{enterpriseId}`
- **Name ID format**: `EMAIL`
- **Name ID**: `Basic Information > Primary email`

**4. Attribute Mapping**

| Google Directory attributes | App attributes |
|----------------------------|----------------|
| Primary email | email |
| First name | firstName |
| Last name | lastName |

**5. User Access**

1. Select organizational units or groups
2. Turn ON for selected users

---

### Auth0 (OIDC)

**1. Create Application**

1. Log in to Auth0 Dashboard
2. Go to **Applications** → **Create Application**
3. Name: `WaterAccessOptimizer`, Type: **Regular Web Application**

**2. Settings**

- **Allowed Callback URLs**: `https://your-domain.com/api/auth/sso/callback`
- **Allowed Logout URLs**: `https://your-domain.com/`
- **Allowed Web Origins**: `https://your-domain.com`

**3. Copy Credentials**

- **Domain** → Issuer URL: `https://YOUR-TENANT.auth0.com`
- **Client ID** → Client ID
- **Client Secret** → Client Secret

**4. Configure in WaterAccessOptimizer**

- Authorization Endpoint: `https://YOUR-TENANT.auth0.com/authorize`
- Scopes: `openid email profile`

---

## Testing SSO

### Test Configuration

1. Go to SSO Settings page
2. Click **"Test Configuration"**
3. Review test results:
   - ✓ IdP connectivity: OK
   - ✓ IdP certificate: OK
   - ✓ OAuth credentials: OK

### Test Login Flow

1. **Logout** from WaterAccessOptimizer
2. Go to login page
3. Enter email with SSO-enabled domain (e.g., `user@company.com`)
4. Click **"Continue with SSO"** button
5. Redirected to Identity Provider login page
6. Enter IdP credentials
7. Redirected back to WaterAccessOptimizer
8. Automatically logged in

### Expected Behavior

**First-time SSO user (Auto-provision enabled)**:
- User account created automatically
- Default role assigned
- Attributes mapped from IdP
- Redirected to dashboard

**Existing user**:
- User authenticated
- Attributes updated (if JIT provisioning enabled)
- Redirected to dashboard

---

## Troubleshooting

### Common Issues

#### 1. "SSO not configured for domain"

**Cause**: Email domain not in allowed domains list

**Solution**:
- Go to SSO Settings
- Add domain to "Allowed Email Domains"
- Save configuration

#### 2. "SAML assertion signature invalid"

**Cause**: Incorrect IdP certificate

**Solution**:
- Download correct certificate from IdP
- Ensure it's in PEM format (-----BEGIN CERTIFICATE-----)
- Paste entire certificate including headers
- Save configuration

#### 3. "User not found and auto-provisioning disabled"

**Cause**: User doesn't exist and auto-provision is off

**Solution**:
- Enable "Auto-provision users" in SSO settings
- OR manually create user account first

#### 4. "Invalid redirect URI"

**Cause**: Callback URL mismatch

**Solution**:
- Ensure IdP callback URL is: `https://your-domain.com/api/auth/sso/callback`
- Check for http vs https mismatch
- Verify no trailing slashes

#### 5. "OAuth token exchange failed"

**Cause**: Invalid client credentials

**Solution**:
- Verify Client ID and Client Secret
- Check for extra spaces or line breaks
- Regenerate credentials if needed

### Debug Mode

**View SSO Logs**:
1. Go to SSO Settings
2. Scroll to "SSO Authentication Logs"
3. Review recent SSO attempts
4. Check for error messages

**API Endpoint**: `GET /api/auth/sso/logs?page=0&size=50`

---

## Security Best Practices

### 1. Certificate Management

- **Rotate certificates** before expiration
- **Store certificates securely** (encrypted in database)
- **Monitor expiration dates**
- **Update WaterAccessOptimizer** when IdP certificate changes

### 2. Domain Verification

- **Verify domain ownership** before adding to allowed domains
- **Remove unused domains** promptly
- **Audit domain list** regularly

### 3. User Provisioning

- **Review default role** assignment (avoid ADMIN for new users)
- **Enable JIT provisioning** to keep user data current
- **Audit auto-provisioned accounts** regularly

### 4. Session Management

- **Set appropriate timeout** (8 hours default)
- **Enable Single Logout** (SLO) if supported
- **Monitor active SSO sessions**

### 5. Monitoring & Auditing

- **Review SSO auth logs** weekly
- **Set up alerts** for failed SSO attempts
- **Audit SSO configuration changes**
- **Monitor IdP service health**

### 6. Multi-Factor Authentication

- **Enforce MFA at IdP level** (not in WaterAccessOptimizer)
- **Require MFA** for all enterprise users
- **Use IdP's MFA capabilities** (stronger than app-level MFA)

### 7. Force SSO

- **Enable "Force SSO"** to prevent password-based attacks
- **Disable local passwords** for SSO users
- **Maintain emergency admin account** (non-SSO SUPER_ADMIN)

---

## API Reference

### Configure SSO

```bash
POST /api/auth/sso/configure
Authorization: Bearer <ENTERPRISE_ADMIN_TOKEN>
Content-Type: application/json

{
  "provider": "okta",
  "protocol": "SAML2",
  "idpEntityId": "http://www.okta.com/exk...",
  "idpSsoUrl": "https://dev-123456.okta.com/app/.../sso/saml",
  "idpCertificate": "-----BEGIN CERTIFICATE-----...",
  "allowedDomains": ["company.com"],
  "forceSso": true,
  "autoProvision": true,
  "defaultRole": "USER"
}
```

### Check Domain for SSO

```bash
POST /api/auth/sso/check-domain?email=user@company.com

Response:
{
  "ssoEnabled": true,
  "provider": "okta",
  "requireSso": true
}
```

### Initiate SSO Login

```bash
GET /api/auth/sso/login/okta?email=user@company.com

Response:
{
  "redirectUrl": "https://dev-123456.okta.com/app/.../sso/saml",
  "provider": "okta"
}
```

### Get SAML Metadata

```bash
GET /api/auth/sso/metadata/{enterpriseId}

Response: (XML)
<?xml version="1.0" encoding="UTF-8"?>
<md:EntityDescriptor ...>
  ...
</md:EntityDescriptor>
```

---

## Support

### Need Help?

- **Documentation**: https://docs.wateraccessoptimizer.org/sso
- **Email Support**: support@wateraccessoptimizer.org
- **Enterprise Support**: Available for PROFESSIONAL and ENTERPRISE tiers

### Feature Requests

SSO-related feature requests:
- GitHub Issues: https://github.com/wateraccess/optimizer/issues
- Label with `enhancement` and `sso`

---

**Last Updated**: October 2024
**Version**: 1.0.0

© 2024 WaterAccessOptimizer - Secure Enterprise Authentication
