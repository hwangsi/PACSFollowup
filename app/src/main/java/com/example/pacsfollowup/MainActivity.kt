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
                            title = { Text("권한 필요") },
                            text = { Text("카메라와 마이크 권한이 필요합니다.\n설정에서 권한을 허용해주세요.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    permissionLauncher.launch(requiredPermissions)
                                    showRationale = false
                                }) { Text("다시 요청") }
                            },
                            dismissButton = {
                                TextButton(onClick = { finish() }) { Text("종료") }
                            }
                        )
                    }
                }
            }
        }
    }
}
