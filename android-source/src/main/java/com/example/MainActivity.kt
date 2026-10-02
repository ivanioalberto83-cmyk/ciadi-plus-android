package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.core.session.SessionManager
import com.example.data.repository.SupabaseAuthRepositoryImpl
import com.example.data.repository.SupabaseModulesRepositoryImpl
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.CiadiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SessionManager(applicationContext)
        val authRepository = SupabaseAuthRepositoryImpl(sessionManager)
        val modulesRepository = SupabaseModulesRepositoryImpl(sessionManager)

        setContent {
            CiadiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authRepository = authRepository,
                        modulesRepository = modulesRepository
                    )
                }
            }
        }
    }
}
