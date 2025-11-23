package com.example.indivassignment6q3

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.indivassignment6q3.ui.theme.IndivAssignment6Q3Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.log10

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndivAssignment6Q3Theme {
                SoundMeterScreen()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun SoundMeterScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // State for Decibels
    var dbLevel by remember { mutableFloatStateOf(0f) }
    
    // Threshold for "Loud"
    val THRESHOLD_DB = 80f
    val isTooLoud = dbLevel > THRESHOLD_DB

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Audio Recording & dB Calculation Logic
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            withContext(Dispatchers.IO) {
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                    val audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )

                    val buffer = ShortArray(bufferSize)
                    audioRecord.startRecording()

                    while (isActive) {
                        val readResult = audioRecord.read(buffer, 0, bufferSize)
                        if (readResult > 0) {
                            var max = 0
                            for (i in 0 until readResult) {
                                val value = abs(buffer[i].toInt())
                                if (value > max) {
                                    max = value
                                }
                            }
                            
                            // Convert Amplitude to Decibels
                            val db = if (max > 0) {
                                20 * log10(max.toDouble())
                            } else {
                                0.0
                            }
                            
                            dbLevel = db.toFloat()
                        }
                    }
                    audioRecord.stop()
                    audioRecord.release()
                }
            }
        }
    }

    // Animate background color slightly when too loud
    val backgroundColor by animateColorAsState(
        targetValue = if (isTooLoud) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.background,
        label = "bgColor"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (hasPermission) {
                Text(
                    text = "Sound Meter",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Display dB Value
                Text(
                    text = "${String.format("%.1f", dbLevel)} dB",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isTooLoud) Color.Red else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Visual Sound Meter (Progress Bar)
                val animatedProgress by animateFloatAsState(
                    targetValue = (dbLevel / 100f).coerceIn(0f, 1f),
                    label = "dbProgress"
                )
                
                val barColor = when {
                    dbLevel < 50 -> Color.Green
                    dbLevel < 80 -> Color.Yellow
                    else -> Color.Red
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = barColor,
                    trackColor = Color.LightGray
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                // Threshold Alert Box
                if (isTooLoud) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "WARNING: NOISE LEVEL TOO HIGH!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Invisible placeholder to keep layout stable
                     Box(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = " ",
                            fontSize = 18.sp
                        )
                    }
                }
                
            } else {
                Text("Permission Needed", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Microphone Permission")
                }
            }
        }
    }
}
