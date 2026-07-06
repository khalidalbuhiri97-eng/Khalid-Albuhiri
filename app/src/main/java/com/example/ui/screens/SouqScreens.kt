@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.SouqViewModel
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- Global Theme Mapping Helpers ---
fun getAvatarColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFFF97316), // Orange
        Color(0xFF10B981), // Green
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEF4444), // Red
        Color(0xFFF59E0B), // Yellow
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6)  // Teal
    )
    return colors.getOrElse(index) { colors[0] }
}

val professionsList = listOf(
    "سباكة",
    "كهرباء",
    "نجارة",
    "تكييف",
    "دهان",
    "تنظيف",
    "نقل أثاث",
    "حدادة",
    "صيانة أجهزة منزلية",
    "أعمال بناء",
    "خدمات أخرى"
)

// Main Flow router
@Composable
fun SouqAppFlow(viewModel: SouqViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    val currentUser by viewModel.currentUser.collectAsState()

    // Simulate Splash Screen timing
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else if (currentUser == null) {
        LoginAndRegisterScreen(viewModel)
    } else {
        MainContainerScreen(viewModel)
    }
}

// --- Splash Screen ---
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "سوق",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "المنصة المحلية للخدمات المجاورة",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

// --- Login & Prototyping Quick Login ---
@Composable
fun LoginAndRegisterScreen(viewModel: SouqViewModel) {
    var mode by remember { mutableStateOf("SIGN_IN") } // "SIGN_IN", "SIGN_UP", "COMPLETE_PROFILE"
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    // Email & Password Fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register Form Fields (Profile setup)
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CUSTOMER") } // "CUSTOMER" or "TECHNICIAN"
    var profession by remember { mutableStateOf("سباكة") }
    var experience by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("الرياض") }
    var neighborhood by remember { mutableStateOf("حي الياسمين") }
    var bio by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogleToken(idToken) { success, status ->
                        if (success) {
                            Toast.makeText(context, "تم تسجيل الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                        } else if (status == "PROFILE_INCOMPLETE") {
                            name = account.displayName ?: ""
                            mode = "COMPLETE_PROFILE"
                        } else {
                            Toast.makeText(context, status ?: "خطأ غير متوقع", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "فشل الحصول على رمز Google ID Token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "خطأ تسجيل الدخول: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Custom alignment wrapper
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (mode) {
                            "SIGN_IN" -> "تسجيل الدخول إلى سوق"
                            "SIGN_UP" -> "إنشاء حساب جديد"
                            else -> "إكمال ملفك الشخصي"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when (mode) {
                            "SIGN_IN" -> "سجل دخولك ببريدك الإلكتروني، حساب Google، أو استخدم المحاكاة السريعة"
                            "SIGN_UP" -> "أنشئ حساباً آمناً عبر البريد الإلكتروني لتتمكن من استخدام سوق"
                            else -> "يرجى تعبئة التفاصيل أدناه لإكمال حسابك وربطك بالحي"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )

                    if (authError != null && authError != "PROFILE_INCOMPLETE") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = authError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (mode == "SIGN_IN") {
                        // Quick Prototyping Logins (AWESOME!)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "بوابة المحاكاة التجريبية (دخول سريع)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loginById("cust_khaled") },
                                    modifier = Modifier.fillMaxWidth().testTag("quick_login_customer"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("دخول كعميل تجريبي (خالد)")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.loginById("tech_ahmed") },
                                    modifier = Modifier.fillMaxWidth().testTag("quick_login_tech"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Handyman, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("دخول كفني تجريبي (أحمد النجار)")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.loginById("admin_super") },
                                    modifier = Modifier.fillMaxWidth().testTag("quick_login_admin"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("دخول كمدير النظام (أبو فهد)")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "أو عبر خيارات الدخول الآمن لحسابك:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("البريد الإلكتروني") },
                            modifier = Modifier.fillMaxWidth().testTag("email_input"),
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            modifier = Modifier.fillMaxWidth().testTag("password_input"),
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                val description = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = description)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (email.isNotEmpty() && password.isNotEmpty()) {
                                    viewModel.signInWithEmail(email, password) { success, status ->
                                        if (success) {
                                            Toast.makeText(context, "أهلاً بك مجدداً!", Toast.LENGTH_SHORT).show()
                                        } else if (status == "PROFILE_INCOMPLETE") {
                                            mode = "COMPLETE_PROFILE"
                                        } else {
                                            Toast.makeText(context, status ?: "حدث خطأ ما", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "الرجاء إدخال البريد الإلكتروني وكلمة المرور", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login_submit_button")
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تسجيل الدخول الآمن")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Sign In Actions
                        Button(
                            onClick = {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken("google_placeholder_web_client_id")
                                    .requestEmail()
                                    .build()
                                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                try {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    // Fallback / simulation toggle for demo environments
                                    Toast.makeText(context, "البيئة لا تدعم خدمات Google. تفعيل وضع المحاكاة...", Toast.LENGTH_LONG).show()
                                    // Simulation of Google Auth successful redirect:
                                    val randomGoogleUid = "google_${System.currentTimeMillis().toString().takeLast(6)}"
                                    viewModel.loginById(randomGoogleUid) { exists ->
                                        if (exists) {
                                            Toast.makeText(context, "تم تسجيل الدخول بـ Google (محاكاة)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            name = "مستخدم جوجل المحاكي"
                                            mode = "COMPLETE_PROFILE"
                                        }
                                    }
                                }
                            },
                            enabled = !isAuthLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("google_login_button")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل الدخول بواسطة Google", color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        TextButton(onClick = { mode = "SIGN_UP" }) {
                            Text("ليس لديك حساب؟ إنشاء حساب جديد", fontWeight = FontWeight.Bold)
                        }

                    } else if (mode == "SIGN_UP") {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("البريد الإلكتروني") },
                            modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور الجديدة") },
                            modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                val description = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = description)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (email.isNotEmpty() && password.length >= 6) {
                                    viewModel.signUpWithEmail(email, password) { success, status ->
                                        if (success) {
                                            Toast.makeText(context, "تم إنشاء الحساب! يرجى إكمال ملفك الشخصي", Toast.LENGTH_LONG).show()
                                            mode = "COMPLETE_PROFILE"
                                        } else {
                                            Toast.makeText(context, status ?: "خطأ أثناء إنشاء الحساب", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else if (password.length < 6) {
                                    Toast.makeText(context, "يجب أن تكون كلمة المرور 6 أحرف على الأقل", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "الرجاء تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("signup_submit_button")
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.AppRegistration, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنشاء حساب بريد إلكتروني")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { mode = "SIGN_IN" }) {
                            Text("لديك حساب بالفعل؟ العودة لتسجيل الدخول", fontWeight = FontWeight.Bold)
                        }

                    } else if (mode == "COMPLETE_PROFILE") {
                        // Registration Screen fields
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("الاسم الكامل") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الجوال") },
                            modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, null) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("المدينة") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.LocationCity, null) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = neighborhood,
                            onValueChange = { neighborhood = it },
                            label = { Text("الحي") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Map, null) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("نبذة قصيرة") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Info, null) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("نوع الحساب:", fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { role = "CUSTOMER" }.padding(8.dp)
                            ) {
                                RadioButton(selected = role == "CUSTOMER", onClick = { role = "CUSTOMER" })
                                Text("عميل")
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { role = "TECHNICIAN" }.padding(8.dp)
                            ) {
                                RadioButton(selected = role == "TECHNICIAN", onClick = { role = "TECHNICIAN" })
                                Text("مقدم خدمة (فني)")
                            }
                        }

                        if (role == "TECHNICIAN") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("تخصص ومجال عمل الفني", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Custom Profession Selection
                                    var expandedProf by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { expandedProf = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("التخصص: $profession")
                                        }
                                        DropdownMenu(expanded = expandedProf, onDismissRequest = { expandedProf = false }) {
                                            professionsList.forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text(p) },
                                                    onClick = {
                                                        profession = p
                                                        expandedProf = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = experience,
                                        onValueChange = { experience = it },
                                        label = { Text("سنوات الخبرة") },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_experience_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (name.isNotEmpty() && phone.isNotEmpty()) {
                                    viewModel.registerUser(
                                        name = name,
                                        phone = phone,
                                        role = role,
                                        profession = profession,
                                        experienceYears = experience.toIntOrNull() ?: 0,
                                        city = city,
                                        neighborhood = neighborhood,
                                        bio = bio,
                                        onSuccess = {
                                            Toast.makeText(context, "تم إعداد الملف الشخصي والدخول!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "يرجى تعبئة الاسم ورقم الجوال", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_submit_button")
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ بيانات الملف الشخصي والدخول")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = {
                            viewModel.logout()
                            mode = "SIGN_IN"
                        }) {
                            Text("إلغاء والعودة لتسجيل الدخول")
                        }
                    }
                }
            }
        }
    }
}

// --- Main Container with bottom navigation and role toggling ---
@Composable
fun MainContainerScreen(viewModel: SouqViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var showCreateRequestDialog by remember { mutableStateOf(false) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showCreateOfferDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val unreadNotifications = notifications.filter { !it.isRead }.size

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سوق", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    },
                    navigationIcon = {
                        // Role switching dropdown simulation (Superb UX for testing and demonstrating)
                        var expandedSwitch by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expandedSwitch = true }, modifier = Modifier.testTag("role_switcher")) {
                                Icon(Icons.Default.SwapHoriz, "تبديل الحساب")
                            }
                            DropdownMenu(expanded = expandedSwitch, onDismissRequest = { expandedSwitch = false }) {
                                Text("محاكاة تبديل الحسابات:", modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = Color.Gray)
                                Divider()
                                allUsers.forEach { u ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(10.dp).background(getAvatarColor(u.avatarColor), CircleShape))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${u.name} (${when(u.role){"CUSTOMER"->"عميل" "TECHNICIAN"->"فني" else -> "مشرف"}})")
                                            }
                                        },
                                        onClick = {
                                            viewModel.loginAsUser(u)
                                            expandedSwitch = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Quick badge for notifications
                        Box {
                            IconButton(onClick = {
                                viewModel.markNotificationsRead()
                                selectedTab = 4 // Navigate to Profile / Settings where notifications are visible or display dialog
                            }) {
                                Icon(Icons.Default.Notifications, "الإشعارات")
                            }
                            if (unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(18.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadNotifications.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = mutableListOf(
                        Triple("الرئيسية", Icons.Default.Home, Icons.Outlined.Home),
                        Triple("طلباتي", Icons.Default.Assignment, Icons.Outlined.Assignment),
                        Triple("الحي", Icons.Default.Forum, Icons.Outlined.Forum),
                        Triple("العروض", Icons.Default.LocalOffer, Icons.Outlined.LocalOffer),
                        Triple("الحساب", Icons.Default.Person, Icons.Outlined.Person)
                    )

                    // Inject admin tab if current user is admin
                    if (currentUser?.role == "ADMIN") {
                        tabs.add(2, Triple("الإدارة", Icons.Default.AdminPanelSettings, Icons.Outlined.AdminPanelSettings))
                    }

                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) tab.second else tab.third,
                                    contentDescription = tab.first
                                )
                            },
                            label = { Text(tab.first, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            floatingActionButton = {
                // Show action FAB depending on active role and tab
                when {
                    selectedTab == 0 && currentUser?.role == "CUSTOMER" -> {
                        ExtendedFloatingActionButton(
                            onClick = { showCreateRequestDialog = true },
                            icon = { Icon(Icons.Default.Add, null) },
                            text = { Text("طلب خدمة") },
                            modifier = Modifier.testTag("create_request_fab")
                        )
                    }
                    selectedTab == 2 && currentUser?.role != "ADMIN" -> { // Neighborhood Social
                        ExtendedFloatingActionButton(
                            onClick = { showCreatePostDialog = true },
                            icon = { Icon(Icons.Default.Create, null) },
                            text = { Text("منشور") },
                            modifier = Modifier.testTag("create_post_fab")
                        )
                    }
                    selectedTab == 3 && currentUser?.role == "TECHNICIAN" -> { // Tech Offers
                        ExtendedFloatingActionButton(
                            onClick = { showCreateOfferDialog = true },
                            icon = { Icon(Icons.Default.AddCircle, null) },
                            text = { Text("إعلان عرض") },
                            modifier = Modifier.testTag("create_offer_fab")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Tab Routing
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    val adjustedTab = if (currentUser?.role != "ADMIN" && targetTab >= 2) targetTab else targetTab
                    when {
                        currentUser?.role == "ADMIN" -> {
                            when (targetTab) {
                                0 -> HomeScreen(viewModel, onNavigateToRequests = { selectedTab = 1 })
                                1 -> RequestsScreen(viewModel)
                                2 -> AdminScreen(viewModel)
                                3 -> NeighborhoodScreen(viewModel)
                                4 -> OffersScreen(viewModel)
                                5 -> ProfileScreen(viewModel)
                            }
                        }
                        else -> {
                            when (targetTab) {
                                0 -> HomeScreen(viewModel, onNavigateToRequests = { selectedTab = 1 })
                                1 -> RequestsScreen(viewModel)
                                2 -> NeighborhoodScreen(viewModel)
                                3 -> OffersScreen(viewModel)
                                4 -> ProfileScreen(viewModel)
                            }
                        }
                    }
                }
            }

            // Dialogs
            if (showCreateRequestDialog) {
                CreateRequestDialog(
                    viewModel = viewModel,
                    onDismiss = { showCreateRequestDialog = false }
                )
            }

            if (showCreatePostDialog) {
                CreatePostDialog(
                    viewModel = viewModel,
                    onDismiss = { showCreatePostDialog = false }
                )
            }

            if (showCreateOfferDialog) {
                CreateOfferDialog(
                    viewModel = viewModel,
                    onDismiss = { showCreateOfferDialog = false }
                )
            }
        }
    }
}

// --- Home Screen Composable ---
@Composable
fun HomeScreen(
    viewModel: SouqViewModel,
    onNavigateToRequests: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allTechnicians by viewModel.allTechnicians.collectAsState()
    val allOffers by viewModel.allOffers.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()
    val requests by viewModel.userRequests.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }
    var selectedProfessionFilter by remember { mutableStateOf<String?>(null) }

    val filteredTechs = allTechnicians.filter {
        (selectedProfessionFilter == null || it.profession == selectedProfessionFilter) &&
                (searchKeyword.isEmpty() || it.name.contains(searchKeyword) || it.profession.contains(searchKeyword))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Welcome and location banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(getAvatarColor(currentUser?.avatarColor ?: 0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.name?.take(1) ?: "ع",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مرحباً، ${currentUser?.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentUser?.city}، ${currentUser?.neighborhood}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search text field
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .testTag("search_field"),
                        placeholder = { Text("ابحث عن فني، كهربائي، نجار...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Services categories grid
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "التصنيفات والمهن",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedProfessionFilter == null,
                            onClick = { selectedProfessionFilter = null },
                            label = { Text("الكل") }
                        )
                    }
                    items(professionsList) { p ->
                        FilterChip(
                            selected = selectedProfessionFilter == p,
                            onClick = { selectedProfessionFilter = p },
                            label = { Text(p) }
                        )
                    }
                }
            }
        }

        // Active request warning alert for technicians or active tracking banner for customers
        val activeTracking = requests.filter { it.status != "COMPLETED" && it.status != "CANCELLED" }
        if (activeTracking.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToRequests() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "لديك طلب قيد التنفيذ حالياً!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "اضغط للمتابعة وتتبع الحالة والدردشة مع الفني.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                }
            }
        }

        // Active Offers preview
        if (allOffers.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "عروض حصرية في منطقتك",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(allOffers.take(5)) { offer ->
                            Card(
                                modifier = Modifier
                                    .width(260.dp)
                                    .height(140.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = offer.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = offer.description,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 2,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = offer.price,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = offer.techName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nearby Technicians list header
        item {
            Text(
                text = "الفنيون القريبون منك",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        // Technicians list
        if (filteredTechs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد فنيون يطابقون البحث حالياً", color = Color.Gray)
                    }
                }
            }
        } else {
            items(filteredTechs) { tech ->
                var showProfileDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { showProfileDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(getAvatarColor(tech.avatarColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tech.name.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = tech.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (tech.isOnline) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (tech.isOnline) "متاح" else "مشغول",
                                        color = if (tech.isOnline) Color(0xFF137333) else Color(0xFFC5221F),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${tech.profession} • خبرة ${tech.experienceYears} سنوات",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, "التقييم", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = tech.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "${tech.completedOrders} منجز", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                }

                // Tech Bio Dialog
                if (showProfileDialog) {
                    Dialog(onDismissRequest = { showProfileDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(getAvatarColor(tech.avatarColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tech.name.take(1), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(tech.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "${tech.profession} • خبرة ${tech.experienceYears} سنوات",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107))
                                    Text(" ${tech.rating} (${tech.completedOrders} طلب منجز)")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = tech.bio.ifEmpty { "لا توجد نبذة تعريفية مضافة حالياً." },
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row {
                                    OutlinedButton(
                                        onClick = { showProfileDialog = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("إغلاق")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Requests Tracking & History Composable ---
@Composable
fun RequestsScreen(viewModel: SouqViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userRequests by viewModel.userRequests.collectAsState()
    val techPendingRequests by viewModel.techPendingRequests.collectAsState()

    var activeRequestForDetail by remember { mutableStateOf<ServiceRequestEntity?>(null) }
    var ratingRequest by remember { mutableStateOf<ServiceRequestEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // If technician, display matching incoming neighbor requests to accept (The Race System!)
        if (currentUser?.role == "TECHNICIAN") {
            item {
                Text(
                    text = "طلبات أهل الحي الجديدة لمجالك (${currentUser?.profession})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (techPendingRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد طلبات معلقة لـ ${currentUser?.profession} في الحي حالياً.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(techPendingRequests) { req ->
                    var isAccepting by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "طلب ${req.serviceType}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF7E0), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(req.urgency, color = Color(0xFFB06000), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = req.description, fontSize = 13.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Text(" ${req.neighborhood} • ${req.timeRequired}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row {
                                Button(
                                    onClick = {
                                        isAccepting = true
                                        viewModel.acceptRequest(req.id) { success ->
                                            isAccepting = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("accept_button"),
                                    enabled = !isAccepting,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    if (isAccepting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                    else Text("قبول الطلب والبدء")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Active/My Orders list
        item {
            Text(
                text = if (currentUser?.role == "TECHNICIAN") "طلباتي التي قبلتها" else "سجل طلباتي",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (userRequests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AssignmentLate, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد طلبات في سجلك حالياً", color = Color.Gray)
                    }
                }
            }
        } else {
            items(userRequests) { req ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            viewModel.selectRequestForChat(req.id)
                            activeRequestForDetail = req
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "طلب ${req.serviceType} #${req.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            val (bg, fg, statusLabel) = when (req.status) {
                                "NEW" -> Triple(Color(0xFFE8F0FE), Color(0xFF1967D2), "جديد")
                                "PENDING" -> Triple(Color(0xFFFEF7E0), Color(0xFFB06000), "بانتظار قبول")
                                "ACCEPTED" -> Triple(Color(0xFFE6F4EA), Color(0xFF137333), "مقبول")
                                "ON_WAY" -> Triple(Color(0xFFF1F3F4), Color(0xFF3C4043), "في الطريق")
                                "STARTED" -> Triple(Color(0xFFE2F1FD), Color(0xFF1A73E8), "بدأ العمل")
                                "COMPLETED" -> Triple(Color(0xFFE6F4EA), Color(0xFF137333), "مكتمل")
                                "CANCELLED" -> Triple(Color(0xFFFCE8E6), Color(0xFFC5221F), "ملغي")
                                else -> Triple(Color.Gray, Color.White, req.status)
                            }
                            Box(
                                modifier = Modifier
                                    .background(bg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(statusLabel, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(req.description, fontSize = 13.sp, color = Color.Gray, maxLines = 2)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentUser?.role == "TECHNICIAN") "العميل: ${req.customerName}" else "الفني: ${req.techName ?: "بانتظار قبول..."}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Quick actions
                            Row {
                                if (req.status == "COMPLETED" && req.ratingStars == 0 && currentUser?.role == "CUSTOMER") {
                                    TextButton(
                                        onClick = { ratingRequest = req }
                                    ) {
                                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تقييم الخدمة")
                                    }
                                }
                                if (req.status != "COMPLETED" && req.status != "CANCELLED") {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.selectRequestForChat(req.id)
                                            activeRequestForDetail = req
                                        },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("متابعة ودردشة", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Sheet / Chat Dialog
    if (activeRequestForDetail != null) {
        RequestDetailDialog(
            viewModel = viewModel,
            onDismiss = { activeRequestForDetail = null }
        )
    }

    // Rating Dialog
    if (ratingRequest != null) {
        RateTechDialog(
            viewModel = viewModel,
            requestId = ratingRequest!!.id,
            techName = ratingRequest!!.techName ?: "الفني",
            onDismiss = { ratingRequest = null }
        )
    }
}

// --- Request Details & Live Chat Room ---
@Composable
fun RequestDetailDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    val currentRequest by viewModel.selectedRequest.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var chatText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (currentRequest == null) return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "تفاصيل طلب #${currentRequest!!.id}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                        Text(
                            "الخدمة المطلوبة: ${currentRequest!!.serviceType}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        Text(
                            "الموعد: ${currentRequest!!.timeRequired} • الحي: ${currentRequest!!.neighborhood}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Scrollable details & Status Tracking Steps
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Status Progress Tracker
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("تتبع حالة الطلب:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Basic dynamic workflow buttons for Tech or Customer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (currentUser?.role == "TECHNICIAN" && currentRequest!!.techId == currentUser!!.uid) {
                                    when (currentRequest!!.status) {
                                        "ACCEPTED" -> {
                                            Button(onClick = { viewModel.updateRequestStatus(currentRequest!!.id, "ON_WAY") }) {
                                                Text("أنا في الطريق")
                                            }
                                        }
                                        "ON_WAY" -> {
                                            Button(onClick = { viewModel.updateRequestStatus(currentRequest!!.id, "STARTED") }) {
                                                Text("بدء العمل")
                                            }
                                        }
                                        "STARTED" -> {
                                            Button(onClick = { viewModel.updateRequestStatus(currentRequest!!.id, "COMPLETED") }) {
                                                Text("إكمال المنجز")
                                            }
                                        }
                                    }
                                }

                                if (currentRequest!!.status != "COMPLETED" && currentRequest!!.status != "CANCELLED") {
                                    OutlinedButton(
                                        onClick = { viewModel.updateRequestStatus(currentRequest!!.id, "CANCELLED") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("إلغاء الطلب")
                                    }
                                }
                            }
                        }
                    }

                    // Live Chat messages view
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        reverseLayout = false
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("شرح المشكلة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(currentRequest!!.description, fontSize = 13.sp, color = Color.DarkGray)
                                }
                            }
                        }

                        items(chatMessages) { msg ->
                            val isMe = msg.senderId == currentUser?.uid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isMe) 12.dp else 0.dp,
                                        bottomEnd = if (isMe) 0.dp else 12.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) MaterialTheme.colorScheme.primary else Color(0xFFECEFF1)
                                    ),
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (!isMe) {
                                            Text(
                                                msg.senderName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = msg.content,
                                            fontSize = 13.sp,
                                            color = if (isMe) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat Input field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.sendMessage("📍 [مشاركة الموقع الحالي على الخريطة]", "LOCATION") }) {
                        Icon(Icons.Default.LocationOn, "موقع", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.sendMessage("📸 [صورة مرفقة لمكان المشكلة]", "IMAGE_PLACEHOLDER") }) {
                        Icon(Icons.Default.PhotoCamera, "كاميرا", tint = Color.Gray)
                    }
                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        placeholder = { Text("اكتب رسالة...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = {
                            if (chatText.isNotEmpty()) {
                                viewModel.sendMessage(chatText)
                                chatText = ""
                            }
                        },
                        modifier = Modifier.testTag("send_chat_button")
                    ) {
                        Icon(Icons.Default.Send, "إرسال", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// --- Neighborhood Screen Feed Composable ---
@Composable
fun NeighborhoodScreen(viewModel: SouqViewModel) {
    val posts by viewModel.allPosts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "منشورات حيك (${currentUser?.neighborhood})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "تواصل مع جيرانك، اطلب توصية لفني، أو شارك نصائح عامة لأهل الحي.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد منشورات في الحي حالياً.", color = Color.Gray)
                }
            }
        } else {
            items(posts) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(getAvatarColor(post.authorAvatarColor), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(post.authorName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (post.authorRole == "TECHNICIAN") "فني" else "جار",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text("في ${post.neighborhood}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(post.content, fontSize = 13.sp, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FavoriteBorder, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${post.likesCount} إعجاب", fontSize = 12.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Comment, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${post.commentsCount} تعليق", fontSize = 12.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Offers Market Composable ---
@Composable
fun OffersScreen(viewModel: SouqViewModel) {
    val offers by viewModel.allOffers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var bookingSuccessOffer by remember { mutableStateOf<OfferEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "سوق عروض الصيانة والتنظيف",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "باقات موفرة وخصومات حصرية مقدمة من فنيي حيك مباشرة بدون عمولة.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (offers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد عروض مضافة حالياً في سوق العروض.", color = Color.Gray)
                }
            }
        } else {
            items(offers) { offer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(getAvatarColor(offer.techAvatarColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(offer.techName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(offer.techName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(offer.techProfession, fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            if (currentUser?.uid == offer.techId) {
                                IconButton(onClick = { viewModel.deleteOffer(offer.id) }) {
                                    Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(offer.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(offer.description, fontSize = 13.sp, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = offer.price,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 18.sp
                            )

                            if (currentUser?.role == "CUSTOMER") {
                                Button(
                                    onClick = { bookingSuccessOffer = offer }
                                ) {
                                    Text("احجز العرض الآن")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (bookingSuccessOffer != null) {
        Dialog(onDismissRequest = { bookingSuccessOffer = null }) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("تم حجز العرض بنجاح!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "لقد قمت بحجز عرض: \"${bookingSuccessOffer!!.title}\". تم فتح قناة اتصال تواصل فورية لمتابعة الموعد مع الفني ${bookingSuccessOffer!!.techName}.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        // Create service request based on offer automatically
                        viewModel.createRequest(
                            serviceType = bookingSuccessOffer!!.techProfession,
                            description = "[حجز عرض] ${bookingSuccessOffer!!.title} - ${bookingSuccessOffer!!.description}",
                            urgency = "عادي",
                            timeRequired = "في أقرب وقت",
                            neighborhood = currentUser?.neighborhood ?: "حي الياسمين",
                            onSuccess = {}
                        )
                        bookingSuccessOffer = null
                    }) {
                        Text("موافق (الانتقال لطلباتي)")
                    }
                }
            }
        }
    }
}

// --- Profile & Custom Settings Composable ---
@Composable
fun ProfileScreen(viewModel: SouqViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phoneNumber ?: "") }
    var city by remember(currentUser) { mutableStateOf(currentUser?.city ?: "") }
    var neighborhood by remember(currentUser) { mutableStateOf(currentUser?.neighborhood ?: "") }
    var bio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "") }
    var profession by remember(currentUser) { mutableStateOf(currentUser?.profession ?: "") }
    var experience by remember(currentUser) { mutableStateOf(currentUser?.experienceYears?.toString() ?: "0") }
    var isOnline by remember(currentUser) { mutableStateOf(currentUser?.isOnline ?: true) }

    var isEditing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(getAvatarColor(currentUser?.avatarColor ?: 0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentUser?.name?.take(1) ?: "ع", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(currentUser?.name ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = when(currentUser?.role) {
                            "ADMIN" -> "مدير عام المنصة"
                            "TECHNICIAN" -> "فني (${currentUser?.profession})"
                            else -> "عميل محلي"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Edit Profile section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("معلومات الحساب", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = {
                            if (isEditing) {
                                viewModel.updateUserProfile(
                                    name = name,
                                    phone = phone,
                                    city = city,
                                    neighborhood = neighborhood,
                                    bio = bio,
                                    profession = profession,
                                    experienceYears = experience.toIntOrNull() ?: 0,
                                    isOnline = isOnline
                                )
                            }
                            isEditing = !isEditing
                        }) {
                            Text(if (isEditing) "حفظ التغييرات" else "تعديل البيانات")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isEditing) {
                        ProfileInfoRow(Icons.Default.Person, "الاسم الكامل", currentUser?.name ?: "")
                        ProfileInfoRow(Icons.Default.Phone, "رقم الجوال", currentUser?.phoneNumber ?: "")
                        ProfileInfoRow(Icons.Default.LocationCity, "المدينة والحي", "${currentUser?.city} - ${currentUser?.neighborhood}")
                        ProfileInfoRow(Icons.Default.Info, "النبذة الشخصية", currentUser?.bio?.ifEmpty { "لا توجد نبذة" } ?: "")

                        if (currentUser?.role == "TECHNICIAN") {
                            ProfileInfoRow(Icons.Default.Handyman, "التخصص والمهنة", currentUser?.profession ?: "")
                            ProfileInfoRow(Icons.Default.Badge, "سنوات الخبرة", "${currentUser?.experienceYears} سنوات")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.OnlinePrediction, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("حالة التوفر للخدمة", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                Switch(
                                    checked = isOnline,
                                    onCheckedChange = {
                                        isOnline = it
                                        viewModel.updateUserProfile(
                                            name = name, phone = phone, city = city, neighborhood = neighborhood, bio = bio,
                                            profession = profession, experienceYears = experience.toIntOrNull() ?: 0, isOnline = it
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الجوال") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("المدينة") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = neighborhood, onValueChange = { neighborhood = it }, label = { Text("الحي") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("النبذة الشخصية") }, modifier = Modifier.fillMaxWidth())

                        if (currentUser?.role == "TECHNICIAN") {
                            Spacer(modifier = Modifier.height(12.dp))
                            var expandedProf by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { expandedProf = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("التخصص: $profession")
                                }
                                DropdownMenu(expanded = expandedProf, onDismissRequest = { expandedProf = false }) {
                                    professionsList.forEach { p ->
                                        DropdownMenuItem(text = { Text(p) }, onClick = { profession = p; expandedProf = false })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = experience, onValueChange = { experience = it }, label = { Text("سنوات الخبرة") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        // Notification center logs inside profile for clarity
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("مركز الإشعارات والتنبيهات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (notifications.isEmpty()) {
                        Text("لا يوجد إشعارات جديدة حالياً.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        notifications.take(5).forEach { notif ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (notif.isRead) Color.Gray else MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                            .padding(top = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(notif.content, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Divider(modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Log out button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("logout_button")
                ) {
                    Text("تسجيل الخروج من الحساب")
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.Gray, fontSize = 11.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// --- Admin Panel Dashboard Composable ---
@Composable
fun AdminScreen(viewModel: SouqViewModel) {
    val allUsers by viewModel.allUsers.collectAsState()
    val allRequests by viewModel.userRequests.collectAsState()
    val allOffers by viewModel.allOffers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp)
            ) {
                Column {
                    Text("لوحة إدارة منصة سوق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("إشراف عام، مراقبة الإحصائيات الفورية، وإدارة المستخدمين والطلبات.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        // Stats Cards Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي المستخدمين", fontSize = 11.sp, color = Color.DarkGray)
                        Text(allUsers.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("إجمالي الطلبات", fontSize = 11.sp, color = Color.DarkGray)
                        Text(allRequests.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("العروض الفعالة", fontSize = 11.sp, color = Color.DarkGray)
                        Text(allOffers.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00))
                    }
                }
            }
        }

        // Users Administration
        item {
            Text("إدارة المستخدمين النشطين", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        items(allUsers) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(getAvatarColor(user.avatarColor), CircleShape), contentAlignment = Alignment.Center) {
                            Text(user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("الرتبة: ${user.role} • الحي: ${user.neighborhood}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    if (user.uid != "admin_super") {
                        IconButton(onClick = { viewModel.adminDeleteUser(user.uid) }) {
                            Icon(Icons.Default.DeleteForever, "حظر وحذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// --- Create Request Dialog ---
@Composable
fun CreateRequestDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    var serviceType by remember { mutableStateOf("سباكة") }
    var description by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("متوسط") }
    var timeRequired by remember { mutableStateOf("في أقرب وقت") }
    var neighborhood by remember { mutableStateOf(currentUser?.neighborhood ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("تفاصيل طلب الخدمة الجديدة", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                Text("نوع الخدمة المطلوبة:", fontWeight = FontWeight.Bold)
                var expandedProf by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedProf = true }, modifier = Modifier.fillMaxWidth().testTag("service_type_dropdown")) {
                        Text(serviceType)
                    }
                    DropdownMenu(expanded = expandedProf, onDismissRequest = { expandedProf = false }) {
                        professionsList.forEach { p ->
                            DropdownMenuItem(text = { Text(p) }, onClick = { serviceType = p; expandedProf = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("صف مشكلتك بالتفصيل...") },
                    modifier = Modifier.fillMaxWidth().testTag("request_desc_field")
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("مستوى الاستعجال:", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("عاجل", "متوسط", "عادي").forEach { u ->
                        FilterChip(
                            selected = urgency == u,
                            onClick = { urgency = u },
                            label = { Text(u) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = timeRequired,
                    onValueChange = { timeRequired = it },
                    label = { Text("الوقت المطلوب للزيارة") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = { neighborhood = it },
                    label = { Text("الحي لتغطية الخدمة") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (description.isNotEmpty()) {
                                viewModel.createRequest(
                                    serviceType = serviceType,
                                    description = description,
                                    urgency = urgency,
                                    timeRequired = timeRequired,
                                    neighborhood = neighborhood,
                                    onSuccess = onDismiss
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("submit_request_button")
                    ) {
                        Text("إرسال الطلب")
                    }
                }
            }
        }
    }
}

// --- Create Post Dialog ---
@Composable
fun CreatePostDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("كتابة منشور جديد في الحي", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("ماذا تريد أن تشارك مع جيرانك؟") },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("post_desc_field")
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (content.isNotEmpty()) {
                                viewModel.createPost(content, onDismiss)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("submit_post_button")
                    ) {
                        Text("نشر")
                    }
                }
            }
        }
    }
}

// --- Create Offer Dialog ---
@Composable
fun CreateOfferDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("إعلان عرض صيانة جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان العرض (مثال: تنظيف 3 مكيفات)") },
                    modifier = Modifier.fillMaxWidth().testTag("offer_title_field")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل العرض والمميزات...") },
                    modifier = Modifier.fillMaxWidth().testTag("offer_desc_field")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("السعر الإجمالي (مثال: 150 ريال)") },
                    modifier = Modifier.fillMaxWidth().testTag("offer_price_field")
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && price.isNotEmpty()) {
                                viewModel.createOffer(title, description, price, onDismiss)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("submit_offer_button")
                    ) {
                        Text("إعلان العرض")
                    }
                }
            }
        }
    }
}

// --- Rate Tech Dialog ---
@Composable
fun RateTechDialog(
    viewModel: SouqViewModel,
    requestId: Int,
    techName: String,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("تقييم جودة الخدمة", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("الرجاء تقييم الفني: $techName", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Stars rating selection
                Row {
                    (1..5).forEach { index ->
                        IconButton(onClick = { rating = index }) {
                            Icon(
                                imageVector = if (index <= rating) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (index <= rating) Color(0xFFFFB300) else Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("اكتب رأيك بصراحة عن الخدمة...") },
                    modifier = Modifier.fillMaxWidth().testTag("rating_desc_field")
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("لاحقاً")
                    }
                    Button(
                        onClick = {
                            viewModel.submitRating(requestId, rating, comment)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).testTag("submit_rating_button")
                    ) {
                        Text("حفظ التقييم")
                    }
                }
            }
        }
    }
}
