package sktech.schedulify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import sktech.schedulify.ui.SchedulifyApp
import sktech.schedulify.ui.theme.SchedulifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SchedulifyTheme {
                SchedulifyApp()
            }
        }
    }
}
