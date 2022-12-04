package lerpmusic.btle.domain.note

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val channel: Int,
    val pitch: Int,
)
