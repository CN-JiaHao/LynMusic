package top.iwesley.lyn.music

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Android 端实现：播放页音量条直接读写系统「媒体」音量（STREAM_MUSIC）。
 * 系统会用 Settings 里的音量与外放/耳机一致，音量键改动也会同步回界面。
 */
class AndroidSystemVolumeController(
    context: Context,
) : SystemVolumeController {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    private val currentVolume = MutableStateFlow(readSystemVolume())

    override val isAvailable: Boolean = true

    override val volume: StateFlow<Float> get() = currentVolume

    init {
        scope.launch {
            while (isActive) {
                currentVolume.value = readSystemVolume()
                delay(400)
            }
        }
    }

    override fun setVolume(value: Float) {
        val target = (value.coerceIn(0f, 1f) * maxVolume).roundToInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        currentVolume.value = target.toFloat() / maxVolume.toFloat()
    }

    private fun readSystemVolume(): Float {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
    }
}
