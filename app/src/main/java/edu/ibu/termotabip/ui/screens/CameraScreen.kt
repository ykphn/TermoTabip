package edu.ibu.termotabip.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import edu.ibu.termotabip.model.WoundAnalysisResult
import edu.ibu.termotabip.ui.theme.Level0
import edu.ibu.termotabip.ui.theme.Level1
import edu.ibu.termotabip.ui.theme.Level2
import edu.ibu.termotabip.ui.theme.Level3
import edu.ibu.termotabip.ui.theme.Level4
import edu.ibu.termotabip.viewmodel.WoundAnalysisViewModel
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    viewModel: WoundAnalysisViewModel = viewModel(),
    modifier: Modifier

) {
    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == 
        PackageManager.PERMISSION_GRANTED
    )}
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewModel.initModel(context)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.capturedImageUri == null) {
            // Kamera görünümü
            if (hasCameraPermission) {
                CameraView(
                    onImageCaptured = { bitmap ->
                        viewModel.analyzeWoundImage(bitmap, context)
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Kamera izni yok
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Kamera izni gerekli")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("İzin Ver")
                    }
                }
            }
        } else {
            // Analiz sonucu görünümü
            AnalysisResultScreen(
                uiState = uiState,
                onReset = { viewModel.resetState() }
            )
        }
        
        // Yükleniyor göstergesi
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CameraView(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        onError("Kamera başlatılamadı: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Fotoğraf çekme butonu
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            IconButton(
                onClick = {
                    captureImage(
                        imageCapture = imageCapture,
                        executor = executor,
                        onImageCaptured = onImageCaptured,
                        onError = onError
                    )
                },
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Fotoğraf Çek",
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

private fun captureImage(
    imageCapture: ImageCapture?,
    executor: Executor,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture?.let {
        val photoFile = File.createTempFile("IMG_", ".jpg")
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        it.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        onImageCaptured(bitmap)
                    } catch (e: Exception) {
                        onError("Fotoğraf işlenemedi: ${e.message}")
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    onError("Fotoğraf çekilemedi: ${exception.message}")
                }
            }
        )
    } ?: onError("Kamera hazır değil")
}

@Composable
fun AnalysisResultScreen(
    uiState: edu.ibu.termotabip.viewmodel.WoundAnalysisUiState,
    onReset: () -> Unit
) {
//    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Başlık
        Text(
            text = "Yara Analiz Sonucu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Çekilen fotoğraf
        uiState.capturedImageUri?.let { uri ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(uri)),
                    contentDescription = "Çekilen Fotoğraf",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Analiz sonucu
        uiState.analysisResult?.let { result ->
            WoundLevelCard(result)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Yeniden çekim butonu
        Button(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Yeniden Çek"
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Yeni Fotoğraf Çek")
        }
    }
}

@Composable
fun WoundLevelCard(result: WoundAnalysisResult) {
    val levelColor = when (result.woundLevel) {
        0 -> Level0
        1 -> Level1
        2 -> Level2
        3 -> Level3
        4 -> Level4
        else -> MaterialTheme.colorScheme.primary
    }
    
    val levelText = when (result.woundLevel) {
        0 -> "Sağlıklı Doku"
        1 -> "Seviye 1 - Hafif Yara"
        2 -> "Seviye 2 - Orta Şiddetli Yara"
        3 -> "Seviye 3 - Ciddi Yara"
        4 -> "Seviye 4 - Çok Ciddi Yara"
        else -> "Bilinmeyen Seviye"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yara Seviyesi:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(levelColor, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = levelText,
                style = MaterialTheme.typography.headlineSmall,
                color = levelColor,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Güven Oranı: %${(result.confidence * 100).toInt()}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val recommendation = when (result.woundLevel) {
                0 -> "Herhangi bir tedaviye gerek yok."
                1 -> "Hafif tedavi gerekebilir. Yarayı temiz tutun."
                2 -> "Düzenli pansuman ve tedavi gerekli."
                3 -> "Acil tıbbi müdahale gerekli. Doktora başvurun."
                4 -> "Acil hastane müdahalesi gerekli!"
                else -> "Doktora danışın."
            }
            
            Text(
                text = "Öneri: $recommendation",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
} 