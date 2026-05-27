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