package nu.linkan.localdiscgolf.data.local.model

data class HoleVariantWithNames(
    val id: Long,
    val holeId: Long,
    val teeId: Long,
    val basketId: Long,
    val teeName: String,
    val basketName: String,
    val lengthMeters: Int,
    val parValue: Int
)