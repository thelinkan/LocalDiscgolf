package nu.linkan.localdiscgolf.data.local.model

data class PendingRoundForSync(
    val id: Long,
    val serverId: Long?,
    val courseId: Long,
    val serverCourseId: Long?,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val syncStatus: String,
    val currentSequenceNumber: Int?
)

data class RoundSyncPlayer(
    val id: Long,
    val playSessionId: Long,
    val playerId: Long,
    val serverId: Long?,
    val serverPlayerId: Long,
    val layoutId: Long,
    val serverLayoutId: Long,
    val displayName: String,
    val startOrder: Int,
    val approvalRequired: Boolean,
    val approvalState: String
)

data class RoundSyncHole(
    val id: Long,
    val sessionPlayerId: Long,
    val sequenceNumber: Int,
    val serverId: Long?,
    val serverSessionPlayerId: Long?,
    val serverHoleId: Long,
    val serverHoleVariantId: Long?,
    val throwsCount: Int?,
    val dirty: Boolean
)

data class ServerSessionPlayerIdMapping(
    val localSessionPlayerId: Long,
    val serverSessionPlayerId: Long
)

data class ServerSessionPlayerHoleIdMapping(
    val localSessionPlayerHoleId: Long,
    val serverSessionPlayerHoleId: Long,
    val serverSessionPlayerId: Long
)