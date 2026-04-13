package com.apislens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.apislens.ui.navigation.ApisLensNavGraph
import com.apislens.ui.theme.ApisLensTheme
import com.apislens.ui.theme.ThemeSettings
import com.apislens.ui.theme.ThemeSettingsProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeSettings: ThemeSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThemeSettingsProvider(settings = themeSettings) { themeMode, dynamicColor ->
                ApisLensTheme(
                    themeMode = themeMode,
                    useDynamicColor = dynamicColor
                ) {
                    val navController = rememberNavController()
                    ApisLensNavGraph(navController = navController)
                }
            }
        }
    }
}
