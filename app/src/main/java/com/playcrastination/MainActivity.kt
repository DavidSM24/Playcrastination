package com.playcrastination

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.playcrastination.core.designsystem.PlaycrastinationTheme
import com.playcrastination.core.navigation.PlaycrastinationApp
import com.playcrastination.data.steam.SteamSyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var steamScheduler: SteamSyncScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            PlaycrastinationTheme { PlaycrastinationApp() }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            try {
                steamScheduler.schedule()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Log.w("SteamSync", "Steam synchronization could not be scheduled")
            }
        }
    }
}
