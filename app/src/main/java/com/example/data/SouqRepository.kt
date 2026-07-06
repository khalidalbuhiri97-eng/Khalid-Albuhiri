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
        lng: Double
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
            lng = lng
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
        authorAvatarUri: String? = null
    ) {
        dao.insertPost(
            PostEntity(
                authorId = authorId,
                authorName = authorName,
                authorRole = authorRole,
                authorAvatarColor = avatarColor,
                content = content,
                neighborhood = neighborhood,
                authorAvatarUri = authorAvatarUri
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
        val usersCount = dao.getAllUsers().firstOrNull()?.size ?: 0
        if (usersCount == 0) {
            // Prepopulate some default test users
            val testUsers = listOf(
                // Customers
                UserEntity(
                    uid = "cust_khaled",
                    name = "خالد البحيصي",
                    phoneNumber = "0511111111",
                    role = "CUSTOMER",
                    city = "الرياض",
                    neighborhood = "حي الياسمين",
                    lat = 24.81,
                    lng = 46.63,
                    avatarColor = 0,
                    bio = "أبحث عن أفضل جودة وسرعة استجابة للخدمات المنزلية.",
                    avatarUri = "https://api.dicebear.com/7.x/adventurer/png?seed=khaled"
                ),
                UserEntity(
                    uid = "cust_sara",
                    name = "سارة أحمد",
                    phoneNumber = "0522222222",
                    role = "CUSTOMER",
                    city = "الرياض",
                    neighborhood = "حي الروضة",
                    lat = 24.73,
                    lng = 46.77,
                    avatarColor = 1,
                    bio = "مرحباً بكم، مهتمة بمتابعة عروض الصيانة الدورية لمنزلي.",
                    avatarUri = "https://api.dicebear.com/7.x/adventurer/png?seed=sara"
                ),
                UserEntity(
                    uid = "tech_ahmed",
                    name = "أحمد النجّار",
                    phoneNumber = "0533333333",
                    role = "TECHNICIAN",
                    city = "الرياض",
                    neighborhood = "حي الياسمين",
                    lat = 24.805,
                    lng = 46.635,
                    avatarColor = 2,
                    bio = "نجار محترف لتركيب وتصليح جميع أنواع غرف النوم، المطابخ، والأبواب الخشبية. خبرة في الفك والتركيب.",
                    profession = "نجارة",
                    experienceYears = 7,
                    isOnline = true,
                    rating = 4.8,
                    completedOrders = 42,
                    avatarUri = "https://api.dicebear.com/7.x/adventurer/png?seed=ahmed"
                ),
                UserEntity(
                    uid = "tech_mohammed",
                    name = "محمد الكهربائي",
                    phoneNumber = "0544444444",
                    role = "TECHNICIAN",
                    city = "الرياض",
                    neighborhood = "حي الروضة",
                    lat = 24.735,
                    lng = 46.772,
                    avatarColor = 3,
                    bio = "متخصص في تأسيس وصيانة شبكات الكهرباء المنزلية وتصليح الأعطال وتركيب الإنارة الحديثة والديكورات.",
                    profession = "كهرباء",
                    experienceYears = 10,
                    isOnline = true,
                    rating = 4.9,
                    completedOrders = 85,
                    avatarUri = "https://api.dicebear.com/7.x/adventurer/png?seed=mohammed"
                ),

                UserEntity(
                    uid = "tech_khaled",
                    name = "خالد السباك",
                    phoneNumber = "0555555555",
                    role = "TECHNICIAN",
                    city = "الرياض",
                    neighborhood = "حي النخيل",
                    lat = 24.75,
                    lng = 46.61,
                    avatarColor = 4,
                    bio = "صيانة وتأسيس شبكات السباكة والصرف الصحي وكشف تسريبات المياه بدقة عالية بأحدث الأجهزة.",
                    profession = "سباكة",
                    experienceYears = 5,
                    isOnline = true,
                    rating = 4.6,
                    completedOrders = 29
                ),
                UserEntity(
                    uid = "tech_abdulrahman",
                    name = "عبد الرحمن للتكييف",
                    phoneNumber = "0566666666",
                    role = "TECHNICIAN",
                    city = "الرياض",
                    neighborhood = "حي الياسمين",
                    lat = 24.812,
                    lng = 46.628,
                    avatarColor = 5,
                    bio = "صيانة وتنظيف وفك وتركيب مكيفات السبليت والدولابي مع شحن الفريون وقطع غيار أصلية.",
                    profession = "تكييف",
                    experienceYears = 8,
                    isOnline = true,
                    rating = 4.7,
                    completedOrders = 64
                ),
                UserEntity(
                    uid = "tech_yasser",
                    name = "ياسر الدهان",
                    phoneNumber = "0577777777",
                    role = "TECHNICIAN",
                    city = "الرياض",
                    neighborhood = "حي الروضة",
                    lat = 24.728,
                    lng = 46.778,
                    avatarColor = 6,
                    bio = "تنفيذ أرقى أصباغ الجدران والديكورات الداخلية والورق الجدران الخارجي والداخلي بدقة وبأسعار ممتازة.",
                    profession = "دهان",
                    experienceYears = 6,
                    isOnline = false,
                    rating = 4.5,
                    completedOrders = 37
                ),
                // Admin
                UserEntity(
                    uid = "admin_super",
                    name = "مدير النظام (أبو فهد)",
                    phoneNumber = "0599999999",
                    role = "ADMIN",
                    city = "الرياض",
                    neighborhood = "حي الياسمين",
                    lat = 24.8,
                    lng = 46.6,
                    avatarColor = 7,
                    bio = "مدير عام منصة سوق للخدمات المحلية في الرياض."
                )
            )

            testUsers.forEach { dao.insertUser(it) }

            // Insert initial Neighborhood Posts
            val testPosts = listOf(
                PostEntity(
                    authorId = "cust_khaled",
                    authorName = "خالد البحيصي",
                    authorRole = "CUSTOMER",
                    authorAvatarColor = 0,
                    content = "السلام عليكم يا جيراننا، أبحث عن فني تكييف أمين ومجرب لتنظيف 4 مكيفات سبليت في بيتي. الجو بدأ يسخن ومحتاج غسيل ممتاز.",
                    neighborhood = "حي الياسمين",
                    likesCount = 5,
                    commentsCount = 2
                ),
                PostEntity(
                    authorId = "tech_mohammed",
                    authorName = "محمد الكهربائي",
                    authorRole = "TECHNICIAN",
                    authorAvatarColor = 3,
                    content = "نصيحة فنية لأهل الحي: في الصيف مع زيادة الأحمال، يُفضل فحص مفاتيح الطبلون الرئيسية والتأكد من عدم وجود حرارة زائدة لتفادي الالتماسات. صيفاً آمناً للجميع!",
                    neighborhood = "حي الروضة",
                    likesCount = 18,
                    commentsCount = 4
                ),
                PostEntity(
                    authorId = "cust_sara",
                    authorName = "سارة أحمد",
                    authorRole = "CUSTOMER",
                    authorAvatarColor = 1,
                    content = "تجربة ممتازة اليوم مع منصة سوق! طلبت فني سباكة وجاني خالد السباك في أقل من ساعة وحل مشكلة تسريب المطبخ بسرعة وبدون تعقيدات. أنصح به بشدة.",
                    neighborhood = "حي الروضة",
                    likesCount = 12,
                    commentsCount = 1
                )
            )
            testPosts.forEach { dao.insertPost(it) }

            // Insert initial offers
            val testOffers = listOf(
                OfferEntity(
                    techId = "tech_abdulrahman",
                    techName = "عبد الرحمن للتكييف",
                    techProfession = "تكييف",
                    techAvatarColor = 5,
                    title = "عرض الصيف الخاص: غسيل 3 مكيفات سبليت بمضخة الضغط",
                    description = "غسيل خارجي وداخلي بالجراب الوقائي لحماية الأثاث والجدران، مع فحص مجاني لمستوى الفريون وضمان لمدة شهر.",
                    price = "199 ريال"
                ),
                OfferEntity(
                    techId = "tech_mohammed",
                    techName = "محمد الكهربائي",
                    techProfession = "كهرباء",
                    techAvatarColor = 3,
                    title = "عرض تأسيس إنارة ديكورية (ليد سبوت لايت)",
                    description = "تعديل وتأسيس إضاءة مخفية وإنارة ديكورية حديثة للصالات والمجالس بخصم 25%. الفحص والتقدير مجاني.",
                    price = "يبدأ من 150 ريال"
                ),
                OfferEntity(
                    techId = "tech_ahmed",
                    techName = "أحمد النجّار",
                    techProfession = "نجارة",
                    techAvatarColor = 2,
                    title = "صيانة وتزييت الأبواب الخشبية وعلاج الخدوش",
                    description = "صيانة الأبواب وحل مشكلة الأصوات المزعجة وتعديل الأقفال والمفاصل المهترئة مع طلاء واقٍ مجاني للخدوش السطحية.",
                    price = "99 ريال"
                )
            )
            testOffers.forEach { dao.insertOffer(it) }

            // Insert some completed requests for statistics
            val testRequests = listOf(
                ServiceRequestEntity(
                    customerId = "cust_khaled",
                    customerName = "خالد البحيصي",
                    customerPhone = "0511111111",
                    techId = "tech_ahmed",
                    techName = "أحمد النجّار",
                    techPhone = "0533333333",
                    serviceType = "نجارة",
                    description = "إصلاح دولاب ملابس مكسور في غرفة النوم وتغيير المفصلات التالفة",
                    status = "COMPLETED",
                    urgency = "متوسط",
                    timeRequired = "اليوم مساءً",
                    neighborhood = "حي الياسمين",
                    lat = 24.81,
                    lng = 46.63,
                    ratingStars = 5,
                    ratingComment = "عمل رائع وسريع والنجار أخلاقه عالية جداً وأنصح بالتعامل معه."
                ),
                ServiceRequestEntity(
                    customerId = "cust_sara",
                    customerName = "سارة أحمد",
                    customerPhone = "0522222222",
                    techId = "tech_mohammed",
                    techName = "محمد الكهربائي",
                    techPhone = "0544444444",
                    serviceType = "كهرباء",
                    description = "تركيب ثريا جديدة في الصالة مع مفتاح تعتيم ذكي",
                    status = "COMPLETED",
                    urgency = "عادي",
                    timeRequired = "غداً صباحاً",
                    neighborhood = "حي الروضة",
                    lat = 24.73,
                    lng = 46.77,
                    ratingStars = 5,
                    ratingComment = "مبدع ومتقن لعمله وأنصح بالتعامل معه لجميع خدمات الكهرباء."
                )
            )
            testRequests.forEach { dao.insertRequest(it) }

            // Insert initial Jobs
            val testJobs = listOf(
                JobEntity(
                    title = "مطلوب معلم خصوصي لتدريس الرياضيات (ثانوي)",
                    category = "تعليم",
                    description = "أبحث عن معلم متمكن لتدريس ابني في الصف الثاني ثانوي لمادة الرياضيات مجاور لحي الياسمين.",
                    postedBy = "خالد البحيصي",
                    posterId = "cust_khaled",
                    posterPhone = "0511111111",
                    payment = "١٢٠ ريال / ساعة",
                    neighborhood = "حي الياسمين",
                    jobType = "عمل حر / مؤقت"
                ),
                JobEntity(
                    title = "مطلوب موصل شحنات جزئي لمتجر بالحي",
                    category = "توصيل",
                    description = "متجر تمور بالحي يبحث عن شاب نشيط لتوصيل الطلبات داخل الحي وخارجه لفترة المساء من ٤م إلى ٩م.",
                    postedBy = "سارة أحمد",
                    posterId = "cust_sara",
                    posterPhone = "0522222222",
                    payment = "٢٥٠٠ ريال شهرياً",
                    neighborhood = "حي الروضة",
                    jobType = "دوام جزئي"
                ),
                JobEntity(
                    title = "فني صيانة دورية لحديقة المجمع السكني",
                    category = "صيانة",
                    description = "مطلوب فني زراعي لقص الحشائش وتنسيق الحديقة العامة للمجمع السكني مرة كل أسبوعين.",
                    postedBy = "مدير النظام (أبو فهد)",
                    posterId = "admin_super",
                    posterPhone = "0599999999",
                    payment = "٣٥٠ ريال شهرياً",
                    neighborhood = "حي الياسمين",
                    jobType = "عمل حر / مؤقت"
                ),
                JobEntity(
                    title = "بائعة دوام جزئي في كشك عطور ومستحضرات تجميل",
                    category = "أخرى",
                    description = "مطلوب بائعة متمكنة ولبقة للعمل في كشك عطور بمجمع تجاري لفترة المساء من ٥م إلى ١٠م.",
                    postedBy = "سارة أحمد",
                    posterId = "cust_sara",
                    posterPhone = "0522222222",
                    payment = "٣٠٠٠ ريال شهرياً",
                    neighborhood = "حي الياسمين",
                    jobType = "دوام جزئي"
                ),
                JobEntity(
                    title = "مطلوب مصمم جرافيك لتهيئة حسابات التواصل الاجتماعي",
                    category = "أخرى",
                    description = "أبحث عن مصمم محترف لتصميم بنرات ومنشورات انستغرام وتويتر لمتجر محلي ناشئ بالحي.",
                    postedBy = "خالد البحيصي",
                    posterId = "cust_khaled",
                    posterPhone = "0511111111",
                    payment = "١٥٠٠ ريال مقطوع",
                    neighborhood = "حي الروضة",
                    jobType = "عمل حر / مؤقت"
                ),
                JobEntity(
                    title = "معلمة لغة إنجليزية للتأسيس والمتابعة للأطفال",
                    category = "تعليم",
                    description = "مطلوب معلمة ذات خبرة لتأسيس طفلين في المرحلة الابتدائية بمادة اللغة الإنجليزية ٣ مرات أسبوعياً بموقعنا.",
                    postedBy = "سلوى محمد",
                    posterId = "cust_salwa",
                    posterPhone = "0533333333",
                    payment = "١٥٠ ريال / حصة",
                    neighborhood = "حي الياسمين",
                    jobType = "عمل حر / مؤقت"
                ),
                JobEntity(
                    title = "مندوب توصيل شحنات وتوزيع سريعة داخل الحي",
                    category = "توصيل",
                    description = "مطلوب مندوب بسيارته لتوصيل طلبات تموينية وخضار لسكان الحي التابعين لجمعية تعاونية محلية.",
                    postedBy = "مدير النظام (أبو فهد)",
                    posterId = "admin_super",
                    posterPhone = "0599999999",
                    payment = "١٥ ريال لكل طلب",
                    neighborhood = "حي الياسمين",
                    jobType = "عمل حر / مؤقت"
                )
            )
            testJobs.forEach { dao.insertJob(it) }

            // Insert initial channel messages
            val testChannelMessages = listOf(
                // General
                ChannelMessageEntity(
                    channelId = "general",
                    senderId = "cust_khaled",
                    senderName = "خالد البحيصي",
                    senderAvatarColor = 0,
                    senderRole = "CUSTOMER",
                    content = "السلام عليكم يا جيران، وش رأيكم في إنارة ممشى الحي الجديدة؟"
                ),
                ChannelMessageEntity(
                    channelId = "general",
                    senderId = "cust_sara",
                    senderName = "سارة أحمد",
                    senderAvatarColor = 1,
                    senderRole = "CUSTOMER",
                    content = "وعليكم السلام، خطوة ممتازة جداً وتشجع على رياضة المشي في المساء مع الأبناء."
                ),
                ChannelMessageEntity(
                    channelId = "general",
                    senderId = "tech_ahmed",
                    senderName = "أحمد النجّار",
                    senderAvatarColor = 2,
                    senderRole = "TECHNICIAN",
                    content = "ما شاء الله، حركية جميلة بالحي والإنارة مريحة جداً للعين."
                ),
                // News
                ChannelMessageEntity(
                    channelId = "news",
                    senderId = "admin_super",
                    senderName = "مدير النظام (أبو فهد)",
                    senderAvatarColor = 7,
                    senderRole = "ADMIN",
                    content = "📢 تنبيه: سيتم إغلاق المخرج الشرقي للحي غداً لأعمال الصيانة الإسفلتية من الساعة ٨ صباحاً وحتى ٤ مساءً. نرجو استخدام المخرج الجنوبي البديل."
                ),
                ChannelMessageEntity(
                    channelId = "news",
                    senderId = "cust_khaled",
                    senderName = "خالد البحيصي",
                    senderAvatarColor = 0,
                    senderRole = "CUSTOMER",
                    content = "شكراً جزيلاً لجهودكم وتنبيهكم يا بو فهد."
                ),
                // Services
                ChannelMessageEntity(
                    channelId = "services",
                    senderId = "tech_ahmed",
                    senderName = "أحمد النجّار",
                    senderAvatarColor = 2,
                    senderRole = "TECHNICIAN",
                    content = "يا جيران، أي شخص عنده أبواب خشب تصدر صريراً بسبب الرطوبة، يكتب سؤاله هنا وسأجيبكم مجاناً بطرق علاجها السهلة."
                ),
                ChannelMessageEntity(
                    channelId = "services",
                    senderId = "tech_khaled",
                    senderName = "خالد السباك",
                    senderAvatarColor = 4,
                    senderRole = "TECHNICIAN",
                    content = "مبادرة رائعة يا بو سليمان! وأنا أيضاً بخدمتكم لأي استفسار حول كشف تسريبات المياه أو ضعف تدفق المياه بالحنفيات."
                ),
                ChannelMessageEntity(
                    channelId = "services",
                    senderId = "cust_sara",
                    senderName = "سارة أحمد",
                    senderAvatarColor = 1,
                    senderRole = "CUSTOMER",
                    content = "الله يجزاكم خير فنيينا المبدعين على هذا الدعم والتعاون المستمر لأهل الحي."
                ),
                // Market
                ChannelMessageEntity(
                    channelId = "market",
                    senderId = "cust_khaled",
                    senderName = "خالد البحيصي",
                    senderAvatarColor = 0,
                    senderRole = "CUSTOMER",
                    content = "عندي طاولة طعام خشبية ممتازة بـ ٦ كراسي، شبه جديدة وخالية من العيوب، للبيع لأعلى سومة والتحميل على المشتري."
                ),
                ChannelMessageEntity(
                    channelId = "market",
                    senderId = "tech_ahmed",
                    senderName = "أحمد النجّار",
                    senderAvatarColor = 2,
                    senderRole = "TECHNICIAN",
                    content = "هل بالإمكان تصوير الطاولة يا بو سليمان؟ عندي زبون يبحث عن طاولة خشب جيدة."
                )
            )
            testChannelMessages.forEach { dao.insertChannelMessage(it) }
        }
    }
}
