package com.densitech.scrollsmooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.densitech.scrollsmooth.ui.commerce.data.CommerceStore
import com.densitech.scrollsmooth.ui.commerce.data.CommerceSync
import com.densitech.scrollsmooth.ui.commerce.data.api.TokenStore
import com.densitech.scrollsmooth.ui.main.MainScreen
import com.densitech.scrollsmooth.ui.theme.ScrollSmoothTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Local state is restored, then the session, before any surface reads
        // either. The first refresh is fire-and-forget: with no server the
        // seeded catalogue is what stays on screen.
        CommerceStore.init(applicationContext)
        TokenStore.init(applicationContext)
        CommerceSync.refreshAll()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ScrollSmoothTheme {
                val navController = rememberNavController()
                MainScreen(navController)
            }
        }
    }
}
