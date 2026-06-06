package nu.linkan.localdiscgolf.data.local.model

data class LocalResumeRoundListItem(
    val id: Long,
    val serverId: Long?,
    val courseName: String,
    val startedAt: Long,
    val syncStatus: String,
    val currentSequenceNumber: Int?,
    val hasDirtyHoles: Boolean
)