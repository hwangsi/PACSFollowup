package com.example.pacsfollowup

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.pacsfollowup.navigation.AppNavigation
import com.example.pacsfollowup.ui.theme.PACSFollowupTheme

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private var permissionsGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher.launch(requiredPermissions)

        setContent {
            PACSFollowupTheme {
                if (permissionsGranted) {
                    AppNavigation()
                } else {
                    var showRationale by remember { mutableStateOf(true) }
                    if (showRationale) {
                        AlertDialog(
                            onDismissRequest = { showRationale = false },
                            title = { Text("Permissions Required") },
                            text = { Text("Camera and microphone permissions are required.\nPlease allow permissions in settings.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    permissionLauncher.launch(requiredPermissions)
                                    showRationale = false
                                }) { Text("Request Again") }
                            },
                            dismissButton = {
                                TextButton(onClick = { finish() }) { Text("Exit") }
                            }
                        )
                    }
                }
            }
        }
    }
}
