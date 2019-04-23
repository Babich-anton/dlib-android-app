package com.tzutalin.dlibtest

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.Trace
import android.util.Log
import android.view.WindowManager

import com.tzutalin.dlib.Constants
import com.tzutalin.dlib.FaceDet
import com.tzutalin.dlib.VisionDetRet
import org.junit.Assert

import java.io.File

/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with dlib lib.
 */
@SuppressLint("LogNotTimber")
class OnGetImageListener : OnImageAvailableListener {

    private var mScreenRotation = 90

    private var mPreviewWidth = 0
    private var mPreviewHeight = 0
    private var mYUVBytes: Array<ByteArray?>? = null
    private var mRGBBytes: IntArray? = null
    private var mRGBFrameBitmap: Bitmap? = null
    private var mCroppedBitmap: Bitmap? = null

    private var mIsComputing = false
    private var mInferenceHandler: Handler? = null

    private var mContext: Context? = null
    private var mFaceDet: FaceDet? = null
    private var mTransparentTitleView: TransparentTitleView? = null
    private var mWindow: FloatingCameraWindow? = null
    private var mFaceLandmarkPaint: Paint? = null

    fun initialize(
            context: Context,
            assetManager: AssetManager,
            scoreView: TransparentTitleView,
            handler: Handler) {
        this.mContext = context
        this.mTransparentTitleView = scoreView
        this.mInferenceHandler = handler
        mFaceDet = FaceDet(Constants.faceShapeModelPath)
        mWindow = FloatingCameraWindow(mContext!!)

        mFaceLandmarkPaint = Paint()
        mFaceLandmarkPaint!!.color = Color.GREEN
        mFaceLandmarkPaint!!.strokeWidth = 2f
        mFaceLandmarkPaint!!.style = Paint.Style.STROKE
    }

    fun deInitialize() {
        synchronized(this@OnGetImageListener) {
            if (mFaceDet != null) {
                mFaceDet!!.release()
            }

            if (mWindow != null) {
                mWindow!!.release()
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun drawResizedBitmap(src: Bitmap, dst: Bitmap?) {
        val getOrient = (mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        getOrient.getSize(point)
        val screenWidth = point.x
        val screenHeight = point.y
        Log.d(TAG, String.format("screen size (%d,%d)", screenWidth, screenHeight))
        mScreenRotation = if (screenWidth < screenHeight) {
            90
        } else {
            0
        }

        Assert.assertEquals(dst!!.width, dst.height)
        val minDim = Math.min(src.width, src.height).toFloat()

        val matrix = Matrix()

        // We only want the center square out of the original rectangle.
        val translateX = -Math.max(0f, (src.width - minDim) / 2)
        val translateY = -Math.max(0f, (src.height - minDim) / 2)
        matrix.preTranslate(translateX, translateY)

        val scaleFactor = dst.height / minDim
        matrix.postScale(scaleFactor, scaleFactor)

        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.width / 2.0f, -dst.height / 2.0f)
            matrix.postRotate((-mScreenRotation).toFloat())
            matrix.postTranslate(dst.width / 2.0f, dst.height / 2.0f)
        }

        val canvas = Canvas(dst)
        canvas.drawBitmap(src, matrix, null)
    }

    override fun onImageAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()

            if (image == null) {
                return
            }

            // No mutex needed as this method is not reentrant.
            if (mIsComputing) {
                image.close()
                return
            }
            mIsComputing = true

            Trace.beginSection("imageAvailable")

            val planes = image.planes

            // Initialize the storage bitmaps once when the resolution is known.
            if (mPreviewWidth != image.width || mPreviewHeight != image.height) {
                mPreviewWidth = image.width
                mPreviewHeight = image.height

                Log.d(TAG, String.format("Initializing at size %dx%d", mPreviewWidth, mPreviewHeight))
                mRGBBytes = IntArray(mPreviewWidth * mPreviewHeight)
                mRGBFrameBitmap = Bitmap.createBitmap(mPreviewWidth, mPreviewHeight, Config.ARGB_8888)
                mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888)

                mYUVBytes = arrayOfNulls(planes.size)
                for (i in planes.indices) {
                    mYUVBytes!![i] = ByteArray(planes[i].buffer.capacity())
                }
            }

            for (i in planes.indices) {
                planes[i].buffer.get(mYUVBytes!![i])
            }

            val yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            ImageUtils.convertYUV420ToARGB8888(
                    mYUVBytes!![0]!!,
                    mYUVBytes!![1]!!,
                    mYUVBytes!![2]!!,
                    mRGBBytes!!,
                    mPreviewWidth,
                    mPreviewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    false)

            image.close()
        } catch (e: Exception) {
            image?.close()
            Log.e(TAG, "Exception!", e)
            Trace.endSection()
            return
        }

        mRGBFrameBitmap!!.setPixels(mRGBBytes, 0, mPreviewWidth, 0, 0, mPreviewWidth, mPreviewHeight)
        drawResizedBitmap(mRGBFrameBitmap!!, mCroppedBitmap)

        mInferenceHandler!!.post {
            if (!File(Constants.faceShapeModelPath).exists()) {
                mTransparentTitleView!!.setText("Copying landmark model to " + Constants.faceShapeModelPath)
                FileUtils.copyFileFromRawToOthers(mContext!!, R.raw.shape_predictor_68_face_landmarks, Constants.faceShapeModelPath)
            }

            val startTime = System.currentTimeMillis()
            val results: List<VisionDetRet>?
            synchronized(this@OnGetImageListener) {
                results = mFaceDet!!.detect(mCroppedBitmap!!)
            }
            val endTime = System.currentTimeMillis()
            mTransparentTitleView!!.setText("Time cost: " + (endTime - startTime) / 1000f + " sec")
            // Draw on bitmap
            if (results != null) {
                for (ret in results) {
                    val resizeRatio = 1.0f
                    val bounds = Rect()
                    bounds.left = (ret.mLeft * resizeRatio).toInt()
                    bounds.top = (ret.mTop * resizeRatio).toInt()
                    bounds.right = (ret.mRight * resizeRatio).toInt()
                    bounds.bottom = (ret.mBottom * resizeRatio).toInt()
                    val canvas = Canvas(mCroppedBitmap!!)
                    canvas.drawRect(bounds, mFaceLandmarkPaint!!)

                    // Draw landmark
                    val landmarks = ret.mLandmarkPoints
                    for (point in landmarks) {
                        val pointX = (point.x * resizeRatio).toInt()
                        val pointY = (point.y * resizeRatio).toInt()
                        canvas.drawCircle(pointX.toFloat(), pointY.toFloat(), 2f, mFaceLandmarkPaint!!)
                    }
                }
            }

            mWindow!!.setRGBBitmap(mCroppedBitmap!!)
            mIsComputing = false
        }

        Trace.endSection()
    }

    companion object {
        private const val TAG = "OnGetImageListener"

        private const val INPUT_SIZE = 224
    }
}