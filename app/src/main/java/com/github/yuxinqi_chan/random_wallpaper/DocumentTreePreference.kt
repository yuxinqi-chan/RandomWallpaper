package com.github.yuxinqi_chan.random_wallpaper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.preference.EditTextPreference

class DocumentTreePreference : EditTextPreference {
    private lateinit var launcher: ActivityResultLauncher<Uri>

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

    override fun getSummary(): CharSequence {
        return Uri.parse(text).path ?: super.getSummary()
    }

    override fun onClick() {
        launcher.launch(null)
    }

    fun registerForActivityResult(fragment: Fragment) {
        launcher =
            fragment.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    /*永久获取目录权限*/
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    text = uri.toString()
                }
            }
    }
}