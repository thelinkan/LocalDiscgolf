package nu.linkan.localdiscgolf.data.local.model

data class RoundHolePlayerRow(
    val sessionPlayerHoleId: Long,
    val sessionPlayerId: Long,
    val playerId: Long,
    val playerName: String?,
    val sequenceNumber: Int,
    val holeId: Long,
    val holeNumberSnapshot: Int,
    val holeNameSnapshot: String?,
    val lengthSnapshotMeters: Int,
    val parSnapshot: Int,
    val throwsCount: Int?
)