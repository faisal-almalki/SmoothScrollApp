package com.densitech.scrollsmooth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.densitech.scrollsmooth.ui.commerce.data.CommerceStore
import com.densitech.scrollsmooth.ui.main.MainScreen
import com.densitech.scrollsmooth.ui.theme.ScrollSmoothTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Carts and orders are restored before any commerce surface reads them.
        CommerceStore.init(applicationContext)

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
