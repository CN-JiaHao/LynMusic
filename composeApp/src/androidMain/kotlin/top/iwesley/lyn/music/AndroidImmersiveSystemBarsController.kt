package top.iwesley.lyn.music

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Android 端实现：直接驱动 InsetsController 隐藏/恢复状态栏与导航栏。
 *
 * 当前状态记录在 pendingImmersive，转屏 / 回前台时由 MainActivity 重新应用，
 * 防止部分 ROM 在配置变化后把系统栏又放出来。
 * 隐藏模式为 BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE：边缘轻扫临时显示，松手自动隐藏。
 */
class AndroidImmersiveSystemBarsController(
    private val activity: Activity,
) : ImmersiveSystemBarsController {
    internal var pendingImmersive = false

    override fun setImmersive(active: Boolean) {
        pendingImmersive = active
        apply()
    }

    internal fun apply() {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (pendingImmersive) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
