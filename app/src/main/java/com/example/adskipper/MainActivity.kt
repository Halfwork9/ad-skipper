package com.example.adskipper

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        // Average time you avoid per skipped ad (watch ~5s of a ~20s+ ad)
        private const val EST_SECONDS_PER_AD = 15
        private val COLOR_ON = Color.parseColor("#4ADE80")
        private val COLOR_OFF = Color.parseColor("#F87171")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnOpenAccessibilitySettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<TextView>(R.id.tvReset).setOnClickListener {
            getSharedPreferences("stats", Context.MODE_PRIVATE).edit().clear().apply()
            refreshStats()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val prefs = getSharedPreferences("stats", Context.MODE_PRIVATE)
        val count = prefs.getInt("ads_skipped", 0)

        findViewById<TextView>(R.id.tvCount).text = count.toString()
        findViewById<TextView>(R.id.tvTimeSaved).text = formatDuration(count * EST_SECONDS_PER_AD)

        val enabled = isServiceEnabled()
        findViewById<TextView>(R.id.tvStatus).apply {
            text = if (enabled) "Service running" else "Service disabled"
            setTextColor(if (enabled) COLOR_ON else COLOR_OFF)
        }
        findViewById<TextView>(R.id.tvDot).setTextColor(if (enabled) COLOR_ON else COLOR_OFF)

        val first = prefs.getLong("first_skip", 0L)
        findViewById<TextView>(R.id.tvSince).text = if (first != 0L)
            "Tracking since " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(first))
        else
            "Stats appear after your first skipped ad"
    }

    private fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds == 0) return "0s"
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    private fun isServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val info = it.resolveInfo.serviceInfo
                info.packageName == packageName && info.name == SkipAdService::class.java.name
            }
    }
}