package nu.linkan.localdiscgolf.data.local.model

data class CourseListRow(
    val courseId: Long,
    val courseName: String,
    val holeCount: Int,
    val layoutCount: Int
)