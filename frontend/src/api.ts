export type ApiFlag = boolean | number

export interface LoginResponse {
  access_token: string
  token_type: string
  user_id: number
  username: string
  role: string
  must_change_password: ApiFlag
}

export interface MeResponse {
  id: number
  username: string
  role: string
  is_active: ApiFlag
  must_change_password: ApiFlag
}

export interface CourseApiResponse {
  id: number
  name: string
  is_active: number
  hole_count: number
  layout_count: number
}

export interface PlayerRoundApiResponse {
  id: number
  course_name: string
  layout_name: string | null
  started_at: string
  ended_at: string | null
  status: string
  approval_required: number
  approval_state: string
  total_throws: number
  total_par: number
  played_holes: number
  layout_hole_count: number
  player_count: number
}

export interface RoundDetailHoleApiResponse {
  id: number
  session_player_id: number
  sequence_number: number
  course_id: number
  hole_id: number
  hole_variant_id: number | null
  hole_number_snapshot: number
  hole_name_snapshot: string | null
  tee_name_snapshot: string | null
  basket_name_snapshot: string | null
  length_snapshot_meters: number
  par_snapshot: number
  throws_count: number | null
  is_completed: number
}

export interface RoundDetailPlayerApiResponse {
  id: number
  play_session_id: number
  player_id: number
  player_name: string
  layout_id: number | null
  layout_name: string | null
  display_name_snapshot: string | null
  start_order: number
  added_by_user_id: number
  added_by_username: string
  approval_required: number
  approval_state: string
  approved_by_user_id: number | null
  approved_by_username: string | null
  approved_at: string | null
  holes: RoundDetailHoleApiResponse[]
}

export interface RoundDetailApiResponse {
  id: number
  course_id: number
  course_name: string
  created_by_user_id: number
  created_by_username: string
  started_at: string
  ended_at: string | null
  status: string
  players: RoundDetailPlayerApiResponse[]
}

export interface PublicCourseApiResponse {
  id: number
  name: string
  is_active: number
  hole_count: number
  layout_count: number
}

export interface PublicLayoutApiResponse {
  id: number
  course_id: number
  name: string
  description: string | null
  is_active: number
  hole_count: number
  total_par: number
  total_length_meters: number
}

export interface PublicLayoutHoleApiResponse {
  sequence_number: number
  hole_id: number
  hole_number: number
  hole_name: string | null
  tee_name: string | null
  basket_name: string | null
  length_meters: number
  par_value: number
}

export interface PublicCourseHoleApiResponse {
  id: number
  course_id: number
  hole_number: number
  name: string | null
  length_meters: number
  par_value: number
  notes: string | null
  is_active: number
}

export interface HoleCreateRequest {
  hole_number: number
  name?: string | null
  length_meters: number
  par_value: number
  notes?: string | null
}

export class ApiError extends Error {
  statusCode: number
  responseBody: string

  constructor(statusCode: number, responseBody: string) {
    super(`HTTP ${statusCode}: ${responseBody}`)
    this.statusCode = statusCode
    this.responseBody = responseBody
  }
}

const API_BASE_URL = `${import.meta.env.BASE_URL}api`

export function flagIsTrue(value: ApiFlag): boolean {
  return value === true || value === 1
}

async function parseResponse<T>(response: Response): Promise<T> {
  const responseBody = await response.text()

  if (!response.ok) {
    throw new ApiError(response.status, responseBody)
  }

  return JSON.parse(responseBody) as T
}

async function parseVoidResponse(response: Response): Promise<void> {
  const responseBody = await response.text()

  if (!response.ok) {
    throw new ApiError(response.status, responseBody)
  }
}

export async function changePassword(
  token: string,
  username: string,
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/change-password`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      username,
      current_password: currentPassword,
      new_password: newPassword,
    }),
  })

  return parseVoidResponse(response)
}

export async function login(
  username: string,
  password: string,
): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      username,
      password,
    }),
  })

  return parseResponse<LoginResponse>(response)
}

export async function getMe(token: string): Promise<MeResponse> {
  const response = await fetch(`${API_BASE_URL}/me`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  return parseResponse<MeResponse>(response)
}

export interface CourseApiResponse {
  id: number
  name: string
  is_active: number
  hole_count: number
  layout_count: number
}

export interface PlayerApiResponse {
  id: number
  name: string
  owner_user_id: number | null
  created_by_user_id: number | null
  is_guest: number
  is_active: number
  round_count: number
}

export interface ScoreablePlayerApiResponse extends PlayerApiResponse {
  permission_level: string
}

export interface UserPlayersResponse {
  user: {
    id: number
    username: string
    role: string
    is_active: number
  }
  own_player: PlayerApiResponse | null
  guest_players: PlayerApiResponse[]
  scoreable_players: ScoreablePlayerApiResponse[]
}

export interface PlayerLayoutStatsApiResponse {
  course_id: number
  course_name: string
  layout_id: number
  layout_name: string
  total_par: number
  hole_count: number
  total_length_meters: number
  round_count: number
  personal_best_throws: number
  personal_best_relative_to_par: number
  average_throws: number
  average_relative_to_par: number
  last_10_average_throws: number | null
  last_10_average_relative_to_par: number | null
}

export interface PlayerHoleStatsApiResponse {
  course_id: number
  course_name: string
  hole_id: number
  hole_variant_id: number | null
  hole_number: number
  hole_name: string | null
  tee_name: string | null
  basket_name: string | null
  length_meters: number
  par_value: number
  played_count: number
  personal_best_throws: number
  streak: number
  average_throws: number
  last_10_average_throws: number | null
  birdie_or_better_count: number
  par_count: number
  bogey_count: number
  double_bogey_count: number
  triple_bogey_or_worse_count: number
}

export async function getCourses(token: string): Promise<CourseApiResponse[]> {
  const response = await fetch(`${API_BASE_URL}/courses`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  return parseResponse<CourseApiResponse[]>(response)
}

export async function getUserPlayers(
  token: string,
  username: string,
): Promise<UserPlayersResponse> {
  const response = await fetch(
    `${API_BASE_URL}/users/${encodeURIComponent(username)}/players`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  )

  return parseResponse<UserPlayersResponse>(response)
}

export async function getPlayerLayoutStats(
  token: string,
  playerId: number,
  courseId: number | null,
): Promise<PlayerLayoutStatsApiResponse[]> {
  const query = courseId === null ? '' : `?course_id=${courseId}`

  const response = await fetch(
    `${API_BASE_URL}/players/${playerId}/stats/layouts${query}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  )

  return parseResponse<PlayerLayoutStatsApiResponse[]>(response)
}

export async function getPlayerHoleStats(
  token: string,
  playerId: number,
  courseId: number | null,
): Promise<PlayerHoleStatsApiResponse[]> {
  const query = courseId === null ? '' : `?course_id=${courseId}`

  const response = await fetch(
    `${API_BASE_URL}/players/${playerId}/stats/holes${query}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  )

  return parseResponse<PlayerHoleStatsApiResponse[]>(response)
}

export async function getPlayerRounds(
  token: string,
  playerId: number,
): Promise<PlayerRoundApiResponse[]> {
  const response = await fetch(
    `${API_BASE_URL}/players/${playerId}/rounds`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  )

  return parseResponse<PlayerRoundApiResponse[]>(response)
}

export async function getRoundDetail(
  token: string,
  roundId: number,
): Promise<RoundDetailApiResponse> {
  const response = await fetch(
    `${API_BASE_URL}/rounds/${roundId}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  )

  return parseResponse<RoundDetailApiResponse>(response)
}

export async function getPublicCourses(
  includeInactive = false,
): Promise<PublicCourseApiResponse[]> {
  const query = includeInactive ? '?include_inactive=true' : ''
  const response = await fetch(`${API_BASE_URL}/courses${query}`)

  return parseResponse<PublicCourseApiResponse[]>(response)
}

export async function getPublicCourseLayouts(
  courseId: number,
  includeInactive: boolean,
): Promise<PublicLayoutApiResponse[]> {
  const query = includeInactive ? '?include_inactive=true' : ''

  const response = await fetch(
    `${API_BASE_URL}/courses/${courseId}/layouts${query}`,
  )

  return parseResponse<PublicLayoutApiResponse[]>(response)
}

export async function getPublicLayoutHoles(
  layoutId: number,
): Promise<PublicLayoutHoleApiResponse[]> {
  const response = await fetch(`${API_BASE_URL}/layouts/${layoutId}/holes`)

  return parseResponse<PublicLayoutHoleApiResponse[]>(response)
}

export interface CourseCreateRequest {
  name: string
}

export interface CourseUpdateRequest {
  name?: string
  is_active?: boolean
}

export async function createCourse(
  token: string,
  requestBody: CourseCreateRequest,
): Promise<PublicCourseApiResponse> {
  const response = await fetch(`${API_BASE_URL}/courses`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(requestBody),
  })

  return parseResponse<PublicCourseApiResponse>(response)
}

export async function updateCourse(
  token: string,
  courseId: number,
  requestBody: CourseUpdateRequest,
): Promise<PublicCourseApiResponse> {
  const response = await fetch(`${API_BASE_URL}/courses/${courseId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(requestBody),
  })

  return parseResponse<PublicCourseApiResponse>(response)
}

export async function deleteCourse(
  token: string,
  courseId: number,
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/courses/${courseId}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  await parseResponse<unknown>(response)
}

export async function getCourseHoles(
  courseId: number,
  includeInactive = false,
): Promise<PublicCourseHoleApiResponse[]> {
  const query = includeInactive ? '?include_inactive=true' : ''
  const response = await fetch(`${API_BASE_URL}/courses/${courseId}/holes${query}`)

  return parseResponse<PublicCourseHoleApiResponse[]>(response)
}

export interface HoleUpdateRequest {
  hole_number?: number
  name?: string | null
  length_meters?: number
  par_value?: number
  notes?: string | null
  is_active?: boolean
}

export async function updateHole(
  token: string,
  holeId: number,
  requestBody: HoleUpdateRequest,
): Promise<PublicCourseHoleApiResponse> {
  const response = await fetch(`${API_BASE_URL}/holes/${holeId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(requestBody),
  })

  return parseResponse<PublicCourseHoleApiResponse>(response)
}

export async function deleteHole(
  token: string,
  holeId: number,
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/holes/${holeId}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  await parseResponse<unknown>(response)
}

export async function createHole(
  token: string,
  courseId: number,
  requestBody: HoleCreateRequest,
): Promise<PublicCourseHoleApiResponse> {
  const response = await fetch(`${API_BASE_URL}/courses/${courseId}/holes`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(requestBody),
  })

  return parseResponse<PublicCourseHoleApiResponse>(response)
}