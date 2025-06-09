package com.example.background_replacer_v5

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Size
import android.view.View
import com.google.mlkit.vision.face.Face

class SegmentOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var faces = emptyArray<Face>()
    private var previewWidth: Int = 0
    private var widthScaleFactor = 1.0f
    private var previewHeight: Int = 0
    private var heightScaleFactor = 1.0f
    private var orientation = Configuration.ORIENTATION_LANDSCAPE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
    }


    override fun onDraw(canvas: Canvas) {
        widthScaleFactor = width.toFloat() / previewWidth
        heightScaleFactor = height.toFloat() / previewHeight
        for(face in faces)
        {
            drawFaceBorder(face, canvas)
        }
    }

    private fun drawFaceBorder(face: Face, canvas: Canvas)
    {
        val bounds = face.boundingBox
        val left = width - translateX(bounds.right.toFloat())
        val top = translateY(bounds.top.toFloat())
        val right = width - translateX(bounds.left.toFloat())
        val bottom = translateY(bounds.bottom.toFloat())
        canvas.drawRect(left, top, right, bottom, paint)
    }

    fun setFaces(faceList: List<Face>)
    {
        faces = faceList.toTypedArray()
        postInvalidate()
    }

    fun setPreviewSize(size: Size) {
        // Need to swap width and height when in portrait, since camera's natural orientation is landscape.
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            previewWidth = size.width
            previewHeight = size.height
        } else {
            previewWidth = size.height
            previewHeight = size.width
        }
    }

    private fun translateX(x: Float): Float = x * widthScaleFactor
    private fun translateY(y: Float): Float = y * heightScaleFactor
}

