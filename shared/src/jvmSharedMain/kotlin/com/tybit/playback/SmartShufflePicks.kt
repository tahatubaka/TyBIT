package com.tybit.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The recommendations Smart Shuffle slipped into the playing queue, by video
 * id, so the queue can mark them ✦. Process-local: a queue restored after the
 * app was killed simply loses the marks, which is all they are.
 */
object SmartShufflePicks {
    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    @Volatile private var pending: Set<String> = emptySet()

    /** The picks of the queue about to be started. */
    fun forNextQueue(ids: Set<String>) {
        pending = ids
    }

    /** Called as any queue starts: it carries the picks asked for it, or none. */
    fun startQueue() {
        _ids.value = pending
        pending = emptySet()
    }
}
