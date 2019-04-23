package com.tzutalin.dlib

import android.graphics.Bitmap
import android.util.Log

import androidx.annotation.Keep
import androidx.annotation.WorkerThread

import java.util.Arrays


class FaceDet {

    // accessed by native methods
    private val mNativeFaceDetContext: Long = 0
    private var mLandMarkPath = ""

    constructor() {
        jniInit(mLandMarkPath)
    }

    constructor(landMarkPath: String) {
        mLandMarkPath = landMarkPath
        jniInit(mLandMarkPath)
    }

    @WorkerThread
    fun detect(path: String): List<VisionDetRet>? {
        val detRets = jniDetect(path)
        return Arrays.asList(*detRets)
    }

    @WorkerThread
    fun detect(bitmap: Bitmap): List<VisionDetRet>? {
        val detRets = jniBitmapDetect(bitmap)
        return Arrays.asList(*detRets)
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        release()
    }

    fun release() {
        jniDeInit()
    }

    @Keep
    @Synchronized
    private external fun jniInit(landmarkModelPath: String): Int

    @Keep
    @Synchronized
    private external fun jniDeInit(): Int

    @Keep
    @Synchronized
    private external fun jniBitmapDetect(bitmap: Bitmap): Array<VisionDetRet>

    @Keep
    @Synchronized
    private external fun jniDetect(path: String): Array<VisionDetRet>

    companion object {
        private const val TAG = "dlib"

        init {
            try {
                System.loadLibrary("android_dlib")
                jniNativeClassInit()
                Log.d(TAG, "jniNativeClassInit success")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "library not found")
            }

        }

        @Keep
        private external fun jniNativeClassInit()
    }
}