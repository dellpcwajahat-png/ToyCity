# Quick Start Testing Guide

## Pre-Deployment Setup

### 1. Firebase Firestore Configuration

**Set up Firestore rules** in Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Global settings - only admin can write
    match /settings/global {
      allow read: if request.auth != null;
      allow write: if request.auth.token.email == "wajahatabbasicentral@gmail.com";
    }
    
    // User settings - users can only access their own
    match /settings/users/{userId}/{document=**} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Catch all - deny by default
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### 2. Test Accounts Setup

**Create test accounts in Firebase Auth:**
- Account 1: `wajahatabbasicentral@gmail.com` (ADMIN)
- Account 2: `user_test1@example.com` (USER)
- Account 3: `user_test2@example.com` (USER)

### 3. Initial Data (Optional)

**Create initial global settings document** in Firestore console:
```
Collection: settings
Document: global

Data:
{
  "name": "Toy City",
  "address": "123 Main Street",
  "phone": "+1-234-567-8900",
  "note": "Thank you for your business!",
  "ntnNo": "12345-6789-01"
}
```

---

## Test Cases

### Test 1: Admin Edits Global Settings ✅

**Preconditions:**
- Login as `wajahatabbasicentral@gmail.com` (admin)
- Open Receipt Designer in Settings

**Steps:**
1. Verify Business Name field is **enabled** (editable)
2. Verify Address field is **enabled**
3. Verify Phone field is **enabled**
4. Verify NTN field is **enabled**
5. Verify Thank You Note field is **enabled**
6. Verify Sales Person Name field is **enabled**
7. Change Business Name to "Toy City Premium"
8. Click "Save Configuration" button
9. Verify toast: "Global receipt settings saved successfully"

**Expected Result:**
- ✅ All fields are editable for admin
- ✅ Save button shows "Save Configuration"
- ✅ Changes saved to Firestore `settings/global`

---

### Test 2: Regular User Can Only Edit Sales Person Name ✅

**Preconditions:**
- Login as `user_test1@example.com` (non-admin)
- Open Receipt Designer in Settings

**Steps:**
1. Verify info banner appears with text about admin-controlled settings
2. Verify Business Name field is **disabled** (greyed out)
3. Verify Address field is **disabled**
4. Verify Phone field is **disabled**
5. Verify NTN field is **disabled**
6. Verify Thank You Note field is **disabled**
7. Verify Sales Person Name field is **enabled**
8. Enter "John Smith" in Sales Person Name
9. Click "Save Sales Person Name" button
10. Verify toast: "Your sales person name saved successfully"

**Expected Result:**
- ✅ Info banner shown
- ✅ Global fields disabled (cannot click, greyed out)
- ✅ Sales Person Name enabled
- ✅ Save button shows "Save Sales Person Name"
- ✅ Changes saved to Firestore `settings/users/{userId}`

---

### Test 3: Multi-User Sync ✅

**Preconditions:**
- Admin (`wajahatabbasicentral@gmail.com`) logged in on Device A
- User (`user_test1@example.com`) logged in on Device B

**Steps:**
1. On Device A (Admin):
   - Open Receipt Designer
   - Change Business Name to "Toy City Updated"
   - Save
2. On Device B (User):
   - Close and reopen Receipt Designer
   - Verify Business Name shows "Toy City Updated"
3. On Device A (Admin):
   - Change Address to "456 Oak Avenue"
   - Save
4. On Device B (User):
   - Close and reopen Receipt Designer
   - Verify Address shows "456 Oak Avenue"

**Expected Result:**
- ✅ Admin changes sync to all users
- ✅ Users see updates within seconds
- ✅ Global data is consistent across devices

---

### Test 4: User Sales Person Name Persistence ✅

**Preconditions:**
- Same device, user account: `user_test1@example.com`

**Steps:**
1. First Session:
   - Open Receipt Designer
   - Enter "Alice Johnson" in Sales Person Name
   - Save
   - Close app
2. Second Session:
   - Clear app cache (optional)
   - Relaunch app
   - Login as same user
   - Open Receipt Designer
   - Verify Sales Person Name shows "Alice Johnson"

**Expected Result:**
- ✅ Sales person name persists after app restart
- ✅ Data saved in Firestore under user's UID

---

### Test 5: Multi-Device User Sync ✅

**Preconditions:**
- Same user account on two different devices

**Steps:**
1. Device A:
   - Login as `user_test1@example.com`
   - Open Receipt Designer
   - Set Sales Person Name to "Alice"
   - Save and close app
2. Device B:
   - Login as `user_test1@example.com`
   - Open Receipt Designer
   - Verify Sales Person Name shows "Alice"
3. Device B:
   - Change Sales Person Name to "Bob"
   - Save
4. Device A:
   - Close app
   - Relaunch app
   - Open Receipt Designer
   - Verify Sales Person Name shows "Bob"

**Expected Result:**
- ✅ Sales Person Name syncs across devices
- ✅ Latest value is always used
- ✅ User's name follows them across devices

---

### Test 6: New User Default Values ✅

**Preconditions:**
- New user account created in Firebase Auth

**Steps:**
1. Login as new user
2. Open Receipt Designer
3. Verify Sales Person Name field is **empty** (not pre-filled)
4. Verify global fields show admin's values
5. Enter your name in Sales Person Name
6. Save

**Expected Result:**
- ✅ Sales Person Name defaults to empty string
- ✅ Global settings show current admin values
- ✅ New user data created in Firestore

---

### Test 7: Offline Fallback ✅

**Preconditions:**
- User has used app before (cached data exists)
- Device has internet initially, then goes offline

**Steps:**
1. With internet:
   - Open Receipt Designer
   - Note the values displayed
2. Turn off internet (airplane mode)
3. Open Receipt Designer again
   - Verify same values are shown
   - Verify fields work (can edit, scroll)
4. Try to save
   - Verify error message appears
5. Turn internet back on
6. Try to save again
   - Verify success message appears
   - Verify data synced to Firestore

**Expected Result:**
- ✅ Works offline with cached data
- ✅ Error shown on save attempt while offline
- ✅ Auto-syncs when connection restored

---

### Test 8: Admin Cannot Be Changed by Non-Admin ✅

**Preconditions:**
- Non-admin user session

**Steps:**
1. Logout and login as `user_test1@example.com`
2. Try to edit any of:
   - Business Name
   - Address
   - Phone
   - NTN
   - Thank You Note
3. Verify fields are disabled (grayed out, cannot click)
4. Verify no way to enable these fields through UI

**Expected Result:**
- ✅ Non-admin cannot edit global fields
- ✅ UI prevents interaction
- ✅ Only "Sales Person Name" is editable

---

### Test 9: Error Handling ✅

**Preconditions:**
- App running with Firestore connectivity issues simulated

**Steps:**
1. Simulate network error (e.g., using Android Studio network throttling)
2. Try to save settings
3. Verify error message appears: "Failed to save settings: [exception]"
4. Restore connection
5. Try again
6. Verify success

**Expected Result:**
- ✅ Error message shown with details
- ✅ App doesn't crash
- ✅ User can retry after connection restored

---

### Test 10: Concurrent Edits ✅

**Preconditions:**
- Admin and User logged in on different devices
- Both have Receipt Designer open

**Steps:**
1. Admin changes Business Name to "Version 1"
2. Admin saves
3. User is still on Receipt Designer screen
   - Verify Business Name updates to "Version 1"
4. User changes Sales Person Name to "Bob"
5. User saves
6. Admin is still on Receipt Designer screen
   - Verify still shows their own data (no conflict)

**Expected Result:**
- ✅ Admin changes sync to user's screen
- ✅ User's personal data doesn't affect admin
- ✅ No conflicts or data corruption

---

## Verification Checklist

### Code Quality
- [ ] No compilation errors
- [ ] SecurityManager.kt builds successfully
- [ ] MainDashboard.kt builds successfully
- [ ] No new warnings introduced

### Functionality
- [ ] Admin can edit all global fields
- [ ] Non-admin cannot edit global fields
- [ ] All users can edit sales person name
- [ ] Sales person name defaults to empty for new users
- [ ] Cloud sync works (Firestore updated)
- [ ] Settings sync across devices
- [ ] Offline fallback works

### UI/UX
- [ ] Info banner shows for non-admin users
- [ ] Disabled fields show visual feedback (greyed)
- [ ] Save button text changes based on role
- [ ] Toast messages are clear and helpful
- [ ] No UI freezing during cloud sync

### Data
- [ ] Global settings in `settings/global` document
- [ ] User settings in `settings/users/{uid}` documents
- [ ] Local cache in SharedPreferences
- [ ] No data corruption or loss

### Security
- [ ] Only admin email can save global settings
- [ ] Users can only access their own sales person name
- [ ] Firestore rules enforced correctly
- [ ] No sensitive data in logs

---

## Troubleshooting

### Settings not syncing
**Solution:**
- Check Firestore connection in Firebase Console
- Verify Firestore rules are set correctly
- Check user is authenticated
- Check internet connectivity

### Admin changes not visible to users
**Solution:**
- Verify admin email is exactly "wajahatabbasicentral@gmail.com"
- Check user is watching the correct Flow
- Restart app to force refresh
- Check Firestore has the data

### Non-admin can edit global fields
**Solution:**
- Clear app cache
- Verify `enabled = isAdmin` on fields
- Verify `isAdmin` is correctly determined
- Check `SecurityManager.isAdminUser()` logic

### Sales person name not persisting
**Solution:**
- Verify user is logged in
- Check user has UID
- Verify Firestore document exists under `settings/users/{uid}`
- Check internet connectivity for cloud sync

### Offline mode doesn't work
**Solution:**
- Verify user has used app before (cache exists)
- Check SharedPreferences has local data
- Verify offline mode is actually enabled

---

## Performance Testing

### Load Testing
- [ ] Test with 1000+ records in Firestore
- [ ] Test with slow network (3G)
- [ ] Test with multiple concurrent users
- [ ] Verify no UI freezing

### Memory Testing
- [ ] Monitor memory usage during sync
- [ ] Check for memory leaks (no static Firestore)
- [ ] Verify Flow cleanup

### Battery Testing
- [ ] Check battery drain during sync
- [ ] Verify not continuous polling
- [ ] Monitor network usage

---

## Sign-off Checklist

- [ ] All test cases pass
- [ ] No regressions in existing features
- [ ] Documentation complete
- [ ] Code reviewed
- [ ] Firestore rules deployed
- [ ] Backup of previous settings created
- [ ] Admin email confirmed correct
- [ ] Ready for production

---

## Deployment Steps

1. **Backup Current Data**
   ```
   Export all data from Firestore (or SharedPreferences)
   Create backup file
   Store safely
   ```

2. **Update Firestore Rules**
   - Go to Firebase Console
   - Navigate to Firestore Rules
   - Paste the security rules provided in this document
   - Publish rules

3. **Deploy App**
   - Build release APK
   - Test on device
   - Deploy to Play Store (or distribute to team)

4. **Monitor Post-Deployment**
   - Check error logs for first 24 hours
   - Monitor Firestore usage
   - Verify settings sync in real user scenario
   - Be ready to rollback if issues

---

## Support Contact

If issues arise:
1. Check troubleshooting section
2. Review logs in Firebase Console
3. Check Firestore data structure
4. Contact development team with:
   - Error message
   - User email
   - Device/OS
   - Steps to reproduce

---

**Last Updated:** April 10, 2026
**Version:** 1.0
**Status:** Ready for Testing

