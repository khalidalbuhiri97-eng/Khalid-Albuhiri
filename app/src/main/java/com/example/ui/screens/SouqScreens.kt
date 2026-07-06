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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Logo & App Name Header with Custom Font Weight
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "سوق",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Redesigned Premium Hero Banner Card (Using generated Image)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.img_login_hero),
                                contentDescription = "Community Services",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Elegant Dark Overlay for text legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "تواصل وطلب خدمات مباشر بالسوق",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تبادل المهن، الخدمات والطلبات بلمسة زر واحدة وبدون أي عمولات محددة.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode description or error layout
                    if (authError != null && authError != "PROFILE_INCOMPLETE") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = authError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Elegant Input Forms Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Tab selector for SIGN_IN vs SIGN_UP if not in COMPLETE_PROFILE
                            if (mode != "COMPLETE_PROFILE") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.LightGray.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (mode == "SIGN_IN") MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { mode = "SIGN_IN" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "تسجيل الدخول",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (mode == "SIGN_IN") Color.White else Color.Gray
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (mode == "SIGN_UP") MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { mode = "SIGN_UP" }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "حساب جديد",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (mode == "SIGN_UP") Color.White else Color.Gray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            // Render forms based on active mode
                            when (mode) {
                                "SIGN_IN" -> {
                                    Text(
                                        text = "أدخل بيانات حسابك للولوج لسوق:",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("البريد الإلكتروني") },
                                        modifier = Modifier.fillMaxWidth().testTag("email_input"),
                                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("كلمة المرور") },
                                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                            val description = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(imageVector = image, contentDescription = description)
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

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
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("login_submit_button")
                                    ) {
                                        if (isAuthLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Icon(Icons.Default.Login, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تسجيل دخول آمن", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Beautiful Divider
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Divider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                                        Text("أو الدخول عبر جوجل", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 10.dp))
                                        Divider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Premium Google Sign-In Button
                                    OutlinedButton(
                                        onClick = {
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestIdToken("google_placeholder_web_client_id")
                                                .requestEmail()
                                                .build()
                                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                            try {
                                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "بيئة المحاكاة: تفعيل الدخول المحاكي لـ Google...", Toast.LENGTH_SHORT).show()
                                                val randomGoogleUid = "google_${System.currentTimeMillis().toString().takeLast(6)}"
                                                viewModel.loginById(randomGoogleUid) { exists ->
                                                    if (exists) {
                                                        Toast.makeText(context, "تم تسجيل الدخول بـ Google (محاكاة)", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        name = "مستكشف جوجل المحاكي"
                                                        mode = "COMPLETE_PROFILE"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isAuthLoading,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color.LightGray),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("google_login_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Google",
                                            tint = Color(0xFF4285F4),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("تسجيل الدخول بواسطة Google", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                }

                                "SIGN_UP" -> {
                                    Text(
                                        text = "قم بإنشاء حساب جديد بالبريد الإلكتروني للولوج للسوق:",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("البريد الإلكتروني") },
                                        modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("كلمة المرور الجديدة") },
                                        modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
                                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                            val description = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(imageVector = image, contentDescription = description)
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        shape = RoundedCornerShape(8.dp)
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
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("signup_submit_button")
                                    ) {
                                        if (isAuthLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Icon(Icons.Default.AppRegistration, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("إنشاء حساب بريد إلكتروني جديد", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                "COMPLETE_PROFILE" -> {
                                    Text(
                                        text = "يرجى إكمال معلومات الملف الشخصي للبدء مباشرة في سوق:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    )

                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("الاسم الكامل") },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        label = { Text("رقم الجوال") },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = city,
                                            onValueChange = { city = it },
                                            label = { Text("المدينة") },
                                            modifier = Modifier.weight(1f),
                                            leadingIcon = { Icon(Icons.Default.LocationCity, null, tint = Color.Gray) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = neighborhood,
                                            onValueChange = { neighborhood = it },
                                            label = { Text("الحي") },
                                            modifier = Modifier.weight(1f),
                                            leadingIcon = { Icon(Icons.Default.Map, null, tint = Color.Gray) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = bio,
                                        onValueChange = { bio = it },
                                        label = { Text("نبذة قصيرة وتفاصيل إضافية") },
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.Info, null, tint = Color.Gray) },
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("نوع العضوية بالحي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Premium Segmented Account Type Choice
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    color = if (role == "CUSTOMER") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                                )
                                                .clickable { role = "CUSTOMER" }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = role == "CUSTOMER", onClick = { role = "CUSTOMER" })
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("عميل / جار", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    color = if (role == "TECHNICIAN") MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent,
                                                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                                                )
                                                .clickable { role = "TECHNICIAN" }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = role == "TECHNICIAN", onClick = { role = "TECHNICIAN" })
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("مقدم خدمة فني", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }

                                    if (role == "TECHNICIAN") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("مجال التخصص المهني للفني:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.height(10.dp))
                                                
                                                var expandedProf by remember { mutableStateOf(false) }
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedButton(
                                                        onClick = { expandedProf = true },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("التخصص الحالي: $profession", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                    DropdownMenu(expanded = expandedProf, onDismissRequest = { expandedProf = false }) {
                                                        professionsList.forEach { p ->
                                                            DropdownMenuItem(
                                                                text = { Text(p, fontSize = 12.sp) },
                                                                onClick = {
                                                                    profession = p
                                                                    expandedProf = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                OutlinedTextField(
                                                    value = experience,
                                                    onValueChange = { experience = it },
                                                    label = { Text("سنوات الخبرة في المجال") },
                                                    modifier = Modifier.fillMaxWidth().testTag("profile_experience_input"),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

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
                                                        Toast.makeText(context, "تم إعداد الملف الشخصي والدخول بسلام!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            } else {
                                                Toast.makeText(context, "يرجى تعبئة الاسم ورقم الجوال لتسجيل حسابك", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_submit_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("حفظ بيانات الملف الشخصي والدخول", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = {
                                            viewModel.logout()
                                            mode = "SIGN_IN"
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("إلغاء والعودة لتسجيل الدخول", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "سوق © جميع الحقوق محفوظة 2026",
                        fontSize = 10.sp,
                        color = Color.Gray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
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
    val subTab by viewModel.neighborhoodSubTab.collectAsState()

    var showCreateRequestDialog by remember { mutableStateOf(false) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showCreateOfferDialog by remember { mutableStateOf(false) }

    // Drawer and Sidebar related states
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showCreateAdDialog by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val unreadNotifications = notifications.filter { !it.isRead }.size

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerTonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Drawer Header (Profile Information)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!currentUser?.avatarUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = currentUser?.avatarUri,
                                            contentDescription = "صورة الحساب",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = currentUser?.name?.take(1) ?: "",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = currentUser?.name ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "${currentUser?.neighborhood} - ${currentUser?.city}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when(currentUser?.role) {
                                            "CUSTOMER" -> "عميل / جار"
                                            "TECHNICIAN" -> "فني صيانة (${currentUser?.profession})"
                                            "ADMIN" -> "مشرف الحي"
                                            else -> "عضو"
                                        },
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Drawer Options List
                        Text(
                            text = "الخيارات والإعلانات:",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        // 1. Create Advertisement
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary) },
                            label = { Text("إنشاء إعلان مميز", fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showCreateAdDialog = true
                            },
                            badge = {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("جديد", color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )

                        // 2. Create Discussion Channel
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.secondary) },
                            label = { Text("إنشاء قناة نقاش جديدة", fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showCreateChannelDialog = true
                            }
                        )

                        // 3. Application Settings
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, null, tint = Color.DarkGray) },
                            label = { Text("إعدادات التطبيق", fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showSettingsDialog = true
                            }
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Role Switching Dropdown Simulation integrated here for spectacular UX
                        Text(
                            text = "محاكاة الحسابات (عرض سريع):",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        var expandedSwitch by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { expandedSwitch = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.SwapHoriz, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تبديل الحساب الحالي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = expandedSwitch,
                                onDismissRequest = { expandedSwitch = false },
                                modifier = Modifier.width(260.dp)
                            ) {
                                allUsers.forEach { u ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(10.dp).background(getAvatarColor(u.avatarColor), CircleShape))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${u.name} (${when(u.role){"CUSTOMER"->"عميل" "TECHNICIAN"->"فني" else -> "مشرف"}})",
                                                    fontSize = 12.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.loginAsUser(u)
                                            expandedSwitch = false
                                            scope.launch { drawerState.close() }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Logout button at bottom
                        Button(
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.logout()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ExitToApp, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) {
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
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(Icons.Default.Menu, "القائمة")
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
                        selectedTab == 2 && currentUser?.role != "ADMIN" && subTab == 0 -> { // Neighborhood Social - ONLY Feed!
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

                if (showCreateAdDialog) {
                    CreateAdvertisementDialog(
                        viewModel = viewModel,
                        onDismiss = { showCreateAdDialog = false }
                    )
                }

                if (showCreateChannelDialog) {
                    CreateChannelDialog(
                        viewModel = viewModel,
                        onDismiss = { showCreateChannelDialog = false }
                    )
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        viewModel = viewModel,
                        onDismiss = { showSettingsDialog = false }
                    )
                }
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
                            if (!currentUser?.avatarUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = currentUser!!.avatarUri,
                                    contentDescription = "الصورة الشخصية",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = currentUser?.name?.take(1) ?: "ع",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
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
                            if (!tech.avatarUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = tech.avatarUri,
                                    contentDescription = "صورة الفني",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = tech.name.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
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
                                    if (!tech.avatarUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = tech.avatarUri,
                                            contentDescription = "صورة الفني",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(tech.name.take(1), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
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

                            if (!req.imageUri.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = req.imageUri,
                                    contentDescription = "صورة العطل المرفقة",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

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

                        if (!req.imageUri.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = req.imageUri,
                                contentDescription = "صورة العطل المرفقة",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

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
    val context = LocalContext.current

    val chatImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            val localPath = saveUriToInternalStorage(context, selectedUri)
            if (localPath != null) {
                viewModel.sendMessage(localPath, "IMAGE")
            }
        }
    }

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
                                    if (!currentRequest!!.imageUri.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = currentRequest!!.imageUri,
                                            contentDescription = "صورة العطل المرفقة",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
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
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (!isMe) {
                                            Text(
                                                msg.senderName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        when (msg.type) {
                                            "LOCATION" -> {
                                                val context = LocalContext.current
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color(0xFFE8F0FE),
                                                             RoundedCornerShape(8.dp)
                                                         )
                                                         .clickable {
                                                             Toast.makeText(context, "جاري فتح خرائط Google لتوجيهك للموقع...", Toast.LENGTH_SHORT).show()
                                                         }
                                                         .padding(8.dp)
                                                 ) {
                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                         Icon(
                                                             imageVector = Icons.Default.Map,
                                                             contentDescription = "موقع",
                                                             tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                                                             modifier = Modifier.size(24.dp)
                                                         )
                                                         Spacer(modifier = Modifier.width(8.dp))
                                                         Column {
                                                             Text(
                                                                 "الموقع الجغرافي",
                                                                 fontWeight = FontWeight.Bold,
                                                                 fontSize = 13.sp,
                                                                 color = if (isMe) Color.White else Color.Black
                                                             )
                                                             Text(
                                                                 "عرض الموقع على الخريطة",
                                                                 fontSize = 11.sp,
                                                                 color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.DarkGray
                                                             )
                                                         }
                                                     }
                                                 }
                                             }
                                             "IMAGE" -> {
                                                var showImageDetail by remember { mutableStateOf(false) }
                                                Box(
                                                    modifier = Modifier
                                                        .size(150.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.LightGray)
                                                        .clickable { showImageDetail = true }
                                                ) {
                                                    AsyncImage(
                                                        model = msg.content,
                                                        contentDescription = "صورة مرسلة",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }

                                                if (showImageDetail) {
                                                    Dialog(onDismissRequest = { showImageDetail = false }) {
                                                        Card(
                                                            shape = RoundedCornerShape(16.dp),
                                                            modifier = Modifier.padding(16.dp)
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(16.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(300.dp)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                ) {
                                                                    AsyncImage(
                                                                        model = msg.content,
                                                                        contentDescription = "معاينة الصورة",
                                                                        modifier = Modifier.fillMaxSize(),
                                                                        contentScale = ContentScale.Fit
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(16.dp))
                                                                Button(onClick = { showImageDetail = false }) {
                                                                    Text("إغلاق")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "IMAGE_PLACEHOLDER" -> {
                                                var showImageDetail by remember { mutableStateOf(false) }
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color(0xFFF1F3F4),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { showImageDetail = true }
                                                        .padding(8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Image,
                                                            contentDescription = "صورة",
                                                            tint = if (isMe) Color.White else Color.Gray,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                "صورة العطل المرفقة",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = if (isMe) Color.White else Color.Black
                                                            )
                                                            Text(
                                                                "حجم الملف: 1.4 MB • اضغط للعرض",
                                                                fontSize = 10.sp,
                                                                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
                                                            )
                                                        }
                                                    }
                                                }
 
                                                if (showImageDetail) {
                                                    Dialog(onDismissRequest = { showImageDetail = false }) {
                                                        Card(
                                                            shape = RoundedCornerShape(16.dp),
                                                            modifier = Modifier.padding(16.dp)
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.padding(16.dp),
                                                                horizontalAlignment = Alignment.CenterHorizontally
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(200.dp)
                                                                        .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                        Icon(Icons.Default.PhotoCamera, "معاينة العطل", tint = Color.White, modifier = Modifier.size(48.dp))
                                                                        Spacer(modifier = Modifier.height(8.dp))
                                                                        Text("[ معاينة تفصيلية لموقع العطل المرفق ]", color = Color.LightGray, fontSize = 12.sp)
                                                                    }
                                                                }
                                                                Spacer(modifier = Modifier.height(16.dp))
                                                                Text("صورة توضيحية للمشكلة المرسلة من العميل لمساعدة الفني في تحضير الأدوات المناسبة قبل الحضور.", textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
                                                                Spacer(modifier = Modifier.height(16.dp))
                                                                Button(onClick = { showImageDetail = false }) {
                                                                    Text("حسناً")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
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
                    IconButton(onClick = { chatImageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoCamera, "كاميرا", tint = MaterialTheme.colorScheme.primary)
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

// --- Comment Data Class ---
data class PostComment(
    val authorName: String,
    val authorAvatarColor: Int,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val authorAvatarUri: String? = null
)

// --- Neighborhood Screen Feed Composable ---
@Composable
fun NeighborhoodScreen(viewModel: SouqViewModel) {
    val selectedSubTab by viewModel.neighborhoodSubTab.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant Material 3 Sub Tabs
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { viewModel.setNeighborhoodSubTab(0) },
                    text = { Text("أخبار ومناشير", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Forum, "أخبار الحي", modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { viewModel.setNeighborhoodSubTab(1) },
                    text = { Text("فرص وعروض عمل", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Work, "فرص العمل", modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { viewModel.setNeighborhoodSubTab(2) },
                    text = { Text("قنوات النقاش", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Campaign, "قنوات نقاش", modifier = Modifier.size(20.dp)) }
                )
            }

            AnimatedContent(
                targetState = selectedSubTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "SubTabTransition",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    0 -> NeighborhoodFeedContent(viewModel)
                    1 -> NeighborhoodJobsContent(viewModel)
                    2 -> NeighborhoodChannelsContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun NeighborhoodFeedContent(viewModel: SouqViewModel) {
    val posts by viewModel.allPosts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var activePostForComments by remember { mutableStateOf<PostEntity?>(null) }
    val commentsByPost = remember {
        mutableStateMapOf<Int, List<PostComment>>().apply {
            put(1, listOf(
                PostComment("أحمد العتيبي", 2, "أبو سليمان ممتاز جداً وأنصح بالتعامل معه عن تجربة شخصية في تصليح الكهرباء!"),
                PostComment("سلوى محمد", 4, "صحيح، شغله سريع وسعره معقول جداً.")
            ))
            put(2, listOf(
                PostComment("خالد الشهري", 1, "السباك أبو فهد بطل ووصلني في 10 دقائق فقط لتصليح تسريب المطبخ.")
            ))
            put(3, listOf(
                PostComment("عبدالرحمن سليمان", 5, "أنصحك بالفني رائد، نجار متميز في حي الياسمين.")
            ))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "المنشورات العامة للسوق",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "تواصل في السوق، اطلب توصية لفني، أو تصفح الإعلانات والخدمات المتاحة.",
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
                                if (!post.authorAvatarUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = post.authorAvatarUri,
                                        contentDescription = "صورة الناشر",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(post.authorName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                }
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

                        if (!post.imageUri.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AsyncImage(
                                model = post.imageUri,
                                contentDescription = "صورة المنشور المرفقة",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Like Button (Interactive)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.likePost(post.id) }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "إعجاب",
                                    tint = if (post.isLikedByMe) Color(0xFFE91E63) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${post.likesCount} إعجاب",
                                    fontSize = 12.sp,
                                    color = if (post.isLikedByMe) Color(0xFFE91E63) else Color.Gray,
                                    fontWeight = if (post.isLikedByMe) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // Comment Button (Interactive)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activePostForComments = post }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = "تعليق",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${post.commentsCount} تعليق", fontSize = 12.sp, color = Color.Gray)
                            }

                            // Share Button (Toast Interaction)
                            val context = LocalContext.current
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        Toast.makeText(context, "تم نسخ رابط المنشور بنجاح لأهل الحي!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp)
                            ) {
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

    // Comment Dialog
    if (activePostForComments != null) {
        val post = activePostForComments!!
        val postComments = commentsByPost[post.id] ?: emptyList()
        CommentDialog(
            post = post,
            comments = postComments,
            onAddComment = { content ->
                val currentName = currentUser?.name ?: "جار مجهول"
                val currentAvatar = currentUser?.avatarColor ?: 0
                val newComment = PostComment(
                    authorName = currentName,
                    authorAvatarColor = currentAvatar,
                    content = content,
                    authorAvatarUri = currentUser?.avatarUri
                )
                commentsByPost[post.id] = postComments + newComment
                viewModel.incrementCommentsCount(post.id)
            },
            onDismiss = { activePostForComments = null }
        )
    }
}

@Composable
fun NeighborhoodJobsContent(viewModel: SouqViewModel) {
    val jobs by viewModel.allJobs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedJobType by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateJobDialog by remember { mutableStateOf(false) }

    val categories = listOf("الكل", "صيانة", "توصيل", "تعليم", "مساعدة في الحي", "أخرى")
    val jobTypes = listOf("الكل", "دوام جزئي", "عمل حر / مؤقت", "مكافأة تقديرية")

    val filteredJobs = jobs.filter { job ->
        val matchesCategory = selectedCategory == "الكل" || job.category == selectedCategory
        val matchesJobType = selectedJobType == "الكل" || job.jobType == selectedJobType
        val matchesSearch = searchQuery.isEmpty() || job.title.contains(searchQuery, ignoreCase = true) || job.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesJobType && matchesSearch
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Mourjan-Style Premium Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دليل الوظائف وفرص العمل بالسوق",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "دليل متكامل لتوظيف الكفاءات والخبرات وتبادل العمل بالسوق مباشرة وبدون عمولات.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showCreateJobDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نشر فرصة عمل أو طلب خدمة بالحي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Advanced Search & Filters Section (Mourjan/Haraj style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في عنوان الوظيفة أو التفاصيل... (مثال: تدريس، توصيل)", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category selection label
                Text(
                    text = "تصفية حسب التخصص:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Horizontal Category Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Job Type selection label
                Text(
                    text = "نوع العمل والتعاقد:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Horizontal Job Type Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(jobTypes) { jt ->
                        FilterChip(
                            selected = selectedJobType == jt,
                            onClick = { selectedJobType = jt },
                            label = { Text(jt, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }
        }

        // Stats badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الفرص المتاحة (${filteredJobs.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            if (searchQuery.isNotEmpty() || selectedCategory != "الكل" || selectedJobType != "الكل") {
                TextButton(
                    onClick = {
                        searchQuery = ""
                        selectedCategory = "الكل"
                        selectedJobType = "الكل"
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("إعادة ضبط الفلاتر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Jobs Directory List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (filteredJobs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد فرص عمل مطابقة لمعايير البحث حالياً.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredJobs) { job ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Category & Job Type badges side-by-side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = job.category,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = job.jobType,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                // Beautiful relative simulation date badge
                                Text(
                                    text = "اليوم",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Job Title
                            Text(
                                text = job.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Payment Details Badge (Haraj/Mourjan green badge)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "المقابل المادي",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "المقابل المتوقع: ${job.payment}",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Location badge (Mourjan style)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "الموقع",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "المنطقة: الرياض - ${job.neighborhood}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Description
                            Text(
                                text = job.description,
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Poster details and actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(getAvatarColor(job.id % 8), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!job.posterAvatarUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = job.posterAvatarUri,
                                                contentDescription = "صورة الناشر",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = job.postedBy.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = job.postedBy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "من أهل ${job.neighborhood}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            Toast.makeText(
                                                context,
                                                "جاري التحويل لدردشة الواتساب مع ${job.postedBy} (هاتف: ${job.posterPhone})...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFE8F5E9), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "واتساب",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (!job.isAppliedByMe) {
                                                viewModel.applyToJob(job.id)
                                                Toast.makeText(
                                                    context,
                                                    "تم التقديم للفرصة بنجاح! تم إشعار صاحب الإعلان.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (job.isAppliedByMe) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        if (job.isAppliedByMe) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تم التقديم (${job.applicantsCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("تقدم للفرصة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    if (showCreateJobDialog) {
        CreateJobDialog(
            viewModel = viewModel,
            onDismiss = { showCreateJobDialog = false }
        )
    }
}

data class NeighborhoodChannel(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun NeighborhoodChannelsContent(viewModel: SouqViewModel) {
    val activeChannelId by viewModel.activeChannelId.collectAsState()
    val messages by viewModel.activeChannelMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val customChannels by viewModel.customChannels.collectAsState()

    var textInput by remember { mutableStateOf("") }

    val defaultChannels = listOf(
        NeighborhoodChannel("general", "المجلس العام للسوق", "أحاديث ونقاشات السوق اليومية", Icons.Default.Forum, Color(0xFF4CAF50)),
        NeighborhoodChannel("news", "أخبار وإعلانات السوق", "أخبار وإعلانات إدارة السوق الرسمية", Icons.Default.Campaign, Color(0xFFFF9800)),
        NeighborhoodChannel("services", "تجارب الصيانة والخدمات", "استشارات وحلول مشاكل الصيانة والخدمات المتنوعة", Icons.Default.Handyman, Color(0xFF2196F3)),
        NeighborhoodChannel("market", "حراج وبيع وشراء", "تبادل وبيع السلع والمنتجات المستعملة والجديدة", Icons.Default.LocalOffer, Color(0xFFE91E63))
    )

    val channelsList = defaultChannels + customChannels.map { cc ->
        NeighborhoodChannel(
            id = cc.id,
            title = cc.title,
            description = cc.description,
            icon = when (cc.iconName) {
                "forum" -> Icons.Default.Forum
                "campaign" -> Icons.Default.Campaign
                "handyman" -> Icons.Default.Handyman
                "local_offer" -> Icons.Default.LocalOffer
                "groups" -> Icons.Default.Groups
                "chat" -> Icons.Default.Chat
                else -> Icons.Default.Forum
            },
            color = try {
                Color(android.graphics.Color.parseColor(cc.colorHex))
            } catch (e: Exception) {
                Color(0xFF4CAF50)
            }
        )
    }

    val currentChannel = channelsList.find { it.id == activeChannelId } ?: channelsList.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Horizontal channels selection row
        Text(
            text = "اختر قناة نقاش للمشاركة والتواصل بالسوق:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channelsList) { ch ->
                val isSelected = ch.id == activeChannelId
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { viewModel.selectChannel(ch.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) ch.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) ch.color else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = ch.icon,
                                contentDescription = null,
                                tint = ch.color,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ch.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ch.description,
                            fontSize = 9.sp,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Room Title banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(currentChannel.color.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = currentChannel.icon,
                    contentDescription = null,
                    tint = currentChannel.color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "# " + currentChannel.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = currentChannel.color
                    )
                    Text(
                        text = currentChannel.description,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Message List (Scrollable)
        val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                lazyListState.animateScrollToItem(messages.size - 1)
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "كن أول من يبدأ النقاش في قناة #${currentChannel.title}!",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUser?.uid
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        // User Name & Role Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isMe) "أنت" else msg.senderName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = when (msg.senderRole) {
                                            "ADMIN" -> Color(0xFFE53935).copy(alpha = 0.1f)
                                            "TECHNICIAN" -> Color(0xFF1E88E5).copy(alpha = 0.1f)
                                            else -> Color(0xFF757575).copy(alpha = 0.1f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (msg.senderRole) {
                                        "ADMIN" -> "مشرف"
                                        "TECHNICIAN" -> "فني"
                                        else -> "جار"
                                    },
                                    color = when (msg.senderRole) {
                                        "ADMIN" -> Color(0xFFE53935)
                                        "TECHNICIAN" -> Color(0xFF1E88E5)
                                        else -> Color(0xFF757575)
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Bubble
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isMe) 12.dp else 2.dp,
                                bottomEnd = if (isMe) 2.dp else 12.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = msg.content,
                                    fontSize = 13.sp,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Chat Input Row
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("اكتب رسالتك في قناة #${currentChannel.title}...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentChannel.color,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (textInput.isNotEmpty()) {
                            viewModel.sendChannelMessage(currentChannel.id, textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(currentChannel.color, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateJobDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("صيانة") }
    var description by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("عمل حر / مؤقت") }

    val categories = listOf("صيانة", "توصيل", "تعليم", "مساعدة في الحي", "أخرى")
    val jobTypes = listOf("عمل حر / مؤقت", "دوام جزئي", "مكافأة تقديرية", "دوام كامل")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "نشر فرصة عمل أو طلب مساعدة في الحي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الفرصة (مثال: معلم خصوصي رياضيات)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Text("تصنيف العمل بالحي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        CustomFilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = cat
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Text("نوع العمل/الدوام:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    jobTypes.forEach { type ->
                        CustomFilterChip(
                            selected = jobType == type,
                            onClick = { jobType = type },
                            label = type
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = payment,
                    onValueChange = { payment = it },
                    label = { Text("المقابل المادي (مثال: ١٢٠ ريال / ساعة)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل ومتطلبات المساعدة المطلوبة") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && description.isNotEmpty() && payment.isNotEmpty()) {
                                viewModel.createJob(
                                    title = title,
                                    category = category,
                                    description = description,
                                    payment = payment,
                                    jobType = jobType
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(2f),
                        enabled = title.isNotEmpty() && description.isNotEmpty() && payment.isNotEmpty()
                    ) {
                        Text("نشر الآن")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CommentDialog(
    post: PostEntity,
    comments: List<PostComment>,
    onAddComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                Text(
                    text = "التعليقات والمناقشة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Divider()

                // Post context preview
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(post.content, fontSize = 11.sp, color = Color.DarkGray, maxLines = 2)
                    }
                }

                // Comments List
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (comments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد تعليقات بعد. كن أول من يعلق!", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(comments) { comment ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(getAvatarColor(comment.authorAvatarColor), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!comment.authorAvatarUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = comment.authorAvatarUri,
                                                contentDescription = "صورة المعلق",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(comment.authorName.take(1), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(comment.content, fontSize = 12.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Input Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("اكتب تعليقاً...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newCommentText.trim().isNotEmpty()) {
                                onAddComment(newCommentText.trim())
                                newCommentText = ""
                            }
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إغلاق")
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
                                    if (!offer.techAvatarUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = offer.techAvatarUri,
                                            contentDescription = "صورة الفني",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(offer.techName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
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

// --- Helper to save selected URI to private internal storage for persistence ---
fun saveUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "profile_avatar_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
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
    var avatarColor by remember(currentUser) { mutableStateOf(currentUser?.avatarColor ?: 0) }

    var isEditing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            val localPath = saveUriToInternalStorage(context, selectedUri)
            if (localPath != null) {
                viewModel.updateUserProfile(
                    name = name,
                    phone = phone,
                    city = city,
                    neighborhood = neighborhood,
                    bio = bio,
                    profession = profession,
                    experienceYears = experience.toIntOrNull() ?: 0,
                    isOnline = isOnline,
                    avatarColor = avatarColor,
                    avatarUri = localPath
                )
                Toast.makeText(context, "تم حفظ الصورة الشخصية بنجاح!", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                            .size(90.dp)
                            .clickable { launcher.launch("image/*") }
                            .background(getAvatarColor(avatarColor), CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUser?.avatarUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = currentUser!!.avatarUri,
                                contentDescription = "الصورة الشخصية",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(name.take(1).ifEmpty { "ع" }, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Camera action overlay button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "تغيير الصورة",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    if (!currentUser?.avatarUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "إزالة الصورة الشخصية",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    viewModel.updateUserProfile(
                                        name = name,
                                        phone = phone,
                                        city = city,
                                        neighborhood = neighborhood,
                                        bio = bio,
                                        profession = profession,
                                        experienceYears = experience.toIntOrNull() ?: 0,
                                        isOnline = isOnline,
                                        avatarColor = avatarColor,
                                        avatarUri = null
                                    )
                                    Toast.makeText(context, "تمت إزالة الصورة الشخصية", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (currentUser?.role == "TECHNICIAN") Icons.Default.Handyman else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when(currentUser?.role) {
                                "ADMIN" -> "مدير عام المنصة"
                                "TECHNICIAN" -> "فني مرخص"
                                else -> "ابن الحي (عميل)"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isEditing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("اختر لون الحساب التعريفي المفضل:", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            for (i in 0 until 8) {
                                val isSelected = avatarColor == i
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(getAvatarColor(i))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { avatarColor = i },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "محدد", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Profile Stats Cards Row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("التقييم العام", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, "نجمة", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (currentUser?.role == "TECHNICIAN") "${currentUser?.rating ?: 4.5}" else "5.0",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.LightGray))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("الطلبات المنجزة", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentUser?.role == "TECHNICIAN") "${currentUser?.completedOrders ?: 0} طلبات" else "عضو نشط",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.LightGray))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("المنطقة", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = neighborhood.ifEmpty { "غير محدد" }.take(10),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        Text("الملف التعريفي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        TextButton(
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            onClick = {
                                if (isEditing) {
                                    viewModel.updateUserProfile(
                                        name = name,
                                        phone = phone,
                                        city = city,
                                        neighborhood = neighborhood,
                                        bio = bio,
                                        profession = profession,
                                        experienceYears = experience.toIntOrNull() ?: 0,
                                        isOnline = isOnline,
                                        avatarColor = avatarColor
                                    )
                                    Toast.makeText(context, "تم حفظ تعديلات الملف الشخصي بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                                isEditing = !isEditing
                            }
                        ) {
                            Text(
                                text = if (isEditing) "حفظ التغييرات" else "تعديل البيانات",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isEditing) {
                        ProfileInfoRow(Icons.Default.Person, "الاسم الكامل", currentUser?.name ?: "")
                        ProfileInfoRow(Icons.Default.Phone, "رقم الجوال", currentUser?.phoneNumber ?: "")
                        ProfileInfoRow(Icons.Default.LocationCity, "المدينة والحي", "${currentUser?.city} - ${currentUser?.neighborhood}")
                        ProfileInfoRow(Icons.Default.Info, "النبذة الشخصية", currentUser?.bio?.ifEmpty { "لا توجد نبذة حالياً. اكتب نبذة تعريفية ليتعرف عليك الآخرون بالسوق!" } ?: "")

                        if (currentUser?.role == "TECHNICIAN") {
                            ProfileInfoRow(Icons.Default.Handyman, "التخصص والمهنة", currentUser?.profession ?: "")
                            ProfileInfoRow(Icons.Default.Badge, "سنوات الخبرة", "${currentUser?.experienceYears} سنوات من العطاء")
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
                                    Text("حالة التوفر للخدمة فوراً", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                Switch(
                                    checked = isOnline,
                                    onCheckedChange = {
                                        isOnline = it
                                        viewModel.updateUserProfile(
                                            name = name, phone = phone, city = city, neighborhood = neighborhood, bio = bio,
                                            profession = profession, experienceYears = experience.toIntOrNull() ?: 0, isOnline = it,
                                            avatarColor = avatarColor
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("الاسم الكامل") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الجوال") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_phone_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("المدينة") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_city_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = neighborhood,
                            onValueChange = { neighborhood = it },
                            label = { Text("الحي") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_neighborhood_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Bio suggestions and Text field
                        Text(
                            text = "النبذة الشخصية المخصصة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("اكتب نبذة تعريفية قصيرة عنك") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
                            maxLines = 3
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "اقتراحات سريعة لنص النبذة (اضغط للنسخ للنبذة):",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        val bioTemplates = if (currentUser?.role == "TECHNICIAN") {
                            listOf(
                                "فني محترف صيانة وتركيب بخبرة طويلة ودقة عالية بالعمل.",
                                "متخصص في كشف تسريبات المياه وإصلاح الأعطال الطارئة في أسرع وقت.",
                                "تأسيس وتشطيب شبكات الإنارة وتصليح اللوحات الكهربائية بضمان وأمان."
                            )
                        } else {
                            listOf(
                                "جار متعاون في الحي، نسعى معاً لتطوير مجتمعنا وحل المشكلات يداً بيد.",
                                "محب للتطوع وتقديم المساعدة لأهل الحي لتسهيل وتيسير الخدمات.",
                                "مهتم برفع مستوى الصيانة في الحي والمشاركة المجتمعية الفعالة."
                            )
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            bioTemplates.forEach { template ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .clickable { bio = template }
                                        .padding(8.dp)
                                ) {
                                    Text(template, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        if (currentUser?.role == "TECHNICIAN") {
                            Spacer(modifier = Modifier.height(16.dp))
                            var expandedProf by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { expandedProf = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("التخصص الحالي: $profession")
                                }
                                DropdownMenu(expanded = expandedProf, onDismissRequest = { expandedProf = false }) {
                                    professionsList.forEach { p ->
                                        DropdownMenuItem(text = { Text(p) }, onClick = { profession = p; expandedProf = false })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = experience,
                                onValueChange = { experience = it },
                                label = { Text("سنوات الخبرة") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
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
                    Text("أحدث التنبيهات والإشعارات بالحي", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
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
                            if (!user.avatarUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = user.avatarUri,
                                    contentDescription = "صورة العضو",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
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
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            val localPath = saveUriToInternalStorage(context, selectedUri)
            selectedImageUri = localPath
        }
    }

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

                Spacer(modifier = Modifier.height(12.dp))
                Text("إرفاق صورة للمشكلة (اختياري):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "صورة المشكلة",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, "إزالة", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر صورة من المعرض", fontSize = 12.sp)
                    }
                }

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
                                    imageUri = selectedImageUri,
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
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { selectedUri ->
            val localPath = saveUriToInternalStorage(context, selectedUri)
            selectedImageUri = localPath
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("كتابة منشور جديد بالسوق", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("ماذا تريد أن تشارك في السوق؟") },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("post_desc_field")
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("إرفاق صورة للمنشور (اختياري):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "صورة المنشور",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, "إزالة", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر صورة من المعرض", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (content.isNotEmpty()) {
                                viewModel.createPost(content, selectedImageUri, onDismiss)
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

// --- Create Advertisement Dialog ---
@Composable
fun CreateAdvertisementDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إنشاء إعلان ترويجي مميز",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "سينشر إعلانك تلقائياً في منشورات الحي العامة كمنشور مميز، وكذلك في سوق العروض وباقات الصيانة!",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الإعلان الترويجي") },
                    placeholder = { Text("مثال: عرض صيانة مكيفات الصيف المميز") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل الإعلان وعروضك") },
                    placeholder = { Text("مثال: تنظيف فلاتر مجاني وتعبئة فريون أصلي مع كفالة 6 أشهر للخدمة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("المبلغ / السعر (اختياري)") },
                    placeholder = { Text("مثال: 120 ريال أو حسب العمل") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && description.isNotEmpty()) {
                                viewModel.createAdvertisement(title, description, if (price.isEmpty()) "غير محدد" else price) {
                                    Toast.makeText(context, "تم نشر إعلانك المميز بنجاح في المنشورات والعروض!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                            } else {
                                Toast.makeText(context, "يرجى تعبئة العنوان والتفاصيل", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("نشر الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Create Channel Dialog ---
@Composable
fun CreateChannelDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Icon selection
    val icons = listOf(
        Pair("forum", "منتدى ونقاش عام"),
        Pair("campaign", "أخبار وإعلانات"),
        Pair("handyman", "شؤون الصيانة والمهن"),
        Pair("local_offer", "بيع وشراء وتبادل"),
        Pair("groups", "أنشطة وفعاليات عامة"),
        Pair("chat", "نقاشات مفتوحة")
    )
    var selectedIconIndex by remember { mutableStateOf(0) }
    var showIconDropdown by remember { mutableStateOf(false) }

    // Color selection
    val colors = listOf(
        Pair("#4CAF50", "أخضر العشب"),
        Pair("#2196F3", "أزرق البحر"),
        Pair("#F44336", "أحمر دافئ"),
        Pair("#9C27B0", "بنفسجي ملكي"),
        Pair("#FFC107", "أصفر ذهبي"),
        Pair("#E91E63", "وردي مشرق")
    )
    var selectedColorIndex by remember { mutableStateOf(0) }
    var showColorDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إنشاء قناة نقاش جديدة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "أنشئ مساحة نقاش تفاعلية تجمع رواد السوق حول موضوع أو اهتمام مشترك.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم القناة الجديدة") },
                    placeholder = { Text("مثال: ملاك ممشى الحي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف القناة") },
                    placeholder = { Text("مثال: مساحة لنقاش وتحسين ومقترحات ممشى الحي والمرافق الرياضية") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Icon dropdown selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showIconDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("أيقونة القناة: ${icons[selectedIconIndex].second}", fontSize = 12.sp)
                        }
                    }
                    DropdownMenu(
                        expanded = showIconDropdown,
                        onDismissRequest = { showIconDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        icons.forEachIndexed { idx, item ->
                            DropdownMenuItem(
                                text = { Text(item.second, fontSize = 12.sp) },
                                onClick = {
                                    selectedIconIndex = idx
                                    showIconDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color dropdown selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showColorDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(android.graphics.Color.parseColor(colors[selectedColorIndex].first)), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("لون هوية القناة: ${colors[selectedColorIndex].second}", fontSize = 12.sp)
                        }
                    }
                    DropdownMenu(
                        expanded = showColorDropdown,
                        onDismissRequest = { showColorDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        colors.forEachIndexed { idx, item ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color(android.graphics.Color.parseColor(item.first)), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(item.second, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    selectedColorIndex = idx
                                    showColorDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && description.isNotEmpty()) {
                                viewModel.createChannel(
                                    title = title,
                                    description = description,
                                    iconName = icons[selectedIconIndex].first,
                                    colorHex = colors[selectedColorIndex].first
                                )
                                Toast.makeText(context, "تم إنشاء قناة النقاش الجديدة #${title}!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "يرجى إدخال اسم ووصف القناة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("إنشاء الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Settings Dialog ---
@Composable
fun SettingsDialog(
    viewModel: SouqViewModel,
    onDismiss: () -> Unit
) {
    val currentDarkMode by viewModel.isDarkMode.collectAsState()
    val currentNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val currentGpsLocationEnabled by viewModel.isGpsLocationEnabled.collectAsState()
    val currentAppLanguage by viewModel.appLanguage.collectAsState()

    var isDarkModeEnabled by remember(currentDarkMode) { mutableStateOf(currentDarkMode) }
    var isNotificationsEnabled by remember(currentNotificationsEnabled) { mutableStateOf(currentNotificationsEnabled) }
    var isGpsLocationEnabled by remember(currentGpsLocationEnabled) { mutableStateOf(currentGpsLocationEnabled) }
    var appLanguage by remember(currentAppLanguage) { mutableStateOf(currentAppLanguage) }
    var isLanguageDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إعدادات تطبيق سوق",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "قم بتخصيص خيارات التطبيق والتنبيهات المباشرة لخدمة الحي.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle 1: Dark Mode
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الوضع الداكن (Dark Mode)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = isDarkModeEnabled,
                        onCheckedChange = { isDarkModeEnabled = it }
                    )
                }

                Divider()

                // Toggle 2: Notifications
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الإشعارات المباشرة للحي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = { isNotificationsEnabled = it }
                    )
                }

                Divider()

                // Toggle 3: GPS
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحديد موقع خدماتي الجغرافي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = isGpsLocationEnabled,
                        onCheckedChange = { isGpsLocationEnabled = it }
                    )
                }

                Divider()

                // Language
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("لغة التطبيق", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box {
                        TextButton(onClick = { isLanguageDropdownExpanded = true }) {
                            Text(appLanguage, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = isLanguageDropdownExpanded,
                            onDismissRequest = { isLanguageDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("العربية") },
                                onClick = {
                                    appLanguage = "العربية"
                                    isLanguageDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("English (Soon)") },
                                onClick = {
                                    isLanguageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                // Supabase Sync Section
                val syncStatus by viewModel.supabaseSyncStatus.collectAsState()
                val statusMsg by viewModel.supabaseStatusMessage.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = when (syncStatus) {
                                SupabaseSyncStatus.CONNECTED -> Color(0xFFE8F5E9)
                                SupabaseSyncStatus.SYNCING -> Color(0xFFE3F2FD)
                                SupabaseSyncStatus.TABLE_NOT_FOUND -> Color(0xFFFFF3E0)
                                SupabaseSyncStatus.ERROR -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (syncStatus) {
                                    SupabaseSyncStatus.CONNECTED -> Icons.Default.DoneAll
                                    SupabaseSyncStatus.SYNCING -> Icons.Default.Refresh
                                    SupabaseSyncStatus.TABLE_NOT_FOUND -> Icons.Default.Warning
                                    SupabaseSyncStatus.ERROR -> Icons.Default.Warning
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (syncStatus) {
                                    SupabaseSyncStatus.CONNECTED -> Color(0xFF2E7D32)
                                    SupabaseSyncStatus.SYNCING -> Color(0xFF1565C0)
                                    SupabaseSyncStatus.TABLE_NOT_FOUND -> Color(0xFFEF6C00)
                                    SupabaseSyncStatus.ERROR -> Color(0xFFC62828)
                                    else -> Color.Gray
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مزامنة السحاب (Supabase)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (syncStatus) {
                                    SupabaseSyncStatus.CONNECTED -> Color(0xFF2E7D32)
                                    SupabaseSyncStatus.SYNCING -> Color(0xFF1565C0)
                                    SupabaseSyncStatus.TABLE_NOT_FOUND -> Color(0xFFEF6C00)
                                    SupabaseSyncStatus.ERROR -> Color(0xFFC62828)
                                    else -> Color.DarkGray
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusMsg,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        if (syncStatus == SupabaseSyncStatus.TABLE_NOT_FOUND) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يرجى إنشاء جداول (users, channel_messages, neighborhood_posts, service_requests) في Supabase مع تمكين صلاحيات القراءة والكتابة (RLS) للتشغيل الكامل.",
                                fontSize = 9.sp,
                                color = Color(0xFFD84315),
                                lineHeight = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مفتاح الربط مفعل تلقائياً",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            TextButton(
                                onClick = { viewModel.syncAll() },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحديث المزامنة الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                darkMode = isDarkModeEnabled,
                                notifications = isNotificationsEnabled,
                                gps = isGpsLocationEnabled,
                                language = appLanguage
                            )
                            Toast.makeText(context, "تم حفظ وتطبيق الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ وإغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
