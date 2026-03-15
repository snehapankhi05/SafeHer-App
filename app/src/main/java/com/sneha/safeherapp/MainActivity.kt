package com.sneha.safeherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sneha.safeherapp.navigation.AppNavigation
import com.sneha.safeherapp.ui.theme.SafeHerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeHerAppTheme {
                AppNavigation(this@MainActivity)
            }
        }
    }
}
