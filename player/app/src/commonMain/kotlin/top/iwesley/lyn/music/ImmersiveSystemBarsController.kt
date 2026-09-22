package top.iwesley.lyn.music

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全屏歌词（纯净模式）的沉浸式系统栏控制。
 *
 * active = true 时隐藏状态栏与导航栏（从屏幕边缘轻扫可临时唤出，松手自动隐藏）；
 * active = false 时恢复显示。非 Android 端使用默认空实现，无副作用。
 */
fun interface ImmersiveSystemBarsController {
    fun setImmersive(active: Boolean)
}

val LocalImmersiveSystemBarsController =
    staticCompositionLocalOf<ImmersiveSystemBarsController> { ImmersiveSystemBarsController { } }
