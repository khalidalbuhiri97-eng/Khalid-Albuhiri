package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.SouqViewModel
import com.example.ui.SouqViewModelFactory
import com.example.ui.screens.SouqAppFlow
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: SouqViewModel by viewModels {
        SouqViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase safely with programmatic fallback if google-services.json is missing
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDummyKeyForInitializationOnly_")
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .setProjectId("souq-kxmpzq")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SouqAppFlow(viewModel = viewModel)
                }
            }
        }
    }
}
