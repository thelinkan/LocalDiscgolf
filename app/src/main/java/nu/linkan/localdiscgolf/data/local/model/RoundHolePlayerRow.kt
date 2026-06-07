package nu.linkan.localdiscgolf.data.local.model

data class RoundHolePlayerRow(
    val sessionPlayerHoleId: Long,
    val sessionPlayerId: Long,
    val playerId: Long,
    val playerName: String?,
    val sequenceNumber: Int,
    val holeId: Long,
    val holeVariantId: Long?,
    val holeNumberSnapshot: Int,
    val holeNameSnapshot: String?,
    val teeNameSnapshot: String?,
    val basketNameSnapshot: String?,
    val lengthSnapshotMeters: Int,
    val parSnapshot: Int,
    val throwsCount: Int?,

    val serverPlayerId: Long?,
    val serverCourseId: Long?,
    val serverHoleVariantId: Long?,

    val previousThrowsTotal: Int,
    val previousRelativeToPar: Int
)