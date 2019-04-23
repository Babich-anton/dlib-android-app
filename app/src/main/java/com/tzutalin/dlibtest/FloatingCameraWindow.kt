package com.tzutalin.dlibtest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.UiThread

import java.lang.ref.WeakReference
import java.util.Objects


internal class FloatingCameraWindow @SuppressLint("ObsoleteSdkInt")
constructor(private val mContext: Context) {
    private var mWindowParam: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var mRootView: FloatCamView? = null
    private val mUIHandler: Handler = Handler(Looper.getMainLooper())

    private var mWindowWidth: Int = 0
    private var mWindowHeight: Int = 0

    private val mScaleWidthRatio = 1.0f
    private val mScaleHeightRatio = 1.0f

    init {
        // Get screen max size
        val size = Point()
        val display = (Objects.requireNonNull(mContext.getSystemService(Context.WINDOW_SERVICE)) as WindowManager).defaultDisplay
        val mScreenMaxHeight: Int
        val mScreenMaxWidth: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            display.getSize(size)
            mScreenMaxWidth = size.x
            mScreenMaxHeight = size.y
        } else {
            mScreenMaxWidth = display.width
            mScreenMaxHeight = display.height
        }
        // Default window size
        mWindowWidth = mScreenMaxWidth / 2
        mWindowHeight = mScreenMaxHeight / 2

        mWindowWidth = if (mWindowWidth in 1 until mScreenMaxWidth) mWindowWidth else mScreenMaxWidth
        mWindowHeight = if (mWindowHeight in 1 until mScreenMaxHeight) mWindowHeight else mScreenMaxHeight
    }

    private fun init() {
        mUIHandler.postAtFrontOfQueue {
            if (mWindowManager == null || mRootView == null) {
                mWindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                mRootView = FloatCamView(this@FloatingCameraWindow)
                mWindowManager!!.addView(mRootView, initWindowParameter())
            }
        }
    }

    fun release() {
        mUIHandler.postAtFrontOfQueue {
            if (mWindowManager != null) {
                mWindowManager!!.removeViewImmediate(mRootView)
                mRootView = null
            }
            mUIHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun initWindowParameter(): WindowManager.LayoutParams {
        mWindowParam = WindowManager.LayoutParams()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWindowParam!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            mWindowParam!!.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        mWindowParam!!.format = 1
        mWindowParam!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mWindowParam!!.flags = mWindowParam!!.flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        mWindowParam!!.flags = mWindowParam!!.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        mWindowParam!!.alpha = 1.0f

        mWindowParam!!.gravity = Gravity.BOTTOM or Gravity.END
        mWindowParam!!.x = 0
        mWindowParam!!.y = 0
        mWindowParam!!.width = mWindowWidth
        mWindowParam!!.height = mWindowHeight

        return mWindowParam!!
    }

    fun setRGBBitmap(rgb: Bitmap) {
        checkInit()
        mUIHandler.post { mRootView!!.setRGBImageView(rgb) }
    }

    private fun checkInit() {
        if (mRootView == null) {
            init()
        }
    }

    @UiThread
    private inner class FloatCamView @SuppressLint("ClickableViewAccessibility")
    constructor(window: FloatingCameraWindow) : FrameLayout(window.mContext) {
        private val mWeakRef: WeakReference<FloatingCameraWindow> = WeakReference(window)
        private var mLastX: Int = 0
        private var mLastY: Int = 0
        private var mFirstX: Int = 0
        private var mFirstY: Int = 0
        private val mLayoutInflater: LayoutInflater = window.mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        private val mColorView: ImageView
        private val mFPSText: TextView
        private val mInfoText: TextView
        private var mIsMoving = false

        init {
            val body = this
            body.setOnTouchListener { _, _ -> false }

            mLayoutInflater.inflate(R.layout.cam_window_view, body, true)
            mColorView = findViewById(R.id.imageView_c)
            mFPSText = findViewById(R.id.fps_text_view)
            mInfoText = findViewById(R.id.info_text_view)
            mFPSText.visibility = View.GONE
            mInfoText.visibility = View.GONE

            val colorMaxWidth = (mWindowWidth * window.mScaleWidthRatio).toInt()
            val colorMaxHeight = (mWindowHeight * window.mScaleHeightRatio).toInt()

            mColorView.layoutParams.width = colorMaxWidth
            mColorView.layoutParams.height = colorMaxHeight
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mLastX = event.rawX.toInt()
                    mLastY = event.rawY.toInt()
                    mFirstX = mLastX
                    mFirstY = mLastY
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX.toInt() - mLastX
                    val deltaY = event.rawY.toInt() - mLastY
                    mLastX = event.rawX.toInt()
                    mLastY = event.rawY.toInt()
                    val totalDeltaX = mLastX - mFirstX
                    val totalDeltaY = mLastY - mFirstY

                    if (mIsMoving
                            || Math.abs(totalDeltaX) >= MOVE_THRESHOLD
                            || Math.abs(totalDeltaY) >= MOVE_THRESHOLD) {
                        mIsMoving = true
                        val windowMgr = mWeakRef.get()?.mWindowManager
                        val param = mWeakRef.get()?.mWindowParam
                        if (event.pointerCount == 1 && windowMgr != null) {
                            param!!.x -= deltaX
                            param!!.y -= deltaY
                            windowMgr.updateViewLayout(this, param)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> mIsMoving = false
            }
            return true
        }

        fun setRGBImageView(rgb: Bitmap?) {
            if (rgb != null && !rgb.isRecycled) {
                mColorView.setImageBitmap(rgb)
            }
        }
    }

    companion object {
        private const val TAG = "FloatingCameraWindow"
        private const val MOVE_THRESHOLD = 10
    }
}