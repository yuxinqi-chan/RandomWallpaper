package com.github.yufield.randomwallpaper

import android.content.Context
import android.util.AttributeSet
import com.takisoft.preferencex.EditTextPreference

class EditTextPreference : EditTextPreference {
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    override fun getSummary(): CharSequence {
        return if (text == "0" || text == "") "已禁用" else text
    }
}