# WaterAccessOptimizer - User Roles & Enterprise Guide

## Overview

WaterAccessOptimizer now includes a comprehensive user management system with multiple roles, enterprise support, and advanced moderation capabilities. This guide explains how to use these features.

## User Roles

### 1. USER (Default)
**Who**: Regular users, researchers, community members
**What they can do**:
- Register and login
- Upload water data (CSV, JSON, GeoJSON)
- Create 3D visualizations
- Get AI predictions
- Join collaboration sessions
- View own data and community data
- Export visualizations

**How to get this role**: Automatic upon registration

---

### 2. MODERATOR
**Who**: Content moderators, community managers
**What they can do**:
- Everything a USER can do, plus:
- Review user reports
- Warn, suspend, or ban problematic users
- Moderate uploaded content
- Resolve community disputes
- Access moderation dashboard at `/moderator`

**How to get this role**: Assigned by ADMIN

**Key Features**:
- **Reports Dashboard**: View and manage user-submitted reports
- **Moderation Actions**: Warn, suspend (temporary), or ban (permanent) users
- **Activity Logs**: Track all moderation actions
- **Guidelines Enforcement**: Ensure data integrity and respectful communication

---

### 3. ADMIN
**Who**: System administrators
**What they can do**:
- Everything a MODERATOR can do, plus:
- Create, update, and delete users
- Assign USER and MODERATOR roles
- View all system statistics
- Manage permissions
- View enterprise information
- Access admin dashboard at `/admin`

**How to get this role**: Assigned by SUPER_ADMIN

**Key Features**:
- **User Management**: Full CRUD operations on users
- **Role Assignment**: Promote users to MODERATOR
- **System Analytics**: View usage statistics, active users, data uploads
- **Audit Logs**: Review all system activities

---

### 4. ENTERPRISE_ADMIN
**Who**: Organization administrators for companies, NGOs, government agencies
**What they can do**:
- Everything a USER can do, plus:
- Manage enterprise account settings
- Invite users to enterprise
- View enterprise usage statistics
- Configure enterprise features
- Manage team members
- View billing information

**How to get this role**: Assigned when enterprise account is created

**Key Features**:
- **Team Management**: Invite and remove team members
- **Usage Analytics**: Track data usage, storage, API calls
- **Subscription Management**: View and upgrade subscription tier
- **Billing**: Access invoices and payment history

---

### 5. SUPER_ADMIN
**Who**: System owner, lead developers
**What they can do**:
- **EVERYTHING** - complete system access
- Create ADMIN and SUPER_ADMIN accounts
- Manage all enterprises
- Access complete audit logs
- System configuration
- Database access
- Critical operations

**How to get this role**: Pre-configured (default: username `admin`)

---

## Enterprise Features

### What is an Enterprise Account?

Enterprise accounts allow organizations to:
- Manage multiple users under one account
- Share data within the organization
- Get dedicated support
- Access advanced features
- Track team usage

### Subscription Tiers

| Tier | Max Users | Storage | Features | Price |
|------|-----------|---------|----------|-------|
| **FREE** | 10 | 5 GB | Basic features | Free |
| **BASIC** | 50 | 50 GB | Priority support | Contact |
| **PROFESSIONAL** | 200 | 200 GB | API access, analytics | Contact |
| **ENTERPRISE** | Unlimited | Custom | Custom features, SLA | Contact |

### Creating an Enterprise

**Option 1: Self-Service (for FREE tier)**
1. Register as a regular user
2. Navigate to Settings → Enterprise
3. Click "Create Enterprise Account"
4. Fill in organization details
5. You become ENTERPRISE_ADMIN automatically

**Option 2: Contact Sales (for paid tiers)**
1. Email: enterprise@wateraccess.org
2. Provide organization details
3. Sales team will set up your account
4. Receive ENTERPRISE_ADMIN credentials

### Managing Enterprise Users

As ENTERPRISE_ADMIN:
1. Navigate to `/admin` (enterprise view)
2. Click "Invite Users"
3. Enter email addresses
4. Users receive invitation email
5. They register and automatically join your enterprise

---

## Common Workflows

### For Regular Users
```
1. Register → Login → Upload Data → Analyze → Get Predictions → Export
```

### For Moderators
```
1. Login → Check Reports Dashboard → Review Report → Take Action (Warn/Suspend/Ban) → Log Resolution
```

### For Admins
```
1. Login → Admin Dashboard → Manage Users → Assign Roles → Monitor System Stats
```

### For Enterprise Admins
```
1. Login → Enterprise Dashboard → Invite Team → Monitor Usage → Manage Subscription
```

---

## Security Best Practices

### Password Requirements
- Minimum 8 characters
- Include uppercase, lowercase, numbers
- Avoid common passwords
- Change regularly

### Account Security
- Enable two-factor authentication (if available)
- Never share credentials
- Log out on shared computers
- Review audit logs regularly (ADMIN/SUPER_ADMIN)

### Data Privacy
- Only upload data you have permission to share
- Respect user privacy in collaboration sessions
- Follow organizational data policies
- Report security issues immediately

---

## Moderation Guidelines

### When to Warn
- First-time minor policy violation
- Unintentional data quality issues
- Minor communication issues

### When to Suspend
- Repeated policy violations
- Spam or promotional content
- Harassment or abusive behavior
- Uploading fake/malicious data

### When to Ban
- Severe policy violations
- Malicious intent to harm users/system
- Repeated suspensions without improvement
- Legal violations

### Resolution Process
1. Review report thoroughly
2. Check user history
3. Consider context
4. Choose appropriate action
5. Document reasoning
6. Notify user
7. Monitor follow-up

---

## Audit Logging

All actions are logged for compliance and security:

**Logged Events**:
- User login/logout
- Data uploads/deletions
- Role changes
- Moderation actions
- Enterprise changes
- System configuration

**Who can view logs**:
- ADMIN: User-level logs
- SUPER_ADMIN: All logs

**Retention**: 90 days (configurable)

---

## API Access

### Authentication
All API requests require JWT token:
```bash
Authorization: Bearer <your-token>
```

### Getting a Token
```bash
POST /api/auth/login
{
  "username": "your_username",
  "password": "your_password"
}
```

Returns:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": { ... }
}
```

### Using the Token
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/integrator/hydro
```

---

## Troubleshooting

### Can't Login
- Check username/password
- Account may be suspended (contact moderator)
- Email may not be verified

### Missing Permissions
- Check your role in profile
- Contact admin for role upgrade
- Enterprise users: ensure you're added to enterprise

### Enterprise Issues
- Verify subscription is active
- Check user limit not exceeded
- Contact support: enterprise@wateraccess.org

---

## Support

**For Users**: help@wateraccess.org
**For Moderators**: moderators@wateraccess.org
**For Admins**: admin@wateraccess.org
**For Enterprises**: enterprise@wateraccess.org

**Documentation**: https://docs.wateraccess.org
**Community**: https://community.wateraccess.org

---

**Built for improving water access worldwide** 🌍💧
