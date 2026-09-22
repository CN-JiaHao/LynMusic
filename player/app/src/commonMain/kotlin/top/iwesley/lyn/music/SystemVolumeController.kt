package top.iwesley.lyn.music

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放页右下角音量条的控制目标。
 *
 * 默认实现只调整播放器内部音量（跨端通用）；Android 端通过
 * LocalSystemVolumeController 注入真正读写系统媒体音量的实现。
 */
interface SystemVolumeController {
    val isAvailable: Boolean
    val volume: StateFlow<Float>

    fun setVolume(value: Float)
}

object NoopSystemVolumeController : SystemVolumeController {
    override val isAvailable: Boolean = false
    override val volume: StateFlow<Float> = MutableStateFlow(1f)

    override fun setVolume(value: Float) = Unit
}

val LocalSystemVolumeController =
    staticCompositionLocalOf<SystemVolumeController> { NoopSystemVolumeController }
