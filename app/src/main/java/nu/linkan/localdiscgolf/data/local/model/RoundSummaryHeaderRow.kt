package nu.linkan.localdiscgolf.data.local.model

data class RoundSummaryHeaderRow(
    val playSessionId: Long,
    val courseName: String,
    val layoutName: String?,
    val startedAt: Long
)