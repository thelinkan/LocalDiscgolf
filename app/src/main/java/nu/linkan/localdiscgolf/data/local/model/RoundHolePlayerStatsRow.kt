package nu.linkan.localdiscgolf.data.local.model

data class RoundHolePlayerStatsRow(
    val playerId: Long,
    val holeId: Long,
    val timesPlayed: Int,
    val bestThrows: Int,
    val avgThrows: Double,
    val birdiesOrBetter: Int,
    val pars: Int,
    val bogeys: Int,
    val doubleBogeys: Int,
    val tripleBogeysOrWorse: Int
)