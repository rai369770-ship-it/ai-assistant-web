package com.stoolkit.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service for BlindTech Nexus
 * This service allows the app to be listed in accessibility settings
 */
class BlindTechAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            // We don't need any specific event types for this basic service
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used - this is a placeholder service for listing in accessibility settings
    }

    override fun onInterrupt() {
        // Not used
    }
}
