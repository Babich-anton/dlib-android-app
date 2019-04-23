package com.tzutalin.dlib

import android.os.Environment


object Constants {
    /**
     * getFaceShapeModelPath
     * @return default face shape model path
     */
    val faceShapeModelPath: String
        get() = Environment.getExternalStorageDirectory().absolutePath + "/shape_predictor_68_face_landmarks.dat"
}