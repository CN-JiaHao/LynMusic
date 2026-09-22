package top.iwesley.lyn.music

import kotlin.math.abs
import top.iwesley.lyn.music.core.model.LyricsDocument
import top.iwesley.lyn.music.core.model.LyricsLine
import top.iwesley.lyn.music.domain.EnhancedLyricsDisplayLine
import top.iwesley.lyn.music.domain.EnhancedLyricsPresentation
import top.iwesley.lyn.music.feature.player.isPlayerLyricsStructureTagLine

internal const val PLAYER_LYRICS_SCROLL_ANIMATION_DURATION_MS = 420
private const val PLAYER_LYRICS_SMOOTH_SCROLL_MAX_INDEX_DISTANCE = 2

internal data class VisiblePlayerLyricsLine(
    val rawIndex: Int,
    val line: LyricsLine,
    val enhancedLine: EnhancedLyricsDisplayLine? = null,
    val translationLine: LyricsLine? = null,
)

internal data class PlayerLyricsVisibleItemInfo(
    val index: Int,
    val offset: Int,
    val size: Int,
)

internal fun buildVisiblePlayerLyricsLines(
    lyrics: LyricsDocument,
    enhancedLyricsPresentation: EnhancedLyricsPresentation? = null,
): List<VisiblePlayerLyricsLine> {
    val rawLines = lyrics.lines.mapIndexedNotNull { rawIndex, line ->
        if (isPlayerLyricsStructureTagLine(line.text)) {
            return@mapIndexedNotNull null
        }
        VisiblePlayerLyricsLine(
            rawIndex = rawIndex,
            line = line,
            enhancedLine = enhancedLyricsPresentation?.lines?.getOrNull(rawIndex),
        )
    }
    return mergeBilingualPlayerLyricsLines(rawLines)
}

/**
 * 定制改动：把「时间戳完全相同的相邻两行」合并成「原文 + 译文」一组。
 *
 * 自定义双语 LRC 的常见写法是同一时间戳写两行，第一行原文、第二行译文：
 *   [00:16.24]You can't catch me boy
 *   [00:16.24]你追不上我，小子
 *
 * 原逻辑逐行映射，这两行会被当成两句独立歌词：各自占一行、字号一致、
 * 高亮也各自独立（唱到这句时只有其中一行亮），完全看不出原文与译文的主次。
 *
 * 合并条件（三条同时满足才合并，避免误伤）：
 * 1. 相邻两行都有时间戳，且时间戳完全相等；
 * 2. 前一行没有自带译文（增强歌词自带译文时以其为准，不覆盖）；
 * 3. 后一行文本非空。
 *
 * rawIndex 保留原始下标，滚动与高亮的索引换算不受影响。
 */
private fun mergeBilingualPlayerLyricsLines(
    lines: List<VisiblePlayerLyricsLine>,
): List<VisiblePlayerLyricsLine> {
    if (lines.size < 2) return lines
    val merged = ArrayList<VisiblePlayerLyricsLine>(lines.size)
    var index = 0
    while (index < lines.size) {
        val current = lines[index]
        val next = lines.getOrNull(index + 1)
        val currentTimestamp = current.line.timestampMs
        val canMerge = currentTimestamp != null &&
            next != null &&
            next.line.timestampMs == currentTimestamp &&
            current.enhancedLine?.translationText.isNullOrBlank() &&
            next.line.text.isNotBlank()
        if (canMerge) {
            merged.add(current.copy(translationLine = next!!.line))
            index += 2
        } else {
            merged.add(current)
            index += 1
        }
    }
    return merged
}

internal fun shouldShowPlayerLyricsEmptyState(
    isLyricsLoading: Boolean,
    hasLyricsLookupCompleted: Boolean,
    lyrics: LyricsDocument?,
    visibleLines: List<VisiblePlayerLyricsLine>,
): Boolean {
    return !isLyricsLoading &&
        hasLyricsLookupCompleted &&
        (lyrics == null || visibleLines.isEmpty())
}

internal fun resolveVisiblePlayerLyricsHighlightedIndex(
    visibleLines: List<VisiblePlayerLyricsLine>,
    highlightedRawIndex: Int,
): Int {
    if (visibleLines.isEmpty() || highlightedRawIndex < 0) return -1
    visibleLines.indexOfFirst { it.rawIndex == highlightedRawIndex }
        .takeIf { it >= 0 }
        ?.let { return it }
    visibleLines.indexOfFirst { it.rawIndex > highlightedRawIndex }
        .takeIf { it >= 0 }
        ?.let { return it }
    return visibleLines.indexOfLast { it.rawIndex < highlightedRawIndex }
}

internal fun resolveVisiblePlayerLyricsScrollTarget(
    lyrics: LyricsDocument?,
    visibleLines: List<VisiblePlayerLyricsLine>,
    highlightedRawIndex: Int,
): Int? {
    if (lyrics == null || visibleLines.isEmpty()) return null
    return when (val highlightedVisibleIndex = resolveVisiblePlayerLyricsHighlightedIndex(visibleLines, highlightedRawIndex)) {
        in visibleLines.indices -> highlightedVisibleIndex
        else -> if (lyrics.isSynced) 0 else null
    }
}

internal fun shouldAnimatePlayerLyricsScroll(
    previousTargetIndex: Int?,
    targetIndex: Int,
    isTargetVisible: Boolean,
): Boolean {
    return previousTargetIndex != null &&
        isTargetVisible &&
        abs(targetIndex - previousTargetIndex) <= PLAYER_LYRICS_SMOOTH_SCROLL_MAX_INDEX_DISTANCE
}

internal fun resolvePlayerLyricsBrowseTargetIndex(
    visibleLines: List<VisiblePlayerLyricsLine>,
    visibleItems: List<PlayerLyricsVisibleItemInfo>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Int? {
    if (visibleLines.isEmpty() || visibleItems.isEmpty()) return null
    val viewportCenter = (viewportStartOffset + viewportEndOffset) / 2
    return visibleItems
        .filter { item -> visibleLines.getOrNull(item.index)?.line?.timestampMs != null }
        .minByOrNull { item ->
            val itemCenter = item.offset + item.size / 2
            abs(itemCenter - viewportCenter)
        }
        ?.index
}

internal fun resolvePlayerLyricsSeekPositionMs(
    line: VisiblePlayerLyricsLine?,
    lyricsOffsetMs: Long,
    durationMs: Long,
): Long? {
    val timestampMs = line?.line?.timestampMs ?: return null
    return (timestampMs - lyricsOffsetMs).coerceIn(0L, durationMs.coerceAtLeast(0L))
}

internal fun resolvePlayerLyricsActiveHighlightedIndex(
    visibleLines: List<VisiblePlayerLyricsLine>,
    playbackHighlightedIndex: Int,
    browseTargetIndex: Int?,
    isBrowsing: Boolean,
): Int {
    if (!isBrowsing) return playbackHighlightedIndex
    val targetIndex = browseTargetIndex ?: return playbackHighlightedIndex
    return if (visibleLines.getOrNull(targetIndex)?.line?.timestampMs != null) {
        targetIndex
    } else {
        playbackHighlightedIndex
    }
}

internal fun resolveVisiblePlayerLyricsSelectedIndices(
    visibleLines: List<VisiblePlayerLyricsLine>,
    selectedRawIndices: Set<Int>,
): Set<Int> {
    if (visibleLines.isEmpty() || selectedRawIndices.isEmpty()) return emptySet()
    return visibleLines.mapIndexedNotNull { visibleIndex, line ->
        visibleIndex.takeIf { line.rawIndex in selectedRawIndices }
    }.toSet()
}
