package com.example.adskipper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipAdService : AccessibilityService() {

    companion object {
        private const val TAG = "SkipAdService"

        // Exact texts of the skip button – add your phone's language if needed
        private val SKIP_TEXTS = listOf("skip", "skip ad", "skip ads")

        private const val SCAN_THROTTLE_MS = 200L   // min gap between UI scans
        private const val CLICK_DEBOUNCE_MS = 1500L  // min gap between clicks
        private const val MAX_ANCESTOR_HOPS = 3      // how far up to find the clickable parent
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
        }
    }

    private fun findAndClickSkip(root: AccessibilityNodeInfo): Boolean {
        // Fast path: known YouTube view IDs (language independent)
        val ids = listOf(
            "com.google.android.youtube:id/skip_ad_button",
            "com.google.android.youtube:id/skip_ad_button_text",
            "com.google.android.youtube:id/skip_button"
        )
        for (id in ids) {
            val node = root.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()
            if (node != null && click(node)) return true
        }
        // Fallback: walk the view tree and match the button text
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

    /** Clicks the node, or its nearest clickable ancestor. Falls back to a synthetic tap. */
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
        // Some views ignore ACTION_CLICK – physically tap the centre instead
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