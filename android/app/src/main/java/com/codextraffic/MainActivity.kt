package com.codextraffic

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.codextraffic.ble.BleTrafficClient
import com.codextraffic.ui.CodexTrafficApp

class MainActivity : ComponentActivity() {
    private val viewModel: TrafficViewModel by viewModels {
        TrafficViewModelFactory(
            BleTrafficRepository(BleTrafficClient(applicationContext))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                viewModel.start()
            }

            LaunchedEffect(Unit) {
                if (viewModel.hasRequiredPermissions()) {
                    viewModel.start()
                } else {
                    permissionLauncher.launch(viewModel.requiredPermissions())
                }
            }

            CodexTrafficApp(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        viewModel.stop()
        super.onDestroy()
    }
}
