package com.tzutalin.dlib

import android.graphics.Bitmap
import android.util.Log

import androidx.annotation.Keep
import androidx.annotation.WorkerThread

import java.util.Arrays


class PedestrianDet {

    // accessed by native methods
    private val mNativeDetContext: Long = 0

    init {
        jniInit()
    }

    @WorkerThread
    fun detect(bitmap: Bitmap): List<VisionDetRet>? {
        val detRets = jniBitmapDetect(bitmap)
        return Arrays.asList(*detRets)
    }

    @WorkerThread
    fun detect(path: String): List<VisionDetRet>? {
        val detRets = jniDetect(path)
        return Arrays.asList(*detRets)
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        release()
    }

    private fun release() {
        jniDeInit()
    }

    @Keep
    private external fun jniInit(): Int

    @Keep
    @Synchronized
    private external fun jniDeInit(): Int

    @Keep
    @Synchronized
    private external fun jniDetect(path: String): Array<VisionDetRet>

    @Keep
    @Synchronized
    private external fun jniBitmapDetect(bitmap: Bitmap): Array<VisionDetRet>

    companion object {
        private const val TAG = "dlib"

        init {
            try {
                System.loadLibrary("android_dlib")
                Log.d(TAG, "jniNativeClassInit success")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "library not found!")
            }

        }
    }
}