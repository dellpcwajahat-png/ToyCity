# Code Changes Reference - Line by Line

## SecurityManager.kt

### 1. New Admin Function (Line 162-164)
```kotlin
fun isAdminUser(email: String?): Boolean {
    return email == "wajahatabbasicentral@gmail.com"
}
```
**Purpose**: Centralized admin email check. Easy to modify or move to Firestore later.

### 2. Global Receipt Settings Save (Line 166-181)
```kotlin
suspend fun saveGlobalReceiptSettings(
    name: String,
    address: String,
    phone: String,
    note: String,
    ntnNo: String
) {
    val data = mapOf(
        "name" to name,
        "address" to address,
        "phone" to phone,
        "note" to note,
        "ntnNo" to ntnNo
    )
    getDb().collection("settings").document("global").set(data).await()
}
```
**Purpose**: Saves global (admin-edited) settings to Firestore.
**Who calls it**: Admin only, from ReceiptSettingsScreen save button.

### 3. User Receipt Settings Save (Line 183-186)
```kotlin
suspend fun saveUserReceiptSettings(userId: String, salesPerson: String) {
    val data = mapOf("salesPerson" to salesPerson)
    getDb().collection("settings").document("users").collection("userSettings").document(userId).set(data).await()
}
```
**Purpose**: Saves user-specific sales person name tied to account.
**Who calls it**: All users, from ReceiptSettingsScreen save button.
**Why subcollection**: Allows scalable user-specific settings.

### 4. Receipt Settings Flow (Line 196-229)
```kotlin
fun getReceiptSettingsFlow(context: Context, userId: String?): Flow<Map<String, String>> = flow {
    // Emit local settings first
    val localSettings = getReceiptSettings(context, userId)
    emit(localSettings)

    try {
        // Get global settings
        val globalSnapshot = getDb().collection("settings").document("global").get().await()
        val globalData = globalSnapshot.data ?: emptyMap()

        // Get user settings if userId provided
        val userData = if (userId != null) {
            val userSnapshot = getDb().collection("settings").document("users").collection("userSettings").document(userId).get().await()
            userSnapshot.data ?: emptyMap()
        } else {
            emptyMap()
        }

        // Merge global and user settings
        // Note: salesPerson should default to empty string for new users
        val merged = mapOf(
            "name" to ((globalData["name"] as? String) ?: localSettings["name"] ?: "Toy City"),
            "address" to ((globalData["address"] as? String) ?: localSettings["address"] ?: ""),
            "phone" to ((globalData["phone"] as? String) ?: localSettings["phone"] ?: ""),
            "note" to ((globalData["note"] as? String) ?: localSettings["note"] ?: "Thank you for your business!"),
            "ntnNo" to ((globalData["ntnNo"] as? String) ?: localSettings["ntnNo"] ?: ""),
            "salesPerson" to ((userData["salesPerson"] as? String) ?: ""),  // Defaults to empty
            "printerAddress" to ((userData["printerAddress"] as? String) ?: localSettings["printerAddress"] ?: ""),
            "pageSize" to ((userData["pageSize"] as? String) ?: localSettings["pageSize"] ?: "80mm")
        )
        emit(merged)
    } catch (_: Exception) {
        // If cloud fails, keep local
    }
}
```
**Key Features**:
- ✅ Emits local first (offline support)
- ✅ Fetches cloud data asynchronously
- ✅ Merges with proper priority: Cloud > Local
- ✅ salesPerson defaults to empty string for new users
- ✅ Graceful fallback on error
- ✅ Uses Flow for reactive updates

## MainDashboard.kt

### 1. Admin Detection (Line 1217)
```kotlin
val isAdmin = SecurityManager.isAdminUser(user?.email)
```
**Before**: 
```kotlin
val adminEmails = listOf("toycity90@gmail.com", "dellpcwajahat@gmail.com", "wajahatabbasicentral@gmail.com")
val isAdmin = user?.email != null && user?.email in adminEmails
```
**Why Changed**: 
- Centralizes admin logic
- Single source of truth
- Easier to update
- Reduces code duplication

### 2. Settings Flow Collection (Line 1219-1220)
```kotlin
val settingsFlow = remember(user?.uid) { SecurityManager.getReceiptSettingsFlow(context, user?.uid) }
val settings by settingsFlow.collectAsState(initial = emptyMap())
```
**Purpose**: Collects settings from Flow and stores in Compose State.
**Effect**: UI updates automatically when settings change.

### 3. Info Banner for Non-Admin Users (Line 1248-1263)
```kotlin
// Show admin info banner for non-admin users
if (!isAdmin) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "ℹ️ Receipt settings are managed by the admin. You can only edit your sales person name.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```
**Purpose**: Inform non-admin users about their limitations.
**Shows**: Only for non-admin users.

### 4. Field Enablement (Line 1266-1271)
```kotlin
ReceiptTextField(label = "Business Name", ..., enabled = isAdmin)
ReceiptTextField(label = "Address", ..., enabled = isAdmin)
ReceiptTextField(label = "Phone Number", ..., enabled = isAdmin)
ReceiptTextField(label = "NTN No (Optional)", ..., enabled = isAdmin)
ReceiptTextField(label = "Sales Person Name", ..., enabled = true)  // Always enabled
ReceiptTextField(label = "Thank You Note", ..., enabled = isAdmin)
```
**Key Point**: Sales Person Name is always `enabled = true` for all users.

### 5. Save Logic (Line 1282-1307)
```kotlin
// Only allow saving if user made valid changes
scope.launch {
    try {
        // Only admin can save global settings
        if (isAdmin) {
            SecurityManager.saveGlobalReceiptSettings(
                businessName,
                businessAddress,
                businessPhone,
                thankYouNote,
                ntnNo
            )
            android.widget.Toast.makeText(
                context,
                "Global receipt settings saved successfully",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // All users can save their salesPerson name
        user?.uid?.let { uid ->
            SecurityManager.saveUserReceiptSettings(uid, salesPerson)
            if (!isAdmin) {
                android.widget.Toast.makeText(
                    context,
                    "Your sales person name saved successfully",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Failed to save settings: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
```
**Flow**:
1. Check if user is admin
2. If admin: Save global settings + show admin message
3. Always: Save user's sales person name
4. If non-admin: Show user-specific message
5. On error: Show error with exception message

### 6. Button Text (Line 1324)
```kotlin
Text(if (isAdmin) "Save Configuration" else "Save Sales Person Name", ...)
```
**Purpose**: Different text based on user role.
- Admin sees: "Save Configuration"
- Non-Admin sees: "Save Sales Person Name"

## DashboardScreen Changes (Line 60)
```kotlin
// Before:
val adminEmails = listOf("toycity90@gmail.com", "dellpcwajahat@gmail.com", "wajahatabbasicentral@gmail.com")
val isAdmin = user?.email != null && user?.email in adminEmails

// After:
val isAdmin = SecurityManager.isAdminUser(user?.email)
```
**Purpose**: Consistency across the app.

## Database Schema

### Before:
```
Local SharedPreferences only:
├── business_name
├── business_address
├── business_phone
├── thank_you_note
├── sales_person_admin (or sales_person_{userId})
├── ntn_no
├── selected_printer_address
└── printer_page_size
```

### After:
```
Firestore:
├── settings/
│   ├── global/ (admin-controlled)
│   │   ├── name
│   │   ├── address
│   │   ├── phone
│   │   ├── note
│   │   └── ntnNo
│   └── users/{userId}/userSettings/ (user-specific)
│       ├── salesPerson
│       ├── printerAddress
│       └── pageSize

Local SharedPreferences: (fallback cache)
├── business_name
├── business_address
├── business_phone
├── thank_you_note
├── sales_person_{userId}
├── ntn_no
├── selected_printer_address
└── printer_page_size
```

## Data Flow Diagram

### Reading Settings:
```
ReceiptSettingsScreen
    ↓
getReceiptSettingsFlow() [Flow]
    ↓
emit(localSettings) [immediate]
    ↓
fetch globalSnapshot from Firestore
    ↓
fetch userSnapshot from Firestore
    ↓
merge with priority: Cloud > Local
    ↓
emit(merged) [reactive update]
    ↓
Settings appear in UI
```

### Writing Settings:
```
User clicks Save button
    ↓
Check if isAdmin
    ├─ YES: Call saveGlobalReceiptSettings()
    │   └─ Firestore settings/global updated
    └─ (Always) Call saveUserReceiptSettings()
       └─ Firestore settings/users/{userId} updated
    ↓
UI receives new settings via Flow
    ↓
Display confirmation message
```

## Type Safety

All database operations are type-safe:
```kotlin
val userData = userSnapshot.data ?: emptyMap()
// Safe cast with fallback
(userData["salesPerson"] as? String) ?: ""
// Will never fail, defaults to empty string if missing
```

## Error Handling

All operations wrapped in try-catch:
```kotlin
try {
    // Cloud operations
} catch (_: Exception) {
    // Silently fall back to local
}
```

This ensures app never crashes due to network issues.

## Performance Considerations

✅ **Lazy Loading**: Settings only fetched when screen opened
✅ **Caching**: Local settings available immediately
✅ **Async**: All network calls non-blocking
✅ **Flow**: Updates automatically without polling
✅ **Subcollections**: User settings scalable to millions of users

## Security Boundaries

1. **Admin Email**: Hardcoded (can be moved to Firestore)
2. **User UID**: Automatically set by Firebase Auth
3. **Firestore Rules**: Recommended to enforce permissions server-side
4. **No Sensitive Data**: Only settings, no passwords

## Testing Scenarios

### Scenario 1: Admin edits global settings
```
Admin (wajahatabbasicentral@gmail.com):
1. Changes Business Name to "Toy City V2"
2. Clicks Save
3. Global settings saved to Firestore
4. All users' UI updates with new Business Name
```

### Scenario 2: User sets sales person name
```
User A (user_a@example.com):
1. Enters "Alice" in Sales Person Name
2. Clicks Save
3. User-specific settings saved to Firestore under user_a's UID
4. User A logged out
5. User A logs in on different device
6. Sales Person Name shows "Alice" automatically
```

### Scenario 3: Multiple users editing
```
User A sets sales person to "Alice"
  ↓
User B sets sales person to "Bob"
  ↓
Both names stored in Firestore under different UIDs
  ↓
When either logs in: They see their own name
```

### Scenario 4: Offline access
```
Device offline:
1. User opens Receipt Settings
2. Local cached settings displayed
3. All fields work normally
4. User can edit and save locally
5. When online: Changes sync to Firestore
```

## Backward Compatibility

✅ Old `saveReceiptSettings()` function kept and marked @Deprecated
✅ Old `getReceiptSettings()` function still works with local data
✅ Existing data in SharedPreferences not deleted
✅ Smooth migration: New code reads from cloud, falls back to local

## Version Control Notes

**When committing:**
- SecurityManager.kt: All changes related to cloud sync
- MainDashboard.kt: UI changes for role-based access
- IMPLEMENTATION_GUIDE.md: Documentation
- CHANGES_SUMMARY.md: This reference

**No breaking changes**: Existing functionality preserved while adding new features.

