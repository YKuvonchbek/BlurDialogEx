package com.example.blurdialogex

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.blurdialogex.databinding.BlurDialogLayoutBinding
import com.example.blurdialogex.lib.Utilities

class MyDialog(
    context: Context,
    private val containerview: View,
    private val leftInset: Int
):
    Dialog(context, R.style.FullScreenDialogStyle) {

    private val binding = BlurDialogLayoutBinding.inflate(LayoutInflater.from(context))
    private var blurredAnimationInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding.blurredView.alpha = 0f
        binding.blurredView.visibility = View.INVISIBLE
        setContentView(binding.root)
        startBlur()
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        super.setOnDismissListener(listener)
    }

    private fun startBlur() {
        if(binding.blurredView.visibility == View.VISIBLE || blurredAnimationInProgress) return

        binding.blurredView.tag = 1
        val w = (containerview.measuredWidth / 6.0f)
        val h = (containerview.measuredHeight / 6.0f)
        val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(1.0f / 6.0f, 1.0f / 6.0f)
        containerview.draw(canvas)
        canvas.translate((binding.root.left - leftInset).toFloat(), 0f)
        binding.root.draw(canvas)
        Utilities.stackBlurBitmap(bitmap, Math.max(7f, Math.max(w, h) / 180).toInt())
        binding.blurredView.background = BitmapDrawable(context.resources, bitmap)
        binding.blurredView.visibility = View.VISIBLE
//        binding.blurredView.animate().alpha(1.0f).setDuration(180)
//            .setListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator) {
//                    blurredAnimationInProgress = false
//                }
//            }).start()

        binding.cover.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
    }

}