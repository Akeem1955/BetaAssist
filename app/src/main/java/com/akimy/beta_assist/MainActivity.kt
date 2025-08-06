package com.akimy.beta_assist

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.akimy.beta_assist.ui.theme.BetaAssistTheme

class MainActivity : ComponentActivity() {
    private var permissionRequested by mutableStateOf(false)

    // Broadcast receiver for permission requests


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the broadcast receiver
        enableEdgeToEdge()
        setContent {
            BetaAssistTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Always show tutorial content
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // Show permission banner if needed
                        PermissionHandlerSection()

                        // Always show tutorial content
                        TutorialContent(modifier = Modifier)
                    }
                }
            }
        }
    }

    @Composable
    private fun PermissionHandlerSection() {
        val context = LocalContext.current

        // Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                permissionRequested = false
                if (granted) {
                    // Permission granted, user can now enable the service
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
            }
        )

        // Check for audio permission
        val hasAudioPermission = remember {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }

        // Request permission when needed
        LaunchedEffect(permissionRequested) {
            if (permissionRequested && !hasAudioPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        // Show permission card if needed
        if (!hasAudioPermission || permissionRequested) {
            PermissionCard(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun PermissionCard(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Microphone Permission Required",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Beta Assist needs microphone access to recognize voice commands and provide assistance.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant Permission")
            }
        }
    }
}