package nu.linkan.localdiscgolf.data.local.model

data class RoundSummaryHoleRow(
    val playerId: Long,
    val playerName: String?,
    val startOrder: Int?,
    val sequenceNumber: Int,
    val holeNumberSnapshot: Int,
    val parSnapshot: Int,
    val throwsCount: Int?
)