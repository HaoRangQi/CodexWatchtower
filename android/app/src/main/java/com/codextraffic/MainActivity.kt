package com.codextraffic

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.codextraffic.ble.BleTrafficClient
import com.codextraffic.http.HttpTrafficClient
import com.codextraffic.ui.CodexTrafficApp

class MainActivity : ComponentActivity() {
    private val viewModel: TrafficViewModel by viewModels {
        TrafficViewModelFactory(
            repository = HybridTrafficRepository(
                primary = BleTrafficRepository(BleTrafficClient(applicationContext)),
                fallback = HttpTrafficRepository(HttpTrafficClient(applicationContext)),
            ),
            hiddenProjectStore = SharedPreferencesHiddenProjectStore(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

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
