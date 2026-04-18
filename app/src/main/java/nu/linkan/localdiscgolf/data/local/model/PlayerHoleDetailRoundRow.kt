package nu.linkan.localdiscgolf.data.local.model

data class PlayerHoleDetailRoundRow(
    val playSessionId: Long,
    val startedAt: Long,
    val courseId: Long,
    val courseName: String,
    val layoutName: String?,
    val holeNumber: Int,
    val parSnapshot: Int,
    val throwsCount: Int
)