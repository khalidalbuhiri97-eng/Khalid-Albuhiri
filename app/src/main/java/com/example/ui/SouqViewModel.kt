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

    // --- State Flow ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

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
            // Log in as default customer to begin with
            val defaultCust = repository.getUserSuspend("cust_khaled")
            if (defaultCust != null) {
                loginAsUser(defaultCust)
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
        try {
            authRepository.signInWithEmailAndPassword(email, password) { result, exception ->
                _isAuthLoading.value = false
                if (result != null) {
                    val uid = result.user?.uid ?: ""
                    loginById(uid) { exists ->
                        if (exists) {
                            onComplete(true, null)
                        } else {
                            onComplete(false, "PROFILE_INCOMPLETE")
                        }
                    }
                } else {
                    val msg = exception?.localizedMessage ?: "فشل تسجيل الدخول"
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

    fun signUpWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        _isAuthLoading.value = true
        _authError.value = null
        try {
            authRepository.createUserWithEmailAndPassword(email, password) { result, exception ->
                _isAuthLoading.value = false
                if (result != null) {
                    onComplete(true, null)
                } else {
                    val msg = exception?.localizedMessage ?: "فشل إنشاء الحساب"
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
            val uid = firebaseUser?.uid ?: "user_${System.currentTimeMillis()}"
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
        isOnline: Boolean = true
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
                isOnline = isOnline
            )
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun createRequest(
        serviceType: String,
        description: String,
        urgency: String,
        timeRequired: String,
        neighborhood: String,
        onSuccess: () -> Unit
    ) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createRequest(
                customerId = current.uid,
                customerName = current.name,
                customerPhone = current.phoneNumber,
                serviceType = serviceType,
                description = description,
                urgency = urgency,
                timeRequired = timeRequired,
                neighborhood = neighborhood.ifEmpty { current.neighborhood },
                lat = current.lat,
                lng = current.lng
            )
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

    fun createPost(content: String, onSuccess: () -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createPost(
                authorId = current.uid,
                authorName = current.name,
                authorRole = current.role,
                avatarColor = current.avatarColor,
                content = content,
                neighborhood = current.neighborhood
            )
            onSuccess()
        }
    }

    fun createOffer(title: String, description: String, price: String, onSuccess: () -> Unit) {
        val current = _currentUser.value ?: return
        if (current.role != "TECHNICIAN") return
        viewModelScope.launch {
            repository.createOffer(
                techId = current.uid,
                techName = current.name,
                profession = current.profession,
                avatarColor = current.avatarColor,
                title = title,
                description = description,
                price = price
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
