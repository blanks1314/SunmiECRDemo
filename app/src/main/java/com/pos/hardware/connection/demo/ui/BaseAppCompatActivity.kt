package com.pos.hardware.connection.demo.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity

open class BaseAppCompatActivity : AppCompatActivity() {

    fun showToast(text: String) {
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }

    fun showToast(@StringRes resId: Int) {
        runOnUiThread { Toast.makeText(this, resId, Toast.LENGTH_SHORT).show() }
    }

}