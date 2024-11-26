package com.pos.hardware.connection.demo.ui;

import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
/**
 * @author: Dadong
 * @date: 2024/11/21
 */


public class BaseAppCompatActivity extends AppCompatActivity {

    public void showToast(String text) {
        runOnUiThread(() -> Toast.makeText(BaseAppCompatActivity.this, text, Toast.LENGTH_SHORT).show());
    }

    public void showToast(@StringRes int resId) {
        runOnUiThread(() -> Toast.makeText(BaseAppCompatActivity.this, resId, Toast.LENGTH_SHORT).show());
    }
}
