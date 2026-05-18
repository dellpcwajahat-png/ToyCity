package com.example.toycity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.toycity.ui.screens.MainDashboard
import com.example.toycity.ui.theme.ToyCityTheme
import kotlinx.coroutines.delay

import com.example.toycity.ui.AuthViewModel
import com.example.toycity.ui.screens.LoginScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.fragment.app.FragmentActivity
import com.example.toycity.utils.SecurityManager

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val user by authViewModel.user.collectAsState()
            
            // Re-calculate lock state based on user login status
            // If user is NOT logged in, we DON'T show the lock
            val isAppLockEnabled = SecurityManager.isAppLockEnabled(this) && user != null
            
            var isUnlocked by remember(user) { mutableStateOf(!isAppLockEnabled) }
            
            LaunchedEffect(user) {
                if (isAppLockEnabled && !isUnlocked && SecurityManager.isBiometricEnabled(this@MainActivity)) {
                    SecurityManager.showBiometricPrompt(
                        activity = this@MainActivity,
                        onSuccess = {
                            isUnlocked = true
                        },
                        onError = {
                            // Handled by manual PIN entry if biometric fails
                        }
                    )
                }
            }

            ToyCityTheme {
                if (user == null) {
                    // Always show LoginScreen if user is null (no lock required)
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = { /* AuthViewModel will update user state */ }
                    )
                } else {
                    // User is logged in, now check lock
                    if (!isUnlocked) {
                        PinEntryScreen(
                            onPinEntered = { pin ->
                                if (pin == SecurityManager.getPin(this@MainActivity)) {
                                    isUnlocked = true
                                } else {
                                    android.widget.Toast.makeText(this@MainActivity, "Incorrect PIN", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        MainDashboard(authViewModel = authViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PinEntryScreen(onPinEntered: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            delay(300)
            onPinEntered(pin)
            delay(200)
            pin = ""
        }
    }

    val bgGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 80.dp)
            ) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your PIN to unlock Toy City",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── PIN Dots ─────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < pin.length
                    val dotSize by animateDpAsState(
                        targetValue = if (isFilled) 20.dp else 14.dp,
                        label = "dotSize"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isFilled) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.outlineVariant,
                        label = "dotColor"
                    )

                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(dotSize),
                            shape = CircleShape,
                            color = dotColor,
                            border = if (!isFilled) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null
                        ) {}
                    }
                }
            }

            // ── Numeric Keypad ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(null, "0", "backspace")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            if (key == null) {
                                Spacer(modifier = Modifier.size(80.dp))
                            } else {
                                KeypadButton(
                                    content = key,
                                    onClick = {
                                        if (key == "backspace") {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        } else {
                                            if (pin.length < 4) {
                                                pin += key
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    content: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (content == "backspace") {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "backspace",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val infiniteTransition = remember { Animatable(0f) }
            
            LaunchedEffect(Unit) {
                infiniteTransition.animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }

            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .rotate(infiniteTransition.value),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Toy City",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun MainContent() {
    MainDashboard()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToyCityTheme {
        Greeting("Android")
    }
}