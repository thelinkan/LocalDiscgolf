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
