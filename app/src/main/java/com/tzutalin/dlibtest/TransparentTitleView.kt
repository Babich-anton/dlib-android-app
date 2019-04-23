package com.tzutalin.dlibtest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View


class TransparentTitleView(context: Context, set: AttributeSet) : View(context, set) {
    private var mShowText: String? = null
    private val mTextSizePx: Float
    private val mFgPaint: Paint
    private val mBgPaint: Paint

    init {

        mTextSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics)
        mFgPaint = Paint()
        mFgPaint.textSize = mTextSizePx

        mBgPaint = Paint()
        mBgPaint.color = -0x33bd7a0c
    }

    fun setText(text: String) {
        this.mShowText = text
        postInvalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        val x = 10
        val y = (mFgPaint.textSize * 1.5f).toInt()

        canvas.drawPaint(mBgPaint)

        if (mShowText != null) {
            canvas.drawText(mShowText!!, x.toFloat(), y.toFloat(), mFgPaint)
        }
    }

    companion object {
        private const val TEXT_SIZE_DIP = 24f
    }
}