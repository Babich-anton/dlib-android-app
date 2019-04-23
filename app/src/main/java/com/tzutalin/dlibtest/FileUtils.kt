package com.tzutalin.dlibtest

import android.content.Context
import androidx.annotation.RawRes

import java.io.FileOutputStream
import java.io.IOException


internal object FileUtils {
    fun copyFileFromRawToOthers(context: Context, @RawRes id: Int, targetPath: String) {
        val `in` = context.resources.openRawResource(id)
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(targetPath)
            out.write(`in`.readBytes())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                `in`.close()
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }
}