package com.example.background_replacer_v5

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.Face
import android.util.Size
import android.annotation.SuppressLint
import android.media.Image
import androidx.lifecycle.Lifecycle
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.OnFailureListener
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Matrix
import android.widget.ImageView
import androidx.core.graphics.scale
import com.slowmac.autobackgroundremover.BackgroundRemover
import com.slowmac.autobackgroundremover.OnBackgroundChangeListener
import kotlin.time.TimeSource


class SelfieAnalyzer(lifecycle: Lifecycle, private val imageView: ImageView, private val overlay: SegmentOverlay) :
    ImageAnalysis.Analyzer {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        //.enableTracking() //disable when contour is enable https://developers.google.com/ml-kit/vision/face-detection/android
        .build()
    private val detector = FaceDetection.getClient(options)
    init {
        //add the detector in lifecycle observer to properly close it when it's no longer needed.
        lifecycle.addObserver(detector)
    }
    override fun analyze(imageProxy: ImageProxy) {
        //Log.d("selfie_analyzer_v2","in_analyze")
        val timeSource = TimeSource.Monotonic
        val start_time = timeSource.markNow()
        overlay.setPreviewSize(Size(imageProxy.width, imageProxy.height))
        var bitmap = imageProxy.toBitmap().copy(Config.RGB_565, false)
        bitmap = bitmap.scale(bitmap.width/2, bitmap.height/2)
        Log.d("selfie_analyzer_v2",(bitmap == null).toString())
        BackgroundRemover.bitmapForProcessing(
            bitmap, trimEmptyPart = false,
            listener = object : OnBackgroundChangeListener {
                override fun onSuccess(bitmap: Bitmap) {
                    Log.d("BackgroundRemover", "Background removal works")
                    detectFaces(imageProxy)
                    val matrix = Matrix()
                    matrix.postScale(-1f, 1f, bitmap.width*2f, bitmap.height/1f)
                    var new_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
                    imageView.setImageBitmap(new_bitmap.scale(overlay.width, overlay.height))
                    Log.d("Frame_time", (timeSource.markNow()-start_time).toString())
                }

                override fun onFailed(exception: Exception) {
                    Log.d("BackgroundRemover", "Background removal failed: ${exception}")
                    detectFaces(imageProxy)
                }
            }
        )
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun detectFaces(imageProxy: ImageProxy) {
        val image = InputImage.fromMediaImage(imageProxy.image as Image, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener(faceDetectSuccessListener)
            .addOnFailureListener(faceDetectFailureListener)
            .addOnCompleteListener{
                imageProxy.close()
            }
    }

    private val faceDetectSuccessListener = OnSuccessListener<List<Face>> { faces ->
        Log.d(SelfieAnalyzer.Companion.TAG, "Number of face detected: " + faces.size)
        overlay.setFaces(faces)
    }

    private val faceDetectFailureListener = OnFailureListener { e ->
        Log.e(SelfieAnalyzer.Companion.TAG, "Face analysis failure.", e)
    }

    companion object {
        private const val TAG = "FaceAnalyzer"
    }

}






