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

data class LayoutApiResponse(
    val id: Long,
    val course_id: Long,
    val course_name: String,
    val name: String,
    val description: String?,
    val is_active: Int,
    val hole_count: Int,
    val total_par: Int,
    val total_length_meters: Int
)

data class LayoutHoleApiResponse(
    val sequence_number: Int,
    val hole_id: Long,
    val hole_number: Int,
    val hole_name: String?,
    val hole_variant_id: Long?,
    val tee_name: String?,
    val basket_name: String?,
    val length_meters: Int,
    val par_value: Int
)

data class UserApiResponse(
    val id: Long,
    val username: String,
    val role: String,
    val is_active: Int
)

data class PlayerApiResponse(
    val id: Long,
    val name: String,
    val owner_user_id: Long?,
    val created_by_user_id: Long?,
    val is_guest: Int,
    val is_active: Int
)

data class ScoreablePlayerApiResponse(
    val id: Long,
    val name: String,
    val owner_user_id: Long?,
    val created_by_user_id: Long?,
    val is_guest: Int,
    val is_active: Int,
    val permission_level: String
)

data class UserPlayersResponse(
    val user: UserApiResponse,
    val own_player: PlayerApiResponse?,
    val guest_players: List<PlayerApiResponse>,
    val scoreable_players: List<ScoreablePlayerApiResponse>
)

data class PlayerRoundApiResponse(
    val id: Long,
    val course_name: String,
    val layout_name: String?,
    val started_at: String,
    val ended_at: String?,
    val status: String,
    val approval_required: Int,
    val approval_state: String,
    val total_throws: Int,
    val total_par: Int,
    val played_holes: Int,
    val layout_hole_count: Int
)

