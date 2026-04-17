package nu.linkan.localdiscgolf.data.local.model

data class InProgressSessionRow(
    val playSessionId: Long,
    val courseId: Long,
    val courseName: String,
    val startedAt: Long
)