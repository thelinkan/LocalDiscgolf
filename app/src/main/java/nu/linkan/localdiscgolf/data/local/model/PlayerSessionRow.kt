package nu.linkan.localdiscgolf.data.local.model

data class PlayerSessionRow(
    val playSessionId: Long,
    val playerId: Long,
    val playerName: String?,
    val courseName: String,
    val layoutName: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String
)