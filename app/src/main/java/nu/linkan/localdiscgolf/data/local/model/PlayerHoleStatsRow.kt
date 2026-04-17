package nu.linkan.localdiscgolf.data.local.model

data class PlayerHoleStatsRow(
    val courseName: String,
    val holeNumber: Int,
    val timesPlayed: Int,
    val bestThrows: Int,
    val avgThrows: Double,
    val birdiesOrBetter: Int,
    val pars: Int,
    val bogeysOrWorse: Int
)