package com.personal.reelsblocker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * خدمة إمكانية وصول تغلق الريلز في إنستغرام وفيسبوك بتنفيذ "رجوع" فور اكتشافها.
 *
 * ⚠️ ميتا تغيّر المعرفات (View IDs) مع التحديثات. إذا توقف الحجب:
 *   1) غيّر DEBUG_LOG_VIEW_IDS إلى true
 *   2) افتح الريلز في التطبيق وراقب Logcat بالفلتر: ReelsBlocker
 *   3) انسخ المعرف الجديد وأضفه للقوائم أدناه.
 */
class ReelsBlockerService : AccessibilityService() {

    companion object {
        const val PREFS_NAME = "reels_blocker_prefs"
        const val KEY_ENABLED = "blocking_enabled"

        private const val TAG = "ReelsBlocker"
        private const val PKG_INSTAGRAM = "com.instagram.android"
        private const val PKG_FACEBOOK = "com.facebook.katana"

        /** فعّلها لطباعة معرفات الواجهة في Logcat وإيجاد المعرفات الجديدة */
        private const val DEBUG_LOG_VIEW_IDS = false

        // ───────────── ✏️ عدّل هنا: معرفات إنستغرام ─────────────
        // أي عنصر ظاهر يحتوي معرّفه على أحد هذه النصوص = شاشة الريلز مفتوحة.
        private val INSTAGRAM_REELS_VIEW_IDS = listOf(
            "clips_viewer_view_pager",
            "clips_viewer_fragment",
            "clips_viewer_container",
            "clips_viewer"
        )
        // تبويب الريلز في الشريط السفلي (يُحجب عندما يكون محدداً Selected)
        private val INSTAGRAM_REELS_TAB_IDS = listOf("clips_tab")

        // ───────────── ✏️ عدّل هنا: فيسبوك ─────────────
        // فيسبوك يشفّر معظم المعرفات، لذا نعتمد أيضاً على النصوص/الوصف.
        private val FACEBOOK_REELS_VIEW_IDS = listOf(
            "reels_viewer", "reel_viewer", "fb_reels"
        )
        // كلمات تبحث عنها في text/contentDescription للعنصر "المحدد" (تبويب الريلز النشط)
        private val FACEBOOK_REELS_KEYWORDS = listOf("reels", "reel", "ريلز", "ريل")

        private const val SCAN_THROTTLE_MS = 250L   // أقل فاصل بين فحصين (لتوفير البطارية)
        private const val BACK_COOLDOWN_MS = 1200L  // بعد الرجوع ننتظر لئلا نضغط رجوع مرتين ونخرج من التطبيق
        private const val MAX_DEPTH = 40
    }

    private var lastScan = 0L
    private var lastBack = 0L

    private val blockingEnabled: Boolean
        get() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg != PKG_INSTAGRAM && pkg != PKG_FACEBOOK) return
        if (!blockingEnabled) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastBack < BACK_COOLDOWN_MS) return
        if (now - lastScan < SCAN_THROTTLE_MS) return
        lastScan = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        if (DEBUG_LOG_VIEW_IDS) dumpTree(root, 0)

        val inReels = when (pkg) {
            PKG_INSTAGRAM -> isInstagramReels(root)
            else -> isFacebookReels(root)
        }

        if (inReels) {
            lastBack = now
            Log.d(TAG, "Reels detected in $pkg -> BACK")
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() {}

    // ───────────────────────── الكشف ─────────────────────────

    private fun isInstagramReels(root: AccessibilityNodeInfo): Boolean =
        anyNode(root) { n ->
            val id = n.viewIdResourceName ?: return@anyNode false
            INSTAGRAM_REELS_VIEW_IDS.any { id.contains(it) } ||
                (n.isSelected && INSTAGRAM_REELS_TAB_IDS.any { id.contains(it) })
        }

    private fun isFacebookReels(root: AccessibilityNodeInfo): Boolean =
        anyNode(root) { n ->
            val id = n.viewIdResourceName
            if (id != null && FACEBOOK_REELS_VIEW_IDS.any { id.contains(it) }) return@anyNode true
            // تبويب الريلز النشط (محدد) حسب النص أو الوصف
            if (n.isSelected) {
                val label = "${n.text ?: ""} ${n.contentDescription ?: ""}".lowercase()
                FACEBOOK_REELS_KEYWORDS.any { label.contains(it) }
            } else false
        }

    /** بحث DFS مع حد أقصى للعمق */
    private fun anyNode(
        node: AccessibilityNodeInfo?,
        depth: Int = 0,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        if (node == null || depth > MAX_DEPTH) return false
        if (predicate(node)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (anyNode(child, depth + 1, predicate)) return true
        }
        return false
    }

    /** وضع التصحيح: يطبع شجرة العناصر في Logcat */
    private fun dumpTree(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null || depth > MAX_DEPTH) return
        Log.d(
            TAG,
            "${"  ".repeat(depth)}id=${node.viewIdResourceName} cls=${node.className} " +
                "text=${node.text} desc=${node.contentDescription} selected=${node.isSelected}"
        )
        for (i in 0 until node.childCount) dumpTree(node.getChild(i), depth + 1)
    }
}
