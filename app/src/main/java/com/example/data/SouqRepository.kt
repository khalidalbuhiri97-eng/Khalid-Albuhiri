package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class SouqRepository(private val dao: SouqDao) {

    // --- Active User Simulation (Memory-based for easy switching) ---
    var currentUser: UserEntity? = null
        private set

    fun setCurrentUser(user: UserEntity?) {
        currentUser = user
    }

    // --- Users ---
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    val allTechnicians: Flow<List<UserEntity>> = dao.getAllTechnicians()

    fun getUser(uid: String): Flow<UserEntity?> = dao.getUserById(uid)
    suspend fun getUserSuspend(uid: String): UserEntity? = dao.getUserByIdSuspend(uid)

    fun getTechniciansByProfession(profession: String): Flow<List<UserEntity>> =
        dao.getTechniciansByProfession(profession)

    suspend fun insertUser(user: UserEntity) {
        dao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        dao.updateUser(user)
        if (currentUser?.uid == user.uid) {
            currentUser = user
        }
    }


    // --- Service Requests ---
    val allRequests: Flow<List<ServiceRequestEntity>> = dao.getAllRequests()

    fun getRequestsByCustomer(customerId: String): Flow<List<ServiceRequestEntity>> =
        dao.getRequestsByCustomer(customerId)

    fun getRequestsByTech(techId: String): Flow<List<ServiceRequestEntity>> =
        dao.getRequestsByTech(techId)

    fun getRequestById(id: Int): Flow<ServiceRequestEntity?> = dao.getRequestById(id)

    suspend fun getRequestByIdSuspend(id: Int): ServiceRequestEntity? = dao.getRequestByIdSuspend(id)

    fun getPendingRequestsForTech(profession: String): Flow<List<ServiceRequestEntity>> =
        dao.getPendingRequestsForTech(profession)

    suspend fun createRequest(
        customerId: String,
        customerName: String,
        customerPhone: String,
        serviceType: String,
        description: String,
        urgency: String,
        timeRequired: String,
        neighborhood: String,
        lat: Double,
        lng: Double,
        imageUri: String? = null
    ): Long {
        val request = ServiceRequestEntity(
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            serviceType = serviceType,
            description = description,
            status = "NEW",
            urgency = urgency,
            timeRequired = timeRequired,
            neighborhood = neighborhood,
            lat = lat,
            lng = lng,
            imageUri = imageUri
        )
        val id = dao.insertRequest(request)

        // Trigger notifications to nearby technicians of this profession
        val technicians = dao.getTechniciansByProfession(serviceType).firstOrNull() ?: emptyList()
        technicians.forEach { tech ->
            dao.insertNotification(
                NotificationEntity(
                    recipientId = tech.uid,
                    title = "طلب خدمة جديد في ${neighborhood}",
                    content = "طلب $serviceType: $description"
                )
            )
        }

        return id
    }

    suspend fun acceptRequest(requestId: Int, techId: String, techName: String, techPhone: String): Boolean {
        // Run simulated Transaction
        val req = dao.getRequestByIdSuspend(requestId)
        if (req != null && (req.status == "NEW" || req.status == "PENDING")) {
            val updated = req.copy(
                status = "ACCEPTED",
                techId = techId,
                techName = techName,
                techPhone = techPhone
            )
            dao.updateRequest(updated)

            // Notify Customer
            dao.insertNotification(
                NotificationEntity(
                    recipientId = req.customerId,
                    title = "تم قبول طلبك!",
                    content = "قَبِل الفني $techName طلبك وهو الآن يتابع معك."
                )
            )

            // Add auto-generated system welcome message
            dao.insertMessage(
                MessageEntity(
                    requestId = requestId,
                    senderId = "SYSTEM",
                    senderName = "النظام",
                    content = "تم قبول الطلب من قبل الفني $techName. يمكنك الآن التواصل معه مباشرة عبر الدردشة أو الاتصال."
                )
            )
            return true
        }
        return false
    }

    suspend fun updateRequestStatus(requestId: Int, newStatus: String) {
        val req = dao.getRequestByIdSuspend(requestId) ?: return
        val updated = req.copy(status = newStatus)
        dao.updateRequest(updated)

        // Title and body translation for notification
        val title = when (newStatus) {
            "ON_WAY" -> "الفني في الطريق"
            "STARTED" -> "بدأ العمل"
            "COMPLETED" -> "اكتمل العمل"
            "CANCELLED" -> "تم إلغاء الطلب"
            else -> "تحديث للطلب"
        }

        val content = when (newStatus) {
            "ON_WAY" -> "الفني ${req.techName ?: "المختص"} في الطريق إليك الآن."
            "STARTED" -> "الفني بدأ العمل على حل المشكلة."
            "COMPLETED" -> "تم إنجاز طلبك بنجاح! الرجاء تقييم الفني."
            "CANCELLED" -> "تم إلغاء الطلب الخاص بك."
            else -> "تغيرت حالة طلبك إلى: $newStatus"
        }

        // Notify customer
        dao.insertNotification(
            NotificationEntity(
                recipientId = req.customerId,
                title = title,
                content = content
            )
        )

        // Notify tech if customer cancelled
        if (newStatus == "CANCELLED" && req.techId != null) {
            dao.insertNotification(
                NotificationEntity(
                    recipientId = req.techId,
                    title = "تم إلغاء الطلب",
                    content = "قام العميل بإلغاء الطلب رقم #$requestId."
                )
            )
        }

        // If completed, increment completed orders for technician
        if (newStatus == "COMPLETED" && req.techId != null) {
            val tech = dao.getUserByIdSuspend(req.techId)
            if (tech != null) {
                dao.updateUser(tech.copy(completedOrders = tech.completedOrders + 1))
            }
        }
    }

    suspend fun submitRating(requestId: Int, stars: Int, comment: String) {
        val req = dao.getRequestByIdSuspend(requestId) ?: return
        val updated = req.copy(
            ratingStars = stars,
            ratingComment = comment,
            status = "COMPLETED" // Ensure status is marked completed
        )
        dao.updateRequest(updated)

        val techId = req.techId ?: return
        val tech = dao.getUserByIdSuspend(techId) ?: return

        // Recalculate rating
        val prevCount = tech.completedOrders.coerceAtLeast(1)
        val newRating = ((tech.rating * (prevCount - 1)) + stars) / prevCount
        dao.updateUser(
            tech.copy(
                rating = String.format("%.1f", newRating).toDoubleOrNull() ?: 4.5
            )
        )

        // Notify Technician
        dao.insertNotification(
            NotificationEntity(
                recipientId = techId,
                title = "تقييم جديد ★ $stars",
                content = "قام العميل ${req.customerName} بتقييمك: \"$comment\""
            )
        )
    }


    // --- Neighborhood Posts ---
    val allPosts: Flow<List<PostEntity>> = dao.getAllPosts()

    suspend fun createPost(
        authorId: String,
        authorName: String,
        authorRole: String,
        avatarColor: Int,
        content: String,
        neighborhood: String,
        authorAvatarUri: String? = null,
        imageUri: String? = null
    ) {
        dao.insertPost(
            PostEntity(
                authorId = authorId,
                authorName = authorName,
                authorRole = authorRole,
                authorAvatarColor = avatarColor,
                content = content,
                neighborhood = neighborhood,
                authorAvatarUri = authorAvatarUri,
                imageUri = imageUri
            )
        )
    }

    suspend fun likePost(postId: Int) {
        val post = dao.getPostById(postId)
        if (post != null) {
            val updatedIsLiked = !post.isLikedByMe
            val updatedLikesCount = if (updatedIsLiked) post.likesCount + 1 else maxOf(0, post.likesCount - 1)
            dao.updatePost(
                post.copy(
                    isLikedByMe = updatedIsLiked,
                    likesCount = updatedLikesCount
                )
            )
        }
    }

    suspend fun incrementCommentsCount(postId: Int) {
        val post = dao.getPostById(postId)
        if (post != null) {
            dao.updatePost(post.copy(commentsCount = post.commentsCount + 1))
        }
    }


    // --- Offers ---
    val allOffers: Flow<List<OfferEntity>> = dao.getAllOffers()

    suspend fun createOffer(
        techId: String,
        techName: String,
        profession: String,
        avatarColor: Int,
        title: String,
        description: String,
        price: String,
        techAvatarUri: String? = null
    ) {
        dao.insertOffer(
            OfferEntity(
                techId = techId,
                techName = techName,
                techProfession = profession,
                techAvatarColor = avatarColor,
                title = title,
                description = description,
                price = price,
                techAvatarUri = techAvatarUri
            )
        )
    }

    suspend fun deleteOffer(id: Int) = dao.deleteOfferById(id)


    // --- Messages ---
    fun getMessagesForRequest(requestId: Int): Flow<List<MessageEntity>> =
        dao.getMessagesForRequest(requestId)

    suspend fun sendMessage(requestId: Int, senderId: String, senderName: String, content: String, type: String = "TEXT") {
        dao.insertMessage(
            MessageEntity(
                requestId = requestId,
                senderId = senderId,
                senderName = senderName,
                content = content,
                type = type
            )
        )

        // Also notify recipient
        val req = dao.getRequestByIdSuspend(requestId) ?: return
        val recipientId = if (senderId == req.customerId) req.techId else req.customerId
        if (recipientId != null) {
            dao.insertNotification(
                NotificationEntity(
                    recipientId = recipientId,
                    title = "رسالة جديدة من $senderName",
                    content = if (type == "TEXT") content else "[مرفق]"
                )
            )
        }
    }


    // --- Notifications ---
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>> =
        dao.getNotificationsForUser(userId)

    suspend fun markAllRead(userId: String) {
        dao.markNotificationsAsRead(userId)
    }

    // --- Jobs / Gigs ---
    val allJobs: Flow<List<JobEntity>> = dao.getAllJobs()

    suspend fun createJob(
        title: String,
        category: String,
        description: String,
        postedBy: String,
        posterId: String,
        posterPhone: String,
        payment: String,
        neighborhood: String,
        jobType: String,
        posterAvatarUri: String? = null
    ) {
        val job = JobEntity(
            title = title,
            category = category,
            description = description,
            postedBy = postedBy,
            posterId = posterId,
            posterPhone = posterPhone,
            payment = payment,
            neighborhood = neighborhood,
            jobType = jobType,
            posterAvatarUri = posterAvatarUri
        )
        dao.insertJob(job)
    }

    suspend fun applyToJob(jobId: Int) {
        val job = dao.getJobById(jobId) ?: return
        val updated = job.copy(
            applicantsCount = job.applicantsCount + 1,
            isAppliedByMe = true
        )
        dao.updateJob(updated)
    }

    // --- Channel Messages ---
    fun getChannelMessages(channelId: String): Flow<List<ChannelMessageEntity>> =
        dao.getChannelMessages(channelId)

    suspend fun createChannelMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        senderAvatarColor: Int,
        senderRole: String,
        content: String,
        senderAvatarUri: String? = null
    ) {
        val msg = ChannelMessageEntity(
            channelId = channelId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarColor = senderAvatarColor,
            senderRole = senderRole,
            content = content,
            senderAvatarUri = senderAvatarUri
        )
        dao.insertChannelMessage(msg)
    }

    // --- Prepulate Database if empty ---
    suspend fun checkAndPrepopulate(context: Context) {
        // Prepopulation of fake users and data disabled as requested to start with clean data
    }
}
