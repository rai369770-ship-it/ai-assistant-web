package com.blindtechnexus.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class BlindTechNexusAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Reserved for accessibility helper automation in future updates.
    }

    override fun onInterrupt() {
        // No-op.
    }
}
