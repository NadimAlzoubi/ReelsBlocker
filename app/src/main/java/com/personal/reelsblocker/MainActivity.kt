package com.personal.reelsblocker

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var statusHint: TextView
    private lateinit var switchBlock: MaterialSwitch

    private val prefs by lazy {
        getSharedPreferences(ReelsBlockerService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        statusHint = findViewById(R.id.statusHint)
        switchBlock = findViewById(R.id.switchBlock)

        findViewById<Button>(R.id.btnOpenSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        // نحدّث المفتاح من الإعدادات المحفوظة بدون إطلاق المستمع
        switchBlock.setOnCheckedChangeListener(null)
        switchBlock.isChecked = prefs.getBoolean(ReelsBlockerService.KEY_ENABLED, true)
        switchBlock.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(ReelsBlockerService.KEY_ENABLED, checked).apply()
            updateStatus()
        }
        updateStatus()
    }

    private fun updateStatus() {
        val systemEnabled = isServiceEnabledInSystem()
        val switchOn = switchBlock.isChecked
        when {
            !systemEnabled -> {
                statusText.setText(R.string.status_off)
                statusHint.setText(R.string.hint_need_permission)
            }
            !switchOn -> {
                statusText.setText(R.string.status_off)
                statusHint.setText(R.string.hint_paused)
            }
            else -> {
                statusText.setText(R.string.status_on)
                statusHint.setText(R.string.hint_active)
            }
        }
    }

    /** هل فعّل المستخدم الخدمة من إعدادات إمكانية الوصول؟ */
    private fun isServiceEnabledInSystem(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                it.resolveInfo.serviceInfo.packageName == packageName &&
                    it.resolveInfo.serviceInfo.name == ReelsBlockerService::class.java.name
            }
    }
}
