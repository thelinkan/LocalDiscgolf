package nu.linkan.localdiscgolf.network

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user_id: Long,
    val username: String,
    val role: String,
    val must_change_password: Boolean
)

data class MeResponse(
    val id: Long,
    val username: String,
    val role: String,
    val is_active: Int,
    val must_change_password: Int
)

data class CourseApiResponse(
    val id: Long,
    val name: String,
    val is_active: Int,
    val hole_count: Int,
    val layout_count: Int
)