# Receipt Designer Admin-Controlled Settings Implementation Guide

## Overview
This document describes the implementation of role-based settings management in the ToyCity app. Admin users can edit global receipt settings (business name, address, phone, NTN, thank you note), while all other users can only view these settings and edit their own sales person name.

## Requirements Met

✅ **Admin-Only Global Settings**: Only the user with email `wajahatabbasicentral@gmail.com` can edit:
   - Business Name
   - Address
   - Phone Number
   - NTN No
   - Thank You Note

✅ **User-Specific Sales Person Name**: All users can set their own sales person name, which is tied to their account and syncs across devices.

✅ **Cloud Sync**: All settings are synchronized via Firebase Firestore with local fallback support.

✅ **Real-time Updates**: Settings use Kotlin Flows for reactive updates when global settings are changed by admin.

## Architecture

### Cloud Firestore Structure

```
Firestore Database:
├── settings/
│   ├── global/ (Document)
│   │   ├── name: String
│   │   ├── address: String
│   │   ├── phone: String
│   │   ├── note: String
│   │   └── ntnNo: String
│   │
│   └── users/ (Collection)
│       └── {userId}/ (Document - userSettings subcollection)
│           ├── salesPerson: String
│           ├── printerAddress: String
│           └── pageSize: String
```

### Data Flow

```
User Interface (ReceiptSettingsScreen)
        ↓
SecurityManager (Cloud Functions)
        ↓
Firebase Firestore (Cloud Database)
        ↓
(Flows back to UI in real-time)
```

## Implementation Details

### 1. SecurityManager.kt Changes

#### New Admin Check Function
```kotlin
fun isAdminUser(email: String?): Boolean {
    return email == "wajahatabbasicentral@gmail.com"
}
```
This function centralizes admin verification, making it easy to change admin emails in the future.

#### Cloud Save Functions
```kotlin
// Admin-only: Save global settings
suspend fun saveGlobalReceiptSettings(
    name: String,
    address: String,
    phone: String,
    note: String,
    ntnNo: String
)

// All users: Save their sales person name
suspend fun saveUserReceiptSettings(userId: String, salesPerson: String)
```

#### Real-time Settings Flow
```kotlin
fun getReceiptSettingsFlow(context: Context, userId: String?): Flow<Map<String, String>>
```
This function:
1. Emits local settings first (offline support)
2. Fetches global settings from Firestore
3. Fetches user-specific settings from Firestore
4. Merges all settings with proper priority: Cloud > Local
5. Returns Flow for reactive UI updates
6. Gracefully falls back to local if cloud fails

### 2. MainDashboard.kt Changes

#### Admin Detection
```kotlin
val isAdmin = SecurityManager.isAdminUser(user?.email)
```

#### UI Field Control
- **Admin-editable fields**: Business Name, Address, Phone, NTN, Thank You Note
  - `enabled = isAdmin` parameter prevents non-admin users from editing
  - TextField shows disabled state with muted colors

- **Everyone editable**: Sales Person Name
  - `enabled = true` allows all users to edit
  - Always editable regardless of admin status

#### User Feedback
- Non-admin users see an info banner explaining they can only edit their sales person name
- Different save button text based on user role:
  - Admin: "Save Configuration"
  - Non-Admin: "Save Sales Person Name"

#### Save Logic
```kotlin
if (isAdmin) {
    // Save global settings to cloud
    SecurityManager.saveGlobalReceiptSettings(...)
}

// All users save their sales person name
user?.uid?.let { uid ->
    SecurityManager.saveUserReceiptSettings(uid, salesPerson)
}
```

## User Experience

### For Admin (wajahatabbasicentral@gmail.com)
1. Opens Receipt Settings screen
2. Sees all fields editable
3. Can change any global setting (business info, thank you note, NTN)
4. Can set their own sales person name
5. Changes sync to all users automatically
6. Receives "Global receipt settings saved successfully" message

### For Regular Users
1. Opens Receipt Settings screen
2. Sees information banner explaining the restriction
3. Can see (but not edit) global settings from admin:
   - Business Name, Address, Phone, NTN, Thank You Note
4. Can only edit their own Sales Person Name field
5. Can save their sales person name
6. Their sales person name is tied to their account
7. Receives "Your sales person name saved successfully" message
8. When they log in on another device with same account, their sales person name is automatically restored

## Firestore Security Rules (Recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Global settings - only admin can write
    match /settings/global {
      allow read: if request.auth != null;
      allow write: if request.auth.token.email == "wajahatabbasicentral@gmail.com";
    }
    
    // User-specific settings - users can only read/write their own
    match /settings/users/{userId}/{document=**} {
      allow read, write: if request.auth.uid == userId;
    }
  }
}
```

## Key Features

### 1. **Offline Support**
- Local SharedPreferences as fallback
- Users can still view cached settings if internet is unavailable
- When connection restored, settings sync automatically

### 2. **Real-time Sync**
- When admin updates global settings, all users see changes immediately (via Flow)
- Each user's sales person name is unique to their account
- Multi-device sync: Login on any device, your sales person name appears

### 3. **Admin Control**
- Single source of truth for business information
- Prevents unauthorized changes to company details
- Clear audit trail in Firestore

### 4. **User Control**
- Users maintain control over their own profile (sales person name)
- Can change their name anytime
- Name follows them across devices

## Testing Checklist

- [ ] Admin can edit all global fields
- [ ] Non-admin users see disabled global fields
- [ ] Non-admin users see the info banner
- [ ] All users can edit Sales Person Name
- [ ] Admin changes to global settings appear for all users (real-time)
- [ ] User's sales person name persists when logging in on another device
- [ ] Sales person name defaults to empty for new users
- [ ] Offline fallback works (cached settings show when no internet)
- [ ] Error messages appear if cloud sync fails
- [ ] Settings save button text changes based on user role

## Future Enhancements

1. **Multi-Admin Support**: Update `isAdminUser()` to check a list of admin emails from Firestore
2. **Audit Logging**: Track who changed what settings and when
3. **Settings Versioning**: Keep history of settings changes
4. **Batch Updates**: Allow admin to bulk edit settings for multiple users
5. **Permission Management**: Add more granular permissions (view-only, edit-only, etc.)

## Troubleshooting

**Issue**: Settings not syncing
- Check Firebase Firestore connection
- Verify Firestore rules allow read/write
- Check user is authenticated

**Issue**: Admin email not recognized
- Verify exact email in SecurityManager.isAdminUser()
- Case sensitivity matters
- Check Firebase Auth shows correct email

**Issue**: Sales person name not persisting
- Verify user is logged in
- Check Firestore shows document in `settings/users/{userId}`
- Check internet connection for cloud sync

**Issue**: Non-admin users can edit fields
- Check ReceiptTextField `enabled` parameter is properly set
- Verify `isAdmin` value is correctly determined
- Clear app cache and relaunch

## Files Modified

1. **SecurityManager.kt**
   - Added `isAdminUser(email: String?)` function
   - Added `saveGlobalReceiptSettings()` suspend function
   - Added `saveUserReceiptSettings()` suspend function
   - Updated `getReceiptSettingsFlow()` to fetch from Firestore
   - Deprecated old `saveReceiptSettings()` function (kept for backward compatibility)

2. **MainDashboard.kt**
   - Updated `ReceiptSettingsScreen()` composable
   - Replaced hardcoded admin email list with `SecurityManager.isAdminUser()`
   - Added info banner for non-admin users
   - Updated save logic to handle admin vs non-admin differently
   - Modified button text to reflect user role
   - Updated `DashboardScreen()` to use centralized admin check

## Dependencies

- Firebase Firestore (already installed)
- Kotlin Coroutines (already installed)
- Compose Material3 (already installed)

No new dependencies required.

