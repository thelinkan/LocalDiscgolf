package nu.linkan.localdiscgolf.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ApiClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun buildBaseUrl(host: String, port: String): String {
        return "http://$host:$port"
    }

    fun login(baseUrl: String, username: String, password: String): Result<LoginResponse> {
        return try {
            val bodyJson = gson.toJson(LoginRequest(username, password))
            val request = Request.Builder()
                .url("$baseUrl/login")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return Result.failure(Exception("Login failed: ${response.code} $responseBody"))
                }

                val parsed = gson.fromJson(responseBody, LoginResponse::class.java)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMe(baseUrl: String, token: String): Result<MeResponse> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /me responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(Exception("Get /me failed: ${response.code} $responseBody"))
                }

                val parsed = gson.fromJson(responseBody, MeResponse::class.java)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCourses(baseUrl: String, token: String): Result<List<CourseApiResponse>> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/courses")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /courses responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(Exception("Get /courses failed: ${response.code} $responseBody"))
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, CourseApiResponse::class.java)
                    .type

                val parsed: List<CourseApiResponse> = gson.fromJson(responseBody, listType)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getCourseLayouts(baseUrl: String, token: String, courseId: Long): Result<List<LayoutApiResponse>> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/courses/$courseId/layouts")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /courses/$courseId/layouts responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(Exception("Get /courses/$courseId/layouts failed: ${response.code} $responseBody"))
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, LayoutApiResponse::class.java)
                    .type

                val parsed: List<LayoutApiResponse> = gson.fromJson(responseBody, listType)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getLayoutHoles(
        baseUrl: String,
        token: String,
        layoutId: Long
    ): Result<List<LayoutHoleApiResponse>> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/layouts/$layoutId/holes")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /layouts/$layoutId/holes responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /layouts/$layoutId/holes failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, LayoutHoleApiResponse::class.java)
                    .type

                val parsed: List<LayoutHoleApiResponse> = gson.fromJson(responseBody, listType)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getUserPlayers(
        baseUrl: String,
        token: String,
        username: String
    ): Result<UserPlayersResponse> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/users/$username/players")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /users/$username/players responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /users/$username/players failed: ${response.code} $responseBody")
                    )
                }

                val parsed = gson.fromJson(responseBody, UserPlayersResponse::class.java)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getPlayerRounds(
        baseUrl: String,
        token: String,
        playerId: Long
    ): Result<List<PlayerRoundApiResponse>> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/players/$playerId/rounds")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /players/$playerId/rounds responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /players/$playerId/rounds failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, PlayerRoundApiResponse::class.java)
                    .type

                val parsed: List<PlayerRoundApiResponse> = gson.fromJson(responseBody, listType)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getRoundDetail(
        baseUrl: String,
        token: String,
        roundId: Long
    ): Result<RoundDetailApiResponse> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/rounds/$roundId")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /rounds/$roundId responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /rounds/$roundId failed: ${response.code} $responseBody")
                    )
                }

                val parsed = gson.fromJson(responseBody, RoundDetailApiResponse::class.java)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}