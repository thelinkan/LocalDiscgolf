package nu.linkan.localdiscgolf.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiHttpException(
    val statusCode: Int,
    val responseBody: String
) : Exception("HTTP $statusCode: $responseBody")

object ApiClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun buildBaseUrl(serverAddress: String, port: String = ""): String {
        val trimmedAddress = serverAddress.trim().trimEnd('/')
        val trimmedPort = port.trim()

        val hasScheme =
            trimmedAddress.startsWith("http://") ||
                    trimmedAddress.startsWith("https://")

        val addressWithScheme =
            if (hasScheme) {
                trimmedAddress
            } else {
                "http://$trimmedAddress"
            }

        val shouldAddPort =
            trimmedPort.isNotBlank() &&
                    !trimmedAddress.substringAfter("://", trimmedAddress).contains(":") &&
                    !trimmedAddress.substringAfter("://", trimmedAddress).contains("/")

        val withPort =
            if (shouldAddPort) {
                "$addressWithScheme:$trimmedPort"
            } else {
                addressWithScheme
            }

        return withPort.trimEnd('/')
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
                    return Result.failure(
                        ApiHttpException(
                            statusCode = response.code,
                            responseBody = responseBody
                        )
                    )
                }

                val parsed = gson.fromJson(responseBody, MeResponse::class.java)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCourses(
        baseUrl: String,
        token: String,
        includeInactive: Boolean = false
    ): Result<List<CourseApiResponse>> {
        return try {
            val query = if (includeInactive) "?include_inactive=true" else ""

            val request = Request.Builder()
                .url("$baseUrl/courses$query")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /courses$query responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /courses failed: ${response.code} $responseBody")
                    )
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

    fun getCourseLayouts(
        baseUrl: String,
        token: String,
        courseId: Long,
        includeInactive: Boolean = false
    ): Result<List<LayoutApiResponse>> {
        return try {
            val query = if (includeInactive) "?include_inactive=true" else ""

            val request = Request.Builder()
                .url("$baseUrl/courses/$courseId/layouts$query")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /courses/$courseId/layouts$query responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /courses/$courseId/layouts failed: ${response.code} $responseBody")
                    )
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

    fun getCourseHoles(
        baseUrl: String,
        token: String,
        courseId: Long,
        includeInactive: Boolean = false
    ): Result<List<HoleApiResponse>> {
        return try {
            val query = if (includeInactive) "?include_inactive=true" else ""

            val request = Request.Builder()
                .url("$baseUrl/courses/$courseId/holes$query")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /courses/$courseId/holes$query responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /courses/$courseId/holes failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, HoleApiResponse::class.java)
                    .type

                val parsed: List<HoleApiResponse> = gson.fromJson(responseBody, listType)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getHoleTees(
        baseUrl: String,
        token: String,
        holeId: Long,
        includeInactive: Boolean = false
    ): Result<List<HoleTeeApiResponse>> {
        return try {
            val query = if (includeInactive) "?include_inactive=true" else ""

            val request = Request.Builder()
                .url("$baseUrl/holes/$holeId/tees$query")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /holes/$holeId/tees$query responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /holes/$holeId/tees failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, HoleTeeApiResponse::class.java)
                    .type

                val parsed: List<HoleTeeApiResponse> = gson.fromJson(responseBody, listType)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getHoleBaskets(
        baseUrl: String,
        token: String,
        holeId: Long,
        includeInactive: Boolean = false
    ): Result<List<HoleBasketApiResponse>> {
        return try {
            val query = if (includeInactive) "?include_inactive=true" else ""

            val request = Request.Builder()
                .url("$baseUrl/holes/$holeId/baskets$query")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /holes/$holeId/baskets$query responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /holes/$holeId/baskets failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, HoleBasketApiResponse::class.java)
                    .type

                val parsed: List<HoleBasketApiResponse> = gson.fromJson(responseBody, listType)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getHoleVariants(
        baseUrl: String,
        token: String,
        holeId: Long,
        includeInactive: Boolean = false
    ): Result<List<HoleVariantApiResponse>> {
        return try {
            val query = if (includeInactive) "?include_inactive=true" else ""

            val request = Request.Builder()
                .url("$baseUrl/holes/$holeId/variants$query")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /holes/$holeId/variants$query responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /holes/$holeId/variants failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, HoleVariantApiResponse::class.java)
                    .type

                val parsed: List<HoleVariantApiResponse> = gson.fromJson(responseBody, listType)
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

    fun createRound(
        baseUrl: String,
        token: String,
        requestBody: CreateRoundApiRequest
    ): Result<RoundDetailApiResponse> {
        return try {
            val bodyJson = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/rounds")
                .header("Authorization", "Bearer $token")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("POST /rounds responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Create round failed: ${response.code} $responseBody")
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

    fun getCurrentRound(
        baseUrl: String,
        token: String,
        roundId: Long
    ): Result<CurrentRoundApiResponse> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/rounds/$roundId/current")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /rounds/$roundId/current responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get /rounds/$roundId/current failed: ${response.code} $responseBody")
                    )
                }

                val parsed = gson.fromJson(responseBody, CurrentRoundApiResponse::class.java)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun updateRoundHole(
        baseUrl: String,
        token: String,
        roundId: Long,
        sequenceNumber: Int,
        requestBody: UpdateHoleApiRequest
    ): Result<RoundDetailApiResponse> {
        return try {
            val bodyJson = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/rounds/$roundId/holes/$sequenceNumber")
                .header("Authorization", "Bearer $token")
                .patch(bodyJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("PATCH /rounds/$roundId/holes/$sequenceNumber responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Update hole failed: ${response.code} $responseBody")
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

    fun getRoundHole(
        baseUrl: String,
        token: String,
        roundId: Long,
        sequenceNumber: Int
    ): Result<CurrentRoundApiResponse> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/rounds/$roundId/holes/$sequenceNumber")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /rounds/$roundId/holes/$sequenceNumber responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(
                            "Get /rounds/$roundId/holes/$sequenceNumber failed: " +
                                    "${response.code} $responseBody"
                        )
                    )
                }

                val parsed = gson.fromJson(
                    responseBody,
                    CurrentRoundApiResponse::class.java
                )

                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun completeRound(
        baseUrl: String,
        token: String,
        roundId: Long,
        requestBody: CompleteRoundApiRequest
    ): Result<RoundDetailApiResponse> {
        return try {
            val bodyJson = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/rounds/$roundId/complete")
                .header("Authorization", "Bearer $token")
                .post(bodyJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("POST /rounds/$roundId/complete responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Complete round failed: ${response.code} $responseBody")
                    )
                }

                val parsed = gson.fromJson(
                    responseBody,
                    RoundDetailApiResponse::class.java
                )

                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getMyInProgressRounds(
        baseUrl: String,
        token: String
    ): Result<List<InProgressServerRoundApiResponse>> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/me/in-progress-rounds")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET /me/in-progress-rounds responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get in-progress rounds failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, InProgressServerRoundApiResponse::class.java)
                    .type

                val parsed: List<InProgressServerRoundApiResponse> =
                    gson.fromJson(responseBody, listType)

                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getPlayerLayoutStats(
        baseUrl: String,
        token: String,
        playerId: Long,
        courseId: Long?
    ): Result<List<PlayerLayoutStatsApiResponse>> {
        return try {
            val url = if (courseId == null) {
                "$baseUrl/players/$playerId/stats/layouts"
            } else {
                "$baseUrl/players/$playerId/stats/layouts?course_id=$courseId"
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET player layout stats responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get layout stats failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, PlayerLayoutStatsApiResponse::class.java)
                    .type

                val parsed: List<PlayerLayoutStatsApiResponse> =
                    gson.fromJson(responseBody, listType)

                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getPlayerHoleStats(
        baseUrl: String,
        token: String,
        playerId: Long,
        courseId: Long?
    ): Result<List<PlayerHoleStatsApiResponse>> {
        return try {
            val url = if (courseId == null) {
                "$baseUrl/players/$playerId/stats/holes"
            } else {
                "$baseUrl/players/$playerId/stats/holes?course_id=$courseId"
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("GET player hole stats responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Get hole stats failed: ${response.code} $responseBody")
                    )
                }

                val listType = com.google.gson.reflect.TypeToken
                    .getParameterized(List::class.java, PlayerHoleStatsApiResponse::class.java)
                    .type

                val parsed: List<PlayerHoleStatsApiResponse> =
                    gson.fromJson(responseBody, listType)

                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun updateRound(
        baseUrl: String,
        token: String,
        roundId: Long,
        requestBody: UpdateRoundApiRequest
    ): Result<RoundDetailApiResponse> {
        return try {
            val bodyJson = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/rounds/$roundId")
                .header("Authorization", "Bearer $token")
                .patch(bodyJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                println("PATCH /rounds/$roundId responseBody: $responseBody")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Update round failed: ${response.code} $responseBody")
                    )
                }

                val parsed = gson.fromJson(
                    responseBody,
                    RoundDetailApiResponse::class.java
                )

                Result.success(parsed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
