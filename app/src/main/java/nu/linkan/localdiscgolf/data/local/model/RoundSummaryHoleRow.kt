package nu.linkan.localdiscgolf.data.local.model

data class RoundSummaryHoleRow(
    val playerId: Long,
    val playerName: String?,
    val startOrder: Int?,
    val sequenceNumber: Int,
    val holeVariantId: Long?,
    val holeNumberSnapshot: Int,
    val teeNameSnapshot: String?,
    val basketNameSnapshot: String?,
    val lengthSnapshotMeters: Int,
    val parSnapshot: Int,
    val throwsCount: Int?
)