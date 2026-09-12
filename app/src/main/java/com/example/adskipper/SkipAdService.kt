package com.example.adskipper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipAdService : AccessibilityService() {

    companion object {
        private const val TAG = "SkipAdService"

        private val SKIP_TEXTS = listOf("skip", "skip ad", "skip ads")

        private const val SCAN_THROTTLE_MS = 200L
        private const val CLICK_DEBOUNCE_MS = 1500L
        private const val MAX_ANCESTOR_HOPS = 3
    }

    private var lastScan = 0L
    private var lastClick = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val now = System.currentTimeMillis()
        if (now - lastScan < SCAN_THROTTLE_MS) return
        lastScan = now
        if (now - lastClick < CLICK_DEBOUNCE_MS) return

        val root = rootInActiveWindow ?: return
        if (findAndClickSkip(root)) {
            lastClick = now
            Log.d(TAG, "Skip button clicked")
            recordSkip()
        }
    }

    /** Silently counts the skip and remembers when tracking started. */
    private fun recordSkip() {
        val prefs = getSharedPreferences("stats", Context.MODE_PRIVATE)
        val count = prefs.getInt("ads_skipped", 0) + 1
        val editor = prefs.edit()
        editor.putInt("ads_skipped", count)
        if (!prefs.contains("first_skip")) {
            editor.putLong("first_skip", System.currentTimeMillis())
        }
        editor.apply()
    }

    private fun findAndClickSkip(root: AccessibilityNodeInfo): Boolean {
        val ids = listOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/skip_ad_button_text",
            "com.google.android.youtube:id/skip_button"
        )
        for (id in ids) {
            val node = root.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()
            if (node != null && click(node)) return true
        }
        return searchForSkip(root, 0)
    }

    private fun searchForSkip(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 50) return false

        val text = node.text?.toString()?.trim()?.lowercase()
        val desc = node.contentDescription?.toString()?.trim()?.lowercase()
        if (text in SKIP_TEXTS || desc in SKIP_TEXTS) {
            if (click(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (searchForSkip(child, depth + 1)) return true
        }
        return false
    }

    private fun click(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var hops = 0
        while (current != null && hops <= MAX_ANCESTOR_HOPS) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            current = current.parent
            hops++
        }
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            val path = Path().apply { moveTo(bounds.centerX().toFloat(), bounds.centerY().toFloat()) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
                .build()
            return dispatchGesture(gesture, null, null)
        }
        return false
    }

    override fun onInterrupt() { /* nothing to do */ }
}