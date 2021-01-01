package com.github.yufield.randomwallpaper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.scale
import androidx.preference.PreferenceManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

interface IRefreshWallpaper {
    val context: Context
    val wallpaperManager: WallpaperManager
        get() = WallpaperManager.getInstance(context)
    val defaultSharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)
    val prefUrl: String?
        get() = defaultSharedPreferences.getString(
            context.getString(R.string.edit_text_preference_1),
            context.getString(R.string.default_url)
        )

    fun refreshWallpaper() {
        val bitmap = getBitmap(prefUrl!!)
        saveBitmap(bitmap)
        wallpaperManager.setBitmap(getScaledBitmap(bitmap))
        Log.v(javaClass.simpleName, "set bitmap as wallpaper")
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}${File.separator}${context.getString(R.string.app_name)}"
                )
            }
        }
        val imageUri =
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        val fos = imageUri?.let { context.contentResolver.openOutputStream(it) }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        Log.v(javaClass.simpleName, "saved bitmap ${imageUri?.path}")
    }

    private fun getScaledBitmap(bitmap: Bitmap): Bitmap {
        var scaledBitmap =
            bitmap.scale(
                bitmap.width * wallpaperManager.desiredMinimumHeight / bitmap.height,
                wallpaperManager.desiredMinimumHeight
            )
        if (scaledBitmap.width > wallpaperManager.desiredMinimumWidth) {
            val xPadding = (scaledBitmap.width - wallpaperManager.desiredMinimumWidth) / 2
            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap,
                xPadding,
                0,
                wallpaperManager.desiredMinimumWidth,
                wallpaperManager.desiredMinimumHeight
            )
        }
        return scaledBitmap
    }

    private fun getBitmap(string: String): Bitmap {
        val url = URL(string)
        Log.v(javaClass.simpleName, "request url:$url")
        val httpURLConnection: HttpURLConnection
        httpURLConnection = url.openConnection() as HttpURLConnection
        httpURLConnection.requestMethod = "GET"
        httpURLConnection.connect()
        if (httpURLConnection.responseCode == HttpURLConnection.HTTP_MOVED_PERM || httpURLConnection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            return getBitmap(httpURLConnection.getHeaderField("Location"))
        }
        val bitmap = BitmapFactory.decodeStream(httpURLConnection.inputStream)
        Log.v(javaClass.simpleName, "download wallpaper size:${bitmap.width}, ${bitmap.height}")
        httpURLConnection.disconnect()
        return bitmap
    }
}