package nu.linkan.localdiscgolf.data.local.model

data class PlayerSessionRow(
    val playSessionId: Long,
    val courseName: String,
    val layoutName: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val playerCount: Int,
    val totalThrows: Int?,
    val totalRelativeToPar: Int?
)