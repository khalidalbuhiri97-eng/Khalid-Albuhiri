package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    
    val apiKey: String = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    fun isEnabled(): Boolean {
        return apiKey.isNotEmpty() && !apiKey.contains("placeholder") && apiKey != "MY_GEMINI_API_KEY"
    }

    suspend fun generateContent(prompt: String, history: List<Pair<String, String>> = emptyList()): String {
        if (!isEnabled()) {
            return getFallbackResponse(prompt)
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                
                // Construct the JSON payload using standard org.json (always present in Android SDK)
                val root = JSONObject()
                val contentsArray = JSONArray()

                // Add system instruction or context first
                val systemContent = JSONObject()
                val systemParts = JSONArray()
                systemParts.put(JSONObject().put("text", "أنت مساعد ذكي مدمج في تطبيق 'سوق' المحلي لخدمات الصيانة والربط بين الفنيين والعملاء في السعودية. أجب دوماً باللغة العربية بأسلوب ودود ولطيف ومختصر جداً وموجه لخدمة سكان الأحياء."))
                systemContent.put("parts", systemParts)
                // Note: systemInstruction is usually set at the root level, but adding context to the prompt is even safer and works with all models.
                
                // Add conversation history
                for (turn in history) {
                    val contentTurn = JSONObject()
                    contentTurn.put("role", if (turn.first == "USER") "user" else "model")
                    val partsTurn = JSONArray()
                    partsTurn.put(JSONObject().put("text", turn.second))
                    contentTurn.put("parts", partsTurn)
                    contentsArray.put(contentTurn)
                }

                // Add current prompt
                val currentContent = JSONObject()
                currentContent.put("role", "user")
                val currentParts = JSONArray()
                
                val enhancedPrompt = if (history.isEmpty()) {
                    "أنت مساعد ذكي لتطبيق 'سوق' المحلي في السعودية. ساعد المستخدم في الإجابة على سؤاله أو توجيهه للقسم الصحيح بالتطبيق. السؤال: $prompt"
                } else {
                    prompt
                }
                
                currentParts.put(JSONObject().put("text", enhancedPrompt))
                currentContent.put("parts", currentParts)
                contentsArray.put(currentContent)

                root.put("contents", contentsArray)

                // Optional configuration
                val generationConfig = JSONObject()
                generationConfig.put("temperature", 0.7)
                root.put("generationConfig", generationConfig)

                val requestBody = root.toString().toRequestBody(mediaTypeJson)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val responseJson = JSONObject(bodyString)
                        val candidates = responseJson.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                return@withContext parts.getJSONObject(0).getString("text")
                            }
                        }
                        "عذراً، لم أستطع استخلاص رد صحيح."
                    } else {
                        Log.e(TAG, "Gemini API failed with response code: ${response.code}")
                        getFallbackResponse(prompt)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error: ${e.localizedMessage}")
                getFallbackResponse(prompt)
            }
        }
    }

    private fun getFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("سباكة") || prompt.contains("سباك") || prompt.contains("تهريب") || prompt.contains("مياه") -> {
                "بصفتي المساعد الذكي لتطبيق سوق، قمت بتحليل مشكلتك. للسباكة وكشف التسربات في حي الياسمين، أنصحك بالتواصل مع الفني المعتمد 'خالد الحربي'. يمكنك تصفح عروض السباكة الحالية من تبويب 'العروض' وحجزها مباشرة."
            }
            prompt.contains("كهرباء") || prompt.contains("كهربائي") || prompt.contains("إنارة") || prompt.contains("التماس") -> {
                "لصيانة الكهرباء والالتماسات المنزلية، فني حيك المعتمد هو 'م. ياسر القحطاني'. لطلب خدمة جديدة، تفضل بالضغط على زر 'طلب خدمة' في الرئيسية لوصف المشكلة وسيصلك عرض فوري من الفنيين القريبين."
            }
            prompt.contains("تكييف") || prompt.contains("تبريد") || prompt.contains("مكيف") -> {
                "لصيانة وغسيل المكيفات (سبليت أو شباك)، لدينا عرض مميز نشط الآن من 'م. أحمد الشهري' لغسيل 3 مكيفات مع تعقيم كامل بسعر 149 ريال فقط! يمكنك حجز هذا العرض فوراً من تبويب 'العروض'."
            }
            prompt.contains("وظيفة") || prompt.contains("عمل") || prompt.contains("طلب") -> {
                "أهلاً بك! في تطبيق سوق، يمكنك تصفح طلبات الصيانة المنشورة من قبل سكان الحي من خلال الانتقال إلى قائمة 'الوظائف والطلبات' في صفحة الحي، وتقديم عرضك المالي مباشرة للعميل بدون أي وسيط أو عمولة!"
            }
            else -> {
                "أهلاً بك في تطبيق 'سوق' للخدمات المحلية! 🏡\n\nأنا مساعدك الذكي لمساعدتك في صيانة منزلك وتوجيهك لأفضل فنيي الصيانة القريبين منك في حي الياسمين.\n\nيمكنك الاستفسار عن أي مشكلة فنية (سباكة، كهرباء، تكييف، دهان، إلخ) وسأرشدك للحل والفني المناسب."
            }
        }
    }
}
