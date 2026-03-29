package com.mspa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mspa.app.ui.MspaApp
import com.mspa.app.ui.theme.MspaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MspaTheme {
                MspaApp()
            }
        }
    }
}
