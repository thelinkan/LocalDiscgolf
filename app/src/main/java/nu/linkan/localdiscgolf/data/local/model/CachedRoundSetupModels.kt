package nu.linkan.localdiscgolf.data.local.model

data class CachedPlayerForRound(
    val id: Long,
    val serverId: Long,
    val name: String,
    val permissionLevel: String?,
    val isGuest: Boolean
)

data class CachedCourseForRound(
    val id: Long,
    val serverId: Long,
    val name: String
)

data class CachedLayoutForRound(
    val id: Long,
    val serverId: Long,
    val courseId: Long,
    val serverCourseId: Long,
    val name: String,
    val description: String?,
    val holeCount: Int,
    val totalPar: Int,
    val totalLengthMeters: Int
)

data class CachedLayoutHoleForRound(
    val layoutHoleId: Long,
    val layoutId: Long,
    val serverLayoutId: Long?,
    val sequenceNumber: Int,
    val holeId: Long,
    val serverHoleId: Long?,
    val holeVariantId: Long?,
    val serverHoleVariantId: Long?,
    val holeNumber: Int,
    val holeName: String?,
    val teeName: String?,
    val basketName: String?,
    val lengthMeters: Int,
    val parValue: Int
)