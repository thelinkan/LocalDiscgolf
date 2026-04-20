package nu.linkan.localdiscgolf.data.local.model

data class LayoutHoleWithHole(
    val layoutHoleId: Long,
    val layoutId: Long,
    val sequenceNumber: Int,
    val holeId: Long,
    val teeId: Long?,
    val basketId: Long?,
    val holeNumber: Int,
    val holeName: String?,
    val lengthMeters: Int,
    val parValue: Int,
    val holeNotes: String?,
    val teeName: String?,
    val basketName: String?
)