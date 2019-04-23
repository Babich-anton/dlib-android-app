package com.tzutalin.dlib


import android.graphics.Point

import java.util.ArrayList

/**
 * A VisionDetRet contains all the information identifying the location and confidence value of the detected object in a bitmap.
 */
class VisionDetRet {
    /**
     * @return The label of the result
     */
    var mLabel: String = ""
    /**
     * @return A confidence factor between 0 and 1. This indicates how certain what has been found is actually the label.
     */
    var mConfidence: Float = 0.0f
    /**
     * @return The X coordinate of the mLeft side of the result
     */
    var mLeft: Int = 0
    /**
     * @return The Y coordinate of the mTop of the result
     */
    var mTop: Int = 0
    /**
     * @return The X coordinate of the mRight side of the result
     */
    var mRight: Int = 0
    /**
     * @return The Y coordinate of the mBottom of the result
     */
    var mBottom: Int = 0
    /**
     * Return the list of landmark points
     * @return ArrayList of android.graphics.Point
     */
    val mLandmarkPoints = ArrayList<Point>()

    internal constructor() {}

    /**
     * @param label      Label name
     * @param confidence A confidence factor between 0 and 1. This indicates how certain what has been found is actually the label.
     * @param l          The X coordinate of the mLeft side of the result
     * @param t          The Y coordinate of the mTop of the result
     * @param r          The X coordinate of the mRight side of the result
     * @param b          The Y coordinate of the mBottom of the result
     */
    constructor(label: String, confidence: Float, l: Int, t: Int, r: Int, b: Int) {
        this.mLabel = label
        this.mLeft = l
        this.mTop = t
        this.mRight = r
        this.mBottom = b
        this.mConfidence = confidence
    }

    /**
     * Add landmark to the list. Usually, call by jni
     * @param x Point x
     * @param y Point y
     * @return true if adding landmark successfully
     */
    fun addLandmark(x: Int, y: Int): Boolean {
        return mLandmarkPoints.add(Point(x, y))
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Left:")
        sb.append(mLeft)
        sb.append(", Top:")
        sb.append(mTop)
        sb.append(", Right:")
        sb.append(mRight)
        sb.append(", Bottom:")
        sb.append(mBottom)
        sb.append(", Label:")
        sb.append(mLabel)
        return sb.toString()
    }
}