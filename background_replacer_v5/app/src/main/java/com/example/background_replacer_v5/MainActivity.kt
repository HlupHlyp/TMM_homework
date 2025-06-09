package com.example.background_replacer_v5

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import androidx.core.app.ActivityCompat
import android.annotation.SuppressLint
import android.net.Uri
import android.widget.VideoView
import android.widget.Button;
import android.widget.ImageView
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue


class MainActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var imageView: ImageView

    private lateinit var overlay: SegmentOverlay
    private lateinit var button: Button
    private var cameraExecutor = Executors.newSingleThreadExecutor()
    private var videoCounter = 0
    private val videoURIs = mapOf(0 to "waterfall", 1 to "lagoon", 2 to "forest")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        videoView = findViewById(R.id.video_view)
        val videoUri: Uri = Uri.parse("android.resource://${packageName}/raw/waterfall")
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            videoView.start()
        }
        imageView = findViewById(R.id.myImageView)
        overlay = findViewById(R.id.segment_overlay)
        button = findViewById(R.id.button)
        button.setOnClickListener {
            changeVideo()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    private fun changeVideo(){
        videoCounter++
        if(videoCounter == 3){
            videoCounter = 0
        }
        val videoUri: Uri = Uri.parse(
            "android.resource://${packageName}/raw/${videoURIs[videoCounter]}")
        videoView.setVideoURI(videoUri)
        videoView.start()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera()
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            val analysisUseCase = ImageAnalysis.Builder().
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).
                build()
                .also {
                    val timeSource = TimeSource.Monotonic
                    val start_time = timeSource.markNow()
                    it.setAnalyzer(cameraExecutor, SelfieAnalyzer(lifecycle,imageView, overlay))
                    Log.d("Frame_time", (timeSource.markNow()-start_time).toString())
                }


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this,cameraSelector,analysisUseCase)
            }
            catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }

}