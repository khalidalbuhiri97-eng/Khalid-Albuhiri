package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

enum class SupabaseSyncStatus {
    IDLE,
    SYNCING,
    CONNECTED,
    TABLE_NOT_FOUND,
    ERROR
}

object SupabaseSyncManager {
    private const val TAG = "SupabaseSync"
    
    val supabaseUrl: String = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
    val supabaseKey: String = try { BuildConfig.SUPABASE_KEY } catch (e: Exception) { "" }

    private val _syncStatus = MutableStateFlow(SupabaseSyncStatus.IDLE)
    val syncStatus: StateFlow<SupabaseSyncStatus> = _syncStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("بانتظار بدء المزامنة")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val isEnabled: Boolean
        get() = supabaseUrl.isNotEmpty() && supabaseKey.isNotEmpty() && !supabaseUrl.contains("placeholder")

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    init {
        if (!isEnabled) {
            _syncStatus.value = SupabaseSyncStatus.ERROR
            _statusMessage.value = "مفاتيح Supabase غير مهيأة بعد في ملف .env"
            Log.w(TAG, "Supabase keys are missing or placeholders. URL: $supabaseUrl")
        } else {
            _syncStatus.value = SupabaseSyncStatus.CONNECTED
            _statusMessage.value = "متصل بـ Supabase وبانتظار المزامنة"
            Log.i(TAG, "Supabase configured successfully with URL: $supabaseUrl")
        }
    }

    // --- Helper to build request with Supabase headers ---
    private fun buildRequest(url: String, method: String, bodyStr: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
        
        if (bodyStr != null) {
            val body = bodyStr.toRequestBody(mediaTypeJson)
            if (method == "POST") {
                builder.post(body)
            } else if (method == "PUT") {
                builder.put(body)
            } else if (method == "PATCH") {
                builder.patch(body)
            }
        } else {
            if (method == "GET") {
                builder.get()
            }
        }
        return builder.build()
    }

    // --- Sync Users ---
    suspend fun syncUsers(dao: SouqDao) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        _syncStatus.value = SupabaseSyncStatus.SYNCING
        
        try {
            val url = "$supabaseUrl/rest/v1/users?select=*"
            val request = buildRequest(url, "GET")
            
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: ""
                        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                        val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                        val supabaseUsers = adapter.fromJson(body) ?: emptyList()
                        
                        var insertedCount = 0
                        for (rawUser in supabaseUsers) {
                            try {
                                val user = UserEntity(
                                    uid = rawUser["uid"] as? String ?: continue,
                                    name = rawUser["name"] as? String ?: "",
                                    phoneNumber = rawUser["phoneNumber"] as? String ?: "",
                                    role = rawUser["role"] as? String ?: "CUSTOMER",
                                    city = rawUser["city"] as? String ?: "الرياض",
                                    neighborhood = rawUser["neighborhood"] as? String ?: "حي الياسمين",
                                    lat = (rawUser["lat"] as? Number)?.toDouble() ?: 24.8,
                                    lng = (rawUser["lng"] as? Number)?.toDouble() ?: 46.6,
                                    avatarColor = (rawUser["avatarColor"] as? Number)?.toInt() ?: 0,
                                    bio = rawUser["bio"] as? String ?: "",
                                    profession = rawUser["profession"] as? String ?: "",
                                    experienceYears = (rawUser["experienceYears"] as? Number)?.toInt() ?: 0,
                                    isOnline = rawUser["isOnline"] as? Boolean ?: true,
                                    rating = (rawUser["rating"] as? Number)?.toDouble() ?: 4.5,
                                    completedOrders = (rawUser["completedOrders"] as? Number)?.toInt() ?: 0,
                                    avatarUri = rawUser["avatarUri"] as? String
                                )
                                dao.insertUser(user)
                                insertedCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Error mapping user: ${e.localizedMessage}")
                            }
                        }
                        _syncStatus.value = SupabaseSyncStatus.CONNECTED
                        _statusMessage.value = "تمت مزامنة $insertedCount من المستخدمين بنجاح"
                    }
                    404 -> {
                        _syncStatus.value = SupabaseSyncStatus.TABLE_NOT_FOUND
                        _statusMessage.value = "جدول 'users' غير موجود في Supabase. يرجى إنشاؤه."
                    }
                    else -> {
                        _syncStatus.value = SupabaseSyncStatus.ERROR
                        _statusMessage.value = "فشلت مزامنة المستخدمين: رمز الاستجابة ${response.code}"
                    }
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = SupabaseSyncStatus.ERROR
            _statusMessage.value = "خطأ اتصال بالمستخدمين: ${e.localizedMessage}"
            Log.e(TAG, "Sync Users error: ${e.localizedMessage}")
        }
    }

    suspend fun upsertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            val url = "$supabaseUrl/rest/v1/users?uid=eq.${user.uid}"
            val userMap = mapOf(
                "uid" to user.uid,
                "name" to user.name,
                "phoneNumber" to user.phoneNumber,
                "role" to user.role,
                "city" to user.city,
                "neighborhood" to user.neighborhood,
                "lat" to user.lat,
                "lng" to user.lng,
                "avatarColor" to user.avatarColor,
                "bio" to user.bio,
                "profession" to user.profession,
                "experienceYears" to user.experienceYears,
                "isOnline" to user.isOnline,
                "rating" to user.rating,
                "completedOrders" to user.completedOrders,
                "avatarUri" to user.avatarUri
            )
            val adapter = moshi.adapter(Map::class.java)
            val bodyJson = adapter.toJson(userMap)
            
            // Prefer: resolution=merge (upsert in Supabase)
            val builder = Request.Builder()
                .url("$supabaseUrl/rest/v1/users")
                .post(bodyJson.toRequestBody(mediaTypeJson))
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                
            client.newCall(builder.build()).execute().use { response ->
                Log.d(TAG, "Upsert user result: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upsert user error: ${e.localizedMessage}")
        }
    }

    // --- Sync Channel Messages ---
    suspend fun syncChannelMessages(channelId: String, dao: SouqDao) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        _syncStatus.value = SupabaseSyncStatus.SYNCING
        
        try {
            val url = "$supabaseUrl/rest/v1/channel_messages?channelId=eq.$channelId&order=createdAt.asc"
            val request = buildRequest(url, "GET")
            
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: ""
                        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                        val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                        val supabaseMessages = adapter.fromJson(body) ?: emptyList()
                        
                        var insertedCount = 0
                        for (rawMsg in supabaseMessages) {
                            try {
                                val msg = ChannelMessageEntity(
                                    id = (rawMsg["id"] as? Number)?.toInt() ?: 0,
                                    channelId = rawMsg["channelId"] as? String ?: channelId,
                                    senderId = rawMsg["senderId"] as? String ?: "",
                                    senderName = rawMsg["senderName"] as? String ?: "",
                                    senderAvatarColor = (rawMsg["senderAvatarColor"] as? Number)?.toInt() ?: 0,
                                    senderRole = rawMsg["senderRole"] as? String ?: "CUSTOMER",
                                    content = rawMsg["content"] as? String ?: "",
                                    createdAt = (rawMsg["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    senderAvatarUri = rawMsg["senderAvatarUri"] as? String
                                )
                                dao.insertChannelMessage(msg)
                                insertedCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Error mapping message: ${e.localizedMessage}")
                            }
                        }
                        _syncStatus.value = SupabaseSyncStatus.CONNECTED
                        _statusMessage.value = "تمت مزامنة $insertedCount رسائل قنوات بنجاح"
                    }
                    404 -> {
                        _syncStatus.value = SupabaseSyncStatus.TABLE_NOT_FOUND
                        _statusMessage.value = "جدول 'channel_messages' غير موجود في Supabase."
                    }
                    else -> {
                        _syncStatus.value = SupabaseSyncStatus.ERROR
                        _statusMessage.value = "فشلت مزامنة رسائل القناة: ${response.code}"
                    }
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = SupabaseSyncStatus.ERROR
            _statusMessage.value = "خطأ اتصال برسائل القنوات: ${e.localizedMessage}"
        }
    }

    suspend fun sendChannelMessage(msg: ChannelMessageEntity) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            val url = "$supabaseUrl/rest/v1/channel_messages"
            val msgMap = mapOf(
                "channelId" to msg.channelId,
                "senderId" to msg.senderId,
                "senderName" to msg.senderName,
                "senderAvatarColor" to msg.senderAvatarColor,
                "senderRole" to msg.senderRole,
                "content" to msg.content,
                "createdAt" to msg.createdAt,
                "senderAvatarUri" to msg.senderAvatarUri
            )
            val adapter = moshi.adapter(Map::class.java)
            val bodyJson = adapter.toJson(msgMap)
            val request = buildRequest(url, "POST", bodyJson)
            
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Send channel message result: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send channel message error: ${e.localizedMessage}")
        }
    }

    // --- Sync Neighborhood Posts ---
    suspend fun syncPosts(dao: SouqDao) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        _syncStatus.value = SupabaseSyncStatus.SYNCING
        
        try {
            val url = "$supabaseUrl/rest/v1/neighborhood_posts?select=*&order=createdAt.desc"
            val request = buildRequest(url, "GET")
            
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: ""
                        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                        val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                        val supabasePosts = adapter.fromJson(body) ?: emptyList()
                        
                        var insertedCount = 0
                        for (rawPost in supabasePosts) {
                            try {
                                val post = PostEntity(
                                    id = (rawPost["id"] as? Number)?.toInt() ?: 0,
                                    authorId = rawPost["authorId"] as? String ?: "",
                                    authorName = rawPost["authorName"] as? String ?: "",
                                    authorRole = rawPost["authorRole"] as? String ?: "CUSTOMER",
                                    authorAvatarColor = (rawPost["authorAvatarColor"] as? Number)?.toInt() ?: 0,
                                    content = rawPost["content"] as? String ?: "",
                                    neighborhood = rawPost["neighborhood"] as? String ?: "حي الياسمين",
                                    createdAt = (rawPost["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    likesCount = (rawPost["likesCount"] as? Number)?.toInt() ?: 0,
                                    commentsCount = (rawPost["commentsCount"] as? Number)?.toInt() ?: 0,
                                    isLikedByMe = false, // Client local state
                                    authorAvatarUri = rawPost["authorAvatarUri"] as? String
                                )
                                dao.insertPost(post)
                                insertedCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Error mapping post: ${e.localizedMessage}")
                            }
                        }
                        _syncStatus.value = SupabaseSyncStatus.CONNECTED
                        _statusMessage.value = "تمت مزامنة $insertedCount من منشورات الحي بنجاح"
                    }
                    404 -> {
                        _syncStatus.value = SupabaseSyncStatus.TABLE_NOT_FOUND
                        _statusMessage.value = "جدول 'neighborhood_posts' غير موجود في Supabase."
                    }
                    else -> {
                        _syncStatus.value = SupabaseSyncStatus.ERROR
                        _statusMessage.value = "فشلت مزامنة منشورات الحي: ${response.code}"
                    }
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = SupabaseSyncStatus.ERROR
            _statusMessage.value = "خطأ اتصال بمنشورات الحي: ${e.localizedMessage}"
        }
    }

    suspend fun sendPost(post: PostEntity) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            val url = "$supabaseUrl/rest/v1/neighborhood_posts"
            val postMap = mapOf(
                "authorId" to post.authorId,
                "authorName" to post.authorName,
                "authorRole" to post.authorRole,
                "authorAvatarColor" to post.authorAvatarColor,
                "content" to post.content,
                "neighborhood" to post.neighborhood,
                "createdAt" to post.createdAt,
                "likesCount" to post.likesCount,
                "commentsCount" to post.commentsCount,
                "authorAvatarUri" to post.authorAvatarUri
            )
            val adapter = moshi.adapter(Map::class.java)
            val bodyJson = adapter.toJson(postMap)
            val request = buildRequest(url, "POST", bodyJson)
            
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Send post result: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send post error: ${e.localizedMessage}")
        }
    }

    // --- Sync Service Requests ---
    suspend fun syncRequests(dao: SouqDao) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        _syncStatus.value = SupabaseSyncStatus.SYNCING
        
        try {
            val url = "$supabaseUrl/rest/v1/service_requests?select=*&order=createdAt.desc"
            val request = buildRequest(url, "GET")
            
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string() ?: ""
                        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                        val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                        val supabaseRequests = adapter.fromJson(body) ?: emptyList()
                        
                        var insertedCount = 0
                        for (rawReq in supabaseRequests) {
                            try {
                                val req = ServiceRequestEntity(
                                    id = (rawReq["id"] as? Number)?.toInt() ?: 0,
                                    customerId = rawReq["customerId"] as? String ?: "",
                                    customerName = rawReq["customerName"] as? String ?: "",
                                    customerPhone = rawReq["customerPhone"] as? String ?: "",
                                    techId = rawReq["techId"] as? String,
                                    techName = rawReq["techName"] as? String,
                                    techPhone = rawReq["techPhone"] as? String,
                                    serviceType = rawReq["serviceType"] as? String ?: "سباكة",
                                    description = rawReq["description"] as? String ?: "",
                                    status = rawReq["status"] as? String ?: "NEW",
                                    urgency = rawReq["urgency"] as? String ?: "عادي",
                                    timeRequired = rawReq["timeRequired"] as? String ?: "اليوم",
                                    neighborhood = rawReq["neighborhood"] as? String ?: "حي الياسمين",
                                    lat = (rawReq["lat"] as? Number)?.toDouble() ?: 24.8,
                                    lng = (rawReq["lng"] as? Number)?.toDouble() ?: 46.6,
                                    createdAt = (rawReq["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    ratingStars = (rawReq["ratingStars"] as? Number)?.toInt() ?: 0,
                                    ratingComment = rawReq["ratingComment"] as? String
                                )
                                dao.insertRequest(req)
                                insertedCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Error mapping service request: ${e.localizedMessage}")
                            }
                        }
                        _syncStatus.value = SupabaseSyncStatus.CONNECTED
                        _statusMessage.value = "تمت مزامنة $insertedCount طلب صيانة بنجاح"
                    }
                    404 -> {
                        _syncStatus.value = SupabaseSyncStatus.TABLE_NOT_FOUND
                        _statusMessage.value = "جدول 'service_requests' غير موجود في Supabase."
                    }
                    else -> {
                        _syncStatus.value = SupabaseSyncStatus.ERROR
                        _statusMessage.value = "فشلت مزامنة طلبات الصيانة: ${response.code}"
                    }
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = SupabaseSyncStatus.ERROR
            _statusMessage.value = "خطأ اتصال بطلبات الصيانة: ${e.localizedMessage}"
        }
    }

    suspend fun sendRequest(request: ServiceRequestEntity) = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext
        try {
            val url = "$supabaseUrl/rest/v1/service_requests"
            val reqMap = mapOf(
                "customerId" to request.customerId,
                "customerName" to request.customerName,
                "customerPhone" to request.customerPhone,
                "techId" to request.techId,
                "techName" to request.techName,
                "techPhone" to request.techPhone,
                "serviceType" to request.serviceType,
                "description" to request.description,
                "status" to request.status,
                "urgency" to request.urgency,
                "timeRequired" to request.timeRequired,
                "neighborhood" to request.neighborhood,
                "lat" to request.lat,
                "lng" to request.lng,
                "createdAt" to request.createdAt,
                "ratingStars" to request.ratingStars,
                "ratingComment" to request.ratingComment
            )
            val adapter = moshi.adapter(Map::class.java)
            val bodyJson = adapter.toJson(reqMap)
            val req = buildRequest(url, "POST", bodyJson)
            
            client.newCall(req).execute().use { response ->
                Log.d(TAG, "Send request result: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send request error: ${e.localizedMessage}")
        }
    }
}
