package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateModifiedCode(
        prompt: String,
        systemInstruction: String,
        baseCode: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing or default. Add your GEMINI_API_KEY credentials to the Secrets Panel in AI Studio to unlock dynamic code editing."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val fullPrompt = """
            Here is the existing code:
            ```python
            $baseCode
            ```
            
            User modification request: $prompt
            
            Please edit this code strictly according to the request. Ensure:
            - The modified code is complete, integrated, and fully functional.
            - Provide descriptive comments for any new additions.
            - Do not truncate, skip lines, or write general placeholders (e.g., `# rest of code here`).
            - Return only the complete modified code blocks.
        """.trimIndent()

        val json = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", fullPrompt)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            if (systemInstruction.isNotEmpty()) {
                val sysInstObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", sysInstObj)
            }
            
            val configObj = JSONObject().apply {
                put("temperature", 0.2)
            }
            put("generationConfig", configObj)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "API Error: ${response.code} $responseString")
                return@withContext "API error ${response.code}. Please ensure your API key is correctly entered and has permission."
            }

            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                if (content != null) {
                    val parts = content.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Error parsing response parts.")
                    }
                }
            }
            "No suggestions found from Gemini. Feel free to try again."
        } catch (e: Exception) {
            Log.e(TAG, "Network API Exception", e)
            "Error contacting Gemini network endpoint: ${e.localizedMessage}"
        }
    }
}
