package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generatePlan(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun getDailyPlan(tasks: List<Task>): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return generateAlgorithmPlan(tasks) + "\n\n⚠️ *Developer tip: Sync your actual Gemini API Key in the AI Studio Secets panel to enable full LLM dynamic planning.*"
        }

        val taskListStr = tasks.joinToString("\n") { task ->
            "- [${if (task.isCompleted) "X" else " "}] ${task.title} (Priority: ${task.priority}, Project: ${task.project})"
        }

        val prompt = """
            You are TimeFlow Smart AI Planner. Review the following personal task list:
            $taskListStr
            
            Based on these tasks, please generate an elegant, daily structured schedule.
            Group tasks by priority, suggest which projects to focus on, and provide a 2-3 sentence motivational quote.
            Be concise and professional.
        """.trimIndent()

        return try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(temperature = 0.7f)
            )
            val response = apiService.generatePlan(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: generateAlgorithmPlan(tasks)
        } catch (e: Exception) {
            generateAlgorithmPlan(tasks) + "\n\n(Network/Key issue: fell back to on-device scheduling)"
        }
    }

    private fun generateAlgorithmPlan(tasks: List<Task>): String {
        val pending = tasks.filter { !it.isCompleted }
        if (pending.isEmpty()) {
            return """
                🌅 **Your Perfect Day is Planned!**
                You have no pending tasks today. Use this time with family, or design some new habits to build deep streaks!
                
                🏆 *“Productivity is being able to do things that you were never able to do before.” – Franz Kafka*
            """.trimIndent()
        }

        val high = pending.filter { it.priority == "High" }
        val medium = pending.filter { it.priority == "Medium" }
        val low = pending.filter { it.priority == "Low" }

        val schedule = StringBuilder()
        schedule.append("🌅 **On-Device Smart Daily Plan**\n\n")
        schedule.append("Here is your structured priority focus for today:\n\n")

        if (high.isNotEmpty()) {
            schedule.append("🔴 **Primary Focus (High Priority):**\n")
            high.forEach { schedule.append("• ${it.title} [${it.project}]\n") }
            schedule.append("\n")
        }

        if (medium.isNotEmpty()) {
            schedule.append("🟡 **Secondary Goals (Medium Priority):**\n")
            medium.forEach { schedule.append("• ${it.title} [${it.project}]\n") }
            schedule.append("\n")
        }

        if (low.isNotEmpty()) {
            schedule.append("🟢 **Secondary Items (Low Priority):**\n")
            low.forEach { schedule.append("• ${it.title} [${it.project}]\n") }
            schedule.append("\n")
        }

        schedule.append("💡 *TimeFlow suggestion: Use your built-in Pomodoro focus timer to tackle the high priority tasks first!*")
        return schedule.toString()
    }
}
