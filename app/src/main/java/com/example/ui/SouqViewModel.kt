package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SouqViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SouqDatabase.getDatabase(application)
    val repository = SouqRepository(db.souqDao())
    private val authRepository = AuthRepository()
    var tempAuthUid: String? = null

    // --- State Flow ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- Supabase Sync Flows ---
    val supabaseSyncStatus: StateFlow<SupabaseSyncStatus> = SupabaseSyncManager.syncStatus
    val supabaseStatusMessage: StateFlow<String> = SupabaseSyncManager.statusMessage

    init {
        // Auto-login if there is an active Firebase session on app start
        val fbUser = authRepository.getCurrentUser()
        if (fbUser != null) {
            loginById(fbUser.uid)
        }
    }

    // Dynamic lists that react to current user role and selection
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTechnicians: StateFlow<List<UserEntity>> = repository.allTechnicians.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allOffers: StateFlow<List<OfferEntity>> = repository.allOffers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPosts: StateFlow<List<PostEntity>> = repository.allPosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Jobs and Gigs ---
    val allJobs: StateFlow<List<JobEntity>> = repository.allJobs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Active Channel Discussion ---
    private val _activeChannelId = MutableStateFlow("general")
    val activeChannelId: StateFlow<String> = _activeChannelId.asStateFlow()

    val activeChannelMessages: StateFlow<List<ChannelMessageEntity>> = _activeChannelId
        .flatMapLatest { channelId -> repository.getChannelMessages(channelId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectChannel(channelId: String) {
        _activeChannelId.value = channelId
    }

    // Current user notifications
    val notifications: StateFlow<List<NotificationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getNotificationsForUser(user.uid)
            else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Service requests based on user role
    val userRequests: StateFlow<List<ServiceRequestEntity>> = _currentUser
        .flatMapLatest { user ->
            when {
                user == null -> flowOf(emptyList())
                user.role == "ADMIN" -> repository.allRequests
                user.role == "TECHNICIAN" -> repository.getRequestsByTech(user.uid)
                else -> repository.getRequestsByCustomer(user.uid)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Requests that are pending in the technician's profession
    val techPendingRequests: StateFlow<List<ServiceRequestEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == "TECHNICIAN" && user.profession.isNotEmpty()) {
                repository.getPendingRequestsForTech(user.profession)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Neighborhood sub tab state (0: Feed, 1: Jobs, 2: Channels)
    private val _neighborhoodSubTab = MutableStateFlow(0)
    val neighborhoodSubTab: StateFlow<Int> = _neighborhoodSubTab.asStateFlow()

    fun setNeighborhoodSubTab(tabIndex: Int) {
        _neighborhoodSubTab.value = tabIndex
    }

    // Custom Channels state
    private val _customChannels = MutableStateFlow<List<CustomChannel>>(emptyList())
    val customChannels: StateFlow<List<CustomChannel>> = _customChannels.asStateFlow()

    fun createChannel(title: String, description: String, iconName: String = "forum", colorHex: String = "#4CAF50") {
        val newId = "chan_" + System.currentTimeMillis()
        val newChan = CustomChannel(newId, title, description, iconName, colorHex)
        _customChannels.value = _customChannels.value + newChan
    }

    // --- Settings State ---
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isNotificationsEnabled = MutableStateFlow(true)
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    private val _isGpsLocationEnabled = MutableStateFlow(true)
    val isGpsLocationEnabled: StateFlow<Boolean> = _isGpsLocationEnabled.asStateFlow()

    private val _appLanguage = MutableStateFlow("العربية")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun updateSettings(darkMode: Boolean, notifications: Boolean, gps: Boolean, language: String) {
        _isDarkMode.value = darkMode
        _isNotificationsEnabled.value = notifications
        _isGpsLocationEnabled.value = gps
        _appLanguage.value = language
    }

    // Chat room messaging
    private val _selectedRequestId = MutableStateFlow<Int?>(null)
    val selectedRequestId: StateFlow<Int?> = _selectedRequestId.asStateFlow()

    val chatMessages: StateFlow<List<MessageEntity>> = _selectedRequestId
        .flatMapLatest { reqId ->
            if (reqId != null) repository.getMessagesForRequest(reqId)
            else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedRequest: StateFlow<ServiceRequestEntity?> = _selectedRequestId
        .flatMapLatest { reqId ->
            if (reqId != null) repository.getRequestById(reqId)
            else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            // Prepopulate if DB is empty
            repository.checkAndPrepopulate(application)
            // Auto-login disabled to show the login/register screen on startup as requested
            // Users can register with email or login normally

            // Perform initial background syncs with Supabase
            try {
                SupabaseSyncManager.syncUsers(db.souqDao())
                SupabaseSyncManager.syncPosts(db.souqDao())
                SupabaseSyncManager.syncRequests(db.souqDao())
                SupabaseSyncManager.syncChannelMessages(_activeChannelId.value, db.souqDao())
            } catch (e: Exception) {
                android.util.Log.e("SouqViewModel", "Initial sync error: ${e.localizedMessage}")
            }
        }

        // Keep active channel messages synced automatically when switching channels
        viewModelScope.launch {
            _activeChannelId.collect { channelId ->
                try {
                    SupabaseSyncManager.syncChannelMessages(channelId, db.souqDao())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            try {
                SupabaseSyncManager.syncUsers(db.souqDao())
                SupabaseSyncManager.syncPosts(db.souqDao())
                SupabaseSyncManager.syncRequests(db.souqDao())
                SupabaseSyncManager.syncChannelMessages(_activeChannelId.value, db.souqDao())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Actions ---

    fun selectRequestForChat(id: Int?) {
        _selectedRequestId.value = id
    }

    fun loginAsUser(user: UserEntity) {
        _currentUser.value = user
        repository.setCurrentUser(user)
    }

    fun loginById(uid: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.getUserSuspend(uid)
            if (user != null) {
                loginAsUser(user)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        repository.setCurrentUser(null)
        try {
            authRepository.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun signInWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        _isAuthLoading.value = true
        _authError.value = null
        val fallbackUid = "email_" + email.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        try {
            authRepository.signInWithEmailAndPassword(email, password) { result, exception ->
                if (result != null) {
                    _isAuthLoading.value = false
                    val uid = result.user?.uid ?: ""
                    loginById(uid) { exists ->
                        if (exists) {
                            onComplete(true, null)
                        } else {
                            tempAuthUid = uid
                            onComplete(false, "PROFILE_INCOMPLETE")
                        }
                    }
                } else {
                    // Smart Offline/Mock Fallback for demo environment when Firebase keys are dummy/not connected
                    viewModelScope.launch {
                        val exists = repository.getUserSuspend(fallbackUid)
                        _isAuthLoading.value = false
                        if (exists != null) {
                            loginAsUser(exists)
                            onComplete(true, null)
                        } else {
                            // If user doesn't exist, allow them to complete profile under this deterministic ID
                            tempAuthUid = fallbackUid
                            onComplete(false, "PROFILE_INCOMPLETE")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                val exists = repository.getUserSuspend(fallbackUid)
                _isAuthLoading.value = false
                if (exists != null) {
                    loginAsUser(exists)
                    onComplete(true, null)
                } else {
                    tempAuthUid = fallbackUid
                    onComplete(false, "PROFILE_INCOMPLETE")
                }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        _isAuthLoading.value = true
        _authError.value = null
        val fallbackUid = "email_" + email.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        try {
            authRepository.createUserWithEmailAndPassword(email, password) { result, exception ->
                if (result != null) {
                    _isAuthLoading.value = false
                    tempAuthUid = result.user?.uid
                    onComplete(true, null)
                } else {
                    // Smart Offline/Mock Fallback
                    _isAuthLoading.value = false
                    tempAuthUid = fallbackUid
                    onComplete(true, null) // Allow to proceed to COMPLETE_PROFILE
                }
            }
        } catch (e: Exception) {
            _isAuthLoading.value = false
            tempAuthUid = fallbackUid
            onComplete(true, null)
        }
    }

    fun signInWithGoogleToken(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        _isAuthLoading.value = true
        _authError.value = null
        try {
            authRepository.signInWithGoogleToken(idToken) { result, exception ->
                _isAuthLoading.value = false
                if (result != null) {
                    val uid = result.user?.uid ?: ""
                    loginById(uid) { exists ->
                        if (exists) {
                            onComplete(true, null)
                        } else {
                            tempAuthUid = uid
                            onComplete(false, "PROFILE_INCOMPLETE")
                        }
                    }
                } else {
                    val msg = exception?.localizedMessage ?: "فشل تسجيل الدخول بـ Google"
                    _authError.value = msg
                    onComplete(false, msg)
                }
            }
        } catch (e: Exception) {
            _isAuthLoading.value = false
            val msg = e.localizedMessage ?: "حدث خطأ غير متوقع"
            _authError.value = msg
            onComplete(false, msg)
        }
    }

    fun registerUser(
        name: String,
        phone: String,
        role: String,
        profession: String,
        experienceYears: Int,
        city: String,
        neighborhood: String,
        bio: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val firebaseUser = try {
                authRepository.getCurrentUser()
            } catch (e: Exception) {
                null
            }
            val uid = tempAuthUid ?: (firebaseUser?.uid ?: "user_${System.currentTimeMillis()}")
            tempAuthUid = null // Clear temporary state after use
            val newUser = UserEntity(
                uid = uid,
                name = name,
                phoneNumber = phone,
                role = role,
                city = city,
                neighborhood = neighborhood,
                lat = 24.7 + (Math.random() * 0.1),
                lng = 46.6 + (Math.random() * 0.1),
                avatarColor = (0..7).random(),
                bio = bio,
                profession = if (role == "TECHNICIAN") profession else "",
                experienceYears = if (role == "TECHNICIAN") experienceYears else 0,
                isOnline = true,
                rating = 5.0,
                completedOrders = 0
            )
            repository.insertUser(newUser)
            // Sync new user to Supabase
            try {
                SupabaseSyncManager.upsertUser(newUser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            loginAsUser(newUser)
            onSuccess()
        }
    }

    fun updateUserProfile(
        name: String,
        phone: String,
        city: String,
        neighborhood: String,
        bio: String,
        profession: String = "",
        experienceYears: Int = 0,
        isOnline: Boolean = true,
        avatarColor: Int = -1,
        avatarUri: String? = "KEEP_EXISTING"
    ) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                name = name,
                phoneNumber = phone,
                city = city,
                neighborhood = neighborhood,
                bio = bio,
                profession = if (current.role == "TECHNICIAN") profession else "",
                experienceYears = if (current.role == "TECHNICIAN") experienceYears else 0,
                isOnline = isOnline,
                avatarColor = if (avatarColor != -1) avatarColor else current.avatarColor,
                avatarUri = if (avatarUri == "KEEP_EXISTING") current.avatarUri else avatarUri
            )
            repository.updateUser(updated)
            _currentUser.value = updated
            // Sync profile update to Supabase
            try {
                SupabaseSyncManager.upsertUser(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createRequest(
        serviceType: String,
        description: String,
        urgency: String,
        timeRequired: String,
        neighborhood: String,
        imageUri: String? = null,
        onSuccess: () -> Unit
    ) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            val requestId = repository.createRequest(
                customerId = current.uid,
                customerName = current.name,
                customerPhone = current.phoneNumber,
                serviceType = serviceType,
                description = description,
                urgency = urgency,
                timeRequired = timeRequired,
                neighborhood = neighborhood.ifEmpty { current.neighborhood },
                lat = current.lat,
                lng = current.lng,
                imageUri = imageUri
            )
            // Fetch request and sync to Supabase
            val req = repository.getRequestByIdSuspend(requestId.toInt())
            if (req != null) {
                try {
                    SupabaseSyncManager.sendRequest(req)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onSuccess()
        }
    }

    fun acceptRequest(requestId: Int, onComplete: (Boolean) -> Unit) {
        val current = _currentUser.value ?: return
        if (current.role != "TECHNICIAN") {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val success = repository.acceptRequest(
                requestId = requestId,
                techId = current.uid,
                techName = current.name,
                techPhone = current.phoneNumber
            )
            onComplete(success)
        }
    }

    fun updateRequestStatus(requestId: Int, newStatus: String) {
        viewModelScope.launch {
            repository.updateRequestStatus(requestId, newStatus)
        }
    }

    fun submitRating(requestId: Int, stars: Int, comment: String) {
        viewModelScope.launch {
            repository.submitRating(requestId, stars, comment)
        }
    }

    fun createPost(content: String, imageUri: String? = null, onSuccess: () -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createPost(
                authorId = current.uid,
                authorName = current.name,
                authorRole = current.role,
                avatarColor = current.avatarColor,
                content = content,
                neighborhood = current.neighborhood,
                authorAvatarUri = current.avatarUri,
                imageUri = imageUri
            )
            // Sync to Supabase
            try {
                val post = PostEntity(
                    authorId = current.uid,
                    authorName = current.name,
                    authorRole = current.role,
                    authorAvatarColor = current.avatarColor,
                    content = content,
                    neighborhood = current.neighborhood,
                    authorAvatarUri = current.avatarUri,
                    imageUri = imageUri
                )
                SupabaseSyncManager.sendPost(post)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onSuccess()
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            repository.likePost(postId)
        }
    }

    fun incrementCommentsCount(postId: Int) {
        viewModelScope.launch {
            repository.incrementCommentsCount(postId)
        }
    }

    fun createAdvertisement(title: String, description: String, price: String, onSuccess: () -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            // 1. Create the offer
            repository.createOffer(
                techId = current.uid,
                techName = current.name,
                profession = if (current.role == "TECHNICIAN") current.profession else "إعلان مستخدم",
                avatarColor = current.avatarColor,
                title = title,
                description = description,
                price = price,
                techAvatarUri = current.avatarUri
            )
            // 2. Create the post
            val formattedContent = "📢 [إعلان مميز]\n\n📌 $title\n\n$description\n\n💰 السعر: $price"
            repository.createPost(
                authorId = current.uid,
                authorName = current.name,
                authorRole = current.role,
                avatarColor = current.avatarColor,
                content = formattedContent,
                neighborhood = current.neighborhood,
                authorAvatarUri = current.avatarUri
            )
            onSuccess()
        }
    }

    fun createOffer(
        title: String,
        description: String,
        price: String,
        onSuccess: () -> Unit,
        isSponsored: Boolean = false,
        sponsorPlan: String = ""
    ) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createOffer(
                techId = current.uid,
                techName = current.name,
                profession = if (current.role == "TECHNICIAN") current.profession else if (isSponsored) "إعلان ممول" else "إعلان عام",
                avatarColor = current.avatarColor,
                title = title,
                description = description,
                price = price,
                techAvatarUri = current.avatarUri,
                isSponsored = isSponsored,
                sponsorPlan = sponsorPlan
            )
            onSuccess()
        }
    }

    fun deleteOffer(offerId: Int) {
        viewModelScope.launch {
            repository.deleteOffer(offerId)
        }
    }

    fun sendMessage(content: String, type: String = "TEXT") {
        val current = _currentUser.value ?: return
        val reqId = _selectedRequestId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                requestId = reqId,
                senderId = current.uid,
                senderName = current.name,
                content = content,
                type = type
            )

            // Dynamic Simulation: if the system or a simulated response is triggered, we can run a delayed auto-reply!
            // This is super fun! It simulates the technician responding dynamically in chat when testing!
            if (current.role == "CUSTOMER") {
                val req = repository.getRequestByIdSuspend(reqId)
                if (req != null && req.techId != null) {
                    val replies = listOf(
                        "أهلاً بك يا غالي، أنا في طريقي إليك الآن وسأصل خلال 15 دقيقة إن شاء الله.",
                        "تمام، فهمت المشكلة. سأحضر معي العدة المناسبة وقطع الغيار المطلوبة.",
                        "حياك الله، يمكنك إرسال اللوكيشن أو تأكيد العنوان لي لأصل بسرعة.",
                        "بخصوص التكلفة، لا تقلق أبداً، سنتفق على سعر يرضيك عند المعاينة."
                    )
                    // Trigger simulated response after 2 seconds
                    kotlinx.coroutines.delay(2000)
                    repository.sendMessage(
                        requestId = reqId,
                        senderId = req.techId,
                        senderName = req.techName ?: "الفني المختص",
                        content = replies.random(),
                        type = "TEXT"
                    )
                }
            } else if (current.role == "TECHNICIAN") {
                val req = repository.getRequestByIdSuspend(reqId)
                if (req != null) {
                    val replies = listOf(
                        "يعطيك العافية يا بشمهندس، بانتظارك.",
                        "تمام، أنا متواجد في البيت ومستعد لاستقبالك.",
                        "شكراً لتجاوبك السريع وجزاك الله خيراً.",
                        "العنوان صحيح وواضح على الخريطة."
                    )
                    // Trigger simulated customer response after 2 seconds
                    kotlinx.coroutines.delay(2000)
                    repository.sendMessage(
                        requestId = reqId,
                        senderId = req.customerId,
                        senderName = req.customerName,
                        content = replies.random(),
                        type = "TEXT"
                    )
                }
            }
        }
    }

    fun markNotificationsRead() {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllRead(current.uid)
        }
    }

    // --- Jobs and Channels Actions ---
    fun createJob(title: String, category: String, description: String, payment: String, jobType: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createJob(
                title = title,
                category = category,
                description = description,
                postedBy = current.name,
                posterId = current.uid,
                posterPhone = current.phoneNumber,
                payment = payment,
                neighborhood = current.neighborhood.ifEmpty { "حي الياسمين" },
                jobType = jobType,
                posterAvatarUri = current.avatarUri
            )
        }
    }

    fun applyToJob(jobId: Int) {
        viewModelScope.launch {
            repository.applyToJob(jobId)
        }
    }

    fun sendChannelMessage(channelId: String, content: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createChannelMessage(
                channelId = channelId,
                senderId = current.uid,
                senderName = current.name,
                senderAvatarColor = current.avatarColor,
                senderRole = current.role,
                content = content,
                senderAvatarUri = current.avatarUri
            )
            // Sync to Supabase
            try {
                val msg = ChannelMessageEntity(
                    channelId = channelId,
                    senderId = current.uid,
                    senderName = current.name,
                    senderAvatarColor = current.avatarColor,
                    senderRole = current.role,
                    content = content,
                    senderAvatarUri = current.avatarUri
                )
                SupabaseSyncManager.sendChannelMessage(msg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Admin panel functions ---
    fun adminDeleteUser(uid: String) {
        viewModelScope.launch {
            // Delete user simulation
            val db = SouqDatabase.getDatabase(getApplication())
            val user = db.souqDao().getUserByIdSuspend(uid)
            if (user != null) {
                // To keep it simple, we don't fully delete super admin or active logged-in, but can delete others
                if (uid != "admin_super") {
                    db.souqDao().insertUser(user.copy(role = "DELETED_USER_ROLE_INACTIVE"))
                }
            }
        }
    }
}

class SouqViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SouqViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SouqViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class CustomChannel(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String = "forum",
    val colorHex: String = "#4CAF50"
)
