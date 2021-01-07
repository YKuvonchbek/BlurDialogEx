package com.example.blurdialogex

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.blurdialogex.databinding.ActivityMainBinding
import com.example.blurdialogex.lib.Utilities
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    companion object {
        init {
            System.loadLibrary("image")
        }
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var lastInsets: WindowInsets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setInsets()

        binding.button.setOnClickListener {
            val dialog = MyDialog(this, binding.root, getLeftInset())
            dialog.show()
        }
    }



    private fun setInsets() {

        if (Build.VERSION.SDK_INT >= 21) {
            binding.root.setOnApplyWindowInsetsListener(View.OnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
                lastInsets = insets
                v.requestLayout()
                insets.consumeSystemWindowInsets()
            })


        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastInsets = window.decorView.rootWindowInsets
    }

    private fun doIt() {
        val fragmentView = binding.parentview
        val w = (fragmentView.measuredWidth / 6.0f)
        val h = (fragmentView.measuredHeight / 6.0f)
        val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        canvas.scale(1.0f / 6.0f, 1.0f / 6.0f)
        fragmentView.draw(canvas)
        canvas.translate(binding.root.left.toFloat() - getLeftInset(), 0f)
        binding.root.draw(canvas)
        Utilities.stackBlurBitmap(bitmap, Math.max(7, Math.max(w.toInt(), h.toInt()) / 180))

        binding.root.background = BitmapDrawable(resources, bitmap)
    }

    private fun getLeftInset(): Int {
        return if (lastInsets != null && Build.VERSION.SDK_INT >= 21) {
            lastInsets.systemWindowInsetLeft
        } else 0
    }




    fun blurWallpaper(src: Bitmap?): Bitmap? {
        if (src == null) {
            return null
        }

        val b = if (src.height > src.width) {
            Bitmap.createBitmap(
                (450f * src.width / src.height).roundToInt(),
                450,
                Bitmap.Config.ARGB_8888
            )
        } else {
            Bitmap.createBitmap(
                450,
                (450f * src.height / src.width).roundToInt(),
                Bitmap.Config.ARGB_8888
            )
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val rect = Rect(0, 0, b.width, b.height)
        Canvas(b).drawBitmap(src, null, rect, paint)
        Utilities.stackBlurBitmap(b, 12)
        return b
    }
}