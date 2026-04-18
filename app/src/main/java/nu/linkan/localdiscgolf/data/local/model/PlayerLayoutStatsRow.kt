package nu.linkan.localdiscgolf.data.local.model

data class PlayerLayoutStatsRow(
    val courseId: Long,
    val courseName: String,
    val layoutName: String,
    val roundsPlayed: Int,
    val bestThrows: Int,
    val avgThrows: Double,
    val bestRelativeToPar: Int,
    val avgRelativeToPar: Double
)