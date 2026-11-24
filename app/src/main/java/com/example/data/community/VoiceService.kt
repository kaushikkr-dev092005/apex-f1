package com.example.data.community

/** Production implementations must use an authorized SFU/WebRTC service with backend-issued room tokens. */
interface VoiceService {
    val configured: Boolean
    suspend fun join(roomId: String): Result<Unit>
    suspend fun setMicrophoneEnabled(enabled: Boolean): Result<Unit>
    suspend fun leave()
}

class UnconfiguredVoiceService : VoiceService {
    override val configured = false
    override suspend fun join(roomId: String) = Result.failure<Unit>(IllegalStateException("Voice provider is not configured. This room is a local preview."))
    override suspend fun setMicrophoneEnabled(enabled: Boolean) = Result.failure<Unit>(IllegalStateException("Microphone is unavailable in room preview."))
    override suspend fun leave() = Unit
}
