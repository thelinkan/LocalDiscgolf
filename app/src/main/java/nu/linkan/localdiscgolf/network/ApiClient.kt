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
}