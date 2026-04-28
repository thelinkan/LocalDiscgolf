package nu.linkan.localdiscgolf.data.local.model

data class PlayerHoleStatsRow(
    val courseId: Long,
    val courseName: String,
    val holeNumber: Int,
    val holeVariantId: Long?,
    val teeName: String?,
    val basketName: String?,
    val lengthMeters: Int,
    val parValue: Int,
    val timesPlayed: Int,
    val bestThrows: Int,
    val avgThrows: Double,
    val birdiesOrBetter: Int,
    val pars: Int,
    val bogeys: Int,
    val doubleBogeys: Int,
    val tripleBogeysOrWorse: Int
)