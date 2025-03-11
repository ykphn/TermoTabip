package edu.ibu.termotabip

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ibu.termotabip.ui.screens.CameraScreen
import edu.ibu.termotabip.ui.theme.TermoTabipTheme
import edu.ibu.termotabip.viewmodel.WoundAnalysisViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = WoundAnalysisViewModel()
        val modelAvailable = viewModel.checkModelAvailability(this)
        Log.d("MainActivity", "TFLite model durumu: ${if (modelAvailable) "Mevcut" else "Bulunamadı"}")
        setContent {
            TermoTabipTheme {
                val viewModel: WoundAnalysisViewModel = viewModel
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}