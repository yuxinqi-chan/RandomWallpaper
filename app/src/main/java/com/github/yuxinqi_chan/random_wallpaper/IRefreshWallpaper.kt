package com.github.yuxinqi_chan.random_wallpaper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.core.graphics.scale
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random


interface IRefreshWallpaper {
    fun requireContext(): Context
    val wallpaperManager: WallpaperManager
        get() = WallpaperManager.getInstance(requireContext())
    val defaultSharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(requireContext())
    val apiUrl: String?
        get() = defaultSharedPreferences.getString(
            getString(R.string.api_url),
            getString(R.string.default_api_url)
        )
    val directoryUri: Uri
        get() = Uri.parse(
            defaultSharedPreferences.getString(
                getString(R.string.directory_uri),
                Uri.EMPTY.toString()
            )
        )

    val apiRandomSwitch: Boolean
        get() = defaultSharedPreferences.getBoolean(
            getString(R.string.api_random_switch),
            false
        )
    val directory_random_switch: Boolean
        get() = defaultSharedPreferences.getBoolean(
            getString(R.string.directory_random_switch),
            false
        )

    fun getString(@StringRes resId: Int): String
    fun refreshWallpaper() {
        Log.v(javaClass.simpleName, "refreshWallpaper")
        if (apiRandomSwitch && !directory_random_switch) {
            if (apiUrl != null && apiUrl!!.isNotEmpty()) {
                setWallPaperFromApi()
            }
        }
        if (!apiRandomSwitch && directory_random_switch) {
            setWallPaperFromDirectory()
        }
        if (apiRandomSwitch && directory_random_switch) {
            if (Random.nextBoolean()) {
                setWallPaperFromApi()
            } else {
                setWallPaperFromDirectory()
            }
        }
    }

    private fun setWallPaperFromDirectory() {
        val dir = DocumentFile.fromTreeUri(requireContext(), directoryUri)
        if (dir != null) {
            val images = getAllImages(dir, ArrayList())
            Log.v(javaClass.simpleName, "randomImageSize:${images.size}")
            if (images.isNotEmpty()) {
                val randomImage = images[Random.nextInt(images.size)]
                Log.v(javaClass.simpleName, "randomImage:${randomImage.uri}")
                setBitmap(
                    BitmapFactory.decodeStream(
                        requireContext().contentResolver.openInputStream(randomImage.uri)
                    )
                )
            }
        }
    }

    fun getAllImages(
        dir: DocumentFile,
        imageList: ArrayList<DocumentFile>
    ): ArrayList<DocumentFile> {
        val listFile = dir.listFiles()
        if (listFile.isNotEmpty()) {
            for (file in listFile) {
                if (file.isDirectory) {
                    getAllImages(file, imageList)
                } else {
                    val name = file.name
                    if (name != null && (name.endsWith(".png")
                                || name.endsWith(".jpg")
                                || name.endsWith(".jpeg")
                                || name.endsWith(".gif")
                                || name.endsWith(".bmp")
                                || name.endsWith(".webp"))
                    ) {
                        imageList.add(file)
                    }
                }
            }
        }
        return imageList
    }

    private fun setWallPaperFromApi() {
        val bitmap = getBitmapFromApi(apiUrl!!)
        saveBitmapFromApi(bitmap)
        setBitmap(bitmap)
    }

    private fun setBitmap(bitmap: Bitmap) {
        val height = wallpaperManager.desiredMinimumHeight
        val width = wallpaperManager.desiredMinimumWidth
        Log.v("wallpaper HW", "$height,$width")
        Log.v("bitmap HW", "${bitmap.height},${bitmap.width}")
//        val resWidth = (bitmap.height * width / height).coerceAtMost(bitmap.width)
//        val startX = (bitmap.width - resWidth) / 2
//        val resBitmap = Bitmap.createBitmap(bitmap, startX, 0, resWidth, bitmap.height)
        wallpaperManager.setBitmap(getScaledBitmap(bitmap))
    }

    private fun saveBitmapFromApi(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}${File.separator}${getString(R.string.app_name)}"
            )
        }

        val imageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        val fos = imageUri?.let { requireContext().contentResolver?.openOutputStream(it) }
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

    private fun getBitmapFromApi(string: String): Bitmap {
        val url = URL(string)
        Log.v(javaClass.simpleName, "request url:$url")
        val httpURLConnection: HttpURLConnection
        httpURLConnection = url.openConnection() as HttpURLConnection
        httpURLConnection.requestMethod = "GET"
        httpURLConnection.connect()
        if (httpURLConnection.responseCode == HttpURLConnection.HTTP_MOVED_PERM || httpURLConnection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            return getBitmapFromApi(httpURLConnection.getHeaderField("Location"))
        }
        val bitmap = BitmapFactory.decodeStream(httpURLConnection.inputStream)
        Log.v(javaClass.simpleName, "download wallpaper size:${bitmap.width}, ${bitmap.height}")
        httpURLConnection.disconnect()
        return bitmap
    }
}