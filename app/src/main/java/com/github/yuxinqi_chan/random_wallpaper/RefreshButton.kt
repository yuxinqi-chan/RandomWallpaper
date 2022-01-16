package com.github.yuxinqi_chan.random_wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.github.quadflask.smartcrop.Options
import com.github.quadflask.smartcrop.SmartCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class RefreshButton : Preference, IRefreshWallpaper, CoroutineScope {
    private val scope = MainScope()
    override val coroutineContext: CoroutineContext = scope.coroutineContext
    lateinit var fragment: Fragment

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    override fun onClick() {
        Log.v(javaClass.name, context.getString(R.string.refresh_now))
//        val randomImage = getRandomImage()
//        if (randomImage != null) {
//            val options = Options.newInstance()
//            val screenWidth = context.resources.displayMetrics.widthPixels
//            val screenHeight = context.resources.displayMetrics.heightPixels
//            val imageWidth = randomImage.width
//            val imageHeight = randomImage.height
//            Log.v(
//                javaClass.name,
//                "image: ${imageWidth},${imageHeight}"
//            )
//            val screenAspectRatio = screenWidth.toFloat() / screenHeight
//            val targetWidth: Int = if (imageHeight * screenAspectRatio > imageWidth) {
//                imageWidth
//            } else {
//                (imageHeight * screenAspectRatio).toInt()
//            }
//            val targetHeight: Int = (targetWidth / screenAspectRatio).toInt()
//            val analyse = SmartCrop.analyze(options, randomImage)
//        }
        isEnabled = false
        title = context.getString(R.string.refreshing)
        launch(Dispatchers.IO) {
            try {
                refreshWallpaper()
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    toast(e.message)
                }
            } finally {
                launch(Dispatchers.Main) {
                    isEnabled = true
                    title = context.getString(R.string.refresh_now)
                }
            }
        }
    }

    override fun requireContext(): Context {
        return context
    }
}