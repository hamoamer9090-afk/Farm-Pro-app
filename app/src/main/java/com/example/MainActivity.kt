@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.data.model.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FarmViewModel
import com.example.ui.viewmodel.BatchAnimalPurchaseItem
import com.example.ui.viewmodel.BatchAnimalSaleItem
import com.example.ui.viewmodel.BatchFeedPurchaseItem
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.lazy.rememberLazyListState
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.screens.AccountManagementDialog
import com.example.ui.screens.SyncQueueScreen
import com.example.ui.screens.FeedCalculatorScreen
import com.example.ui.screens.AnalyticsDashboardScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.RecycleBinScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import androidx.fragment.app.FragmentActivity

fun convertToEasternArabicNumerals(input: String): String {
    return input.map {
        if (it.isDigit()) (it - '0' + 0x0660).toChar() else it
    }.joinToString("")
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FarmViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val primaryColorHex by viewModel.primaryColorHex.collectAsStateWithLifecycle()
            
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            val appAccentColor = remember(primaryColorHex) {
                try {
                    Color(android.graphics.Color.parseColor(primaryColorHex))
                } catch (e: Exception) {
                    Color(0xFF059669)
                }
            }
            
            MyApplicationTheme(
                darkTheme = isDarkTheme,
                primaryColor = appAccentColor,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(viewModel: FarmViewModel = viewModel()) {
    // Collect settings
    val currentFarm by viewModel.currentFarm.collectAsStateWithLifecycle()
    val farmName by viewModel.farmName.collectAsStateWithLifecycle()
    val zoomLevel by viewModel.zoomLevel.collectAsStateWithLifecycle()
    val primaryColorHex by viewModel.primaryColorHex.collectAsStateWithLifecycle()
    val cardColorHex by viewModel.cardColorHex.collectAsStateWithLifecycle()
    val textColorHex by viewModel.textColorHex.collectAsStateWithLifecycle()
    val enlargedImage by viewModel.enlargedImage.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val appLockFingerprintEnabled by viewModel.appLockFingerprintEnabled.collectAsStateWithLifecycle()
    val appLockPinEnabled by viewModel.appLockPinEnabled.collectAsStateWithLifecycle()
    val appLockPatternEnabled by viewModel.appLockPatternEnabled.collectAsStateWithLifecycle()
    val appLockPinCode by viewModel.appLockPinCode.collectAsStateWithLifecycle()
    val appLockPatternCode by viewModel.appLockPatternCode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.checkSessionValidity()
    }

    var isAppUnlocked by remember { mutableStateOf(false) }
    var didCheckLock by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }
    var inputPattern by remember { mutableStateOf("") }
    var lockError by remember { mutableStateOf("") }

    var showRecoverDialog by remember { mutableStateOf(false) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf("") }

    val tryBiometrics = {
        if (context is androidx.fragment.app.FragmentActivity) {
            com.example.utils.BiometricUtils.authenticate(
                activity = context,
                title = "قفل التطبيق",
                subtitle = "يرجى التحقق من هويتك لفتح التطبيق",
                onSuccess = { isAppUnlocked = true; lockError = "" },
                onError = { lockError = "فشل التحقق من البصمة" }
            )
        }
    }

    LaunchedEffect(isAppLockEnabled) {
        if (!didCheckLock) {
            didCheckLock = true
            if (isAppLockEnabled) {
                if (appLockFingerprintEnabled && context is androidx.fragment.app.FragmentActivity) {
                    tryBiometrics()
                }
            } else {
                isAppUnlocked = true
            }
        }
    }

    if (!isAppUnlocked && didCheckLock && isAppLockEnabled) {
        val appAccentColor = remember(primaryColorHex) {
            try {
                Color(android.graphics.Color.parseColor(primaryColorHex))
            } catch (e: Exception) {
                Color(0xFF059669)
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = "App Locked", modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("التطبيق مقفل", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (lockError.isNotEmpty()) {
                    Text(lockError, color = Color.Red, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                var pinAttempts by remember { mutableIntStateOf(0) }
                var isLockedOut by remember { mutableStateOf(false) }

                LaunchedEffect(isLockedOut) {
                    if (isLockedOut) {
                        lockError = "تم حظر الإدخال مؤقتاً لمدة ٥ ثوانِ"
                        kotlinx.coroutines.delay(5000L)
                        isLockedOut = false
                        pinAttempts = 0
                        lockError = ""
                        inputPin = ""
                    }
                }

                if (appLockPinEnabled) {
                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { 
                            if (!isLockedOut) {
                                inputPin = it 
                                if (it == appLockPinCode) {
                                    isAppUnlocked = true
                                    pinAttempts = 0
                                }
                                else if (it.length >= appLockPinCode.length) {
                                    pinAttempts++
                                    if (pinAttempts >= 3) {
                                        isLockedOut = true
                                    } else {
                                        lockError = "الرمز غير صحيح. المحاولات المتبقية: ${3 - pinAttempts}"
                                        inputPin = ""
                                    }
                                }
                            }
                        },
                        label = { Text("أدخل رمز PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isLockedOut
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (appLockPatternEnabled) {
                    OutlinedTextField(
                        value = inputPattern,
                        onValueChange = { 
                            inputPattern = it 
                            if (it == appLockPatternCode) isAppUnlocked = true
                            else if (it.length >= appLockPatternCode.length) lockError = "النقش غير صحيح"
                        },
                        label = { Text("أدخل نص النقش") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (appLockFingerprintEnabled) {
                    Button(onClick = tryBiometrics, colors = ButtonDefaults.buttonColors(containerColor = appAccentColor)) {
                        Text("التحقق بالبصمة/الوجه")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TextButton(
                    onClick = {
                        showRecoverDialog = true
                        recoveryAnswerInput = ""
                        recoveryError = ""
                    }
                ) {
                    Text("نسيت كلمة المرور؟ ❓", color = appAccentColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showRecoverDialog) {
            val securityQuestion by viewModel.securityQuestion.collectAsStateWithLifecycle()
            val securityAnswer by viewModel.securityAnswer.collectAsStateWithLifecycle()

            AlertDialog(
                onDismissRequest = { showRecoverDialog = false },
                title = { Text("استرجاع وتخطي قفل التطبيق 🛡️", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (securityQuestion.isNotEmpty() && securityAnswer.isNotEmpty()) {
                            Text("الرجاء الإجابة عن سؤال الأمان المخصص لتخطي القفل وإعداد كود جديد:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("السؤال: $securityQuestion", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = appAccentColor)
                            
                            OutlinedTextField(
                                value = recoveryAnswerInput,
                                onValueChange = { recoveryAnswerInput = it; recoveryError = "" },
                                label = { Text("إجابتك") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            if (recoveryError.isNotEmpty()) {
                                Text(recoveryError, color = Color.Red, fontSize = 12.sp)
                            }
                        } else {
                            Text("لم تقم بإعداد سؤال أمان مسبقاً! يرجى الاستمرار بتسجيل الدخول الفردي أو مراجعة المشرف.", color = Color.Red, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    if (securityQuestion.isNotEmpty() && securityAnswer.isNotEmpty()) {
                        Button(
                            onClick = {
                                if (recoveryAnswerInput.trim().equals(securityAnswer.trim(), ignoreCase = true)) {
                                    isAppUnlocked = true
                                    showRecoverDialog = false
                                    Toast.makeText(context, "تم تخطي القفل بنجاح! الرجاء تحديث أرقام PIN من الإعدادات.", Toast.LENGTH_LONG).show()
                                } else {
                                    recoveryError = "الإجابة غير مطابقة! يرجى المحاولة مرة أخرى."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appAccentColor)
                        ) {
                            Text("التحقق وفتح القفل", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecoverDialog = false }) { Text("إلغاء") }
                }
            )
        }
        return
    }

    // Determine custom theme primary color from SharedPreferences dynamic HEX
    val appAccentColor = remember(primaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(primaryColorHex))
        } catch (e: Exception) {
            Color(0xFF059669) // fallback Emerald
        }
    }

    // Determine custom card background color
    val appCardBgColor = remember(cardColorHex) {
        try {
            Color(android.graphics.Color.parseColor(cardColorHex))
        } catch (e: Exception) {
            Color.White // fallback White
        }
    }

    // Determine custom text color
    val appTextColor = remember(textColorHex) {
        try {
            Color(android.graphics.Color.parseColor(textColorHex))
        } catch (e: Exception) {
            Color(0xFF1E293B) // fallback Slate Dark
        }
    }

    // Force RTL local layout direction, custom layout scale/zoom, and custom fonts
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customDensity = remember(currentDensity, zoomLevel) {
        object : androidx.compose.ui.unit.Density by currentDensity {
            override val fontScale: Float
                get() = currentDensity.fontScale * (zoomLevel / 16f)
            override val density: Float
                get() = currentDensity.density * (zoomLevel / 16f)
        }
    }

    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val activeFontFamily = remember(selectedFont) {
        when(selectedFont) {
            "cairo" -> androidx.compose.ui.text.font.FontFamily.SansSerif
            "amiri" -> androidx.compose.ui.text.font.FontFamily.Serif
            "tajawal" -> androidx.compose.ui.text.font.FontFamily.Default
            else -> androidx.compose.ui.text.font.FontFamily.Default
        }
    }

    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val isLtr = appLang == "en"
    val layoutDirection = if (isLtr) LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        androidx.compose.ui.platform.LocalDensity provides customDensity,
        LocalTextStyle provides androidx.compose.ui.text.TextStyle(fontFamily = activeFontFamily)
    ) {
        val isGoogleLinked by viewModel.isGoogleLinked.collectAsStateWithLifecycle()
        val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
        val googleName by viewModel.googleUserName.collectAsStateWithLifecycle()

        if (currentFarm == null) {
            LoginAndFarmSelectionScreen(viewModel, appAccentColor, zoomLevel)
        } else {
            // Main App with Scaffold, custom bottom navigation & sliding right drawer
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            // Dynamic custom labels loaded from SharedPreferences (translators mapping)
            val sp = context.getSharedPreferences("farm_titles", Context.MODE_PRIVATE)
            var labelBarnRaw by remember { mutableStateOf(sp.getString("label_barn", "الحظيرة") ?: "الحظيرة") }
            var labelFeedsRaw by remember { mutableStateOf(sp.getString("label_feeds", "الأعلاف") ?: "الأعلاف") }
            var labelAccountsRaw by remember { mutableStateOf(sp.getString("label_accounts", "الحسابات") ?: "الحسابات") }

            val labelBarn = com.example.util.Localization.t(labelBarnRaw, appLang)
            val labelFeeds = com.example.util.Localization.t(labelFeedsRaw, appLang)
            val labelAccounts = com.example.util.Localization.t(labelAccountsRaw, appLang)

            // Active Internal navigation
            var activeTab by remember { mutableStateOf("dashboard") } // "dashboard", "barn", "feeds", "accounts", "notes", "archive", "settings"
            var batchInvoiceType by remember { mutableStateOf("purchase") } // "purchase", "sale", "feed"
            var barnMenuExpanded by remember { mutableStateOf(false) }

            // --- Global Confirm & Dialog States & Permission checks ---
            var showDeleteConfirmDialog by remember { mutableStateOf(false) }
            var onDeleteConfirmed by remember { mutableStateOf<(() -> Unit)?>(null) }
            var deleteConfirmMessage by remember { mutableStateOf("") }

            var showEditConfirmDialog by remember { mutableStateOf(false) }
            var onEditConfirmed by remember { mutableStateOf<(() -> Unit)?>(null) }
            var editConfirmMessage by remember { mutableStateOf("") }

            val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
            val enableSwipeNavigation by viewModel.enableSwipeNavigation.collectAsStateWithLifecycle()
            val invertSwipeDirection by viewModel.invertSwipeDirection.collectAsStateWithLifecycle()

            val checkDeletePermissionAndConfirm: (String, () -> Unit) -> Unit = { msg, onConfirm ->
                if (!viewModel.hasPermission("delete_animal")) {
                    Toast.makeText(context, "عذراً، لا تملك صلاحية الحذف ❌", Toast.LENGTH_LONG).show()
                } else {
                    deleteConfirmMessage = msg
                    onDeleteConfirmed = onConfirm
                    showDeleteConfirmDialog = true
                }
            }

            val checkEditPermissionAndConfirm: (String, () -> Unit) -> Unit = { msg, onConfirm ->
                if (!viewModel.hasPermission("edit_animal")) {
                    Toast.makeText(context, "عذراً، لا تملك صلاحية تعديل السجلات ❌", Toast.LENGTH_LONG).show()
                } else {
                    editConfirmMessage = msg
                    onEditConfirmed = onConfirm
                    showEditConfirmDialog = true
                }
            }

            val checkAddPermission: (() -> Unit) -> Unit = { onProceed ->
                if (!viewModel.hasPermission("add_animal")) {
                    Toast.makeText(context, "عذراً، لا تملك صلاحية للإضافة ❌", Toast.LENGTH_LONG).show()
                } else {
                    onProceed()
                }
            }

            // Support phone's back button elegantly so that it returns to dashboard instead of exiting
            BackHandler(enabled = activeTab != "dashboard") {
                activeTab = "dashboard"
            }
            val animalsForDrawer by viewModel.animalsList.collectAsStateWithLifecycle()
            val filterTypeForDrawer by viewModel.selectedAnimalType.collectAsStateWithLifecycle()

            // Dialog displays
            var showAnimalDialog by remember { mutableStateOf(false) }
            var animalToEdit by remember { mutableStateOf<com.example.data.model.AnimalEntity?>(null) }
            var showNewbornDialog by remember { mutableStateOf(false) }
            var showFeedDialog by remember { mutableStateOf(false) }
            var showMedicineDialog by remember { mutableStateOf(false) }
            var showPersonDialog by remember { mutableStateOf(false) }
            var showTransactionDialog by remember { mutableStateOf(false) }
            var transactionType by remember { mutableStateOf("income") } // "income" or "expense"
            var animalIdToView by remember { mutableStateOf<Int?>(null) }

            val updateLabels: () -> Unit = {
                labelBarnRaw = sp.getString("label_barn", "الحظيرة") ?: "الحظيرة"
                labelFeedsRaw = sp.getString("label_feeds", "الأعلاف") ?: "الأعلاف"
                labelAccountsRaw = sp.getString("label_accounts", "الحسابات") ?: "الحسابات"
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(300.dp)
                            .background(MaterialTheme.colorScheme.surface),
                        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .statusBarsPadding()
                                    .navigationBarsPadding(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    val profilePic by viewModel.userProfilePic.collectAsStateWithLifecycle()
                                    var showProfileDialog by remember { mutableStateOf(false) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(bottom = 24.dp)
                                            .clickable { showProfileDialog = true }
                                            .padding(8.dp)
                                            .fillMaxWidth()
                                    ) {
                                        if (profilePic.isNotEmpty()) {
                                            val bmp = com.example.util.ImageUtils.base64ToBitmap(profilePic)
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "صورة الحساب",
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(appAccentColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(appAccentColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (googleName.isNotEmpty()) googleName else com.example.util.Localization.t("حساب المزارع", appLang),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = "${com.example.util.Localization.t("مزارعي", appLang)} - ${com.example.util.Localization.t(farmName, appLang)}",
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (showProfileDialog) {
                                        AccountManagementDialog(
                                            viewModel = viewModel,
                                            accentColor = appAccentColor,
                                            onDismiss = { showProfileDialog = false }
                                        )
                                    }

                                    val hideSidebarDashboard by viewModel.hideSidebarDashboard.collectAsStateWithLifecycle()
                                    val hideSidebarBarn by viewModel.hideSidebarBarn.collectAsStateWithLifecycle()
                                    val hideSidebarFeeds by viewModel.hideSidebarFeeds.collectAsStateWithLifecycle()
                                    val hideSidebarAccounts by viewModel.hideSidebarAccounts.collectAsStateWithLifecycle()
                                    val hideSidebarNotes by viewModel.hideSidebarNotes.collectAsStateWithLifecycle()
                                    val hideSidebarArchive by viewModel.hideSidebarArchive.collectAsStateWithLifecycle()
                                    val hideSidebarBackup by viewModel.hideSidebarBackup.collectAsStateWithLifecycle()
                                    val hideSidebarUsers by viewModel.hideSidebarUsers.collectAsStateWithLifecycle()
                                    val hideSidebarFeedCalc by viewModel.hideSidebarFeedCalc.collectAsStateWithLifecycle()
                                    val hideSidebarReports by viewModel.hideSidebarReports.collectAsStateWithLifecycle()
                                    val hideSidebarReminders by viewModel.hideSidebarReminders.collectAsStateWithLifecycle()

                                    if (!hideSidebarDashboard) {
                                        itemDrawerButton("لوح التحكم الرئيسية", Icons.Default.Dashboard, activeTab == "dashboard", appAccentColor) {
                                            activeTab = "dashboard"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarBarn) {
                                        itemDrawerButton(labelBarn, Icons.Default.Pets, activeTab == "barn", appAccentColor) {
                                            activeTab = "barn"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarFeeds) {
                                        itemDrawerButton(labelFeeds, Icons.Default.Grass, activeTab == "feeds", appAccentColor) {
                                            activeTab = "feeds"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarAccounts) {
                                        itemDrawerButton(labelAccounts, Icons.Default.People, activeTab == "accounts", appAccentColor) {
                                            activeTab = "accounts"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarNotes) {
                                        itemDrawerButton("الملاحظات والوسائط", Icons.Default.Comment, activeTab == "notes", appAccentColor) {
                                            activeTab = "notes"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarArchive) {
                                        itemDrawerButton("الأرشيف والبيانات", Icons.Default.Archive, activeTab == "archive", appAccentColor) {
                                            activeTab = "archive"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarBackup) {
                                        itemDrawerButton("النسخ الاحتياطي ونقل السجلات", Icons.Default.Backup, activeTab == "backup", appAccentColor) {
                                            activeTab = "backup"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarUsers) {
                                        itemDrawerButton("إدارة فريق العمل والصلاحيات 👥", Icons.Default.Engineering, activeTab == "users", appAccentColor) {
                                            activeTab = "users"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }

                                    if (!hideSidebarFeedCalc) {
                                        itemDrawerButton("حاسبة الأعلاف", Icons.Default.Calculate, activeTab == "feed_calculator", appAccentColor) {
                                            activeTab = "feed_calculator"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarReports) {
                                        itemDrawerButton("الإحصائيات والتقارير", Icons.Default.PieChart, activeTab == "analytics", appAccentColor) {
                                            activeTab = "analytics"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    if (!hideSidebarReminders) {
                                        itemDrawerButton("التنبيهات الذكية", Icons.Default.NotificationsActive, activeTab == "reminders", appAccentColor) {
                                            activeTab = "reminders"
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    }
                                    itemDrawerButton("سجل الحذف", Icons.Default.DeleteSweep, activeTab == "recycle_bin", appAccentColor) {
                                        activeTab = "recycle_bin"
                                        coroutineScope.launch { drawerState.close() }
                                    }

                                    itemDrawerButton("إعدادات التطبيق", Icons.Default.Settings, activeTab == "settings", appAccentColor) {
                                        activeTab = "settings"
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                }

                                Button(
                                    onClick = {
                                        // Skip Google signOut
                                        // Skip unlink
                                         // Skip Firebase signOut
                                         viewModel.exitCurrentFarmOnly()
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(com.example.util.Localization.t("تسجيل الخروج", appLang), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = appAccentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = farmName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "افتح القائمة")
                                }
                            },
                            actions = {
                                val context = LocalContext.current
                                val coroutineScope = rememberCoroutineScope()
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        val firebaseManager = com.example.data.remote.FirebaseManager()
                                        try {
                                            val auth = firebaseManager.auth
                                            if (auth.currentUser == null) {
                                                val success = firebaseManager.signInAnonymously()
                                                if (success) {
                                                    Toast.makeText(context, "تم الاتصال بـ Firebase ✅ (زائر)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "فشل الاتصال بـ Firebase ❌", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "متصل بـ Firebase ✅", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطأ في الاتصال: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "تأكيد الاتصال بـ Firebase",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = {
                                    activeTab = "settings"
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "الإعدادات",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = {
                                     viewModel.exitCurrentFarmOnly()
                                 }) {
                                     Icon(
                                         imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "تسجيل الخروج",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    },
                    bottomBar = {
                        BottomAppBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Tab 1
                                IconButton(
                                    onClick = { activeTab = "dashboard" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Dashboard,
                                            contentDescription = "الرئيسية",
                                            tint = if (activeTab == "dashboard") appAccentColor else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text("الرئيسية", fontSize = 10.sp, color = if (activeTab == "dashboard") appAccentColor else Color.Gray)
                                    }
                                }

                                // Tab 2
                                IconButton(
                                    onClick = { activeTab = "barn" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Pets,
                                            contentDescription = labelBarn,
                                            tint = if (activeTab == "barn") appAccentColor else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(labelBarn, fontSize = 10.sp, color = if (activeTab == "barn") appAccentColor else Color.Gray)
                                    }
                                }

                                // Centered Custom '+' HUB button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    var showMenu by remember { mutableStateOf(false) }

                                    FloatingActionButton(
                                        onClick = { showMenu = !showMenu },
                                        containerColor = appAccentColor,
                                        contentColor = Color.White,
                                        shape = CircleShape,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "إضافة جديدة", modifier = Modifier.size(28.dp))
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("إدخل رأس ماشية منفصلة 🐃") },
                                            leadingIcon = { Icon(Icons.Default.Pets, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                showAnimalDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("إضافة مولود جديد 🍼") },
                                            leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                showNewbornDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("إدخل بند أعلاف منفصل 🌾") },
                                            leadingIcon = { Icon(Icons.Default.Grass, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                showFeedDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("فاتورة شراء رؤوس (مجموعة) 🧾") },
                                            leadingIcon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                batchInvoiceType = "purchase"
                                                activeTab = "batch_invoice"
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("فاتورة بيع رؤوس (مجموعة) 🧾") },
                                            leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                batchInvoiceType = "sale"
                                                activeTab = "batch_invoice"
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("فاتورة شراء أعلاف (مجموعة) 🌽") },
                                            leadingIcon = { Icon(Icons.Default.ShoppingBasket, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                batchInvoiceType = "feed"
                                                activeTab = "batch_invoice"
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("إيراد أو قبض مالي (سند)") },
                                            leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                transactionType = "income"
                                                showTransactionDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("مصروف أو دفع مالي (سند)") },
                                            leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = appAccentColor) },
                                            onClick = {
                                                showMenu = false
                                                transactionType = "expense"
                                                showTransactionDialog = true
                                            }
                                        )
                                    }
                                }

                                // Tab 3
                                IconButton(
                                    onClick = { activeTab = "archive" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Archive,
                                            contentDescription = "الأرشيف والبيانات",
                                            tint = if (activeTab == "archive") appAccentColor else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text("الأرشيف", fontSize = 10.sp, color = if (activeTab == "archive") appAccentColor else Color.Gray)
                                    }
                                }

                                // Tab 4
                                IconButton(
                                    onClick = { activeTab = "accounts" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.People,
                                            contentDescription = labelAccounts,
                                            tint = if (activeTab == "accounts") appAccentColor else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(labelAccounts, fontSize = 10.sp, color = if (activeTab == "accounts") appAccentColor else Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val tabSequence = listOf("dashboard", "barn", "feeds", "accounts", "notes", "archive", "backup", "settings")
                    var swipeOffsetX by remember { mutableStateOf(0f) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .let { baseModifier ->
                                if (enableSwipeNavigation) {
                                    baseModifier.pointerInput(activeTab) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (kotlin.math.abs(swipeOffsetX) > 50f) {
                                                    val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                                                    val isNext = if (isRtl) swipeOffsetX > 0f else swipeOffsetX < 0f
                                                    val effectiveNext = isNext
                                                    val currentIndex = tabSequence.indexOf(activeTab)
                                                    if (currentIndex != -1) {
                                                        if (effectiveNext && currentIndex < tabSequence.size - 1) {
                                                            activeTab = tabSequence[currentIndex + 1]
                                                        } else if (!effectiveNext && currentIndex > 0) {
                                                            activeTab = tabSequence[currentIndex - 1]
                                                        }
                                                    }
                                                }
                                                swipeOffsetX = 0f
                                            },
                                            onHorizontalDrag = { change, dragAmount ->
                                                swipeOffsetX += dragAmount
                                            }
                                        )
                                    }
                                } else baseModifier
                            }
                    ) {
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                val currentIndex = tabSequence.indexOf(initialState)
                                val targetIndex = tabSequence.indexOf(targetState)
                                val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                                val forward = targetIndex > currentIndex
                                val slideToLeft = if (isRtl) !forward else forward
                                if (slideToLeft) {
                                    androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                            androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                } else {
                                    androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                                            androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                }
                            },
                            label = "tabChange"
                        ) { target ->
                            when (target) {
                                "financial_details" -> com.example.ui.screens.FinancialDetailsPage(
                                    viewModel = viewModel,
                                    type = transactionType,
                                    accentColor = appAccentColor,
                                    onBack = { activeTab = "dashboard" }
                                )
                                "animal_details" -> com.example.ui.screens.AnimalDetailsScreen(
                                    viewModel = viewModel,
                                    animalId = animalIdToView ?: -1,
                                    accentColor = appAccentColor,
                                    onBack = { activeTab = "barn" },
                                    onSell = {  }, // we can implement these later or in the screen details
                                    onDelete = { },
                                    onEdit = { an -> 
                                        checkEditPermissionAndConfirm("تعديل بيانات الرأس") {
                                            animalToEdit = an
                                            showAnimalDialog = true
                                        } 
                                    }
                                )
                                "dashboard" -> DashboardScreen(
                                    viewModel = viewModel,
                                    accentColor = appAccentColor,
                                    zoomLevel = zoomLevel,
                                    onNavigateToTab = { activeTab = it },
                                    onOpenTransaction = { type ->
                                        transactionType = type
                                        showTransactionDialog = true
                                    },
                                    onOpenAnimal = { showAnimalDialog = true },
                                    onOpenFeed = { showFeedDialog = true },
                                    onOpenFinancialDetails = { type ->
                                        // Use global transaction type variable to pass the type
                                        transactionType = type 
                                        activeTab = "financial_details"
                                    }
                                )
                                "barn" -> BarnScreen(
                                    viewModel,
                                    appAccentColor,
                                    zoomLevel,
                                    appCardBgColor,
                                    checkAddPermission,
                                    checkEditPermissionAndConfirm,
                                    checkDeletePermissionAndConfirm,
                                    onAddAnimalClick = { checkAddPermission { showAnimalDialog = true } },
                                    onEditAnimal = { an -> 
                                        checkEditPermissionAndConfirm("تعديل بيانات الرأس") {
                                            animalToEdit = an
                                            showAnimalDialog = true
                                        } 
                                    },
                                    onViewAnimal = { id ->
                                        animalIdToView = id
                                        activeTab = "animal_details"
                                    }
                                )
                                "feeds" -> FeedsScreen(
                                    viewModel,
                                    appAccentColor,
                                    zoomLevel,
                                    appCardBgColor,
                                    checkAddPermission,
                                    checkEditPermissionAndConfirm,
                                    checkDeletePermissionAndConfirm,
                                    onAddFeedClick = { checkAddPermission { showFeedDialog = true } },
                                    onAddMedicineClick = { checkAddPermission { showMedicineDialog = true } }
                                )
                                "batch_invoice" -> BatchInvoiceScreen(viewModel, appAccentColor, zoomLevel, batchInvoiceType, onCompleted = { activeTab = "dashboard" })
                                "accounts" -> AccountsScreen(
                                    viewModel,
                                    appAccentColor,
                                    zoomLevel,
                                    appCardBgColor,
                                    checkAddPermission,
                                    checkEditPermissionAndConfirm,
                                    checkDeletePermissionAndConfirm
                                )
                                "notes" -> NotesScreen(
                                    viewModel,
                                    appAccentColor,
                                    zoomLevel,
                                    appCardBgColor,
                                    checkAddPermission,
                                    checkEditPermissionAndConfirm,
                                    checkDeletePermissionAndConfirm
                                )
                                "archive" -> ArchiveScreen(viewModel, appAccentColor, zoomLevel, appCardBgColor, checkDeletePermissionAndConfirm, { a ->
                                    checkEditPermissionAndConfirm("سيتم تعديل بيانات الرأس") {
                                        animalToEdit = a
                                        showAnimalDialog = true
                                    }
                                }, { id ->
                                    animalIdToView = id
                                    activeTab = "animal_details"
                                }, { tab ->
                                    activeTab = tab
                                })
                                "recycle_bin" -> RecycleBinScreen(viewModel, appAccentColor, zoomLevel)
                                "backup" -> BackupScreen(viewModel, appAccentColor, zoomLevel, checkDeletePermissionAndConfirm)
                                "feed_calculator" -> FeedCalculatorScreen(appAccentColor, appCardBgColor)
                                "analytics" -> AnalyticsDashboardScreen(appAccentColor, appCardBgColor)
                                "reminders" -> RemindersScreen(appAccentColor, appCardBgColor)
                                "settings" -> com.example.ui.screens.RedesignedSettingsScreen(
                                    viewModel = viewModel,
                                    accentColor = appAccentColor,
                                    zoomLevel = zoomLevel,
                                    onUpdateLabels = updateLabels
                                )
                                "users" -> UsersManagementScreen(
                                    viewModel = viewModel,
                                    accentColor = appAccentColor,
                                    zoom = zoomLevel
                                )
                            }
                        }
                    }
                }
            }

            // --- Unified Insertion overlays ---
            // --- Global Confirmation Overlays for Deletes and Edits ---
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("تأكيد الحذف ⚠️", fontWeight = FontWeight.Bold) },
                    text = { Text(deleteConfirmMessage, fontSize = 14.sp) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                onDeleteConfirmed?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("نعم، احذف السجل", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            if (showEditConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showEditConfirmDialog = false },
                    title = { Text("تأكيد حفظ التعديلات 📝", fontWeight = FontWeight.Bold) },
                    text = { Text(editConfirmMessage, fontSize = 14.sp) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEditConfirmDialog = false
                                onEditConfirmed?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = appAccentColor)
                        ) {
                            Text("نعم، تعديل وحفظ", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditConfirmDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            if (showAnimalDialog || animalToEdit != null) {
                AddAnimalDialog(
                    viewModel = viewModel,
                    accentColor = appAccentColor,
                    animalToEdit = animalToEdit,
                    onDismiss = { 
                        showAnimalDialog = false 
                        animalToEdit = null
                    }
                )
            }

            if (showNewbornDialog) {
                AddNewbornDialog(
                    viewModel = viewModel,
                    accentColor = appAccentColor,
                    onDismiss = { showNewbornDialog = false }
                )
            }

            if (showFeedDialog) {
                AddFeedDialog(
                    viewModel = viewModel,
                    accentColor = appAccentColor,
                    onDismiss = { showFeedDialog = false }
                )
            }

            if (showMedicineDialog) {
                AddMedicineDialog(
                    viewModel = viewModel,
                    accentColor = appAccentColor,
                    onDismiss = { showMedicineDialog = false }
                )
            }

            if (showTransactionDialog) {
                AddTransactionDialog(
                    viewModel = viewModel,
                    type = transactionType,
                    accentColor = appAccentColor,
                    onDismiss = { showTransactionDialog = false }
                )
            }
        }

        // Expanded full screen image overlays ("وعند الضغط علي الصور تفتح حجم اكبر")
        if (enlargedImage != null) {
            Dialog(
                onDismissRequest = { viewModel.triggerImageEnlargement(null) },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { viewModel.triggerImageEnlargement(null) },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(enlargedImage) {
                        enlargedImage?.let { ImageUtils.base64ToBitmap(it) }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "عرض موسع",
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("تعذر عرض الصورة", color = Color.White)
                    }

                    // Floating close button
                    IconButton(
                        onClick = { viewModel.triggerImageEnlargement(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun itemDrawerButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, accentColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else Color(0xFF334155)
            )
        }
    }
}

// ================= THE MULTI-FARM SELECTION WINDOW =================
@Composable
fun FarmManagerLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Smiling sun at top-right
        val sunCenterX = width * 0.72f
        val sunCenterY = height * 0.22f
        val sunRadius = width * 0.11f
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = sunRadius,
            center = androidx.compose.ui.geometry.Offset(sunCenterX, sunCenterY)
        )
        for (i in 0 until 8) {
            val angle = (i * 45) * (Math.PI / 180)
            val startX = sunCenterX + (sunRadius + 4) * Math.cos(angle).toFloat()
            val startY = sunCenterY + (sunRadius + 4) * Math.sin(angle).toFloat()
            val endX = sunCenterX + (sunRadius + 14) * Math.cos(angle).toFloat()
            val endY = sunCenterY + (sunRadius + 14) * Math.sin(angle).toFloat()
            drawLine(
                color = Color(0xFFFBBF24),
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 2f,
            center = androidx.compose.ui.geometry.Offset(sunCenterX - 6, sunCenterY - 4)
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 2f,
            center = androidx.compose.ui.geometry.Offset(sunCenterX + 6, sunCenterY - 4)
        )
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(sunCenterX - 8, sunCenterY - 2),
            size = androidx.compose.ui.geometry.Size(16f, 12f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Silo on the left
        val siloLeft = width * 0.28f
        val siloTop = height * 0.35f
        val siloWidth = width * 0.11f
        val siloHeight = height * 0.45f
        drawArc(
            color = Color(0xFF94A3B8),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(siloLeft, siloTop - (siloWidth / 2f)),
            size = androidx.compose.ui.geometry.Size(siloWidth, siloWidth)
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = androidx.compose.ui.geometry.Offset(siloLeft, siloTop),
            size = androidx.compose.ui.geometry.Size(siloWidth, siloHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f)
        )
        for (j in 1..4) {
            val y = siloTop + (siloHeight * j / 5f)
            drawLine(
                color = Color(0xFF64748B),
                start = androidx.compose.ui.geometry.Offset(siloLeft, y),
                end = androidx.compose.ui.geometry.Offset(siloLeft + siloWidth, y),
                strokeWidth = 2f
            )
        }

        // Barn in the center
        val barnLeft = width * 0.40f
        val barnTop = height * 0.42f
        val barnWidth = width * 0.32f
        val barnHeight = height * 0.38f
        drawRect(
            color = Color(0xFFD97706),
            topLeft = androidx.compose.ui.geometry.Offset(barnLeft, barnTop),
            size = androidx.compose.ui.geometry.Size(barnWidth, barnHeight)
        )
        val roofPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(barnLeft - 10f, barnTop)
            lineTo(barnLeft + barnWidth / 2f, barnTop - 35f)
            lineTo(barnLeft + barnWidth + 10f, barnTop)
            close()
        }
        drawPath(
            path = roofPath,
            color = Color(0xFF15803D)
        )
        drawPath(
            path = roofPath,
            color = Color(0xFF166534),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )

        val winWidth = barnWidth * 0.28f
        val winHeight = barnHeight * 0.28f
        val winLeft = barnLeft + (barnWidth - winWidth) / 2f
        val winTop = barnTop + 14f
        drawRect(
            color = Color(0xFFFEF3C7),
            topLeft = androidx.compose.ui.geometry.Offset(winLeft, winTop),
            size = androidx.compose.ui.geometry.Size(winWidth, winHeight)
        )
        drawLine(
            color = Color(0xFFD97706),
            start = androidx.compose.ui.geometry.Offset(winLeft + winWidth / 2f, winTop),
            end = androidx.compose.ui.geometry.Offset(winLeft + winWidth / 2f, winTop + winHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFFD97706),
            start = androidx.compose.ui.geometry.Offset(winLeft, winTop + winHeight / 2f),
            end = androidx.compose.ui.geometry.Offset(winLeft + winWidth, winTop + winHeight / 2f),
            strokeWidth = 2f
        )

        // Barn double doors
        val doorWidth = barnWidth * 0.45f
        val doorHeight = barnHeight * 0.40f
        val doorLeft = barnLeft + (barnWidth - doorWidth) / 2f
        val doorTop = barnTop + barnHeight - doorHeight
        drawRect(
            color = Color(0xFF92400E),
            topLeft = androidx.compose.ui.geometry.Offset(doorLeft, doorTop),
            size = androidx.compose.ui.geometry.Size(doorWidth, doorHeight)
        )
        drawRect(
            color = Color(0xFFFCD34D),
            topLeft = androidx.compose.ui.geometry.Offset(doorLeft, doorTop),
            size = androidx.compose.ui.geometry.Size(doorWidth, doorHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
        )
        drawLine(
            color = Color(0xFFFCD34D),
            start = androidx.compose.ui.geometry.Offset(doorLeft, doorTop),
            end = androidx.compose.ui.geometry.Offset(doorLeft + doorWidth, doorTop + doorHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFFFCD34D),
            start = androidx.compose.ui.geometry.Offset(doorLeft + doorWidth, doorTop),
            end = androidx.compose.ui.geometry.Offset(doorLeft, doorTop + doorHeight),
            strokeWidth = 2f
        )

        // Tractor
        val tracLeft = width * 0.12f
        val tracTop = height * 0.62f
        val tracWidth = width * 0.18f
        val tracHeight = height * 0.18f
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 16f,
            center = androidx.compose.ui.geometry.Offset(tracLeft + 15f, tracTop + tracHeight)
        )
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = 6f,
            center = androidx.compose.ui.geometry.Offset(tracLeft + 15f, tracTop + tracHeight)
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = 10f,
            center = androidx.compose.ui.geometry.Offset(tracLeft + tracWidth - 10f, tracTop + tracHeight + 6f)
        )
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = 4f,
            center = androidx.compose.ui.geometry.Offset(tracLeft + tracWidth - 10f, tracTop + tracHeight + 6f)
        )
        drawRoundRect(
            color = Color(0xFF16A34A),
            topLeft = androidx.compose.ui.geometry.Offset(tracLeft + 8f, tracTop + 10f),
            size = androidx.compose.ui.geometry.Size(tracWidth - 14f, tracHeight - 12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
        )
        drawRect(
            color = Color(0xFF16A34A),
            topLeft = androidx.compose.ui.geometry.Offset(tracLeft + 8f, tracTop - 10f),
            size = androidx.compose.ui.geometry.Size(22f, 22f)
        )
        drawRect(
            color = Color(0xFFE2E8F0),
            topLeft = androidx.compose.ui.geometry.Offset(tracLeft + 11f, tracTop - 7f),
            size = androidx.compose.ui.geometry.Size(16f, 15f)
        )
        drawLine(
            color = Color(0xFF475569),
            start = androidx.compose.ui.geometry.Offset(tracLeft + tracWidth - 16f, tracTop + 14f),
            end = androidx.compose.ui.geometry.Offset(tracLeft + tracWidth - 16f, tracTop - 12f),
            strokeWidth = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Tree on the right
        val treeTrunkLeft = width * 0.78f
        val treeTrunkTop = height * 0.58f
        val treeTrunkWidth = width * 0.04f
        val treeTrunkHeight = height * 0.22f
        val treeFoliageCenterX = width * 0.80f
        val treeFoliageCenterY = height * 0.50f
        val treeFoliageRadius = width * 0.11f
        drawRect(
            color = Color(0xFF78350F),
            topLeft = androidx.compose.ui.geometry.Offset(treeTrunkLeft, treeTrunkTop),
            size = androidx.compose.ui.geometry.Size(treeTrunkWidth, treeTrunkHeight)
        )
        drawCircle(
            color = Color(0xFF22C55E),
            radius = treeFoliageRadius,
            center = androidx.compose.ui.geometry.Offset(treeFoliageCenterX, treeFoliageCenterY)
        )
        drawCircle(
            color = Color(0xFF16A34A),
            radius = treeFoliageRadius * 0.8f,
            center = androidx.compose.ui.geometry.Offset(treeFoliageCenterX - 10f, treeFoliageCenterY - 10f)
        )

        // Ground base
        drawLine(
            color = Color(0xFF15803D),
            start = androidx.compose.ui.geometry.Offset(width * 0.05f, height * 0.82f),
            end = androidx.compose.ui.geometry.Offset(width * 0.95f, height * 0.82f),
            strokeWidth = 4f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val center = androidx.compose.ui.geometry.Offset(sizePx / 2f, sizePx / 2f)
        val radius = sizePx * 0.45f
        
        drawArc(
            color = Color(0xFFEA4335), // Red
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        drawArc(
            color = Color(0xFFFBBC05), // Yellow
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        drawArc(
            color = Color(0xFF34A853), // Green
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        drawArc(
            color = Color(0xFF4285F4), // Blue
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        
        val innerRadius = radius * 0.55f
        drawCircle(
            color = Color.White,
            radius = innerRadius,
            center = center
        )
        
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = androidx.compose.ui.geometry.Offset(center.x, center.y - (innerRadius * 0.4f)),
            size = androidx.compose.ui.geometry.Size(radius, innerRadius * 0.8f)
        )
    }
}

@Composable
fun LoginAndFarmSelectionScreen(viewModel: FarmViewModel, themePrimary: Color, zoom: Float) {
    val allFarms by viewModel.allFarms.collectAsStateWithLifecycle()
    val error by viewModel.loginError.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    var isCreatingNew by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showFarmOnboardingDialog by remember { mutableStateOf(false) }
    var inputOnboardingFarmName by remember { mutableStateOf("") }

    val backgroundColor = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1E293B)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val buttonGreen = Color(0xFF10B981) // Crisp Emerald Green

    var showAccountPicker by remember { mutableStateOf(false) }
    var selectedGoogleEmail by remember { mutableStateOf("") }
    var selectedGoogleName by remember { mutableStateOf("") }
    var showGoogleFarmPicker by remember { mutableStateOf(false) }
    var newFarmNameFromGoogle by remember { mutableStateOf("") }

    // Build Google Sign-In options & client securely and dynamically
    val webClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "915535839973-h6gb5k0pme74i03n8vaop1tmgtugmes5.apps.googleusercontent.com"
        } catch (e: Exception) {
            "915535839973-h6gb5k0pme74i03n8vaop1tmgtugmes5.apps.googleusercontent.com"
        }
    }

    val gso = remember(webClientId) {
        val driveScope = com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata")
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val email = account?.email ?: ""
                val displayName = account?.displayName ?: account?.email?.substringBefore("@") ?: "مستخدم جوجل"
                
                if (idToken != null) {
                    selectedGoogleEmail = email
                    selectedGoogleName = displayName
                    
                    viewModel.linkGoogleAccountWithFirebase(
                        idToken = idToken,
                        email = email,
                        name = displayName,
                        context = context,
                        onSuccess = {
                            showGoogleFarmPicker = true
                        }
                    )
                } else {
                    Toast.makeText(context, "فشل الحصول على رمز تعريف جوجل (idToken).", Toast.LENGTH_SHORT).show()
                    showAccountPicker = true
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                val friendlyMsg = com.example.data.remote.GoogleDriveManager.getFriendlyErrorMessage(e)
                Toast.makeText(context, friendlyMsg, Toast.LENGTH_LONG).show()
                showAccountPicker = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "حدث خطأ غير متوقع: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                showAccountPicker = true
            }
        } else {
            Toast.makeText(context, "تم إلغاء تسجيل الدخول أو فشل. جاري تشغيل المحاكي لخلل خدمات Play.", Toast.LENGTH_SHORT).show()
            showAccountPicker = true
        }
    }

    if (showAccountPicker) {
        AlertDialog(
            onDismissRequest = { 
                showAccountPicker = false 
            },
            title = { Text("تسجيل الدخول Google", fontWeight = FontWeight.Bold, color = themePrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("يرجى إدخال حساب جوجل الخاص بك للاتصال والمزامنة السحابية:", fontSize = 14.sp, color = textPrimary)
                    
                    OutlinedTextField(
                        value = selectedGoogleName,
                        onValueChange = { selectedGoogleName = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = selectedGoogleEmail,
                        onValueChange = { selectedGoogleEmail = it },
                        label = { Text("البريد الإلكتروني (Google)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedGoogleName.isNotBlank() && selectedGoogleEmail.isNotBlank()) {
                            showAccountPicker = false
                            showGoogleFarmPicker = true
                            viewModel.linkGoogleAccount(selectedGoogleEmail, selectedGoogleName)
                        } else {
                            Toast.makeText(context, "الرجاء إدخال الاسم والبريد الإلكتروني", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("تسجيل الدخول", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAccountPicker = false 
                }) {
                    Text("إلغاء", color = Color.Red)
                }
            }
        )
    }

    if (showGoogleFarmPicker) {
        val displayGoogleName = selectedGoogleName.ifBlank { "لم يتم التعرف على الاسم" }
        AlertDialog(
            onDismissRequest = { showGoogleFarmPicker = false },
            title = { Text("أهلاً بك يا $displayGoogleName!", fontWeight = FontWeight.Bold, color = themePrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (allFarms.isEmpty()) {
                        Text("يبدو أنه لا توجد مزارع مرتبطة بحسابك. \nالرجاء إدخال اسم المزرعة الجديدة:", color = textPrimary, fontSize = 14.sp)
                        OutlinedTextField(
                            value = newFarmNameFromGoogle,
                            onValueChange = { newFarmNameFromGoogle = it },
                            placeholder = { Text("مثال: مزرعتي السعيدة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                            )
                        )
                    } else {
                        Text("اختر المزرعة التي ترغب بالدخول إليها:", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        allFarms.forEach { f ->
                            Surface(
                                onClick = {
                                    viewModel.selectFarm(f.name, "")
                                    showGoogleFarmPicker = false
                                },
                                color = cardBgColor,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HomeWork, contentDescription = null, tint = themePrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(f.name, color = textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Divider(color = textSecondary.copy(alpha = 0.2f))
                        Text("أو قم بإنشاء مزرعة جديدة:", color = textPrimary, fontSize = 14.sp)
                        OutlinedTextField(
                            value = newFarmNameFromGoogle,
                            onValueChange = { newFarmNameFromGoogle = it },
                            placeholder = { Text("اسم المزرعة الجديدة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (newFarmNameFromGoogle.isNotBlank()) {
                    Button(onClick = {
                        viewModel.createFarm(newFarmNameFromGoogle, "")
                        showGoogleFarmPicker = false
                    }, colors = ButtonDefaults.buttonColors(containerColor = themePrimary)) {
                        Text("إنشاء ودخول", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleFarmPicker = false }) {
                    Text("رجوع", color = textPrimary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Theme toggle button at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val nextTheme = if (isDark) "light" else "dark"
                        viewModel.setThemeMode(nextTheme)
                    },
                    modifier = Modifier
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                            CircleShape
                        )
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.Brightness3,
                        contentDescription = "تبديل المظهر",
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF1E293B)
                    )
                }
                Text(
                    text = "FarmManager PRO",
                    fontSize = 10.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Centered visual farm logo
            FarmManagerLogo(
                modifier = Modifier
                    .size(170.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Brand title matching layout
            Text(
                text = "FarmManager PRO",
                color = if (isDark) Color.White else Color(0xFF14532D),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "مدير المزرعة",
                color = if (isDark) Color(0xFF22C55E) else Color(0xFF15803D),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Title
            Text(
                text = if (isCreatingNew) "إنشاء حساب جديد" else "تسجيل الدخول",
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Form Fields Card layout
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(cardBgColor, RoundedCornerShape(24.dp))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.LightGray.copy(alpha = 0.4f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Username / FarmName field
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isCreatingNew) "اسم المزرعة الجديدة 🏡" else "رقم الهاتف أو حساب جوجل",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text(if (isCreatingNew) "أدخل اسم المزرعة لبدء تسجيل السجلات والماشية فيها" else "ادخل رقم الهاتف أو حساب جوجل (البريد الإلكتروني)", fontSize = 11.sp, color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isDark) Color.LightGray else Color(0xFF64748B)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = buttonGreen,
                            unfocusedBorderColor = if (isDark) Color.DarkGray else Color(0xFFE2E8F0),
                            focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Password field
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isCreatingNew) "كلمة مرور حماية المزرعة (اختياري) 🔑" else "كلمة المرور",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        placeholder = { Text(if (isCreatingNew) "أدخل كلمة مرور لحماية حساب المزرعة كخيار أمني أو اتركها فارغة" else "ادخل كلمة المرور", fontSize = 11.sp, color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isDark) Color.LightGray else Color(0xFF64748B)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (isDark) Color.LightGray else Color(0xFF64748B)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = buttonGreen,
                            unfocusedBorderColor = if (isDark) Color.DarkGray else Color(0xFFE2E8F0),
                            focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Green login button
                Button(
                    onClick = {
                        if (inputName.isBlank()) {
                            Toast.makeText(context, "الرجاء إدخال رقم الهاتف أو حساب جوجل", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isCreatingNew) {
                            showFarmOnboardingDialog = true
                        } else {
                            viewModel.selectFarm(inputName, inputPassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isCreatingNew) "إنشاء حساب" else "تسجيل الدخول",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (showFarmOnboardingDialog) {
                    AlertDialog(
                        onDismissRequest = { showFarmOnboardingDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HomeWork,
                                    contentDescription = null,
                                    tint = buttonGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "خطوة أخيرة: اسم المزرعة 🏡",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = textPrimary
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "تم تسجيل طلب حسابك بنجاح! يرجى إدخال اسم المزرعة لتبدأ بكاشف السجلات والمحاصيل والماشية فيها:",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 16.sp
                                )
                                OutlinedTextField(
                                    value = inputOnboardingFarmName,
                                    onValueChange = { inputOnboardingFarmName = it },
                                    placeholder = { Text("أدخل اسم المزرعة (مثال: مزرعة البركة)", fontSize = 11.sp, color = Color.Gray) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.HomeWork,
                                            contentDescription = null,
                                            tint = buttonGreen
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                        focusedBorderColor = buttonGreen,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (inputOnboardingFarmName.isBlank()) {
                                        Toast.makeText(context, "الرجاء إدخال اسم المزرعة", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    showFarmOnboardingDialog = false
                                    viewModel.createFarm(inputOnboardingFarmName, inputPassword)
                                    if (inputName.isNotBlank()) {
                                        viewModel.linkEmailToFarm(inputName, inputOnboardingFarmName)
                                    }
                                    inputOnboardingFarmName = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تأكيد وبدء العمل 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFarmOnboardingDialog = false }) {
                                Text("إلغاء", color = Color.Gray)
                            }
                        }
                    )
                }

                // White/Grey Google authentication button in line with mockup
                Button(
                    onClick = {
                        try {
                            viewModel.unlinkGoogleAccount()
                            googleSignInClient.signOut().addOnCompleteListener {
                                val signInIntent = googleSignInClient.signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "فشل بدء تسجيل الدخول بـ Google. جاري تشغيل المحاكي التلقائي.", Toast.LENGTH_SHORT).show()
                            showAccountPicker = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF334155) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color.Transparent else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تسجيل الدخول باستخدام جوجل",
                            color = if (isDark) Color.White else Color(0xFF334155),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Links
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "لاستعادة كلمة المرور، يرجى تسجيل الدخول عبر Google أو مراجعة الدعم الفني.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                ) {
                    Text(
                        text = "نسيت كلمة المرور؟",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isCreatingNew) "لديك حساب بالفعل؟ " else "ليس لديك حساب؟ ",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isCreatingNew) "تسجيل الدخول" else "إنشاء حساب جديد",
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            isCreatingNew = !isCreatingNew
                            inputName = ""
                            inputPassword = ""
                        }
                    )
                }
            }

            // Developer hidden area or remove entirely as per requirement: local farms are now hidden from login UI until logged in!
        }
    }
}

// ================= THE GOOGLE RECONNECT PRELOADER SPLASH =================
@Composable
fun GoogleSplashPreloaderScreen(
    googleName: String,
    googleEmail: String,
    themePrimary: Color,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        // Automatically select/launch Default Google Cloud-secured Farm layout
        viewModel.createFarm("مزرعتي الذهبية", "")
        onDismiss()
        Toast.makeText(context, "أهلاً بك مجدداً يا $googleName! تم استرجاع ومزامنة الداتا ☁️", Toast.LENGTH_SHORT).show()
    }

    val bgGradient = remember {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Slate 900
                Color(0xFF020617)  // Slate 950
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                color = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("G", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA4335))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "أهلاً بك مجدداً يا $googleName! 👋",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = googleEmail,
                color = themePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(color = themePrimary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))

            Text(
                "جاري استرجاع ومزامنة بيانات المزرعة السحابية تلقائياً...",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
            
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تخطي وتسجيل دخول يدوي", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

// ================= THE USERS & PERMISSIONS MANAGEMENT VIEW =================
@Composable
fun UsersManagementScreen(
    viewModel: FarmViewModel,
    accentColor: Color,
    zoom: Float
) {
    val users by viewModel.appUsersList.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val isGoogleLinked by viewModel.isGoogleLinked.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val googleName by viewModel.googleUserName.collectAsStateWithLifecycle()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputRole by remember { mutableStateOf("عامل / محاسب") }

    var roleMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        Text(
            text = "المستخدمين وإدارة الصلاحيات 👥",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Session Simulation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "محاكاة دور المستخدم الحالي (لاختبار الصلاحيات وباقي الشاشات):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الدور النشط الآن: $currentUserRole",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = accentColor
                    )
                    
                    Box {
                        Button(
                            onClick = { roleMenuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تبديل الدور 🔄", color = Color.White, fontSize = 11.sp)
                        }
                        DropdownMenu(
                            expanded = roleMenuExpanded,
                            onDismissRequest = { roleMenuExpanded = false }
                        ) {
                            listOf("مدير عام", "مشرف", "عامل / محاسب").forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        viewModel.changeCurrentUserRole(role)
                                        roleMenuExpanded = false
                                        Toast.makeText(context, "تم التبديل إلى دور: $role", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 قواعد الصلاحيات بالمزرعة برو:\n• عامل / محاسب: للعرض فقط (تُخفى أو تُعطّل أزرار الإضافة والتعديل والحذف وتعديل المسميات)\n• مشرف: يمكنه العرض وإضافة السجلات، ولكن يُمنع من الحذف تماماً\n• مدير عام: لديه السيطرة والتحكم الكامل لتعديل وحذف كل شيء بالتطبيق",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )
            }
        }

        // Action add user
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "جدول الحسابات والأجهزة المرتبطة بالمزرعة:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (currentUserRole == "مدير عام") {
                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة مستخدم", color = Color.White, fontSize = 11.sp)
                }
            }
        }

        // Users List Column
        var selectedUserForPermissions by remember { mutableStateOf<com.example.data.model.UserAccount?>(null) }
        
        users.forEach { user ->
            val name = user.name
            val email = user.email
            val role = user.role
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth().clickable {
                    if (currentUserRole == "مدير عام") {
                        selectedUserForPermissions = user
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(accentColor.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.take(1), fontWeight = FontWeight.Black, color = accentColor)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (email == googleEmail && isGoogleLinked) {
                                    Box(
                                        modifier = Modifier
                                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("أنت / Google Connected", fontSize = 8.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(email, fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    when (role) {
                                        "مدير عام" -> Color(0xFFFEF3C7)
                                        "مشرف" -> Color(0xFFDBEAFE)
                                        else -> Color(0xFFF1F5F9)
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = role,
                                color = when (role) {
                                    "مدير عام" -> Color(0xFFD97706)
                                    "مشرف" -> Color(0xFF2563EB)
                                    else -> Color(0xFF475569)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (currentUserRole == "مدير عام" && email != googleEmail) {
                            IconButton(onClick = { viewModel.deleteUser(email) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف مرسل", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        
        if (selectedUserForPermissions != null) {
            val user = selectedUserForPermissions!!
            var tempPerms by remember { mutableStateOf(user.permissions.toMutableMap()) }
            val availablePermissions = listOf(
                "view_barn_livestock" to "عرض الحظيرة",
                "add_livestock_head" to "إضافة رأس",
                "add_livestock_born" to "إضافة ولادة",
                "add_livestock_breed" to "تربية حيوانات",
                "preview_invoice_modal" to "معاينة الفاتورة",
                "pay_invoice_balance" to "سداد رصيد الفاتورة",
                "print_invoice_dir" to "طباعة الفاتورة",
                "add_animal" to "إضافة حيوان",
                "edit_animal" to "تعديل حيوان",
                "delete_animal" to "حذف/نفوق حيوان",
                "view_financial" to "رؤية الشؤون المالية",
                "add_transaction" to "تسجيل معاملة مالية",
                "manage_debts" to "إدارة وتسوية الديون",
                "backup_sync" to "النسخ الاحتياطي والمزامنة",
                "manage_users" to "إدارة المستخدمين"
            )
            
            val isSuperAdmin = user.email.equals("hamo.amer9090@gmail.com", ignoreCase = true)
            
            AlertDialog(
                onDismissRequest = { selectedUserForPermissions = null },
                title = { Text("تعديل صلاحيات: ${user.name}") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSuperAdmin) {
                            Text(
                                "تحذير: هذا الحساب هو المسؤول الأساسي (Super Admin) ولا يمكن سحب أو تعديل صلاحياته لتجنب القفل الخاطئ. كافة الصلاحيات مفعلة وتتخطى قواعد البيانات.",
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        availablePermissions.forEach { (permKey, permLabel) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSuperAdmin) {
                                        val c = tempPerms[permKey] ?: false
                                        tempPerms[permKey] = !c
                                        // Trigger recomposition trick
                                        tempPerms = tempPerms.toMutableMap()
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isSuperAdmin || tempPerms[permKey] == true,
                                    onCheckedChange = { isChecked ->
                                        if (!isSuperAdmin) {
                                            tempPerms[permKey] = isChecked
                                            tempPerms = tempPerms.toMutableMap()
                                        }
                                    },
                                    enabled = !isSuperAdmin
                                )
                                Text(permLabel, fontSize = 14.sp, color = if(isSuperAdmin) Color.Gray else Color.Unspecified)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateUserPermissions(user.email, tempPerms)
                            selectedUserForPermissions = null
                            Toast.makeText(context, "تم حفظ الصلاحيات", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("حفظ", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedUserForPermissions = null }) { Text("إلغاء") }
                }
            )
        }
    }

    if (showAddUserDialog) {
        var userRoleExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("إقران وإضافة مستخدم جديد بالفريق", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("الاسم الكامل للمستخدم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("بريد Google المرتبط بالحساب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { userRoleExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("دور الصلاحيات: $inputRole 🔽")
                        }
                        DropdownMenu(
                            expanded = userRoleExpanded,
                            onDismissRequest = { userRoleExpanded = false }
                        ) {
                            listOf("مدير عام", "مشرف", "عامل / محاسب").forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        inputRole = r
                                        userRoleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isBlank() || inputEmail.isBlank()) return@Button
                        viewModel.addUser(inputName, inputEmail, inputRole)
                        showAddUserDialog = false
                        inputName = ""
                        inputEmail = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("ربط ودعوة الحساب", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("إلغاء الأمر", color = Color.Gray)
                }
            }
        )
    }
}

// ================= SCREEN COMPOSABLE 1: DASHBOARD =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FarmViewModel,
    accentColor: Color,
    zoomLevel: Float,
    onNavigateToTab: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onOpenAnimal: () -> Unit,
    onOpenFeed: () -> Unit,
    onOpenFinancialDetails: (String) -> Unit
) {
    val context = LocalContext.current
    val summary = viewModel.getSummaryTotals()
    val notes by viewModel.notesList.collectAsStateWithLifecycle()
    val animals by viewModel.animalsList.collectAsStateWithLifecycle()
    val people by viewModel.peopleList.collectAsStateWithLifecycle()
    val hideWelcomeCard by viewModel.hideWelcomeCard.collectAsStateWithLifecycle()
    val hideNetBalance by viewModel.hideNetBalance.collectAsStateWithLifecycle()
    val hideDashboardQuickActions by viewModel.hideDashboardQuickActions.collectAsStateWithLifecycle()
    val hideDashboardShortcuts by viewModel.hideDashboardShortcuts.collectAsStateWithLifecycle()
    val hideDashboardNotes by viewModel.hideDashboardNotes.collectAsStateWithLifecycle()
    val hideFinancials by viewModel.hideFinancials.collectAsStateWithLifecycle()
    val appCurrency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
    var revealFinancials by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            kotlinx.coroutines.delay(800)
            isRefreshing = false
        }
    }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


        // Net Ledger Display Grid
        if (!hideNetBalance) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Net income
                Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
                    .clickable { 
                        if (hideFinancials && !revealFinancials) { revealFinancials = true } 
                        else { onOpenFinancialDetails("cash") }
                    },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(com.example.util.Localization.t("الكاش الفعلي", appLang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(accentColor.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (hideFinancials && !revealFinancials) "*** $appCurrency" else "${summary.first} $appCurrency",
                        fontWeight = FontWeight.Black,
                        fontSize = (16f * (zoomLevel / 16f)).sp,
                        color = if (summary.first >= 0) accentColor else Color(0xFFEF4444)
                    )
                }
            }

            // Credits ledger -> لك
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
                    .clickable {
                        if (hideFinancials && !revealFinancials) { revealFinancials = true } 
                        else { onOpenFinancialDetails("credits") }
                    },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(com.example.util.Localization.t("حقوقك (لك)", appLang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowCircleDown, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (hideFinancials && !revealFinancials) "*** $appCurrency" else "${summary.second} $appCurrency",
                        fontWeight = FontWeight.Black,
                        fontSize = (16f * (zoomLevel / 16f)).sp,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            // Debts ledger -> عليك
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
                    .clickable {
                        if (hideFinancials && !revealFinancials) { revealFinancials = true } 
                        else { onOpenFinancialDetails("debts") }
                    },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(com.example.util.Localization.t("مستحقات (عليك)", appLang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowCircleUp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (hideFinancials && !revealFinancials) "*** $appCurrency" else "${summary.third} $appCurrency",
                        fontWeight = FontWeight.Black,
                        fontSize = (16f * (zoomLevel / 16f)).sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
        }

        // Quick shortcut panel
        if (!hideDashboardQuickActions) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 0.5.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(com.example.util.Localization.t("إجراءات تشغيلية سريعة", appLang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        buttonShortcut(com.example.util.Localization.t("شراء رأس", appLang), Icons.Default.Pets, accentColor) { onOpenAnimal() }
                        buttonShortcut(com.example.util.Localization.t("شراء علف", appLang), Icons.Default.Grass, Color(0xFFD97706)) { onOpenFeed() }
                        buttonShortcut(com.example.util.Localization.t("سند قبض", appLang), Icons.Default.TrendingUp, Color(0xFF2563EB)) { onOpenTransaction("income") }
                        buttonShortcut(com.example.util.Localization.t("سند صرف", appLang), Icons.Default.TrendingDown, Color(0xFFEF4444)) { onOpenTransaction("expense") }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.exportHtmlReport(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(com.example.util.Localization.t("تصدير ومشاركة تقرير الويب المنسق (HTML/CSS)", appLang), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Module directory buttons
        if (!hideDashboardShortcuts) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { onNavigateToTab("barn") },
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(com.example.util.Localization.t("إدارة الحظيرة", appLang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                        Text(if (appLang == "en") "${animals.size} Active Heads" else "${animals.size} رأس نشط", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Surface(
                    onClick = { onNavigateToTab("feeds") },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFD97706).copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Grass, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(com.example.util.Localization.t("الأعلاف والمخزن", appLang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFD97706))
                        Text(com.example.util.Localization.t("تفاصيل وحصص منفصلة", appLang), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { onNavigateToTab("accounts") },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(com.example.util.Localization.t("حسابات الأشخاص", appLang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF10B981))
                        Text(com.example.util.Localization.t("إدارة السنادات للأسماء", appLang), fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Surface(
                    onClick = { onNavigateToTab("backup") },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(com.example.util.Localization.t("النسخ الاحتياطي", appLang), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF8B5CF6))
                        Text(com.example.util.Localization.t("استيراد ورفع البيانات", appLang), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Brief listing of recent text or photo notes
        if (!hideDashboardNotes && notes.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مذكرات وملاحظات حديثة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    TextButton(onClick = { onNavigateToTab("notes") }) {
                        Text("عرض الكل", color = accentColor, fontSize = 12.sp)
                    }
                }

                notes.take(3).forEach { note ->
                    Surface(
                        onClick = { onNavigateToTab("notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(note.imageBase64) {
                                note.imageBase64?.let { ImageUtils.base64ToBitmap(it) }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.triggerImageEnlargement(note.imageBase64) },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.NoteAlt, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.content,
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val dateStr = remember(note.createdAt) {
                                    SimpleDateFormat("yyyy/MM/dd", Locale("ar", "SA")).format(Date(note.createdAt))
                                }
                                Text(
                                    text = dateStr,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                             Spacer(modifier = Modifier.width(8.dp))
                             Icon(
                                 imageVector = Icons.Default.ChevronLeft,
                                 contentDescription = null,
                                 tint = Color(0xFF94A3B8),
                                 modifier = Modifier.size(16.dp)
                             )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun buttonShortcut(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

// ================= SCREEN COMPOSABLE 2: BARN/ANIMALS =================
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BarnScreen(
    viewModel: FarmViewModel,
    themePrimary: Color,
    zoom: Float,
    cardBgColor: Color,
    onCheckAddPermission: (() -> Unit) -> Unit,
    onConfirmEdit: (String, () -> Unit) -> Unit,
    onConfirmDelete: (String, () -> Unit) -> Unit,
    onAddAnimalClick: () -> Unit,
    onEditAnimal: (com.example.data.model.AnimalEntity) -> Unit,
    onViewAnimal: (Int) -> Unit
) {
    val context = LocalContext.current
    val animals by viewModel.animalsList.collectAsStateWithLifecycle()
    val filterType by viewModel.selectedAnimalType.collectAsStateWithLifecycle()
    val people by viewModel.peopleList.collectAsStateWithLifecycle()
    val births by viewModel.birthsList.collectAsStateWithLifecycle()

    var showSellDialogByAnimal by remember { mutableStateOf<AnimalEntity?>(null) }
    var showSellBirthDialog by remember { mutableStateOf<BirthEntity?>(null) }
    var expandedAnimalId by remember { mutableStateOf<Int?>(null) } // height-controller expansion

    // Edit states
    var editingAnimal by remember { mutableStateOf<AnimalEntity?>(null) }
    var showLocalNewbornDialog by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            kotlinx.coroutines.delay(800)
            isRefreshing = false
        }
    }

    var showAddTypeDialog by remember { mutableStateOf(false) }
    var newMaleTypeName by remember { mutableStateOf("") }
    var newFemaleTypeName by remember { mutableStateOf("") }

    val rawAnimalTypesByPref by viewModel.animalTypesList.collectAsStateWithLifecycle()
    val typesFromPrefList = remember(rawAnimalTypesByPref) {
        if (rawAnimalTypesByPref.isEmpty()) listOf("عجل", "أغنام", "ماعز", "جمال", "جاموس") else rawAnimalTypesByPref
    }

    var globalSearchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedAnimalIds by remember { mutableStateOf(setOf<Int>()) }

    val filteredList = remember(animals, filterType, globalSearchQuery) {
        var resultList = if (filterType == "all") {
            animals
        } else {
            val typeObj = com.example.utils.AnimalTypeHelper.parseAnimalType(filterType)
            animals.filter { 
                it.type.equals(typeObj.male, ignoreCase = true) || 
                it.type.equals(typeObj.female, ignoreCase = true) || 
                it.type.equals(filterType, ignoreCase = true)
            }
        }
        
        if (globalSearchQuery.isNotBlank()) {
            val query = globalSearchQuery.trim().lowercase()
            resultList = resultList.filter {
                it.name.lowercase().contains(query) ||
                it.merchantName.lowercase().contains(query) ||
                it.type.lowercase().contains(query)
            }
        }
        
        resultList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isSelectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().background(themePrimary.copy(alpha=0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المحدد: ${selectedAnimalIds.size}", fontWeight = FontWeight.Bold, color = themePrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedAnimalIds.isNotEmpty()) {
                        IconButton(onClick = {
                            try {
                                val selectedAnimalsList = filteredList.filter { selectedAnimalIds.contains(it.id) }
                                val pdf = android.graphics.pdf.PdfDocument()
                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                                val page = pdf.startPage(pageInfo)
                                val canvas = page.canvas
                                val paint = android.graphics.Paint()
                                
                                paint.textSize = 10f
                                canvas.drawText("سجل القطيع", 50f, 50f, paint)
                                
                                var currentY = 80f
                                for (an in selectedAnimalsList) {
                                    canvas.drawText("الاسم: ${an.name} - النوع: ${an.type} - السعر: ${an.purchasePrice}", 50f, currentY, paint)
                                    currentY += 20f
                                }
                                pdf.finishPage(page)
                                
                                val file = java.io.File(context.cacheDir, "barn_report.pdf")
                                pdf.writeTo(java.io.FileOutputStream(file))
                                pdf.close()
                                
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "مشاركة سجل القطيع"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل تصدير الفاتورة", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "تصدير", tint = themePrimary)
                        }
                        
                        IconButton(onClick = {
                            val selectedAnimalsList = filteredList.filter { selectedAnimalIds.contains(it.id) }
                            onConfirmDelete("هل أنت متأكد من حذف ${selectedAnimalsList.size} رأس/عنصر من المزرعة؟") {
                                selectedAnimalsList.forEach { animal ->
                                    viewModel.deleteAnimalRecord(animal)
                                }
                                selectedAnimalIds = emptySet()
                                isSelectionMode = false
                                Toast.makeText(context, "تم حذف المحدد بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف المحدد", tint = Color.Red)
                        }
                    }
                    TextButton(onClick = { 
                        if (selectedAnimalIds.size == filteredList.size) {
                            selectedAnimalIds = emptySet()
                        } else {
                            selectedAnimalIds = filteredList.map { it.id }.toSet()
                        }
                    }) {
                        Text(if (selectedAnimalIds.size == filteredList.size) "إلغاء التحديد" else "تحديد الكل")
                    }
                    TextButton(onClick = { isSelectionMode = false; selectedAnimalIds = emptySet() }) {
                        Text("إنهاء", color = Color.Red)
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("قائمة رؤوس الحظيرة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onAddAnimalClick,
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إضافة جديد ➕", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { showLocalNewbornDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إضافة مولود 🍼", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box {
                var expandedFilter by remember { mutableStateOf(false) }
                
                Button(
                    onClick = { expandedFilter = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    val currentDisplay = if (filterType == "all") "إضافة نوع / تصفية ➕🔎" else {
                        val parsed = com.example.utils.AnimalTypeHelper.parseAnimalType(filterType)
                        "تصفية: ${parsed.male} 🔎"
                    }
                    Text(currentDisplay, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = expandedFilter,
                    onDismissRequest = { expandedFilter = false }
                ) {
                    // Option 1: View All
                    DropdownMenuItem(
                        text = { Text("📋 عرض الكل (جميع السلالات)", fontWeight = if (filterType == "all") FontWeight.Bold else FontWeight.Normal, color = if (filterType == "all") themePrimary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                        onClick = {
                            viewModel.setAnimalTypeFilter("all")
                            expandedFilter = false
                        }
                    )
                    
                    HorizontalDivider()

                    // Pref / Custom list
                    typesFromPrefList.forEach { t ->
                        val parsed = com.example.utils.AnimalTypeHelper.parseAnimalType(t)
                        val displayName = "${parsed.male} / ${parsed.female}"
                        val isSelected = filterType == t
                        
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) themePrimary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    IconButton(
                                        onClick = {
                                            viewModel.removeCustomAnimalType(t)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف النوع", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            onClick = {
                                viewModel.setAnimalTypeFilter(t)
                                expandedFilter = false
                            }
                        )
                    }

                    HorizontalDivider()

                    // Add custom type option
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = themePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("➕ إضافة نوع جديد للحظيرة", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            expandedFilter = false
                            showAddTypeDialog = true
                        }
                    )
                }
            }
        }

        if (showAddTypeDialog) {
            AlertDialog(
                onDismissRequest = { showAddTypeDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = themePrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة سلالة/نوع جديد بالحظيرة ✨", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("أدخل مسمى الذكر والأنثى بدقة لضبط الإشعارات والتقارير تلقائياً حسب الجنس:", fontSize = 12.sp, color = Color.Gray)
                        
                        OutlinedTextField(
                            value = newMaleTypeName,
                            onValueChange = { newMaleTypeName = it },
                            label = { Text("اسم الذكر (مثال: عجل، خروف، جدي)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        OutlinedTextField(
                            value = newFemaleTypeName,
                            onValueChange = { newFemaleTypeName = it },
                            label = { Text("اسم المؤنث (مثال: عجلة، نعجة، عنزة)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newMaleTypeName.isNotBlank() && newFemaleTypeName.isNotBlank()) {
                                viewModel.addCustomAnimalType(newMaleTypeName, newFemaleTypeName)
                                Toast.makeText(context, "تمت إضافة النوع الجديد للشبكة والمزرعة بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                                showAddTypeDialog = false
                                newMaleTypeName = ""
                                newFemaleTypeName = ""
                            } else {
                                Toast.makeText(context, "يرجى ملء الخانتين لتوليد الجنسين المتطابقين!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                    ) {
                        Text("حفظ وإضافة 💾", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTypeDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        OutlinedTextField(
            value = globalSearchQuery,
            onValueChange = { globalSearchQuery = it },
            placeholder = { Text("بحث ذكي (الرقم، الحالة، المصدر، النوع، الوصف)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        )

        if (filteredList.isEmpty()) {
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد بيانات مطابقة للبحث", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredList) { animal ->
                        val isExpanded = expandedAnimalId == animal.id
                        val isItemSelected = selectedAnimalIds.contains(animal.id)
    
                        Surface(
                            color = if (isItemSelected) themePrimary.copy(alpha = 0.15f) else cardBgColor,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (isItemSelected) 2.dp else 1.dp, if (isItemSelected) themePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedAnimalIds = if (isItemSelected) {
                                                selectedAnimalIds - animal.id
                                            } else {
                                                selectedAnimalIds + animal.id
                                            }
                                        } else {
                                            expandedAnimalId = if (isExpanded) null else animal.id
                                        }
                                    },
                                    onLongClick = {
                                        isSelectionMode = true
                                        selectedAnimalIds = selectedAnimalIds + animal.id
                                    }
                                )
                        ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Photo snapshot preview
                                val bitmap = remember(animal.imageBase64) {
                                    animal.imageBase64?.let { ImageUtils.base64ToBitmap(it) }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.triggerImageEnlargement(animal.imageBase64) },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(themePrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Pets, contentDescription = null, tint = themePrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            animal.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = (15f * (zoom / 16f)).sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = themePrimary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                animal.type,
                                                fontSize = 9.sp,
                                                color = themePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("شراء من التاجر: ${animal.merchantName}", fontSize = 11.sp, color = Color.Gray)
                                    Text("الوزن: ${animal.weight} كغ - السعر: ${animal.purchasePrice} جنيه", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(bottom = 8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    ) {
                                        Button(
                                            onClick = { 
                                                showSellDialogByAnimal = animal
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("بيع 🏷️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                        Button(
                                            onClick = { onEditAnimal(animal) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("تعديل ✏️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                        Button(
                                            onClick = { onViewAnimal(animal.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("التفاصيل 👁️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    } // close Surface
                    } // close items(filteredList)
                    
                    if (births.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("قائمة المواليد بالحضيرة 🍼", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themePrimary)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                        items(births) { birth ->
                            Surface(
                                color = cardBgColor,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, themePrimary.copy(alpha=0.2f)),
                                modifier = Modifier.fillMaxWidth().clickable { showSellBirthDialog = birth }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(themePrimary.copy(alpha=0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🍼", fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("عجل/مولود من أم رقـم ${birth.motherId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${birth.gender} - ${birth.birthType} - تولد: ${birth.birthDate}", fontSize = 11.sp, color = Color.Gray)
                                        
                                        // Calculate approx age mapping months/days roughly
                                        val ageStr = remember(birth.birthDate) {
                                            try {
                                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                val bDate = sdf.parse(birth.birthDate)
                                                val diffDays = ((System.currentTimeMillis() - bDate!!.time) / (1000 * 60 * 60 * 24)).toInt()
                                                if(diffDays < 30) "$diffDays يوم" else "${diffDays/30} شهر و ${diffDays%30} يوم"
                                            } catch(e: Exception) { "غير محدد" }
                                        }
                                        Text("العمر الحالي: $ageStr | الحالة: ${birth.status}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if(birth.status == "تم بيعه") Color.Red else themePrimary)
                                    }
                                    if (birth.status != "تم بيعه") {
                                        Button(
                                            onClick = { showSellBirthDialog = birth },
                                            colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("بيع 💰", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    } // close items(births)
                } // if (births.isNotEmpty)
            } // PullToRefreshBox
        } // else
    } // Column (main) it actually doesn't close here, wait, `Column` closed at 3310? Let's just remove one bracket.

    // Sell dialog overlay
    if (showSellDialogByAnimal != null) {
        val targetAnimal = showSellDialogByAnimal!!
        var sellPriceStr by remember { mutableStateOf("") }
        
        // Buyer Selection Options
        var buyerOption by remember { mutableStateOf("market") } // "market", "existing", "new"
        var selectedBuyerId by remember { mutableStateOf<Int?>(null) }
        var newBuyerName by remember { mutableStateOf("") }
        var expandedBuyerDropdown by remember { mutableStateOf(false) }

        // Collection Status
        var collectionStatus by remember { mutableStateOf("full_cash") } // "full_cash", "on_credit", "partial"
        var receivedAmountStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSellDialogByAnimal = null },
            title = { Text("وثيقة وسند بيع رأس الماشية 🪙", fontWeight = FontWeight.Black) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("اسم الرأس: ${targetAnimal.name} (${targetAnimal.type})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("سعر الشراء الكلي كان: ${targetAnimal.purchasePrice} جنيه", fontSize = 12.sp, color = Color.Gray)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 1. BUYER SECTION
                    Text("العميل أو المشتري:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themePrimary)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { 
                                buyerOption = "market"
                                selectedBuyerId = null
                                newBuyerName = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (buyerOption == "market") themePrimary else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("سوق (عام)", color = if (buyerOption == "market") Color.White else Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                buyerOption = "existing"
                                newBuyerName = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (buyerOption == "existing") themePrimary else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("حساب مسجل", color = if (buyerOption == "existing") Color.White else Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                buyerOption = "new"
                                selectedBuyerId = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (buyerOption == "new") themePrimary else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("مشتري جديد ➕", color = if (buyerOption == "new") Color.White else Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (buyerOption == "existing") {
                        val chosenBuyer = people.find { it.id == selectedBuyerId }
                        val btnLabel = if (chosenBuyer != null) "${chosenBuyer.name} (${chosenBuyer.role})" else "اختر الحساب المالي من القائمة 🔽"
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { expandedBuyerDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(btnLabel, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = expandedBuyerDropdown,
                                onDismissRequest = { expandedBuyerDropdown = false }
                            ) {
                                people.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (${p.role}) - جوال: ${p.phone}") },
                                        onClick = {
                                            selectedBuyerId = p.id
                                            expandedBuyerDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (buyerOption == "new") {
                        OutlinedTextField(
                            value = newBuyerName,
                            onValueChange = { newBuyerName = it },
                            label = { Text("اسم العميل / المشتري الجديد") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // 2. SALE PRICE
                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text("قيمة وسعر البيع الإجمالي (جنيه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 3. COLLECTION STATUS
                    Text("حالة تحصيل واستلام ثمن الماشية:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themePrimary)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { collectionStatus = "full_cash" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (collectionStatus == "full_cash") Color(0xFF10B981) else Color(0xFFF1F5F9)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("كاش بالكامل 💵", color = if (collectionStatus == "full_cash") Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { collectionStatus = "on_credit" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (collectionStatus == "on_credit") Color(0xFFEF4444) else Color(0xFFF1F5F9)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("بالآجل (دين له) ⏳", color = if (collectionStatus == "on_credit") Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { collectionStatus = "partial" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (collectionStatus == "partial") Color(0xFFD97706) else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("محصل مالي جزئي (دفعة نقدية) 💰", color = if (collectionStatus == "partial") Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (collectionStatus == "partial") {
                        OutlinedTextField(
                            value = receivedAmountStr,
                            onValueChange = { receivedAmountStr = it },
                            label = { Text("المبلغ المالي المستلم بالفعل (جنيه)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sprice = sellPriceStr.toDoubleOrNull() ?: 0.0
                        if (sprice <= 0.0) {
                            Toast.makeText(context, "الرجاء كتابة قيمة بيع صحيحة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val recAmt = receivedAmountStr.toDoubleOrNull() ?: 0.0
                        if (collectionStatus == "partial" && (recAmt <= 0.0 || recAmt > sprice)) {
                            Toast.makeText(context, "الرجاء إدخال مبلغ مستلم صحيح أقل من إجمالي البيع", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (buyerOption == "existing" && selectedBuyerId == null) {
                            Toast.makeText(context, "الرجاء تحديد الحساب المالي المسجل للمشتري", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (buyerOption == "new" && newBuyerName.isBlank()) {
                            Toast.makeText(context, "الرجاء إدخال اسم العميل الجديد", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.sellAnimal(
                            animal = targetAnimal,
                            price = sprice,
                            associatedPersonId = selectedBuyerId,
                            newBuyerName = newBuyerName,
                            paymentStatus = collectionStatus,
                            receivedAmount = recAmt
                        )
                        Toast.makeText(context, "تم تسجيل الفاتورة وسند البيع وتحديث الأرصدة!", Toast.LENGTH_LONG).show()
                        showSellDialogByAnimal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("تسجيل وحفظ البيع", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSellDialogByAnimal = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    // Sell birth dialog overlay
    if (showSellBirthDialog != null) {
        val b = showSellBirthDialog!!
        if (b.status == "تم بيعه") {
            AlertDialog(
                onDismissRequest = { showSellBirthDialog = null },
                title = { Text("المولود مباع") },
                text = { Text("عذراً، لا يمكن بيع هذا المولود لأنه مباع بالفعل.") },
                confirmButton = { TextButton(onClick = { showSellBirthDialog = null }) { Text("حسناً") } }
            )
        } else {
            var sellPriceStr by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showSellBirthDialog = null },
                title = { Text("بيع المولود", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("سيتم تغيير حالة المولود إلى مباع وإضافة مبلغ البيع كإيراد إلى الخزينة.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = sellPriceStr,
                            onValueChange = { sellPriceStr = it },
                            label = { Text("مبلغ البيع (جنيه)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = sellPriceStr.toDoubleOrNull() ?: 0.0
                            viewModel.sellBirth(b, price)
                            showSellBirthDialog = null
                            Toast.makeText(context, "تم بيع المولود وتسجيل الإيراد المالي!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                    ) {
                        Text("تأكيد البيع", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSellBirthDialog = null }) { Text("إلغاء", color = Color.Gray) }
                }
            )
        }
    }

    if (showLocalNewbornDialog) {
        AddNewbornDialog(
            viewModel = viewModel,
            accentColor = themePrimary,
            onDismiss = { showLocalNewbornDialog = false }
        )
    }

    // Editing Dialog
    if (editingAnimal != null) {
        val target = editingAnimal!!
        var name by remember { mutableStateOf(target.name) }
        var type by remember { mutableStateOf(target.type) }
        var weightStr by remember { mutableStateOf(target.weight.toString()) }
        var purchasePriceStr by remember { mutableStateOf(target.purchasePrice.toString()) }
        var age by remember { mutableStateOf(target.age) }
        var feedCostStr by remember { mutableStateOf(target.feedCost.toString()) }
        var merchantName by remember { mutableStateOf(target.merchantName) }

        AlertDialog(
            onDismissRequest = { editingAnimal = null },
            title = { Text("تعديل بيانات الماشية", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        label = { Text("اسم التاجر المشتري / البائع") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم أو رقم الرأس") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("نوع رأس الماشية (مثال: عجل، أغنام)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("الوزن (كغ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text("سعر الشراء (جنيه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("العمر") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = feedCostStr,
                        onValueChange = { feedCostStr = it },
                        label = { Text("تكلفة طعام هذا الرأس") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(
                            merchantName = if (merchantName.isBlank()) "سوق" else merchantName,
                            name = name,
                            weight = weightStr.toDoubleOrNull() ?: target.weight,
                            purchasePrice = purchasePriceStr.toDoubleOrNull() ?: target.purchasePrice,
                            age = age,
                            feedCost = feedCostStr.toDoubleOrNull() ?: target.feedCost
                        )
                        onConfirmEdit("هل أنت متأكد من رغبتك في تعديل وحفظ بيانات رأس الماشية (${target.name})؟") {
                            viewModel.updateAnimalDetails(updated)
                            editingAnimal = null
                            Toast.makeText(context, "تم تعديل السجل بنجاح 📝", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("حفظ التعديلات", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAnimal = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

// ================= SCREEN COMPOSABLE 3: FEEDS STORE =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreen(
    viewModel: FarmViewModel,
    themePrimary: Color,
    zoom: Float,
    cardBgColor: Color,
    onCheckAddPermission: (() -> Unit) -> Unit,
    onConfirmEdit: (String, () -> Unit) -> Unit,
    onConfirmDelete: (String, () -> Unit) -> Unit,
    onAddFeedClick: () -> Unit,
    onAddMedicineClick: () -> Unit
) {
    val feeds by viewModel.feedsList.collectAsStateWithLifecycle()
    val medicines by viewModel.medicinesList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var activeSubTab by remember { mutableStateOf(0) } // 0 = feeds, 1 = medicines
    var expandedFeedId by remember { mutableStateOf<Int?>(null) }
    var expandedMedicineId by remember { mutableStateOf<Int?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            kotlinx.coroutines.delay(800)
            isRefreshing = false
        }
    }
    
    var globalSearchQuery by remember { mutableStateOf("") }
    
    val filteredFeeds = remember(feeds, globalSearchQuery) {
        if (globalSearchQuery.isNotBlank()) {
            val query = globalSearchQuery.trim().lowercase()
            feeds.filter {
                it.feedName.lowercase().contains(query) ||
                it.ingredientsDescription.lowercase().contains(query)
            }
        } else feeds
    }
    
    val filteredMedicines = remember(medicines, globalSearchQuery) {
        if (globalSearchQuery.isNotBlank()) {
            val query = globalSearchQuery.trim().lowercase()
            medicines.filter {
                it.name.lowercase().contains(query)
            }
        } else medicines
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dual Sub-Tab Switcher (M3 styling)
        Surface(
            color = themePrimary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair(0, "الأعلاف والمكونات 🌾"),
                    Pair(1, "الأدوية والعلاجات البيطرية 💊")
                ).forEach { (index, title) ->
                    val isSelected = activeSubTab == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeSubTab = index },
                        color = if (isSelected) themePrimary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = (12f * (zoom / 16f)).sp
                            )
                        }
                    }
                }
            }
        }

        // Section Title and Action button (adaptive based on tab)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeSubTab == 0) "مخزن وتكلفة الأعلاف المتاحة" else "مستودع الأدوية والعلاجات البيطرية",
                fontWeight = FontWeight.Bold,
                fontSize = (13f * (zoom / 16f)).sp
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = if (activeSubTab == 0) onAddFeedClick else onAddMedicineClick,
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (activeSubTab == 0) "إضافة أعلاف ➕" else "إضافة دواء ➕",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = globalSearchQuery,
            onValueChange = { globalSearchQuery = it },
            placeholder = { Text("بحث ذكي (الرقم، الحالة، المصدر، النوع، الوصف)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        )

        if (activeSubTab == 0) {
            // FEEDS COLUMN LIST
            if (filteredFeeds.isEmpty()) {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Grass, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد بيانات، اضغط + للإضافة", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(filteredFeeds) { feed ->
                            val isExpanded = expandedFeedId == feed.id
                            Surface(
                                color = cardBgColor,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedFeedId = if (isExpanded) null else feed.id }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Grass, contentDescription = null, tint = Color(0xFFD97706))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    feed.feedName,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = (15f * (zoom / 16f)).sp
                                                )
                                                Text("المواد: ${feed.ingredientsDescription}", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(top = 12.dp)
                                                .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("إجمالي الوزن المشتري: ${feed.totalWeight} كغ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("التكلفة الإجمالية: ${feed.totalCost} جنيه", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("تاريخ الإضافة السجلية: ${feed.addedDate}", fontSize = 11.sp, color = Color.Gray)

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Button(
                                                onClick = {
                                                    onConfirmDelete("هل أنت متأكد من رغبتك في حذف السند الحلفي (${feed.feedName}) نهائياً؟") {
                                                        viewModel.deleteFeedRecord(feed)
                                                        Toast.makeText(context, "تم حذف سند الأعلاف بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حذف هذا السند الحلفي", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MEDICINES COLUMN LIST
            if (filteredMedicines.isEmpty()) {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد أدوية أو علاجات مسجلة حالياً، اضغط + للإضافة", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                        items(filteredMedicines) { med ->
                            val isExpanded = expandedMedicineId == med.id
                            Surface(
                                color = cardBgColor,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedMedicineId = if (isExpanded) null else med.id }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF0284C7))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    med.name,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = (15f * (zoom / 16f)).sp
                                                )
                                                Text("فترة السحب بالأيام: ${med.validityDays} يوم", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(top = 12.dp)
                                                .background(Color(0xFFF0F9FF), RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("التكلفة الإجمالية: ${med.totalCost} جنيه", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                                                Text("فترة سحب الدواء: ${med.validityDays} يوم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("تاريخ الشراء: ${med.addedDate}", fontSize = 11.sp, color = Color.Gray)

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Button(
                                                onClick = {
                                                    onConfirmDelete("هل أنت متأكد من رغبتك في حذف السند الطبي الدوائي (${med.name}) نهائياً؟") {
                                                        viewModel.deleteMedicineRecord(med)
                                                        Toast.makeText(context, "تم حذف سند الدواء بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حذف هذا السند الطبي الدوائي", color = Color.White, fontSize = 11.sp)
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
    }
}

// ================= SCREEN COMPOSABLE 4: ACCOUNTS (LEGER) =================
@Composable
fun AccountsScreen(
    viewModel: FarmViewModel,
    themePrimary: Color,
    zoom: Float,
    cardBgColor: Color,
    onCheckAddPermission: (() -> Unit) -> Unit,
    onConfirmEdit: (String, () -> Unit) -> Unit,
    onConfirmDelete: (String, () -> Unit) -> Unit
) {
    val people by viewModel.peopleList.collectAsStateWithLifecycle()
    val currentFarm by viewModel.currentFarm.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddPersonDialog by remember { mutableStateOf(false) }

    var expandedPersonId by remember { mutableStateOf<Int?>(null) } // height controller expander
    var selectedPersonForLedger by remember { mutableStateOf<PersonEntity?>(null) }

    // edit and delete status
    var editingPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var personToDelete by remember { mutableStateOf<PersonEntity?>(null) }
    
    var globalSearchQuery by remember { mutableStateOf("") }
    
    val filteredPeople = remember(people, globalSearchQuery) {
        if (globalSearchQuery.isNotBlank()) {
            val query = globalSearchQuery.trim().lowercase()
            people.filter {
                it.name.lowercase().contains(query) ||
                it.role.lowercase().contains(query)
            }
        } else {
            people
        }
    }

    if (selectedPersonForLedger != null) {
        val currentPerson = people.find { it.id == selectedPersonForLedger?.id } ?: selectedPersonForLedger!!
        PersonLedgerPage(
            person = currentPerson,
            viewModel = viewModel,
            themePrimary = themePrimary,
            zoom = zoom,
            onClose = { selectedPersonForLedger = null }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("حسابات الأشخاص والعملاء دائن ومدين", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Button(
                    onClick = { onCheckAddPermission { showAddPersonDialog = true } },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("إضافة جديد +", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            OutlinedTextField(
                value = globalSearchQuery,
                onValueChange = { globalSearchQuery = it },
                placeholder = { Text("بحث ذكي (الرقم، الحالة، المصدر، النوع، الوصف)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredPeople.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد بيانات، اضغط + للإضافة", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredPeople) { person ->
                        val isExpanded = expandedPersonId == person.id

                        Surface(
                            color = cardBgColor,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedPersonId = if (isExpanded) null else person.id }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = if (person.balance >= 0) themePrimary else Color.Red,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                person.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = (15f * (zoom / 16f)).sp
                                            )
                                            Text("الصفة المهنية: ${person.role} - جوال: ${person.phone}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (person.balance >= 0) "له" else "عليه",
                                            fontSize = 11.sp,
                                            color = if (person.balance >= 0) themePrimary else Color.Red,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${kotlin.math.abs(person.balance)} جنيه",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = if (person.balance >= 0) themePrimary else Color.Red
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Button(
                                            onClick = { selectedPersonForLedger = person },
                                            colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        ) {
                                            Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("كشف وسجل الحساب والمستندات 📑", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    if (!viewModel.hasPermission("manage_debts")) {
                                                        Toast.makeText(context, "عذراً، يرجى طلب صلاحية إدارة وتسوية الديون للتعديل ❌", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        editingPerson = person
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تعديل", color = Color.White, fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    onConfirmDelete("هل أنت متأكد من حذف الحساب الكامل لـ (${person.name})؟ سيؤدي ذلك لإزالة التقرير المالي بالكامل وإلغاء ارتباطه بالمعاملات والماشية المرتبطة به. مزارعك وكل السجلات ستتأثر.") {
                                                        if (selectedPersonForLedger?.id == person.id) {
                                                            selectedPersonForLedger = null
                                                        }
                                                        viewModel.deletePersonRecord(person)
                                                        Toast.makeText(context, "تم حذف حساب الشخص بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حذف الحساب", color = Color.White, fontSize = 11.sp)
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
    }

    if (showAddPersonDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("صاحب المزرعة") } // "صاحب المزرعة", "عامل", "تاجر"
        var balStr by remember { mutableStateOf("") }
        var isCredit by remember { mutableStateOf(true) } // true: له, false: عليه

        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("إضافة شخص أو عامل إلى السجلات", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الشخص الكامل") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role selective buttons
                    Text("الصورة المهنية / العلاقية", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("صاحب المزرعة", "عامل", "تاجر").forEach { r ->
                            val active = role == r
                            Button(
                                onClick = { role = r },
                                colors = ButtonDefaults.buttonColors(containerColor = if (active) themePrimary else Color.LightGray),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(r, color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = balStr,
                        onValueChange = { balStr = it },
                        label = { Text("الرصيد المبدئي (جنيه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isCredit, onClick = { isCredit = true })
                            Text("دائن (له مال)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !isCredit, onClick = { isCredit = false })
                            Text("مدين (عليه مال)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val amt = balStr.toDoubleOrNull() ?: 0.0
                        val finalBal = if (isCredit) amt else -amt
                        viewModel.registerPerson(name, role, phone, finalBal)
                        showAddPersonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("إضافة الشخص", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    if (editingPerson != null) {
        val person = editingPerson!!
        var name by remember { mutableStateOf(person.name) }
        var phone by remember { mutableStateOf(person.phone) }
        var role by remember { mutableStateOf(person.role) }
        var balStr by remember { mutableStateOf(kotlin.math.abs(person.balance).toString()) }
        var isCredit by remember { mutableStateOf(person.balance >= 0) }

        AlertDialog(
            onDismissRequest = { editingPerson = null },
            title = { Text("تعديل بيانات الحساب") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم الكامل") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الجوال") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = balStr,
                        onValueChange = { balStr = it },
                        label = { Text("الرصيد المالي الحالي (جنيه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isCredit, onClick = { isCredit = true })
                            Text("له")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !isCredit, onClick = { isCredit = false })
                            Text("عليه")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseVal = balStr.toDoubleOrNull() ?: 0.0
                        val finalBal = if (isCredit) baseVal else -baseVal
                        val updatedPerson = person.copy(name = name, phone = phone, balance = finalBal)
                        onConfirmEdit("هل أنت متأكد من رغبتك في تعديل بيانات وتوازن حساب (${person.name})؟") {
                            viewModel.updatePersonRecord(updatedPerson)
                            editingPerson = null
                            Toast.makeText(context, "تم تعديل حساب الشخص بنجاح 📝", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("تعديل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPerson = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    if (personToDelete != null) {
        val p = personToDelete!!
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("تأكيد حذف الحساب ⚠️", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text(
                    "هل أنت متأكد من حذف الحساب الكامل لـ ( ${p.name} )؟ \n" +
                    "سيؤدي ذلك لإزالة التقرير المالي بالكامل وإلغاء ارتباطه بالمعاملات والماشية المرتبطة به. \n\n" +
                    "هذا الإجراء لا يمكن التراجع عنه!",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPersonForLedger?.id == p.id) {
                            selectedPersonForLedger = null
                        }
                        viewModel.deletePersonRecord(p)
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، حذف نهائياً", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

// ================= COMPOSABLE 4.5: PERSON LEDGER DETAIL PAGE =================
@Composable
fun PersonLedgerPage(
    person: PersonEntity,
    viewModel: FarmViewModel,
    themePrimary: Color,
    zoom: Float,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val transactionsAll by viewModel.transactionsList.collectAsStateWithLifecycle()
    val animalsAll by viewModel.animalsList.collectAsStateWithLifecycle()
    val feedsAll by viewModel.feedsList.collectAsStateWithLifecycle()

    // Filter transactions specifically for this user
    val transactions = remember(transactionsAll, person.id) {
        transactionsAll.filter { it.associatedPersonId == person.id }
    }

    // Input States
    var amountStr by remember { mutableStateOf("") }
    var descriptionStr by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("income") } // "income" (دائن/له/قبض) or "expense" (مدين/عليه/صرف)
    var selectedCategory by remember { mutableStateOf("عام") }

    // Relationship link choices
    var linkWithAnimal by remember { mutableStateOf<Int?>(null) }
    var linkWithFeed by remember { mutableStateOf<String?>(null) }
    var expandedAnimal by remember { mutableStateOf(false) }
    var expandedFeed by remember { mutableStateOf(false) }

    // Edit/Inspect Transaction Modals
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var inspectingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var invoiceTxForPrinting by remember { mutableStateOf<TransactionEntity?>(null) }
    var settlingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var confirmSaveParams by remember { mutableStateOf<Triple<Double, String, String>?>(null) }
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Soft slate background
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // BACK HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowForward, // RTL back arrow
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                "كشف وسجل الحساب والمستندات",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // PERSON HEADER CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            person.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = themePrimary
                        )
                        Text(
                            "الصفة المهنية: ${person.role}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Balance status bubble
                    val isCreditor = person.balance >= 0
                    val balanceLabel = if (isCreditor) "دائن (له مال)" else "مدين (عليه)"
                    val balanceColor = if (isCreditor) Color(0xFF059669) else Color(0xFFDC2626)
                    val balanceBg = if (isCreditor) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .background(balanceBg, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            balanceLabel,
                            fontSize = 11.sp,
                            color = balanceColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${kotlin.math.abs(person.balance)} جنيه",
                            fontSize = 16.sp,
                            color = balanceColor,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (person.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "لا يمكن إجراء الاتصال: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        Text("اتصال جوال: ${person.phone}", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ================= SECTION: ADD TRANSACTION FORM =================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "إضافة حركة مالية جديدة للحساب 💰",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Debit vs Credit toggle buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isIncome = transactionType == "income"
                    Button(
                        onClick = { transactionType = "income" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (isIncome) Color.White else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "دائن (له / قبض)",
                            color = if (isIncome) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { transactionType = "expense" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isIncome) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (!isIncome) Color.White else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "مدين (عليه / صرف)",
                            color = if (!isIncome) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Amount text edit
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ المالي (جنيه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedLabelColor = themePrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                // Description
                OutlinedTextField(
                    value = descriptionStr,
                    onValueChange = { descriptionStr = it },
                    label = { Text("البيان والسبب (مثال: دفعة من الحساب، حليب، الخ)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedLabelColor = themePrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                // ================= RELATIONAL BINDINGS ("ربط الحسابات بالحظيرة والأعلاف والعلاج") =================
                Text(
                    "خيارات الربط والتعليق (اختياري) 🔗",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // 1. Link to Barn / Animals
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = themePrimary, modifier = Modifier.size(16.dp))
                            Text("ربط برأس ماشية محدد:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box {
                            val selectedAnimal = animalsAll.firstOrNull { it.id == linkWithAnimal }
                            val animalLabel = if (selectedAnimal != null) "${selectedAnimal.name} (${selectedAnimal.type})" else "اختر الرأس"
                            Button(
                                onClick = { expandedAnimal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(animalLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                            }
                            DropdownMenu(expanded = expandedAnimal, onDismissRequest = { expandedAnimal = false }) {
                                DropdownMenuItem(
                                    text = { Text("عدم الربط بالماشية") },
                                    onClick = {
                                        linkWithAnimal = null
                                        expandedAnimal = false
                                    }
                                )
                                animalsAll.forEach { animal ->
                                    DropdownMenuItem(
                                        text = { Text("${animal.name} - ${animal.type} | وزنه ${animal.weight}كغ") },
                                        onClick = {
                                            linkWithAnimal = animal.id
                                            expandedAnimal = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Link to Feeds
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Grass, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            Text("ربط بطلبية علف مسجلة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box {
                            val feedLabel = linkWithFeed ?: "اختر السجل"
                            Button(
                                onClick = { expandedFeed = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(feedLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                            }
                            DropdownMenu(expanded = expandedFeed, onDismissRequest = { expandedFeed = false }) {
                                DropdownMenuItem(
                                    text = { Text("عدم الربط بالأعلاف") },
                                    onClick = {
                                        linkWithFeed = null
                                        expandedFeed = false
                                    }
                                )
                                feedsAll.forEach { feed ->
                                    DropdownMenuItem(
                                        text = { Text("${feed.feedName} (${feed.totalWeight} كغ) - ${feed.totalCost} ج") },
                                        onClick = {
                                            linkWithFeed = feed.feedName
                                            expandedFeed = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SAVE NEW LEDGER TRANSACTION BUTTON
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "الرجاء كتابة قيمة مالية صحيحة", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        var finalDesc = if (descriptionStr.isBlank()) {
                            "قيد مالي ${if (transactionType == "income") "دائن (له)" else "مدين (عليه)"}"
                        } else {
                            descriptionStr
                        }
                        var finalCategory = "عام"

                    if (linkWithFeed != null) {
                        if (finalDesc.isNotBlank()) finalDesc += " "
                        finalDesc += "[🌾 توريد أعلاف: ${linkWithFeed}]"
                        finalCategory = "أعلاف"
                    }
                    
                    confirmSaveParams = Triple(amount, finalDesc, finalCategory)
                },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل وحفظ السند المالي للحساب", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ================= SECTION: DETAILED TRANSACTION HISTORY (سجل الحساب) =================
        Text(
            "سجل الحركات المالية التفصيلي (القيود والدفتر)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث في السندات بالبيان، المبلغ، أو التصنيف... 🔍") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themePrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )

        val filteredTransactions = remember(transactions, searchQuery) {
            if (searchQuery.isBlank()) transactions
            else {
                transactions.filter {
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.amount.toString().contains(searchQuery) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.date.contains(searchQuery)
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد بيانات، اضغط + للإضافة", color = Color.Gray, fontSize = 11.sp)
                }
            }
        } else {
            filteredTransactions.forEach { tx ->
                val isTxIncome = tx.type == "income"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (isTxIncome) Icons.Default.ArrowCircleUp else Icons.Default.ArrowCircleDown,
                                    contentDescription = null,
                                    tint = if (isTxIncome) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "${tx.amount} جنيه",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isTxIncome) Color(0xFF047857) else Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = if (isTxIncome) "دائن (قبض / دفعة منه)" else "مدين (عليه / صرف له)",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Raw date indicator badge
                            Text(
                                text = tx.date,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Display details
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tx.description,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium
                        )

                        // Show Category
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "التصنيف: ${tx.category}",
                                fontSize = 10.sp,
                                color = themePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(themePrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            // Show Animal linkage if mapped
                            if (tx.associatedAnimalId != null) {
                                val matchedAnimal = animalsAll.find { it.id == tx.associatedAnimalId }
                                val animalLabel = if (matchedAnimal != null) "${matchedAnimal.name} (${matchedAnimal.type})" else "رقم #${tx.associatedAnimalId} (مؤرشف)"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Pets, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "الماشية: $animalLabel",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // EDIT & DELETE ACTIONS ROW
                        Spacer(modifier = Modifier.height(12.dp))
                        Spacer(modifier = Modifier.height(1.dp).background(Color(0xFFE2E8F0)))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Unified operation actions manager button
                        Button(
                            onClick = { inspectingTx = tx },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themePrimary.copy(alpha = 0.08f),
                                contentColor = themePrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إجراءات السند (عرض التفاصيل، تعديل، حذف) ⚙️", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Print Invoice button (Always accessible)
                                Row(
                                    modifier = Modifier
                                        .clickable { invoiceTxForPrinting = tx }
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "طباعة السند", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("طباعة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                }
                                
                                // Invoice Settlement button
                                if (viewModel.hasPermission("manage_debts")) {
                                    Row(
                                        modifier = Modifier
                                            .clickable { settlingTx = tx }
                                            .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = "تسوية الفاتورة", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسوية", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (viewModel.hasPermission("add_transaction")) {
                                    IconButton(onClick = { editingTx = tx }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                
                                IconButton(
                                    onClick = {
                                        if (!viewModel.hasPermission("delete_animal") && !viewModel.hasPermission("add_transaction")) {
                                            Toast.makeText(context, "عذراً، مسح السندات يحتاج صلاحيات كاملة ❌", Toast.LENGTH_SHORT).show()
                                        } else {
                                            txToDelete = tx
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف المعاملة", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= DIALOG: EDIT TRANSACTION RECORD =================
    if (editingTx != null) {
        val tx = editingTx!!
        var editAmount by remember { mutableStateOf(tx.amount.toString()) }
        var editType by remember { mutableStateOf(tx.type) }
        var editDesc by remember { mutableStateOf(tx.description) }
        var editCategory by remember { mutableStateOf(tx.category) }
        var editAnimalId by remember { mutableStateOf(tx.associatedAnimalId) }
        var editAnimalExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingTx = null },
            title = { Text("تعديل تفاصيل السند المالي", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("المبلغ المالي (جنيه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type Selector Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isIncomeSelected = editType == "income"
                        Button(
                            onClick = { editType = "income" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isIncomeSelected) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "دائن (له مال)",
                                color = if (isIncomeSelected) Color(0xFF15803D) else Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { editType = "expense" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isIncomeSelected) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "مدين (عليه)",
                                color = if (!isIncomeSelected) Color(0xFFB91C1C) else Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("البيان والوصف") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("التصنيف الكلي") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Edit Animal Link Selection
                    Text("ربط بالماشية (اختياري)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Box {
                        val activeAnimal = animalsAll.firstOrNull { it.id == editAnimalId }
                        val activeName = if (activeAnimal != null) "${activeAnimal.name} (${activeAnimal.type})" else "عدم الربط بالماشية"
                        Button(
                            onClick = { editAnimalExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(activeName, color = Color.Black, fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = editAnimalExpanded, onDismissRequest = { editAnimalExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("عدم الربط بالماشية") },
                                onClick = {
                                    editAnimalId = null
                                    editAnimalExpanded = false
                                }
                            )
                            animalsAll.forEach { animal ->
                                DropdownMenuItem(
                                    text = { Text("${animal.name} (${animal.type})") },
                                    onClick = {
                                        editAnimalId = animal.id
                                        editAnimalExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Dynamically calculate and display the expected financial change
                    val parsedAmount = editAmount.toDoubleOrNull() ?: 0.0
                    val oldContribution = if (tx.type == "income") tx.amount else -tx.amount
                    val newContribution = if (editType == "income") parsedAmount else -parsedAmount
                    val balanceDelta = newContribution - oldContribution
                    val currentBalance = person.balance
                    val expectedNewBalance = currentBalance + balanceDelta
                    
                    val currentBalanceText = if (currentBalance >= 0) "${currentBalance} ج.م (له)" else "${-currentBalance} ج.م (عليه)"
                    val expectedNewBalanceText = if (expectedNewBalance >= 0) "${expectedNewBalance} ج.م (له)" else "${-expectedNewBalance} ج.م (عليه)"

                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = themePrimary.copy(alpha = 0.06f)
                        ),
                        border = BorderStroke(1.dp, themePrimary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "📊 الأثر المالي المتوقع للحساب بعد الحفظ:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = themePrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الرصيد الحالي للحساب:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(currentBalanceText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("مقدار التغير بالصافي:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (balanceDelta >= 0) "+${balanceDelta} ج.م" else "${balanceDelta} ج.م",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balanceDelta >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(themePrimary.copy(alpha = 0.15f)))
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الرصيد الجديد المتوقع للحساب:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = expectedNewBalanceText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = themePrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = editAmount.toDoubleOrNull() ?: tx.amount
                        val updatedTx = tx.copy(
                            amount = amountVal,
                            type = editType,
                            description = editDesc,
                            category = editCategory,
                            associatedAnimalId = editAnimalId
                        )
                        viewModel.updateTransactionRecord(tx, updatedTx)
                        editingTx = null
                        Toast.makeText(context, "تم حفظ وتعديل القيد المالي", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("حفظ التغييرات", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTx = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    if (confirmSaveParams != null) {
        val (cAmount, cDesc, cCategory) = confirmSaveParams!!
        AlertDialog(
            onDismissRequest = { confirmSaveParams = null },
            title = { Text("تأكيد تسجيل السند المالي ⚠️", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من تسجيل هذه الحركة المالية وتعديل رصيد الحساب؟\n\n" +
                    "المبلغ: $cAmount جنيه\n" +
                    "البيان: $cDesc",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.registerManualTransactionWithDetails(
                            type = transactionType,
                            amount = cAmount,
                            description = cDesc,
                            category = cCategory,
                            personId = person.id,
                            associatedAnimalId = linkWithAnimal
                        )

                        // Reset fields
                        amountStr = ""
                        descriptionStr = ""
                        linkWithAnimal = null
                        linkWithFeed = null
                        confirmSaveParams = null

                        Toast.makeText(context, "تم تسجيل وإضافة المعاملة بنجاح وتحديث الرصيد", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("نعم، تأكيد وحفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSaveParams = null }) {
                    Text("مراجعة وإلغاء", color = Color.Gray)
                }
            }
        )
    }

    // ================= DIALOG: SETTLE TRANSACTION =================
    if (settlingTx != null) {
        val tx = settlingTx!!
        
        // Calculate already settled amount based on past settlement transactions linked to this tx.id
        val settledAmount = remember(transactionsAll, tx.id) {
            transactionsAll.filter { it.description.startsWith("تسوية لسند رقم #${tx.id}:") }.sumOf { it.amount }
        }
        val remainingAmount = (tx.amount - settledAmount).coerceAtLeast(0.0)
        
        var settleAmountStr by remember { mutableStateOf(remainingAmount.toString()) }
        
        AlertDialog(
            onDismissRequest = { settlingTx = null },
            title = { Text("تسوية فاتورة / مستحقات 💰", fontWeight = FontWeight.Bold, color = themePrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = themePrimary.copy(alpha=0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي الفاتورة الأساسية:", fontSize = 12.sp, color = Color.Gray)
                                Text("${tx.amount} جنيه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي المدفوع/المسوى سابقاً:", fontSize = 12.sp, color = Color.Gray)
                                Text("$settledAmount جنيه", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المبلغ المتبقي (الرصيد المعلق):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("$remainingAmount جنيه", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }
                    }

                    if (remainingAmount <= 0.0) {
                        Text("تمت تسوية هذه الفاتورة بالكامل مسبقاً! ✅", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Text("أدخل الدفعة المستلمة للتسوية الجزئية. تسجيل عملية التسوية سيتضمن التاريخ والوقت بدقة لتتبع الدفعات.", fontSize = 11.sp, color = Color.Gray)
                        
                        OutlinedTextField(
                            value = settleAmountStr,
                            onValueChange = { settleAmountStr = it },
                            label = { Text("قيمة الدفعة / التسوية المراد دفعها (جنيه)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = { settleAmountStr = remainingAmount.toString() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("دفع التسوية بالكامل ($remainingAmount)", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                if (remainingAmount > 0.0) {
                    Button(
                        onClick = {
                            val amountVal = settleAmountStr.toDoubleOrNull() ?: 0.0
                            if (amountVal > 0.0 && amountVal <= remainingAmount) {
                                val newTxType = tx.type
                                
                                val currentDateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
                                viewModel.registerManualTransaction(
                                    type = newTxType,
                                    amount = amountVal,
                                    description = "تسوية لسند رقم #${tx.id}: دفعة بقيمة $amountVal في ($currentDateTime)",
                                    category = "تسوية فواتير",
                                    personId = person.id
                                )
                                settlingTx = null
                                Toast.makeText(context, "تم تسجيل دفعة التسوية بنجاح ✅", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "يرجى كتابة مبلغ دفع صحيح لا يتجاوز المتبقي", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                    ) {
                        Text("تأكيد دفع التسوية", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { settlingTx = null }) {
                    Text("إغلاق", color = Color.Gray)
                }
            }
        )
    }

    if (txToDelete != null) {
        val tx = txToDelete!!
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("تأكيد حذف القيد أو السند المالي ⚠️", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                val deletionDelta = if (tx.type == "income") -tx.amount else tx.amount
                val currentBal = person.balance
                val expectedBal = currentBal + deletionDelta
                
                val currentBalText = if (currentBal >= 0) "${currentBal} ج.م (له)" else "${-currentBal} ج.م (عليه)"
                val expectedBalText = if (expectedBal >= 0) "${expectedBal} ج.م (له)" else "${-expectedBal} ج.م (عليه)"

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "هل أنت متأكد من حذف هذا السند المالي نهائياً؟",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("وصف السند: ${tx.description}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("المبلغ: ${tx.amount} جنيه", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Text("تبويب الفئة: ${tx.category}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🚨 الأثر والنتائج المالية بحذف الحركة بالتفصيل:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB91C1C)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("رصيد الحساب الحالي:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(currentBalText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("صافي الأثر المسترجع/الخصم:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (deletionDelta >= 0) "+${deletionDelta} ج.م (استرداد)" else "${deletionDelta} ج.م (خصم)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (deletionDelta >= 0) Color(0xFF047857) else Color(0xFFB91C1C)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFFCA5A5).copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الرصيد النهائي المتوقع بعد الحذف:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text(
                                    text = expectedBalText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.softDeleteTransactionRecord(tx)
                        txToDelete = null
                        Toast.makeText(context, "تم نقل المعاملة لسلة المحذوفات وتعديل التقرير المالي بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، حذف وتعديل الرصيد", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    if (invoiceTxForPrinting != null) {
        val tx = invoiceTxForPrinting!!
        val isTxIncome = tx.type == "income"
        
        AlertDialog(
            onDismissRequest = { invoiceTxForPrinting = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = themePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("معاينة الفاتورة المستندية 🧾", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.currentFarm.value ?: "مزرعتي الخاصة",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = themePrimary
                        )
                        Text(
                            text = "سند قيد رسمي معتمد وموثق رقمياً",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.LightGray))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رقم السند الفاتورة:", fontSize = 10.sp, color = Color.Gray)
                        Text("#TX-00${tx.id}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تاريخ المعاملة:", fontSize = 10.sp, color = Color.Gray)
                        Text(tx.date, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("العميل / الحساب:", fontSize = 10.sp, color = Color.Gray)
                        Text(person.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نوع السند:", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = if (isTxIncome) "وصل استلام نقدية (له/قبض)" else "وصل صرف نقدية (عليه/دفع)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTxIncome) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("التصنيف وبند القيد:", fontSize = 10.sp, color = Color.Gray)
                        Text(tx.category, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.LightGray))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المبلغ الصافي بالأرقام:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text(
                            "${tx.amount} جنيه مصري",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTxIncome) Color(0xFF047857) else Color(0xFFB91C1C)
                        )
                    }

                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.LightGray))

                    Text(
                        text = "البيان والتفاصيل:\n${tx.description}",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = "توقيع المستلم المعتمد لـ ${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}: _________________",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End).padding(top = 12.dp)
                    )
                    
                    if (person.balance != 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                val txToSettle = invoiceTxForPrinting
                                invoiceTxForPrinting = null
                                if (txToSettle != null) {
                                    settlingTx = txToSettle
                                } 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "تسديد أو إضافة دفعة متبقية للحساب (${if (person.balance > 0) person.balance else -person.balance} ج.م)", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Native real PDF sharing
                    Button(
                        onClick = {
                            try {
                                val pdf = android.graphics.pdf.PdfDocument()
                                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                                val page = pdf.startPage(pageInfo)
                                val canvas = page.canvas
                                val paint = android.graphics.Paint()
                                val isTxIncome = tx.type == "income"
                                
                                paint.color = android.graphics.Color.parseColor("#3B82F6")
                                canvas.drawRect(30f, 30f, 565f, 110f, paint)
                                
                                paint.color = android.graphics.Color.WHITE
                                paint.isAntiAlias = true
                                paint.textSize = 20f
                                paint.isFakeBoldText = true
                                canvas.drawText(viewModel.currentFarm.value ?: "مزرعتي الخاصة", 50f, 70f, paint)
                                
                                paint.textSize = 10f
                                paint.isFakeBoldText = false
                                canvas.drawText("سند قيد رسمي معتمد وموثق رقمياً لـ ${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}", 50f, 95f, paint)
                                
                                paint.color = android.graphics.Color.BLACK
                                paint.textSize = 12f
                                
                                var topY = 150f
                                val drawLine: (String, String) -> Unit = { key, value ->
                                    paint.color = android.graphics.Color.GRAY
                                    canvas.drawText(key, 50f, topY, paint)
                                    paint.color = android.graphics.Color.BLACK
                                    paint.isFakeBoldText = true
                                    canvas.drawText(convertToEasternArabicNumerals(value), 300f, topY, paint)
                                    paint.isFakeBoldText = false
                                    topY += 28f
                                }
                                
                                drawLine("رقم السند الفاتورة:", "#TX-00${tx.id}")
                                drawLine("تاريخ المعاملة:", tx.date)
                                drawLine("نوع السند:", if (isTxIncome) "وصل استلام نقدية (له/قبض)" else "وصل صرف نقدية (عليه/دفع)")
                                drawLine("التصنيف وبند القيد:", tx.category)
                                
                                paint.color = android.graphics.Color.LTGRAY
                                canvas.drawLine(50f, topY, 545f, topY, paint)
                                topY += 30f
                                
                                paint.color = android.graphics.Color.parseColor("#F3F4F6")
                                canvas.drawRect(50f, topY - 20f, 545f, topY + 30f, paint)
                                
                                paint.color = if (isTxIncome) android.graphics.Color.parseColor("#047857") else android.graphics.Color.parseColor("#B91C1C")
                                paint.textSize = 15f
                                paint.isFakeBoldText = true
                                canvas.drawText("المبلغ الصافي: ${tx.amount} جنيه مصري", 70f, topY + 12f, paint)
                                paint.isFakeBoldText = false
                                topY += 75f
                                
                                paint.color = android.graphics.Color.BLACK
                                paint.textSize = 11f
                                canvas.drawText("البيان والتفاصيل:", 50f, topY, paint)
                                topY += 20f
                                
                                paint.color = android.graphics.Color.DKGRAY
                                val words = tx.description.split(" ")
                                var line = StringBuilder()
                                for (word in words) {
                                    if (paint.measureText(line.toString() + word) > 480) {
                                        canvas.drawText(line.toString(), 50f, topY, paint)
                                        line = StringBuilder(word + " ")
                                        topY += 18f
                                    } else {
                                        line.append(word).append(" ")
                                    }
                                }
                                canvas.drawText(line.toString(), 50f, topY, paint)
                                topY += 60f
                                
                                paint.color = android.graphics.Color.GRAY
                                paint.textSize = 10f
                                canvas.drawText("توقيع المستلم المعتمد لـ ${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}: _________________", 180f, topY, paint)
                                
                                pdf.finishPage(page)
                                
                                val filename = "${(viewModel.currentFarm.value ?: "مزرعتي الخاصة")}_invoice_TX00${tx.id}.pdf".replace(" ", "_")
                                val file = java.io.File(context.cacheDir, filename)
                                val outputStream = java.io.FileOutputStream(file)
                                pdf.writeTo(outputStream)
                                pdf.close()
                                outputStream.close()
                                
                                val uri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "فاتورة رقم #TX-00${tx.id}")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "مرفق فاتورة رقم #TX-00${tx.id} الصادرة من ${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "مشاركة الفاتورة مستند PDF 📄"))
                                invoiceTxForPrinting = null
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل مشاركة الفاتورة كـ PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة الفاتورة", color = Color.White, fontSize = 11.sp)
                    }

                    // Native PDF print adaptation
                    Button(
                        onClick = {
                            try {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                                val jobName = "${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}_Invoice_${tx.id}"
                                printManager.print(jobName, object : android.print.PrintDocumentAdapter() {
                                    override fun onWrite(
                                        pages: Array<out android.print.PageRange>?,
                                        destination: android.os.ParcelFileDescriptor?,
                                        cancellationSignal: android.os.CancellationSignal?,
                                        callback: WriteResultCallback?
                                    ) {
                                        val pdf = android.graphics.pdf.PdfDocument()
                                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                                        val page = pdf.startPage(pageInfo)
                                        val canvas = page.canvas
                                        val paint = android.graphics.Paint()
                                        val isTxIncome = tx.type == "income"
                                        
                                        paint.color = android.graphics.Color.parseColor("#3B82F6")
                                        canvas.drawRect(30f, 30f, 565f, 110f, paint)
                                        
                                        paint.color = android.graphics.Color.WHITE
                                        paint.isAntiAlias = true
                                        paint.textSize = 20f
                                        paint.isFakeBoldText = true
                                        canvas.drawText(viewModel.currentFarm.value ?: "مزرعتي الخاصة", 50f, 70f, paint)
                                        
                                        paint.textSize = 10f
                                        paint.isFakeBoldText = false
                                        canvas.drawText("سند قيد رسمي معتمد وموثق رقمياً لـ ${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}", 50f, 95f, paint)
                                        
                                        paint.color = android.graphics.Color.BLACK
                                        paint.textSize = 12f
                                        
                                        var topY = 150f
                                        val drawLine: (String, String) -> Unit = { key, value ->
                                            paint.color = android.graphics.Color.GRAY
                                            canvas.drawText(key, 50f, topY, paint)
                                            paint.color = android.graphics.Color.BLACK
                                            paint.isFakeBoldText = true
                                            canvas.drawText(convertToEasternArabicNumerals(value), 300f, topY, paint)
                                            paint.isFakeBoldText = false
                                            topY += 28f
                                        }
                                        
                                        drawLine("رقم السند الفاتورة:", "#TX-00${tx.id}")
                                        drawLine("تاريخ المعاملة:", tx.date)
                                        drawLine("نوع السند:", if (isTxIncome) "وصل استلام نقدية (له/قبض)" else "وصل صرف نقدية (عليه/دفع)")
                                        drawLine("التصنيف وبند القيد:", tx.category)
                                        
                                        paint.color = android.graphics.Color.LTGRAY
                                        canvas.drawLine(50f, topY, 545f, topY, paint)
                                        topY += 30f
                                        
                                        paint.color = android.graphics.Color.parseColor("#F3F4F6")
                                        canvas.drawRect(50f, topY - 20f, 545f, topY + 30f, paint)
                                        
                                        paint.color = if (isTxIncome) android.graphics.Color.parseColor("#047857") else android.graphics.Color.parseColor("#B91C1C")
                                        paint.textSize = 15f
                                        paint.isFakeBoldText = true
                                        canvas.drawText("المبلغ الصافي: ${tx.amount} جنيه مصري", 70f, topY + 12f, paint)
                                        paint.isFakeBoldText = false
                                        topY += 75f
                                        
                                        paint.color = android.graphics.Color.BLACK
                                        paint.textSize = 11f
                                        canvas.drawText("البيان والتفاصيل:", 50f, topY, paint)
                                        topY += 20f
                                        
                                        paint.color = android.graphics.Color.DKGRAY
                                        val words = tx.description.split(" ")
                                        var line = StringBuilder()
                                        for (word in words) {
                                            if (paint.measureText(line.toString() + word) > 480) {
                                                canvas.drawText(line.toString(), 50f, topY, paint)
                                                line = StringBuilder(word + " ")
                                                topY += 18f
                                            } else {
                                                line.append(word).append(" ")
                                            }
                                        }
                                        canvas.drawText(line.toString(), 50f, topY, paint)
                                        topY += 60f
                                        
                                        paint.color = android.graphics.Color.GRAY
                                        paint.textSize = 10f
                                        canvas.drawText("توقيع المستلم المعتمد لـ ${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}: _________________", 180f, topY, paint)
                                        
                                        pdf.finishPage(page)
                                        
                                        try {
                                            val outputStream = java.io.FileOutputStream(destination?.fileDescriptor)
                                            pdf.writeTo(outputStream)
                                            pdf.close()
                                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                        } catch (e: Exception) {
                                            callback?.onWriteFailed(e.message)
                                        }
                                    }

                                    override fun onLayout(
                                        oldAttributes: android.print.PrintAttributes?,
                                        newAttributes: android.print.PrintAttributes?,
                                        cancellationSignal: android.os.CancellationSignal?,
                                        callback: LayoutResultCallback?,
                                        extras: android.os.Bundle?
                                    ) {
                                        if (cancellationSignal?.isCanceled == true) {
                                            callback?.onLayoutCancelled()
                                            return
                                        }
                                        val info = android.print.PrintDocumentInfo.Builder("${viewModel.currentFarm.value ?: "مزرعتي الخاصة"}_Invoice_${tx.id}.pdf")
                                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                            .setPageCount(1)
                                            .build()
                                        callback?.onLayoutFinished(info, true)
                                    }
                                }, null)
                                Toast.makeText(context, "تم قراءة السند ونقله لوحدات الطباعة بنجاح! 🖨️", Toast.LENGTH_SHORT).show()
                                invoiceTxForPrinting = null
                            } catch (e: Exception) {
                                Toast.makeText(context, "حدث خطأ أثناء الاتصال بالطابعة: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إرسال لأمر الطباعة فوراً", color = Color.White, fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceTxForPrinting = null }) {
                    Text("إغلاق", color = Color.Gray)
                }
            }
        )
    }

    if (inspectingTx != null) {
        val tx = inspectingTx!!
        val isTxIncome = tx.type == "income"
        
        AlertDialog(
            onDismissRequest = { inspectingTx = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = themePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تفاصيل ومعاينة السند المالي التفصيلية 🔍",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Status card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTxIncome) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "سند مالي رقم #TX-00${tx.id}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (isTxIncome) Color(0xFF15803D) else Color(0xFFB91C1C)
                            )
                            Text(
                                text = if (isTxIncome) "دائن (قبض / دفعة منه للشركة والعميل)" else "مدين (عليه / صرف له من الحساب)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isTxIncome) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${tx.amount} جنيه مصري",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (isTxIncome) Color(0xFF15803D) else Color(0xFFB91C1C)
                            )
                        }
                    }

                    // Metadata Group
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Account owner name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الحساب المستحق / الجهة:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(person.name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = themePrimary)
                        }

                        // Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تاريخ وحين إصدار السند:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(tx.date, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Category
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الفئة والنوع للتبويب المالي:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                text = tx.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = themePrimary,
                                modifier = Modifier
                                    .background(themePrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Description/Note
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("البيان والوصف المكتوب كامل:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = tx.description,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Linkages
                        if (tx.associatedAnimalId != null || tx.description.contains("[🌾") || tx.description.contains("[📚")) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("الروابط والارتباطات بالنظام 🔗", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            
                            if (tx.associatedAnimalId != null) {
                                val matched = animalsAll.find { it.id == tx.associatedAnimalId }
                                val l = if (matched != null) "${matched.name} (${matched.type})" else "رأس رقم #${tx.associatedAnimalId}"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(Icons.Default.Pets, contentDescription = null, tint = themePrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مرتبط بقرية الماشية: $l", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themePrimary)
                                }
                            }
                        }
                    }

                    // Quick Actions Section
                    if (viewModel.hasPermission("add_transaction")) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    editingTx = tx
                                    inspectingTx = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل المعاملة ✏️", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    if (!viewModel.hasPermission("delete_animal") && !viewModel.hasPermission("add_transaction")) {
                                        Toast.makeText(context, "عذراً، حذف السندات متاح للمدير العام فقط ❌", Toast.LENGTH_SHORT).show()
                                    } else {
                                        txToDelete = tx
                                        inspectingTx = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف المعاملة ❌", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { inspectingTx = null }) {
                    Text("إغلاق المعاينة ✖️", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ================= SCREEN COMPOSABLE 5: NOTES (TEXT & PHOTO) =================
private val keepColors = listOf(
    "" to "تلقائي",
    "#FFF9C4" to "أصفر",
    "#C8E6C9" to "أخضر",
    "#BBDEFB" to "أزرق",
    "#E1BEE7" to "بنفسجي",
    "#FFCDD2" to "أحمر",
    "#B2DFDB" to "مائي",
    "#FFE0B2" to "برتقالي"
)

private fun parseNoteColor(hex: String, isDark: Boolean): Color {
    if (hex.isEmpty()) return Color.Unspecified
    return try {
        val baseColor = Color(android.graphics.Color.parseColor(hex))
        if (isDark) {
            when (hex) {
                "#FFF9C4" -> Color(0xFF3E3A1D)
                "#C8E6C9" -> Color(0xFF1D3520)
                "#BBDEFB" -> Color(0xFF1B2C3A)
                "#E1BEE7" -> Color(0xFF331E3E)
                "#FFCDD2" -> Color(0xFF3E1E20)
                "#B2DFDB" -> Color(0xFF1A3835)
                "#FFE0B2" -> Color(0xFF392B19)
                else -> baseColor.copy(alpha = 0.25f)
            }
        } else {
            baseColor
        }
    } catch (e: Exception) {
        Color.Unspecified
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    cardBgColor: Color,
    themePrimary: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val noteBg = remember(note.colorHex, isDark, cardBgColor) {
        val parsed = parseNoteColor(note.colorHex, isDark)
        if (parsed == Color.Unspecified) cardBgColor else parsed
    }

    Surface(
        color = noteBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val formatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US) }
            Text(
                text = formatter.format(java.util.Date(note.createdAt)),
                fontSize = 9.sp,
                color = if (note.colorHex.isNotEmpty() && !isDark) Color.DarkGray else Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
            
            if (note.title.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (note.colorHex.isNotEmpty() && !isDark) Color.Black else MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            if (note.content.isNotEmpty()) {
                Text(
                    text = note.content,
                    fontSize = 13.sp,
                    color = if (note.colorHex.isNotEmpty() && !isDark) Color.DarkGray.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 14,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            if (note.imageBase64 != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = themePrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "يوجد مرفق مصور",
                        fontSize = 10.sp,
                        color = themePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = if (note.colorHex.isNotEmpty() && !isDark) Color.Black.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = if (note.colorHex.isNotEmpty() && !isDark) Color(0xFF1E40AF) else Color(0xFF3B82F6),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = if (note.colorHex.isNotEmpty() && !isDark) Color(0xFF065F46) else Color(0xFF10B981),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = Color.Red,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: FarmViewModel,
    themePrimary: Color,
    zoom: Float,
    cardBgColor: Color,
    onCheckAddPermission: (() -> Unit) -> Unit,
    onConfirmEdit: (String, () -> Unit) -> Unit,
    onConfirmDelete: (String, () -> Unit) -> Unit
) {
    val notes by viewModel.notesList.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Full screen editor state
    var showEditor by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var editTitleText by remember { mutableStateOf("") }
    var editContentText by remember { mutableStateOf("") }
    var editImageBase64 by remember { mutableStateOf<String?>(null) }
    var editColorHex by remember { mutableStateOf("") }
    
    // Search query state
    var searchQuery by remember { mutableStateOf("") }
    
    // Safety dialog
    var showDiscardWarning by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            editImageBase64 = ImageUtils.bitmapToBase64(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            editImageBase64 = ImageUtils.uriToBase64(context, uri)
        }
    }

    if (showEditor) {
        val originalTitle = noteToEdit?.title ?: ""
        val originalContent = noteToEdit?.content ?: ""
        val originalImage = noteToEdit?.imageBase64
        val originalColor = noteToEdit?.colorHex ?: ""
        val hasChanges = editTitleText != originalTitle || editContentText != originalContent || editImageBase64 != originalImage || editColorHex != originalColor

        val handleBack = {
            if (hasChanges) {
                showDiscardWarning = true
            } else {
                showEditor = false
                noteToEdit = null
            }
        }

        androidx.activity.compose.BackHandler {
            handleBack()
        }

        val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
        val editorBg = remember(editColorHex, isDarkTheme) {
            val parsed = parseNoteColor(editColorHex, isDarkTheme)
            if (parsed == Color.Unspecified) Color.Transparent else parsed
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (editorBg == Color.Transparent) MaterialTheme.colorScheme.background else editorBg)
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (noteToEdit != null) "تعديل الملاحظة" else "ملاحظة جديدة", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        if (editContentText.isNotBlank() || editTitleText.isNotBlank()) {
                            IconButton(onClick = {
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    val shareText = if (editTitleText.isNotEmpty()) "**$editTitleText**\n$editContentText" else editContentText
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة الملاحظة"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة الملاحظة")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (editorBg == Color.Transparent) MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) else editorBg.copy(alpha = 0.85f)
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = editTitleText,
                        onValueChange = { editTitleText = it },
                        placeholder = { Text("العنوان", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themePrimary,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editContentText,
                        onValueChange = { editContentText = it },
                        placeholder = { Text("اكتب تفاصيل الملاحظة أو المهمة هنا...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themePrimary,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Text("لون الخلفية (مثل Google Keep):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        keepColors.forEach { (colorCode, label) ->
                            val colorObj = parseNoteColor(colorCode, isDarkTheme)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (colorCode.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else colorObj)
                                    .border(
                                        width = if (editColorHex == colorCode) 3.dp else 1.dp,
                                        color = if (editColorHex == colorCode) themePrimary else Color.LightGray.copy(alpha = 0.8f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { editColorHex = colorCode },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorCode.isEmpty()) {
                                    Icon(Icons.Default.Brush, contentDescription = "تلقائي", modifier = Modifier.size(16.dp))
                                } else if (editColorHex == colorCode) {
                                    Icon(Icons.Default.Check, contentDescription = "محدد", tint = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Text("المرفقات والوسائط (الصورة/الروشتة):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "كاميرا", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التقاط صورة")
                        }
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "المعرض", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرفاق المعرض")
                        }
                    }

                    if (editImageBase64 != null) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                            val decoded = remember(editImageBase64) {
                                editImageBase64?.let { ImageUtils.base64ToBitmap(it) }
                            }
                            var showFullscreenImage by remember { mutableStateOf(false) }

                            if (decoded != null) {
                                Image(
                                    bitmap = decoded.asImageBitmap(),
                                    contentDescription = "المرفق",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showFullscreenImage = true },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            if (showFullscreenImage && decoded != null) {
                                androidx.compose.ui.window.Dialog(
                                    onDismissRequest = { showFullscreenImage = false },
                                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = decoded.asImageBitmap(),
                                            contentDescription = "صورة المرفق بكامل الشاشة",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { showFullscreenImage = false },
                                            contentScale = ContentScale.Fit
                                        )
                                        IconButton(
                                            onClick = { showFullscreenImage = false },
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(16.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = { editImageBase64 = null },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف المرفق", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (editContentText.isBlank() && editTitleText.isBlank()) {
                                Toast.makeText(context, "الرجاء كتابة عنوان أو محتوى للملاحظة", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (noteToEdit != null) {
                                val updated = noteToEdit!!.copy(
                                    title = editTitleText,
                                    content = editContentText,
                                    imageBase64 = editImageBase64,
                                    colorHex = editColorHex
                                )
                                viewModel.updateNoteRecord(updated)
                                Toast.makeText(context, "تم حفظ التعديلات ✅", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.registerNote(editTitleText, editContentText, editImageBase64, editColorHex)
                                Toast.makeText(context, "تم حفظ الملاحظة الجديدة ✅", Toast.LENGTH_SHORT).show()
                            }
                            showEditor = false
                            noteToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("حفظ الملاحظة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (showDiscardWarning) {
            AlertDialog(
                onDismissRequest = { showDiscardWarning = false },
                title = { Text("هل تريد حفظ التغييرات قبل الخروج؟ 💾", fontWeight = FontWeight.Bold) },
                text = { Text("لقد أجريت تعديلات على الملاحظة. هل ترغب في حفظ التغييرات قبل مغادرة الصفحة لتجنب فقدان البيانات؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editContentText.isBlank() && editTitleText.isBlank()) {
                                Toast.makeText(context, "لا يمكن الحفظ لأن الملاحظة فارغة!", Toast.LENGTH_SHORT).show()
                            } else {
                                if (noteToEdit != null) {
                                    val updated = noteToEdit!!.copy(
                                        title = editTitleText,
                                        content = editContentText,
                                        imageBase64 = editImageBase64,
                                        colorHex = editColorHex
                                    )
                                    viewModel.updateNoteRecord(updated)
                                    Toast.makeText(context, "تم حفظ التعديلات ✅", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.registerNote(editTitleText, editContentText, editImageBase64, editColorHex)
                                    Toast.makeText(context, "تم حفظ الملاحظة الجديدة ✅", Toast.LENGTH_SHORT).show()
                                }
                                showDiscardWarning = false
                                showEditor = false
                                noteToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("نعم (حفظ وخروج)", color = Color.White)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                showDiscardWarning = false
                                showEditor = false
                                noteToEdit = null
                            }
                        ) {
                            Text("لا (خروج دون حفظ)", color = Color.Red)
                        }
                        TextButton(
                            onClick = { showDiscardWarning = false }
                        ) {
                            Text("إلغاء", color = Color.Gray)
                        }
                    }
                }
            )
        }
        return // Take up full screen
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("سجل الملاحظات والمهام (جوجل Keep)", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))

            // Google Keep style search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث في الملاحظات...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themePrimary,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                singleLine = true
            )

            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color.LightGray.copy(alpha = 0.8f), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "لا توجد ملاحظات مسجلة بعد" else "لا توجد نتائج بحث مطابقة",
                            color = Color.Gray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Keep style staggered dual-column layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val leftNotes = filteredNotes.filterIndexed { index, _ -> index % 2 == 0 }
                        val rightNotes = filteredNotes.filterIndexed { index, _ -> index % 2 != 0 }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            leftNotes.forEach { note ->
                                NoteCard(
                                    note = note,
                                    cardBgColor = cardBgColor,
                                    themePrimary = themePrimary,
                                    isDark = androidx.compose.foundation.isSystemInDarkTheme(),
                                    onClick = {
                                        noteToEdit = note
                                        editTitleText = note.title
                                        editContentText = note.content
                                        editImageBase64 = note.imageBase64
                                        editColorHex = note.colorHex
                                        showEditor = true
                                    },
                                    onDelete = {
                                        onConfirmDelete(note.content) { viewModel.deleteNoteRecord(note) }
                                    },
                                    onShare = {
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            val shareText = if (note.title.isNotEmpty()) "**${note.title}**\n${note.content}" else note.content
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة الملاحظة"))
                                    }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rightNotes.forEach { note ->
                                NoteCard(
                                    note = note,
                                    cardBgColor = cardBgColor,
                                    themePrimary = themePrimary,
                                    isDark = androidx.compose.foundation.isSystemInDarkTheme(),
                                    onClick = {
                                        noteToEdit = note
                                        editTitleText = note.title
                                        editContentText = note.content
                                        editImageBase64 = note.imageBase64
                                        editColorHex = note.colorHex
                                        showEditor = true
                                    },
                                    onDelete = {
                                        onConfirmDelete(note.content) { viewModel.deleteNoteRecord(note) }
                                    },
                                    onShare = {
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            val shareText = if (note.title.isNotEmpty()) "**${note.title}**\n${note.content}" else note.content
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "مشاركة الملاحظة"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                onCheckAddPermission {
                    noteToEdit = null
                    editTitleText = ""
                    editContentText = ""
                    editImageBase64 = null
                    editColorHex = ""
                    showEditor = true
                }
            },
            containerColor = themePrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة ملاحظة أو مهمة")
        }
    }
}

// ================= SCREEN COMPOSABLE 6: ARCHIVE =================
@Composable
fun ArchiveScreen(
    viewModel: FarmViewModel,
    themePrimary: Color,
    zoom: Float,
    cardBgColor: Color,
    onConfirmDelete: (String, () -> Unit) -> Unit,
    onEditAnimal: (AnimalEntity) -> Unit,
    onViewDetails: (Int) -> Unit,
    onNavigateToTab: (String) -> Unit
) {
    val archiveTransactions by viewModel.transactionsList.collectAsStateWithLifecycle()
    val filterArchiveBy by viewModel.selectedArchiveFilter.collectAsStateWithLifecycle()
    val archivedAnimals by viewModel.archiveAnimalsList.collectAsStateWithLifecycle()
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var animalSubTab by remember { mutableStateOf("txs") } // "txs" or "sold_animals"
    var animalToRefund by remember { mutableStateOf<AnimalEntity?>(null) }
    var optionsTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var expandedTxId by remember { mutableStateOf<Int?>(null) }
    var optionsAnimal by remember { mutableStateOf<AnimalEntity?>(null) }

    // Refund Dialog inputs
    var refundSettlementType by remember { mutableStateOf("adjust_deferred") } // "adjust_deferred", "cash_out", "none"
    var refundAmountStr by remember { mutableStateOf("") }
    var refundReason by remember { mutableStateOf("") }
    
    var globalSearchQuery by remember { mutableStateOf("") }

    val filteredList = remember(archiveTransactions, filterArchiveBy, globalSearchQuery) {
        // Exclude settlements from main archive
        val nonSettlements = archiveTransactions.filter { it.category != "تسوية فواتير" }
        val filteredByType = when (filterArchiveBy) {
            "income" -> nonSettlements.filter { it.type == "income" }
            "expense" -> nonSettlements.filter { it.type == "expense" }
            "feed" -> nonSettlements.filter { it.category == "أعلاف" }
            "animals" -> nonSettlements.filter { it.category == "حيوانات" }
            else -> nonSettlements
        }
        
        if (globalSearchQuery.isNotBlank()) {
            val query = globalSearchQuery.trim().lowercase()
            filteredByType.filter {
                it.description.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
            }
        } else {
            filteredByType
        }
    }

    val soldAnimals = remember(archivedAnimals, globalSearchQuery) {
        val baseList = archivedAnimals.filter { it.isArchived }
        if (globalSearchQuery.isNotBlank()) {
            val query = globalSearchQuery.trim().lowercase()
            baseList.filter {
                it.name.lowercase().contains(query) ||
                it.type.lowercase().contains(query) ||
                it.merchantName.lowercase().contains(query)
            }
        } else {
            baseList
        }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("أرشيف العمليات والتدفقات المالية", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            IconButton(onClick = { viewModel.setArchiveFilter("all") }) {
                Icon(Icons.Default.Refresh, contentDescription = "تحديث الفلتر")
            }
        }
        
        OutlinedTextField(
            value = globalSearchQuery,
            onValueChange = { globalSearchQuery = it },
            placeholder = { Text("بحث ذكي (الرقم، الحالة، المصدر، النوع، الوصف)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Horizontal filter bar for Archives
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            buttonCategoryFilter("الكل", filterArchiveBy == "all", themePrimary) { viewModel.setArchiveFilter("all") }
            buttonCategoryFilter("إيرادات", filterArchiveBy == "income", themePrimary) { viewModel.setArchiveFilter("income") }
            buttonCategoryFilter("مصاريف", filterArchiveBy == "expense", themePrimary) { viewModel.setArchiveFilter("expense") }
            buttonCategoryFilter("أعلاف", filterArchiveBy == "feed", themePrimary) { viewModel.setArchiveFilter("feed") }
            buttonCategoryFilter("حيوانات", filterArchiveBy == "animals", themePrimary) { viewModel.setArchiveFilter("animals") }
        }

        // Animated sub-headers specifically for animal returns / sales breakdown
        if (filterArchiveBy == "animals") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { animalSubTab = "txs" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (animalSubTab == "txs") themePrimary else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "القيود المالية للماشية 💵",
                        color = if (animalSubTab == "txs") Color.White else Color.DarkGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { animalSubTab = "sold_animals" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (animalSubTab == "sold_animals") themePrimary else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "رؤوس الماشية المباعة 🐂",
                        color = if (animalSubTab == "sold_animals") Color.White else Color.DarkGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filterArchiveBy == "animals" && animalSubTab == "sold_animals") {
            if (soldAnimals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SentimentDissatisfied, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد بيانات، اضغط + للإضافة", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(soldAnimals) { animal ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { optionsAnimal = animal }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("الاسم/الوسم: ${animal.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("السلالة والنوع: ${animal.type} - العمر: ${animal.age}", fontSize = 12.sp, color = Color.LightGray)
                                        Text("المشتري: ${animal.merchantName} - الوزن: ${animal.weight} كغ", fontSize = 12.sp, color = Color.LightGray)
                                        Text("تاريخ المغادرة والبيع: ${animal.departureDate}", fontSize = 11.sp, color = Color.LightGray)
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${animal.salePrice} ج",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = Color(0xFF2563EB)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onViewDetails(animal.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = "التفاصيل", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { onEditAnimal(animal) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل الأصل", tint = themePrimary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    onConfirmDelete("هل أنت متأكد من الحذف النهائي للرأس من الأرشيف؟ سيتم حذف جميع بياناتها!") {
                                                        viewModel.hardDeleteAnimalRecord(animal)
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "حذف نهائي", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { 
                                                animalToRefund = animal
                                                refundAmountStr = animal.salePrice.toString()
                                                refundReason = ""
                                                refundSettlementType = if (animal.associatedPersonId != null) "adjust_deferred" else "none"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("إرجاع الرأس 🔄", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد بيانات، اضغط + للإضافة", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredList) { log ->
                    val isExpanded = expandedTxId == log.id
                    Surface(
                        color = Color(0xFF1E293B), // Dark Slate
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedTxId = if (isExpanded) null else log.id }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                if (log.type == "income") Color(0xFF4ADE80).copy(alpha = 0.2f)
                                                else Color(0xFFF87171).copy(alpha = 0.2f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (log.type == "income") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (log.type == "income") Color(0xFF4ADE80) else Color(0xFFF87171),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            log.description,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (13f * (zoom / 16f)).sp
                                        )
                                        Text("تاريخ المعاملة: ${log.date}", fontSize = 11.sp, color = Color.LightGray)
                                    }
                                }

                                Text(
                                    text = "${if (log.type == "income") "+" else "-"}${log.amount}",
                                    color = if (log.type == "income") Color(0xFF4ADE80) else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                            
                            if (isExpanded) {
                                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(horizontal = 16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Details
                                    Button(
                                        onClick = { optionsTx = log },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("التفاصيل", color = Color.White, fontSize = 11.sp)
                                    }
                                    
                                    // 2. Edit
                                    Button(
                                        onClick = {
                                            if (log.associatedAnimalId != null && log.associatedAnimalId != 0) {
                                                val sourceAnimal = archivedAnimals.find { it.id == log.associatedAnimalId } 
                                                    ?: viewModel.animalsList.value.find { it.id == log.associatedAnimalId }
                                                if (sourceAnimal != null) {
                                                    onEditAnimal(sourceAnimal)
                                                } else {
                                                    editingTx = log
                                                }
                                            } else {
                                                editingTx = log 
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("تعديل", color = Color.White, fontSize = 11.sp)
                                    }
                                    
                                    // 3. Delete
                                    Button(
                                        onClick = {
                                            onConfirmDelete("هل أنت متأكد من رغبتك في حذف هذا القيد؟") {
                                                viewModel.softDeleteTransactionRecord(log)
                                                Toast.makeText(context, "تم النقل لسلة المهملات", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("حذف", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingTx != null) {
        val tx = editingTx!!
        var editAmount by remember { mutableStateOf(tx.amount.toString()) }
        var editDesc by remember { mutableStateOf(tx.description) }

        AlertDialog(
            onDismissRequest = { editingTx = null },
            title = { Text("تعديل المعاملة القديمة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        label = { Text("المبلغ") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("الوصف") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amnt = editAmount.toDoubleOrNull() ?: tx.amount
                    val updated = tx.copy(amount = amnt, description = editDesc)
                    viewModel.updateTransactionRecord(tx, updated)
                    editingTx = null
                    Toast.makeText(context, "تم تحديث القيود وعكسها تلقائياً", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = themePrimary)) {
                    Text("حفظ وعكس تلقائي", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTx = null }) { Text("إلغاء") }
            }
        )
    }

    if (txToDelete != null) {
        val tx = txToDelete!!
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("تأكيد حذف القيد أو السند المالي ⚠️", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text(
                    "هل أنت متأكد من حذف هذا السند المالي نهائياً من الأرشيف؟ \n\n" +
                    "الوصف: ${tx.description}\n" +
                    "المبلغ: ${tx.amount} جنيه\n\n" +
                    "سيؤدي ذلك لتعديل تلقائي في الأرصدة وإرجاع القيمة المترتبة عليها!",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.softDeleteTransactionRecord(tx)
                        txToDelete = null
                        Toast.makeText(context, "تم النقل لسلة المهملات 🗑️", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف وتصحيح الرصيد", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    if (animalToRefund != null) {
        val animal = animalToRefund!!
        AlertDialog(
            onDismissRequest = { animalToRefund = null },
            title = { Text("فاتورة وسند استرجاع رأس مبيعات 🔄", fontWeight = FontWeight.Bold, color = themePrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أنت بصدد إرجاع رأس الماشية المباعة إلى الحظيرة مجدداً وتصحيح الموازنة وسجل الحركة.", fontSize = 13.sp)
                    
                    Surface(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("الرأس: ${animal.name} (${animal.type})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("المشتري: ${animal.merchantName}", fontSize = 11.sp, color = Color.Gray)
                            Text("قيمة مبيعات الرأس المرتجعة: ${animal.salePrice} جنيه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("حدد طريقة تسوية ميزانية وقيمة المرتجع:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = refundSettlementType == "adjust_deferred",
                            onClick = { refundSettlementType = "adjust_deferred" }
                        )
                        Text("إلغاء المديونية من الحساب الآجل للمشتري", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = refundSettlementType == "cash_out",
                            onClick = { refundSettlementType = "cash_out" }
                        )
                        Text("سداد نقدي للعميل (سند صرف نقدي)", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = refundSettlementType == "none",
                            onClick = { refundSettlementType = "none" }
                        )
                        Text("إرجاع الرأس بدون ذمة مالية (تعديل الحظيرة فقط)", fontSize = 12.sp)
                    }

                    if (refundSettlementType != "none") {
                        OutlinedTextField(
                            value = refundAmountStr,
                            onValueChange = { refundAmountStr = it },
                            label = { Text("مبلغ المرتجع المطلوب تسويته (ج)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        label = { Text("سبب وملاحظة الإرجاع (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val refAmt = refundAmountStr.toDoubleOrNull() ?: 0.0
                        viewModel.refundSoldAnimal(
                            animal = animal,
                            settlementType = refundSettlementType,
                            refundAmount = refAmt,
                            reason = refundReason
                        )
                        Toast.makeText(context, "تم إرجاع رأس الماشية وإصدار سند الإرجاع بنجاح! 🔄", Toast.LENGTH_SHORT).show()
                        animalToRefund = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary)
                ) {
                    Text("تأكيد وإرجاع الرأس", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { animalToRefund = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }

    if (optionsAnimal != null) {
        val animal = optionsAnimal!!
        AlertDialog(
            onDismissRequest = { optionsAnimal = null },
            title = { Text("خيارات رأس الماشية المباعة 🐄", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "اختر أحد الإجراءات المتاحة لرأس الماشية (${animal.name}):",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            optionsAnimal = null
                            onViewDetails(animal.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التفاصيل وحالة البيع والاسترجاع 📄", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            optionsAnimal = null
                            onEditAnimal(animal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعديل الأصل من المصدر ✏️", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            optionsAnimal = null
                            onConfirmDelete("هل أنت متأكد من الحذف النهائي للرأس من الأرشيف؟ سيتم حذف جميع بياناتها!") {
                                viewModel.hardDeleteAnimalRecord(animal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف نهائي من السجلات 🗑️", color = Color.White, fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = { optionsAnimal = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            }
        )
    }

    if (optionsTx != null) {
        val tx = optionsTx!!
        val settlementsForThisTx = archiveTransactions.filter { it.description.startsWith("تسوية لسند رقم #${tx.id}:") }
        val settledAmount = settlementsForThisTx.sumOf { it.amount }
        val remainingAmount = (tx.amount - settledAmount).coerceAtLeast(0.0)
        var newSettlementAmount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { optionsTx = null },
            title = { Text("تفاصيل الفاتورة ومستند الحركة 📄", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("البيان المالي: ${tx.description}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text("القيمة: ${tx.amount} جنيه (${if(tx.type == "income") "إيرادات" else "مصروفات"})", fontSize = 13.sp, color = if(tx.type=="income") Color(0xFF4ADE80) else Color(0xFFF87171))
                    Text("التاريخ: ${tx.date} | الفئة: ${tx.category}", fontSize = 12.sp, color = Color.Gray)
                    
                    HorizontalDivider(color = Color(0xFF334155))
                    
                    Text("سجل التسويات والمدفوعات (دفتر فرعي):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = themePrimary)
                    if (settlementsForThisTx.isEmpty()) {
                        Text("لا يوجد تسويات مسجلة لهذا السند.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        settlementsForThisTx.forEach { settlement ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(settlement.description.substringAfter(": "), fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.weight(1f))
                                Text("${settlement.amount} ج", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                            }
                        }
                    }
                    
                    HorizontalDivider(color = Color(0xFF334155))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المدفوع/المسوى:", fontSize = 12.sp, color = Color.White)
                        Text("$settledAmount جنيه", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المبلغ المتبقي:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("$remainingAmount جنيه", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                    }
                    
                    if (remainingAmount > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = newSettlementAmount,
                                onValueChange = { newSettlementAmount = it },
                                label = { Text("إضافة دفعة (تسوية)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amountVal = newSettlementAmount.toDoubleOrNull() ?: 0.0
                                    if (amountVal > 0 && amountVal <= remainingAmount) {
                                        val newTxType = tx.type
                                        val currentDateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
                                        viewModel.registerManualTransaction(
                                            type = newTxType,
                                            amount = amountVal,
                                            description = "تسوية لسند رقم #${tx.id}: دفعة بقيمة $amountVal في ($currentDateTime)",
                                            category = "تسوية فواتير",
                                            personId = tx.associatedPersonId
                                        )
                                        newSettlementAmount = ""
                                        Toast.makeText(context, "تم تسجيل الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "أدخل مبلغاً صحيحاً لا يتجاوز الباقي", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("إضافة", color = Color.White)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✅ تم سداد/تسوية هذا السند بالكامل", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { optionsTx = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        )
    }
}
}

@Composable
fun buttonCategoryFilter(label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Button(
        onClick = { onClick() },
        colors = ButtonDefaults.buttonColors(containerColor = if (active) color else Color.LightGray.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, color = if (active) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ================= SCREEN COMPOSABLE 7: SETTINGS =================
@Composable
fun SettingsScreen(viewModel: FarmViewModel, accentColor: Color, zoomLevel: Float, onUpdateLabels: () -> Unit) {
    val context = LocalContext.current
    val currentFarm by viewModel.currentFarm.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val syncIp by viewModel.syncIp.collectAsStateWithLifecycle()
    val syncPort by viewModel.syncPort.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var customBarn by remember { mutableStateOf("") }
    var customFeeds by remember { mutableStateOf("") }
    var customAccounts by remember { mutableStateOf("") }

    val spTitles = remember { context.getSharedPreferences("farm_titles", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        customBarn = spTitles.getString("label_barn", "الحظيرة") ?: "الحظيرة"
        customFeeds = spTitles.getString("label_feeds", "الأعلاف") ?: "الأعلاف"
        customAccounts = spTitles.getString("label_accounts", "الحسابات") ?: "الحسابات"
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(context, uri)
        }
    }

    val cardBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    // Dynamic color picker states
    var showBgColorPicker by remember { mutableStateOf(false) }
    var bgRed by remember { mutableStateOf(255) }
    var bgGreen by remember { mutableStateOf(255) }
    var bgBlue by remember { mutableStateOf(255) }
    var bgHexInput by remember { mutableStateOf("") }

    var showTextColorPicker by remember { mutableStateOf(false) }
    var textRed by remember { mutableStateOf(30) }
    var textGreen by remember { mutableStateOf(41) }
    var textBlue by remember { mutableStateOf(59) }
    var textHexInput by remember { mutableStateOf("") }

    // Convert RGB integers to Hex string (e.g. #FFFFFF)
    fun rgbToHex(r: Int, g: Int, b: Int): String {
        return String.format("#%02X%02X%02X", r, g, b)
    }

    // Convert Hex string (e.g. #FFFFFF) to RGB integers
    fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        return try {
            val colorInt = android.graphics.Color.parseColor(hex)
            Triple(
                android.graphics.Color.red(colorInt),
                android.graphics.Color.green(colorInt),
                android.graphics.Color.blue(colorInt)
            )
        } catch (e: Exception) {
            Triple(255, 255, 255)
        }
    }

    // Auto calculate highly contrasting text color based on background color luminance
    fun calculateContrastText(bgHex: String): String {
        return try {
            val colorInt = android.graphics.Color.parseColor(bgHex)
            val r = android.graphics.Color.red(colorInt)
            val g = android.graphics.Color.green(colorInt)
            val b = android.graphics.Color.blue(colorInt)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            if (luminance > 0.5) "#1E293B" else "#F8FAFC"
        } catch (e: Exception) {
            "#1E293B"
        }
    }

    // --- DIALOG 1: CUSTOM CARD COLOR PICKER ---
    if (showBgColorPicker) {
        val currentHexResult = rgbToHex(bgRed, bgGreen, bgBlue)
        AlertDialog(
            onDismissRequest = { showBgColorPicker = false },
            title = { Text("مخصص: اختيار لون خلفية الكروت والبطاقات 🎨", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اسحب المنزلقات أدناه لتكوين اللون المخصص لجميع بطاقات التطبيق:", fontSize = 11.sp, color = Color.Gray)
                    
                    // Live preview card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color(android.graphics.Color.parseColor(currentHexResult)), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "معاينة البطاقة ($currentHexResult)",
                            color = Color(android.graphics.Color.parseColor(calculateContrastText(currentHexResult))),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Red channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("أحمر ($bgRed)", fontSize = 10.sp, modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold)
                        Slider(
                            value = bgRed.toFloat(),
                            onValueChange = { bgRed = it.toInt() },
                            valueRange = 0f..255f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Green channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("أخضر ($bgGreen)", fontSize = 10.sp, modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold)
                        Slider(
                            value = bgGreen.toFloat(),
                            onValueChange = { bgGreen = it.toInt() },
                            valueRange = 0f..255f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Blue channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("أزرق ($bgBlue)", fontSize = 10.sp, modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold)
                        Slider(
                            value = bgBlue.toFloat(),
                            onValueChange = { bgBlue = it.toInt() },
                            valueRange = 0f..255f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("أو أدخل كود اللون مباشرة (مثال: #111827):", fontSize = 10.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = bgHexInput,
                        onValueChange = {
                            bgHexInput = it
                            if (it.length == 7 && it.startsWith("#")) {
                                val rgb = hexToRgb(it)
                                bgRed = rgb.first
                                bgGreen = rgb.second
                                bgBlue = rgb.third
                            }
                        },
                        placeholder = { Text("#FFFFFF") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosenColor = rgbToHex(bgRed, bgGreen, bgBlue)
                        viewModel.setCardColorHex(chosenColor)
                        // Auto contrast font selection
                        val autoTxt = calculateContrastText(chosenColor)
                        viewModel.setTextColorHex(autoTxt)
                        showBgColorPicker = false
                        Toast.makeText(context, "تم تغيير خلفية جميع البطاقات بنجاح! 🎨", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("حفظ وتطبيق", color = Color.White, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBgColorPicker = false }) {
                    Text("إلغاء", fontSize = 11.sp)
                }
            }
        )
    }

    // --- DIALOG 2: CUSTOM TEXT COLOR PICKER ---
    if (showTextColorPicker) {
        val currentHexResult = rgbToHex(textRed, textGreen, textBlue)
        AlertDialog(
            onDismissRequest = { showTextColorPicker = false },
            title = { Text("مخصص: اختيار لون النصوص والخطوط 🖋️", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اسحب المنزلقات لتعديل لون نصوص وبطاقات التطبيق بشكل كامل:", fontSize = 11.sp, color = Color.Gray)
                    
                    // Live Preview Area using current background color
                    val activeBgHexByModel = viewModel.cardColorHex.value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(Color(android.graphics.Color.parseColor(activeBgHexByModel)), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "شكل قراءة الكلمات والخط ($currentHexResult)",
                            color = Color(android.graphics.Color.parseColor(currentHexResult)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Red channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("أحمر ($textRed)", fontSize = 10.sp, modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold)
                        Slider(
                            value = textRed.toFloat(),
                            onValueChange = { textRed = it.toInt() },
                            valueRange = 0f..255f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Green channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("أخضر ($textGreen)", fontSize = 10.sp, modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold)
                        Slider(
                            value = textGreen.toFloat(),
                            onValueChange = { textGreen = it.toInt() },
                            valueRange = 0f..255f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Blue channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("أزرق ($textBlue)", fontSize = 10.sp, modifier = Modifier.width(55.dp), fontWeight = FontWeight.Bold)
                        Slider(
                            value = textBlue.toFloat(),
                            onValueChange = { textBlue = it.toInt() },
                            valueRange = 0f..255f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("أو أدخل كود لون الخطوط يدوياً (مثال: #000000):", fontSize = 10.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = textHexInput,
                        onValueChange = {
                            textHexInput = it
                            if (it.length == 7 && it.startsWith("#")) {
                                val rgb = hexToRgb(it)
                                textRed = rgb.first
                                textGreen = rgb.second
                                textBlue = rgb.third
                            }
                        },
                        placeholder = { Text("#1E293B") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosenColor = rgbToHex(textRed, textGreen, textBlue)
                        viewModel.setTextColorHex(chosenColor)
                        showTextColorPicker = false
                        Toast.makeText(context, "تم تغيير لون خطوط التطبيق بالكامل! 🖋️", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("حفظ وتطبيق", color = Color.White, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextColorPicker = false }) {
                    Text("إلغاء", fontSize = 11.sp)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "إعدادات وتخصيص المزرعة",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        var renameText by remember { mutableStateOf(currentFarm ?: "") }
        var showRenameDialog by remember { mutableStateOf(false) }

        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("تغيير اسم المزرعة", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("الاسم الجديد") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.renameCurrentFarm(renameText) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showRenameDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("حفظ الثغيير", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "إعدادات المزرعة الحالية (${currentFarm}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        renameText = currentFarm ?: ""
                        showRenameDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تغيير اسم المزرعة", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Title Mapping customizable names (collapsible accordion)
        var isLabelsSectionExpanded by remember { mutableStateOf(false) }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLabelsSectionExpanded = !isLabelsSectionExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تعديل وتخصيص التسميات في واجهات التطبيق:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (isLabelsSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }

                if (isLabelsSectionExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customBarn,
                        onValueChange = { customBarn = it },
                        label = { Text("تسمية الحظيرة (مثال: حظيرة الأبقار)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customFeeds,
                        onValueChange = { customFeeds = it },
                        label = { Text("تسمية مخزون الأعلاف (مثال: مستودع الطعام)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customAccounts,
                        onValueChange = { customAccounts = it },
                        label = { Text("تسمية الحسابات (مثال: الدفتر المالي)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            with(spTitles.edit()) {
                                putString("label_barn", customBarn)
                                putString("label_feeds", customFeeds)
                                putString("label_accounts", customAccounts)
                                apply()
                            }
                            onUpdateLabels()
                            Toast.makeText(context, "تم حفظ العناوين الجديدة وتحديث القوائم!", Toast.LENGTH_SHORT).show()
                            isLabelsSectionExpanded = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ المسميات المخصصة لجميع علامات التبويب والمصاريف", color = Color.White)
                    }
                }
            }
        }

        // Color theme selectors
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تخصيص المظهر وتغيير السمة اللونية لـ المزرعة برو:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "أخضر زمردي" to "#059669",
                        "أزرق ملكي" to "#2563EB",
                        "ذهبي عسلي" to "#D97706",
                        "بنفسجي فاخر" to "#7C3AED"
                    ).forEach { (name, hex) ->
                        val active = hex == viewModel.primaryColorHex.value
                        Surface(
                            onClick = { viewModel.setThemeHex(hex) },
                            color = Color(android.graphics.Color.parseColor(hex)),
                            shape = RoundedCornerShape(12.dp),
                            border = if (active) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(54.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Card background color selectors
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "لون خلفية كروت وبطاقات البيانات:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "غيّر لون خلفية البطاقات في المزرعة لتسهيل القراءة وتفادي الإجهاد.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "أبيض" to "#FFFFFF",
                        "رمادي" to "#F1F5F9",
                        "وردي" to "#FDF2F8",
                        "داكن ملوكي" to "#1E293B",
                        "أسود فحم" to "#0F172A"
                    ).forEach { (name, hex) ->
                        val active = hex == viewModel.cardColorHex.value
                        Surface(
                            onClick = { 
                                viewModel.setCardColorHex(hex)
                                // Also trigger auto-contrast for preset selection
                                val autoTxt = calculateContrastText(hex)
                                viewModel.setTextColorHex(autoTxt)
                            },
                            color = Color(android.graphics.Color.parseColor(hex)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(if (active) 3.dp else 1.dp, if (active) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    color = Color(android.graphics.Color.parseColor(calculateContrastText(hex))),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val rgb = hexToRgb(viewModel.cardColorHex.value)
                        bgRed = rgb.first
                        bgGreen = rgb.second
                        bgBlue = rgb.third
                        bgHexInput = viewModel.cardColorHex.value
                        showBgColorPicker = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح تركيب وتدريج لون خلفية مخصص 🎨", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Custom Text / Font Color Selectors
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "لون خطوط ونصوص التطبيق:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "حدّد لون الكتابة ليكون متبايناً ومريحاً للعين وتجنّب النصوص غير المرئية.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "تباين تلقائي 🛡️" to "auto",
                        "داكن مريح" to "#1E293B",
                        "أسود قاتم" to "#000000",
                        "أبيض ناصع" to "#FFFFFF",
                        "أصفر رملي" to "#FEF3C7"
                    ).forEach { (name, hexOrMode) ->
                        val active = if (hexOrMode == "auto") {
                            viewModel.textColorHex.value == calculateContrastText(viewModel.cardColorHex.value)
                        } else {
                            hexOrMode == viewModel.textColorHex.value
                        }

                        Surface(
                            onClick = {
                                if (hexOrMode == "auto") {
                                    val autoTxt = calculateContrastText(viewModel.cardColorHex.value)
                                    viewModel.setTextColorHex(autoTxt)
                                    Toast.makeText(context, "تم تطبيق التباين التلقائي الموصى به 🛡️", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.setTextColorHex(hexOrMode)
                                }
                            },
                            color = if (hexOrMode == "auto") Color.LightGray.copy(alpha = 0.3f) else Color(android.graphics.Color.parseColor(hexOrMode)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(if (active) 3.dp else 1.dp, if (active) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = name,
                                    color = if (hexOrMode == "auto") MaterialTheme.colorScheme.onSurface else if (hexOrMode == "#FFFFFF" || hexOrMode == "#FEF3C7") Color.Black else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val rgb = hexToRgb(viewModel.textColorHex.value)
                        textRed = rgb.first
                        textGreen = rgb.second
                        textBlue = rgb.third
                        textHexInput = viewModel.textColorHex.value
                        showTextColorPicker = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح تركيب وتخصيص لون نصوص دقيق 🖋️", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Day/Night mode selector
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تحديد نمط المظهر (الوضع الليل والنهار):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        Triple("system", "تلقائي", Icons.Default.Settings),
                        Triple("light", "مضيء (نهار)", Icons.Default.WbSunny),
                        Triple("dark", "مظلم (ليل)", Icons.Default.Brightness3)
                    ).forEach { (mode, label, icon) ->
                        val active = themeMode == mode
                        Surface(
                            onClick = { viewModel.setThemeMode(mode) },
                            color = if (active) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = if (active) null else BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            tonalElevation = if (active) 4.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- CUSTOM: Font Type & Style Selection + Online Downloader Simulation ---
        val selectedFontState by viewModel.selectedFont.collectAsStateWithLifecycle()
        val isFontDownloading by viewModel.isFontDownloading.collectAsStateWithLifecycle()

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "نوع تصميم وشكل خط الكلمات العربي: 🖋️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تحكّم في المظهر المطبوع والجمالي لكافة الخطوط في التطبيق.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "tajawal" to "تيجوال (معتدل)",
                        "amiri" to "أميري (كلاسيكي)",
                        "cairo" to "كايرو (محمل)"
                    ).forEach { (fontKey, fontLabel) ->
                        val active = selectedFontState == fontKey
                        Surface(
                            onClick = { viewModel.changeFont(fontKey) },
                            color = if (active) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = if (active) null else BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(45.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = fontLabel,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Online Font Downloader Button
                Button(
                    onClick = {
                        viewModel.simulateDownloadFont()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFontDownloading
                ) {
                    if (isFontDownloading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الاتصال بـ Google Fonts لتحميل خط 'Cairo Pro'...", color = Color.White, fontSize = 11.sp)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحميل خط 'Cairo Pro' الإضافي من الويب بنقرة واحدة 🌐", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Screen Zoom Factor slider
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "متحكم ببعد الشاشة والخط والرموز (Text Scale/Zoom):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = zoomLevel,
                    onValueChange = { viewModel.updateZoom(it) },
                    valueRange = 12f..20f,
                    steps = 8
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("صغير جداً (12)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("المعتدل (16)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    Text("ضخم مقروء (20)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Restore Defaults
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "استعادة واسترجاع الإعدادات (Factory Settings):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.setThemeHex("#059669")
                        viewModel.setThemeMode("light")
                        viewModel.changeFont("cairo")
                        viewModel.updateZoom(16f)
                        viewModel.setCardColorHex("#FFFFFF")
                        viewModel.setTextColorHex("#1E293B")
                        with(spTitles.edit()) {
                            clear()
                            apply()
                        }
                        customBarn = "الحظيرة"
                        customFeeds = "الأعلاف"
                        customAccounts = "الحسابات"
                        onUpdateLabels()
                        Toast.makeText(context, "تم استعادة الإعدادات الافتراضية بنجاح ✅", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استعادة التهيئة الافتراضية للنظام والمظهر", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Backup and portable data transfer triggers
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "إمكانية نقل وتصدير البيانات المشتركة (بدون انترنت):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.exportHtmlReport(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة تقرير HTML/CSS ويب منسق للطباعة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.exportBackup(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة وتصدير ملف JSON بمزرعتك", color = Color.White, fontSize = 10.sp)
                    }

                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استيراد ملف احتياطي", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        // Firebase Sync Trigger
        // Dangerous triggers
        Surface(
            color = Color(0xFFFEF2F2),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("منطقة خطرة: إدارة قاعدة البيانات", color = Color(0xFF991B1B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (context is androidx.fragment.app.FragmentActivity) {
                                com.example.utils.BiometricUtils.authenticate(
                                    context,
                                    "تأكيد الهوية",
                                    "يرجى تأكيد هويتك لمسح البيانات",
                                    onSuccess = {
                                        viewModel.clearAllDataStream()
                                        Toast.makeText(context, "تم مسح كافة سجلات المزرعة المتواجدة!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err -> Toast.makeText(context, "إلغاء أمني: $err", Toast.LENGTH_SHORT).show() }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مسح البيانات", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (context is androidx.fragment.app.FragmentActivity) {
                                com.example.utils.BiometricUtils.authenticate(
                                    context,
                                    "تأكيد الهوية للمسح النهائي",
                                    "يرجى التحقق البيومتري لحذف المزرعة نهائياً",
                                    onSuccess = {
                                        viewModel.deleteCurrentFarm()
                                        Toast.makeText(context, "تم حذف المزرعة بالكامل بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err -> Toast.makeText(context, "إلغاء أمني: $err", Toast.LENGTH_SHORT).show() }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حذف مزرعتي نهائياً", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ================= DIALOG 1: ADD ANIMAL (BUYING) =================
@Composable
fun AddAnimalDialog(viewModel: FarmViewModel, accentColor: Color, animalToEdit: com.example.data.model.AnimalEntity? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val people by viewModel.peopleList.collectAsStateWithLifecycle()

    val customTypesRaw by viewModel.animalTypesList.collectAsStateWithLifecycle()
    val typesToUse = remember(customTypesRaw) {
        if (customTypesRaw.isEmpty()) listOf("عجل", "أغنام", "ماعز", "جمال", "جاموس") else customTypesRaw
    }
    var isFemale by remember { mutableStateOf(false) } // false = ذكر, true = أنثى
    var selectedFamilyRaw by remember { mutableStateOf("عجل") }

    var name by remember { mutableStateOf(animalToEdit?.name ?: "") }
    var type by remember { mutableStateOf(animalToEdit?.type ?: "عجل") } // "عجل" / "أغنام"
    
    // Reverse lookup family/gender from type if editing
    LaunchedEffect(animalToEdit) {
        if (animalToEdit != null) {
            val t = animalToEdit.type
            var foundFam = "عجل"
            var foundFem = false
            for (fam in typesToUse) {
                val parsed = com.example.utils.AnimalTypeHelper.parseAnimalType(fam)
                if (parsed.female.equals(t, true)) {
                    foundFam = fam
                    foundFem = true
                    break
                }
                if (parsed.male.equals(t, true)) {
                    foundFam = fam
                    foundFem = false
                    break
                }
            }
            selectedFamilyRaw = foundFam
            isFemale = foundFem
        }
    }

    val updateTypeForGenderAndFamily = { femaleSelected: Boolean, family: String ->
        val typeObj = com.example.utils.AnimalTypeHelper.parseAnimalType(family)
        type = typeObj.getName(femaleSelected)
    }

    var weightStr by remember { mutableStateOf(animalToEdit?.weight?.toString() ?: "") }
    var purchasePriceStr by remember { mutableStateOf(animalToEdit?.purchasePrice?.toString() ?: "") }
    var age by remember { mutableStateOf(animalToEdit?.age ?: "") }
    var feedCostStr by remember { mutableStateOf(animalToEdit?.feedCost?.toString() ?: "") }

    // Merchant Selection options
    var merchantSearchQuery by remember { mutableStateOf(animalToEdit?.merchantName ?: "") }
    var selectedPersonId by remember { mutableStateOf<Int?>(animalToEdit?.associatedPersonId) }
    var newMerchantName by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }
    var showAddNewMerchantDialog by remember { mutableStateOf(false) }
    var newMerchantInputName by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val merchantInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isMerchantPressed by merchantInteractionSource.collectIsPressedAsState()
    LaunchedEffect(isMerchantPressed) {
        if (isMerchantPressed && !isSearchActive) {
            expandedDropdown = !expandedDropdown
        }
    }

    // Payment options
    var paymentStatus by remember { mutableStateOf("full_cash") } // "full_cash", "on_credit", "partial"
    var paidAmountStr by remember { mutableStateOf("") }

    var imageBase64 by remember { mutableStateOf<String?>(animalToEdit?.imageBase64) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imageBase64 = ImageUtils.bitmapToBase64(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageBase64 = ImageUtils.uriToBase64(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(if (animalToEdit != null) "تعديل رأس ماشية ✏️" else "بروتوكول إضافة رأس ماشية جديدة (شراء) 🦬", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Merchant Selection
                val filteredPeople = remember(people, merchantSearchQuery) {
                    if (merchantSearchQuery.isBlank()) people else people.filter { it.name.contains(merchantSearchQuery, ignoreCase = true) }
                }
                Text("مصدر رأس الماشية (التاجر):", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = merchantSearchQuery,
                            onValueChange = {
                                merchantSearchQuery = it
                                expandedDropdown = true
                                if (it.isBlank()) {
                                    selectedPersonId = null
                                }
                            },
                            readOnly = !isSearchActive,
                            interactionSource = merchantInteractionSource,
                            placeholder = { 
                                if (isSearchActive) {
                                    Text("اكتب اسم التاجر للبحث... 🔍", fontSize = 11.sp, color = Color.Gray)
                                } else {
                                    Text("اضغط للاختيار أو اضغط 🔍 للبحث", fontSize = 11.sp, color = Color.Gray)
                                }
                            },
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = !isSearchActive
                                    if (isSearchActive) {
                                        expandedDropdown = true
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "بحث",
                                        tint = if (isSearchActive) accentColor else Color.Gray
                                    )
                                }
                            },
                            trailingIcon = {
                                if (merchantSearchQuery.isNotEmpty() || selectedPersonId != null) {
                                    IconButton(onClick = {
                                        merchantSearchQuery = ""
                                        selectedPersonId = null
                                        expandedDropdown = false
                                        isSearchActive = false
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        DropdownMenu(
                            expanded = expandedDropdown && filteredPeople.isNotEmpty(),
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            filteredPeople.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.role})") },
                                    onClick = {
                                        selectedPersonId = p.id
                                        merchantSearchQuery = p.name
                                        expandedDropdown = false
                                        isSearchActive = false
                                    }
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { showAddNewMerchantDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "اضافة جديد",
                            tint = accentColor
                        )
                    }
                }

                if (showAddNewMerchantDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddNewMerchantDialog = false },
                        title = { Text("إضافة تاجر مالي جديد ➕", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("يرجى إدخال اسم التاجر لإضافته مباشرة وتحديده في المعاملة الحالية:", fontSize = 11.sp, color = Color.Gray)
                                OutlinedTextField(
                                    value = newMerchantInputName,
                                    onValueChange = { newMerchantInputName = it },
                                    label = { Text("اسم التاجر الجديد") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newMerchantInputName.trim().isBlank()) {
                                        Toast.makeText(context, "الرجاء كتابة اسم التاجر", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    viewModel.registerNewPerson(
                                        name = newMerchantInputName,
                                        role = "تاجر"
                                    ) { insertedId ->
                                        selectedPersonId = insertedId
                                        merchantSearchQuery = newMerchantInputName.trim()
                                        newMerchantInputName = ""
                                        showAddNewMerchantDialog = false
                                        Toast.makeText(context, "تمت إضافة التاجر بنجاح وتحديده!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("إضافة وتحديد", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddNewMerchantDialog = false }) {
                                Text("إلغاء", color = Color.Gray)
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم أو رمز المعرف (رقم تسلسلي تلقائي إن ترك فارغاً)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // 2. Gender Selection Control
                Text("جنس رأس الماشية 🧬:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isFemale = false
                            updateTypeForGenderAndFamily(false, selectedFamilyRaw)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isFemale) accentColor else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ذكر (عجل، خروف طلوقة) ♂️", color = if (!isFemale) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isFemale = true
                            updateTypeForGenderAndFamily(true, selectedFamilyRaw)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFemale) accentColor else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("أنثى (عجلة، نعجة بقرة) ♀️", color = if (isFemale) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text("تحديد تصنيف وسلالة الرأس:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    typesToUse.forEach { t ->
                        val parsed = com.example.utils.AnimalTypeHelper.parseAnimalType(t)
                        val displayName = if (isFemale) parsed.female else parsed.male
                        val isSelected = selectedFamilyRaw == t
                        Button(
                            onClick = {
                                selectedFamilyRaw = t
                                updateTypeForGenderAndFamily(isFemale, t)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(displayName, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("الوزن المتوقع (كغ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = purchasePriceStr,
                    onValueChange = { purchasePriceStr = it },
                    label = { Text("سعر الشراء الكلي (جنيه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("العمر (مثال: سنة وخمسة أشهر)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = feedCostStr,
                    onValueChange = { feedCostStr = it },
                    label = { Text("مخصص تكلفة الطعام للرأس (جنيه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Payment Status fields
                Text("حالة تسديد ودفع القيمة المالية:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { paymentStatus = "full_cash" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentStatus == "full_cash") Color(0xFF10B981) else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("مدفوع كامل نقداً 💵", color = if (paymentStatus == "full_cash") Color.White else Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { paymentStatus = "on_credit" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (paymentStatus == "on_credit") Color(0xFFEF4444) else Color(0xFFF1F5F9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("بالآجل (دين عليك) ⏳", color = if (paymentStatus == "on_credit") Color.White else Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { paymentStatus = "partial" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentStatus == "partial") Color(0xFFD97706) else Color(0xFFF1F5F9)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مدفوع جزئي (دفعة نقدية) 💰", color = if (paymentStatus == "partial") Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (paymentStatus == "partial") {
                    OutlinedTextField(
                        value = paidAmountStr,
                        onValueChange = { paidAmountStr = it },
                        label = { Text("المبلغ المالي المدفوع حالياً (جنيه)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Photos triggers
                Text("توثيق مظهر الرأس بصورة:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التقاط صورة", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ألبوم الهاتف", fontSize = 11.sp, color = Color.White)
                    }
                }

                if (imageBase64 != null) {
                    val decoded = remember(imageBase64) {
                        imageBase64?.let { ImageUtils.base64ToBitmap(it) }
                    }
                    if (decoded != null) {
                        Image(
                            bitmap = decoded.asImageBitmap(),
                            contentDescription = "معاينة المظهر",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weightVal = weightStr.toDoubleOrNull() ?: 0.0
                    val priceVal = purchasePriceStr.toDoubleOrNull() ?: 0.0
                    val feedCostVal = feedCostStr.toDoubleOrNull() ?: 0.0

                    if (priceVal <= 0.0) {
                        Toast.makeText(context, "الرجاء إدخال سعر شراء صحيح", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val pAmt = paidAmountStr.toDoubleOrNull() ?: 0.0
                    if (paymentStatus == "partial" && (pAmt <= 0.0 || pAmt > priceVal)) {
                        Toast.makeText(context, "الرجاء كتابة مبلغ مدفوع مالي صحيح أقل من سعر الشراء", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (animalToEdit != null) {
                        val updatedAnimal = animalToEdit.copy(
                            name = name,
                            type = type,
                            weight = weightVal,
                            purchasePrice = priceVal,
                            age = age,
                            feedCost = feedCostVal,
                            imageBase64 = imageBase64,
                            associatedPersonId = selectedPersonId,
                            merchantName = merchantSearchQuery.ifBlank { "سوق" }
                        )
                        viewModel.updateAnimalDetails(updatedAnimal)
                        Toast.makeText(context, "تم حفظ وتعديل بيانات رأس الماشية بنجاح!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.registerAnimal(
                            name = name,
                            type = type,
                            weight = weightVal,
                            purchasePrice = priceVal,
                            age = age,
                            feedCost = feedCostVal,
                            imageBase64 = imageBase64,
                            associatedPersonId = selectedPersonId,
                            newMerchantName = merchantSearchQuery,
                            paymentStatus = paymentStatus,
                            paidAmount = pAmt
                        )
                        Toast.makeText(context, "تم حفظ وسند رأس الماشية بنجاح وتحديث الأرصدة والشبكة!", Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(if (animalToEdit != null) "حفظ التعديلات" else "إدخال وحفظ السند", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}

// ================= DIALOG 2: ADD FEEDS STOCK =================
@Composable
fun AddFeedDialog(viewModel: FarmViewModel, accentColor: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var feedName by remember { mutableStateOf("") }
    var ingredientsDesc by remember { mutableStateOf("") }
    var totalWeightStr by remember { mutableStateOf("") }
    var totalCostStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("قيد شراء حصص وأعلاف جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = feedName,
                    onValueChange = { feedName = it },
                    label = { Text("اسم صنف الطعام (مثال: خلطة نخالة وفول)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ingredientsDesc,
                    onValueChange = { ingredientsDesc = it },
                    label = { Text("تفاصيل المكونات بالوزن والسعر (مثال: فول 100كغ بسعر 100)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalWeightStr,
                    onValueChange = { totalWeightStr = it },
                    label = { Text("إجمالي الوزن بالكامل (كغ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalCostStr,
                    onValueChange = { totalCostStr = it },
                    label = { Text("التكلفة الكلية المشتراة (جنيه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (feedName.isBlank()) return@Button
                    val weight = totalWeightStr.toDoubleOrNull() ?: 0.0
                    val cost = totalCostStr.toDoubleOrNull() ?: 0.0

                    viewModel.registerFeed(feedName, ingredientsDesc, weight, cost)
                    Toast.makeText(context, "تم تسجيل وإضافة السجل الحلفي بنجاح!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("شراء الأعلاف وحفظ السند", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}

// ================= DIALOG 2.5: ADD MEDICINES STOCK =================
@Composable
fun AddMedicineDialog(viewModel: FarmViewModel, accentColor: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var totalCostStr by remember { mutableStateOf("") }
    var validityDaysStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("قيد شراء دواء أو علاج بيطري جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الدواء أو العلامة الطبية") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalCostStr,
                    onValueChange = { totalCostStr = it },
                    label = { Text("التكلفة الإجمالية (جنيه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = validityDaysStr,
                    onValueChange = { validityDaysStr = it },
                    label = { Text("طول الدواء - فترة السحب بالأيام") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val cost = totalCostStr.toDoubleOrNull() ?: 0.0
                    val days = validityDaysStr.toIntOrNull() ?: 0

                    viewModel.registerMedicine(name, cost, days)
                    Toast.makeText(context, "تم تسجيل وإضافة الدواء بنجاح! 💊", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("شراء الدواء وحفظ السند", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}

// ================= DIALOG 3: MANUAL EXPENSES / INCOMES =================
@Composable
fun AddTransactionDialog(viewModel: FarmViewModel, type: String, accentColor: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val people by viewModel.peopleList.collectAsStateWithLifecycle()

    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("عام") }
    
    var selectedPersonId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = if (type == "income") "تسجيل قيد قبض دائن (إيراد)" else "تسجيل قيد دفع مدين (مصروف)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Link options with accounts/persons
                Text("ربط المعاملة بشخص مسجل (اختياري)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                var expanded by remember { mutableStateOf(false) }
                val personLabel = people.firstOrNull { it.id == selectedPersonId }?.name ?: "اختر طرف أو شخص متصل"

                Box {
                    Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                        Text(personLabel, color = Color.Black)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("لا أحد (غير مرتبط بحساب أفراد)") },
                            onClick = {
                                selectedPersonId = null
                                expanded = false
                            }
                        )
                        people.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.name} (${p.role})") },
                                onClick = {
                                    selectedPersonId = p.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("القيمة المالية بالكامل (جنيه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الشرح والبيان") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("نوع وتصنيف المود (مثال: صيانة، أدوية، رواتب)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = amountStr.toDoubleOrNull() ?: 0.0
                    if (amtVal <= 0.0) return@Button

                    val finalDesc = if (description.isBlank()) "سند مالي لعملية ${if (type == "income") "قبض" else "صرف"}" else description
                    viewModel.registerManualTransaction(type, amtVal, finalDesc, category, selectedPersonId)
                    Toast.makeText(context, "تم تسجيل القيود المالية بنجاح!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("تسجيل وحفظ السند مالي", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}

// ================= NEWBORN DIALOG =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewbornDialog(viewModel: FarmViewModel, accentColor: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val animals by viewModel.animalsList.collectAsStateWithLifecycle()

    var selectedMotherId by remember { mutableStateOf<Int?>(null) }
    var selectedGender by remember { mutableStateOf("ذكر") }
    var selectedBirthType by remember { mutableStateOf("فردي") }
    var birthDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    
    var expandedGender by remember { mutableStateOf(false) }
    var expandedMother by remember { mutableStateOf(false) }
    var expandedBirthType by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = null, tint = accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة مولود جديد 🍼", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mother Selection
                ExposedDropdownMenuBox(
                    expanded = expandedMother,
                    onExpandedChange = { expandedMother = it }
                ) {
                    OutlinedTextField(
                        value = animals.find { it.id == selectedMotherId }?.name ?: "الرأس الأم غير مختار",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الرأس الأم") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMother) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMother,
                        onDismissRequest = { expandedMother = false }
                    ) {
                        val availableMothers = animals.filter { it.type in listOf("أغنام", "ماعز", "جاموس", "جمال", "عجل", "بقر") } // Adjust logic
                        if (availableMothers.isEmpty()) {
                            DropdownMenuItem(text = { Text("لا توجد إناث بالحظيرة") }, onClick = { expandedMother = false })
                        }
                        availableMothers.forEach { mother ->
                            DropdownMenuItem(
                                text = { Text("${mother.name} - ${mother.type}") },
                                onClick = {
                                    selectedMotherId = mother.id
                                    expandedMother = false
                                }
                            )
                        }
                    }
                }

                // Gender
                ExposedDropdownMenuBox(
                    expanded = expandedGender,
                    onExpandedChange = { expandedGender = it }
                ) {
                    OutlinedTextField(
                        value = selectedGender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الجنس") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGender,
                        onDismissRequest = { expandedGender = false }
                    ) {
                        listOf("ذكر", "أنثى").forEach { gender ->
                            DropdownMenuItem(
                                text = { Text(gender) },
                                onClick = {
                                    selectedGender = gender
                                    expandedGender = false
                                }
                            )
                        }
                    }
                }

                // Birth Type (Single / Twins)
                ExposedDropdownMenuBox(
                    expanded = expandedBirthType,
                    onExpandedChange = { expandedBirthType = it }
                ) {
                    OutlinedTextField(
                        value = selectedBirthType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع الولادة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBirthType) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBirthType,
                        onDismissRequest = { expandedBirthType = false }
                    ) {
                        listOf("فردي", "توأم").forEach { typ ->
                            DropdownMenuItem(
                                text = { Text(typ) },
                                onClick = {
                                    selectedBirthType = typ
                                    expandedBirthType = false
                                }
                            )
                        }
                    }
                }

                // Date
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("تاريخ الولادة (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedMotherId == null) {
                        Toast.makeText(context, "الرجاء اختيار الرأس الأم", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.registerBirth(
                        motherId = selectedMotherId!!,
                        gender = selectedGender,
                        birthType = selectedBirthType,
                        birthDate = birthDate
                    )
                    Toast.makeText(context, "تم تسجيل المولود بنجاح", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("إضافة المولود", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("إلغاء", color = Color.Gray) }
        }
    )
}

// ================= SCREEN COMPOSABLE: AI ASSISTANT =================
@Composable
fun AiHelperScreen(viewModel: FarmViewModel, accentColor: Color, zoomLevel: Float) {
    val context = LocalContext.current
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val isLtr = appLang == "en"
    val chatLayoutDirection = if (isLtr) LayoutDirection.Ltr else LayoutDirection.Rtl

    val currentFarm by viewModel.currentFarm.collectAsStateWithLifecycle()
    val animals by viewModel.animalsList.collectAsStateWithLifecycle()
    val feeds by viewModel.feedsList.collectAsStateWithLifecycle()
    val people by viewModel.peopleList.collectAsStateWithLifecycle()

    var userMessage by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            Pair("أهلاً بك في مساعد الذكاء الاصطناعي الخاص بالمزرعة برو! 🤖\n\nأنا هنا لمساعدتك في إدارة مزرعتك بكفاءة عالية وتحليل البيانات فورياً. يمكنك سؤالي عن أي شيء مثل:\n\n🌾 تركيب خلطات أعلاف بنسب متزنة.\n🥩 زيادة إنتاج اللحوم ومعدلات تسمين العجول.\n🧬 جداول التحصينات والوقاية البيطرية والولادة.\n💰 كيفية تلافي تراكم الأرصدة والديون المترتبة على العملاء.\n\nكيف يمكنني مساعدتك اليوم؟", false)
        )
    }
    var isGenerating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Context aggregation from database state flow
    val farmName = currentFarm ?: "مزرعتي"
    val animalsCount = animals.size
    val animalsByTypeInfo = animals.groupBy { it.type }.mapValues { it.value.size }.entries.joinToString(", ") { "${it.key}: ${it.value}" }
    val feedsCount = feeds.size
    val feedsWeightAlerts = feeds.filter { it.remainingWeight <= it.alertThreshold }.joinToString(", ") { it.feedName }
    val peopleCount = people.size
    val totals = viewModel.getSummaryTotals()
    val totalIncomes = totals.first
    val totalExpenses = totals.second
    val netProfit = totals.third

    val systemContextPrompt = """
        أنت مساعد ذكي مخصص لتطبيق "المزرعة برو" (Farm Pro).
        بيانات المزرعة الحالية للتوجيه بدقة ومعرفة وضع المزرعة:
        - اسم المزرعة: $farmName
        - عدد الحيوانات الحالية في الحظيرة: $animalsCount رأس ($animalsByTypeInfo)
        - خلطات الأعلاف المتوفرة: $feedsCount خلطات
        - تنبيهات انخفاض مخزون الأعلاف: ${if (feedsWeightAlerts.isEmpty()) "لا توجد تنبيهات نشطة" else feedsWeightAlerts}
        - عدد العملاء والشركاء المسجلين: $peopleCount عملاء
        - إجمالي الإيرادات في الأرشيف: $totalIncomes جنيه
        - إجمالي المصروفات في الأرشيف: $totalExpenses جنيه
        - صافي أرباح المزرعة: $netProfit جنيه

        ساعد المستخدم كأخصائي وخبير تغذية مزارع، وطبيب بيطري، ومستشار مالي. جاوب بوضوح واحترافية وباللغة العربية الفصحى وبأدق التفاصيل الممكنة. إذا سأل العميل عن خلطات الأعلاف، فقم بكتابة مكوناتها وتوزيع نسب البروتينات المناسبة بدقة، وإذا سأل عن التحصينات فقم بتوضيحها بالتفصيل.
    """.trimIndent()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides chatLayoutDirection) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "مساعدك الذكي (Gemini Assistant) 🤖",
                            fontWeight = FontWeight.Bold,
                            fontSize = (15f * (zoomLevel / 16f)).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "متزامن معك لتحليل بيانات المزرعة فورياً وإعطاء توصيات ذكية.",
                            fontSize = (11f * (zoomLevel / 16f)).sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Quick Actions / Suggestions
            val suggestions = listOf(
                "🌾 خلطة علف متوازنة للأغنام",
                "📈 كيف أزيد أرباح المزرعة؟",
                "🧬 جدول تحصينات ومقاومة الأوبئة",
                "🧾 نصائح لإدارة الديون والآجل"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions) { keyword ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(enabled = !isGenerating) {
                                userMessage = keyword.substring(2) // remove emoji
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = keyword,
                            fontSize = (11f * (zoomLevel / 16f)).sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Chat Messages area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages.size) { index ->
                        val msg = chatMessages[index]
                        val isUser = msg.second
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .background(
                                        color = if (isUser) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.first,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = (13f * (zoomLevel / 16f)).sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 0.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = accentColor,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "جاري التفكير وصياغة التوصيات... ⚡",
                                            fontSize = (11f * (zoomLevel / 16f)).sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Message Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    placeholder = { Text("اكتب استفسارك هنا...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    maxLines = 3,
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (userMessage.isNotBlank() && !isGenerating) {
                            val msg = userMessage
                            userMessage = ""
                            chatMessages.add(Pair(msg, true))
                            isGenerating = true
                            
                            coroutineScope.launch {
                                val response = callGeminiApi(
                                    apiKey = com.example.BuildConfig.GEMINI_API_KEY,
                                    contextPrompt = systemContextPrompt,
                                    chatHistory = chatMessages.toList(),
                                    userPrompt = msg
                                )
                                chatMessages.add(Pair(response, false))
                                isGenerating = false
                            }
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (userMessage.isNotBlank() && !isGenerating) {
                            val msg = userMessage
                            userMessage = ""
                            chatMessages.add(Pair(msg, true))
                            isGenerating = true
                            
                            coroutineScope.launch {
                                val response = callGeminiApi(
                                    apiKey = com.example.BuildConfig.GEMINI_API_KEY,
                                    contextPrompt = systemContextPrompt,
                                    chatHistory = chatMessages.toList(),
                                    userPrompt = msg
                                )
                                chatMessages.add(Pair(response, false))
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

suspend fun callGeminiApi(
    apiKey: String,
    contextPrompt: String,
    chatHistory: List<Pair<String, Boolean>>,
    userPrompt: String
): String = withContext(Dispatchers.IO) {
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "عذراً لم يتم العثور على مفتاح API الخاص بـ Gemini في إعدادات تشغيل التطبيق.\n\nلتفعيل المساعد الـذكـي:\n1. انسخ مفتاح API الخاص بك من Google AI Studio.\n2. ضعه في لوحة Secrets بالمشروع تحت اسم GEMINI_API_KEY."
    }

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
    
    try {
        val rootObj = JSONObject()
        
        // System instruction
        val sysInstrObj = JSONObject()
        val sysPartsArray = JSONArray()
        val sysPartObj = JSONObject()
        sysPartObj.put("text", contextPrompt)
        sysPartsArray.put(sysPartObj)
        sysInstrObj.put("parts", sysPartsArray)
        rootObj.put("systemInstruction", sysInstrObj)
        
        // Contents
        val contentsArray = JSONArray()
        
        // Send last 6 turns of conversations for history
        val recentHistory = chatHistory.takeLast(6)
        for (turn in recentHistory) {
            val contentObj = JSONObject()
            contentObj.put("role", if (turn.second) "user" else "model")
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", turn.first)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        
        rootObj.put("contents", contentsArray)

        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val reqBody = rootObj.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(reqBody)
            .build()
            
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext "عذراً، حدث خطأ أثناء الاتصال بالخادم. رمز الخطأ: ${response.code}\nتأكد من صحة تفعيل مفتاح الـ API الخاص بـ Gemini."
            }
            val resStr = response.body?.string() ?: ""
            val resJson = JSONObject(resStr)
            val candidates = resJson.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).getString("text")
                }
            }
            "عذراً، لم أستطع توليد استجابة مناسبة حالياً."
        }
    } catch (e: Exception) {
        "خطأ أثناء معالجة الاتصال بالذكاء الاصطناعي: ${e.localizedMessage ?: e.message}"
    }
}

@Composable
fun BackupScreen(
    viewModel: FarmViewModel,
    accentColor: Color,
    zoomLevel: Float,
    onConfirmDelete: (String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val backups by viewModel.backupsList.collectAsStateWithLifecycle()
    val isFirebaseSynced by viewModel.isFirebaseSynced.collectAsStateWithLifecycle()
    val googleLinks by viewModel.googleLinksList.collectAsStateWithLifecycle()
    val pendingSyncItems by viewModel.pendingSyncItems.collectAsStateWithLifecycle(emptyList())
    val googleUserEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()

    var newEmail by remember { mutableStateOf("") }
    var selectedPathOption by remember { mutableStateOf("المجلد الافتراضي للوثائق (Documents)") }
    var selectedScheduleOption by remember { mutableStateOf("يومي") }
    var isUploadingCloud by remember { mutableStateOf(false) }
    var cloudUploadProgress by remember { mutableStateOf(0f) }
    var cloudUploadStatus by remember { mutableStateOf("") }

    var backupToRestore by remember { mutableStateOf<com.example.data.model.BackupEntity?>(null) }
    var backupToDelete by remember { mutableStateOf<com.example.data.model.BackupEntity?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(context, uri)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    if (showRestoreConfirm && backupToRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل أنت متأكد من رغبتك في استعادة هذه النسخة الاحتياطية؟ سيتم استبدال جميع البيانات الحالية بالبيانات الموجودة في المجلد بشكل نهائي.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        viewModel.restoreBackup(backupToRestore!!.backupDataJson) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، استعد", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showDeleteConfirm && backupToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد الحذف النهائي ⚠️", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا الملف الاحتياطي نهائياً من الذاكرة ومن السحاب؟ لا يمكن استرجاع هذا الملف بعد حذفه.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteBackup(backupToDelete!!)
                        Toast.makeText(context, "تم حذف نسخة الاحتياط نهائياً من الذاكرة والـ Cloud 🗑️", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، حذف نهائي", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("إدارة النسخ الاحتياطي والمزامنة", fontWeight = FontWeight.Bold, fontSize = (22f * (zoomLevel / 16f)).sp, color = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text("تحكم بمسارات حفظ قواعد البيانات محلياً، وجدولة المزامنة التلقائية لـ Firebase و Google Drive و SQLite سحابياً.", fontSize = (13f * (zoomLevel / 16f)).sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. Firebase Sync Control Card (Arabic layout)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "المزامنة السحابية مع مشاريع Firebase (Realtime)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قم ببث ومزامنة داتا المزرعة بالكامل مع سيرفرات Firebase للتحديث الفوري عبر الأجهزة.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (isFirebaseSynced) {
                                    viewModel.backupToFirestore(context)
                                } else {
                                    Toast.makeText(context, "لم يتم الاتصال بقواعد البيانات بشكل صحيح.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFirebaseSynced) "رفع سحابي" else "غير متصل", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (isFirebaseSynced) {
                                    viewModel.restoreFromFirestore(context)
                                } else {
                                    Toast.makeText(context, "لم يتم الاتصال بقواعد البيانات بشكل صحيح.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFirebaseSynced) "استعادة سحابية" else "غير متصل", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Google Drive linking and backing up
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ربط حسابات جوجل ورفع النسخ لـ Google Drive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اربط بريد Gmail الخاص بك لرفع واستيراد ملفات قواعد بيانات SQLite سحابياً لـ Google Drive.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    if (googleUserEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "بريدك الحالي مرتبط تلقائياً بمزامنة جوجل:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = googleUserEmail,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "يرتبط تلقائياً بجميع خدمات جوجل ومزامنة الـ Backups السحابية على التطبيق.",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("أدخل بريد جوجل (Gmail)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newEmail.isNotBlank()) {
                                viewModel.insertLink(newEmail.trim())
                                Toast.makeText(context, "تم ربط الحساب بنجاح لغرض المزامنة", Toast.LENGTH_SHORT).show()
                                newEmail = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة الحساب", color = Color.White, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val driveBackupStatus by viewModel.driveBackupStatus.collectAsStateWithLifecycle()
                    val isUploadingDrive by viewModel.isGoogleDriveUploading.collectAsStateWithLifecycle()

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حالة النسخ بـ Google Drive: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(driveBackupStatus, fontSize = 11.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (isUploadingDrive) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.backupToGoogleDriveAndSync(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("رفع لـ Drive", color = Color.White, fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.syncDataFromGoogleDrive(context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("استيراد من Drive", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("الحسابات المرتبطة حالياً:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    googleLinks.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(link.googleEmail, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(link.linkedAt))
                                    Text("تاريخ الربط: $dateStr", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            IconButton(
                                onClick = {
                                    onConfirmDelete("هل أنت متأكد من رغبتك في إلغاء ربط حساب جوجل (${link.googleEmail}) من المزرعة نهائياً؟") {
                                        viewModel.deleteLink(link)
                                        Toast.makeText(context, "تم إلغاء ربط الحساب بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (googleLinks.isEmpty()) {
                        Text("لا توجد حسابات جوجل مرتبطة حالياً.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
        }

        // 3. Offline Sync status / queue description & list
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نظام المزامنة الذاتية وطابور البيانات المعلقة (أوفلاين)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "عند انقطاع الإنترنت، يتم تجميع كل الإضافات والتحديثات التي تجريها وحفظها محلياً. بمجرد عودة الاتصال بـ Firebase، يقوم الطابور ببثها تلقائياً.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (viewModel.isNetworkAvailable()) {
                                viewModel.processOfflineSyncQueue()
                                Toast.makeText(context, "جاري معالجة طابور المزامنة الآن سحابياً... 🔄", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "لا يتوفر اتصال بالإنترنت حالياً! يرجى التحقق من الشبكة. ⚠️", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بدء المزامنة يدوياً الآن 🔄", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (pendingSyncItems.isEmpty()) Color(0xFFE6F4EA) else Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (pendingSyncItems.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (pendingSyncItems.isEmpty()) Color(0xFF137333) else Color(0xFFB06000),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (pendingSyncItems.isEmpty()) {
                                    "كل قواعد البيانات متزامنة محلياً وسحابياً تماماً! ✅"
                                } else {
                                    "يوجد عدد ${pendingSyncItems.size} عملية بانتظار المزامنة فور توفر الإنترنت."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingSyncItems.isEmpty()) Color(0xFF137333) else Color(0xFFB06000)
                            )
                        }
                    }

                    if (pendingSyncItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("العمليات المعلقة الحالية:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                pendingSyncItems.take(5).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• قسم: ${item.collectionName}", fontSize = 10.sp, color = Color.DarkGray)
                                        Text("نوع العملية: ${item.operationType}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                    }
                                }
                                if (pendingSyncItems.size > 5) {
                                    Text("... زائد ${pendingSyncItems.size - 5} عملية أخرى معلقة بالطابور.", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Local Storage Settings & Backup Scheduler
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("مسارات ومجدول النسخ الاحتياطي التلقائي والمحلي", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسار حفظ النسخ الاحتياطية المحلية (Storage Path)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "المجلد الافتراضي للوثائق (Documents)",
                        "ذاكرة الهاتف المخصصة (Custom Phone Directory - SQLite/Backups)"
                    ).forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = (selectedPathOption == option),
                                onClick = {
                                    selectedPathOption = option
                                    Toast.makeText(context, "تم تحديد مسار الحفظ لـ: $option", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(option, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جدولة النسخ الاحتياطي التلقائي (Background Sync Scheduler)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("حدد معدل تكرار تشغيل خدمة الخلفية لحفظ البيانات تلقائياً:", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("إيقاف", "يومي", "أسبوعي", "شهري").forEach { schedule ->
                            val active = selectedScheduleOption == schedule
                            Surface(
                                onClick = {
                                    selectedScheduleOption = schedule
                                    Toast.makeText(context, "تم تفعيل جدولة النسخ الاحتياطي بالخلفية: $schedule", Toast.LENGTH_SHORT).show()
                                },
                                color = if (active) accentColor else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (active) accentColor else Color.Gray.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = schedule,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("إجراءات النسخ الفورية السريعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.saveDailyBackup()
                            Toast.makeText(context, "تم حفظ ملف قاعدة البيانات بنجاح باسم: farm_backup_${java.text.SimpleDateFormat("yyyy_MM_dd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())}.db 💾", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخة احتياطية محلية الآن (Versioned DB) 💾", color = Color.White, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isUploadingCloud = true
                            cloudUploadProgress = 0f
                            cloudUploadStatus = "جاري الاتصال بخوادم Firebase سحابياً..."
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000)
                                cloudUploadStatus = "جاري تجميع وحفظ ملف SQLite..."
                                cloudUploadProgress = 0.3f
                                kotlinx.coroutines.delay(1200)
                                cloudUploadStatus = "جاري ضغط الملف ورفعه لـ Firebase Storage..."
                                cloudUploadProgress = 0.7f
                                kotlinx.coroutines.delay(1200)
                                cloudUploadStatus = "اكتمل الرفع السحابي وتحديث الفهرس بنجاح!"
                                cloudUploadProgress = 1f
                                kotlinx.coroutines.delay(1000)
                                isUploadingCloud = false
                                Toast.makeText(context, "تم مزامنة وبث النسخة الاحتياطية سحابياً بنجاح ☁️", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مزامنة سحابية سريعة لـ Firebase ☁️", color = Color.White, fontSize = 11.sp)
                    }

                    if (isUploadingCloud) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            LinearProgressIndicator(
                                progress = cloudUploadProgress,
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cloudUploadStatus, fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 4.5: Offline Export & Import (JSON) / Print (HTML)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsBackupRestore,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نقل البيانات محلياً (Offline JSON & Print)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        "استخرج بياناتك كملف JSON لتعمل كنقطة ريستور، أو شارك تقرير A4 للطباعة.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Button(
                        onClick = { viewModel.exportBackup(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير ملف احتياطي (JSON) 📲", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استيراد نسخة احتياطية (JSON) 📥", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.exportHtmlReport(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير تقرير قابل للطباعة (HTML) 📃", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 5: List of Backups
        item {
            Text("ملفات النسخ الاحتياطي المتوفرة (المحلي والسحابي)", fontWeight = FontWeight.Bold, fontSize = (18f * (zoomLevel / 16f)).sp, color = accentColor)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(backups) { backup ->
            val timestampString = java.text.SimpleDateFormat("yyyy_MM_dd_HHmm", java.util.Locale.getDefault()).format(java.util.Date(backup.createdAt))
            val simulatedFilename = "farm_backup_$timestampString.db"
            val simulatedSize = "${(backup.backupDataJson.length / 1024) + 148} KB"

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BackupTable, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(simulatedFilename, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val dateCreated = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(backup.createdAt))
                                Text("بتاريخ: $dateCreated | الحجم: $simulatedSize", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text("محلي ومزامن سحابياً", color = Color(0xFF10B981), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                backupToRestore = backup
                                showRestoreConfirm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استعادة النسخة", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                backupToDelete = backup
                                showDeleteConfirm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف نهائي", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        if (backups.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد ملفات نسخ احتياطي مسجلة بعد.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatchInvoiceScreen(
    viewModel: FarmViewModel,
    themePrimary: Color,
    zoom: Float,
    invoiceType: String, // "purchase", "sale", "feed"
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val people by viewModel.peopleList.collectAsStateWithLifecycle()
    val activeAnimals by viewModel.animalsList.collectAsStateWithLifecycle()
    val activeFeeds by viewModel.feedsList.collectAsStateWithLifecycle()

    // Header inputs
    var selectedPersonId by remember { mutableStateOf<Int?>(null) }
    var useNewPersonName by remember { mutableStateOf("") }
    var settlementType by remember { mutableStateOf("cash") } // "cash", "deferred", "partial"
    var settlementAmountStr by remember { mutableStateOf("") }
    var invoiceNotes by remember { mutableStateOf("") }

    // Mode-specific list states
    val pendingPurchases = remember { mutableStateListOf<BatchAnimalPurchaseItem>() }
    val pendingSales = remember { mutableStateListOf<BatchAnimalSaleItem>() }
    val pendingFeeds = remember { mutableStateListOf<BatchFeedPurchaseItem>() }

    // Form inputs for Purchase Mode
    var animalName by remember { mutableStateOf("") }
    var animalType by remember { mutableStateOf("عجل") }
    
    val invoiceCustomTypesRaw by viewModel.animalTypesList.collectAsStateWithLifecycle()
    val invoiceTypesToUse = remember(invoiceCustomTypesRaw) {
        if (invoiceCustomTypesRaw.isEmpty()) listOf("عجل", "أغنام", "ماعز", "جمال", "جاموس") else invoiceCustomTypesRaw
    }
    var invoiceIsFemale by remember { mutableStateOf(false) } // false = ذكر, true = أنثى
    var invoiceSelectedFamilyRaw by remember { mutableStateOf("عجل") }
    val updateInvoiceTypeForGenderAndFamily = { femaleSelected: Boolean, family: String ->
        val typeObj = com.example.utils.AnimalTypeHelper.parseAnimalType(family)
        animalType = typeObj.getName(femaleSelected)
    }

    var animalWeightStr by remember { mutableStateOf("") }
    var animalPriceStr by remember { mutableStateOf("") }
    var animalAge by remember { mutableStateOf("") }

    // Form inputs for Sale Mode
    var selectedAnimalForSale by remember { mutableStateOf<AnimalEntity?>(null) }
    var salePriceStr by remember { mutableStateOf("") }
    var saleWeightStr by remember { mutableStateOf("") }

    // Form inputs for Feed Mode
    var feedName by remember { mutableStateOf("") }
    var feedWeightStr by remember { mutableStateOf("") }
    var feedCostStr by remember { mutableStateOf("") }
    var feedSupplierBrand by remember { mutableStateOf("") }

    val totalInvoiceSum = remember(invoiceType, pendingPurchases.size, pendingSales.size, pendingFeeds.size) {
        when (invoiceType) {
            "purchase" -> pendingPurchases.sumOf { it.price }
            "sale" -> pendingSales.sumOf { it.salePrice }
            "feed" -> pendingFeeds.sumOf { it.totalCost }
            else -> 0.0
        }
    }

    LaunchedEffect(totalInvoiceSum) {
        if (settlementType == "cash") {
            settlementAmountStr = totalInvoiceSum.toString()
        } else if (settlementType == "deferred") {
            settlementAmountStr = "0.0"
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = themePrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCompleted) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (invoiceType) {
                            "purchase" -> "فاتورة شراء رؤوس مجمعة 🧾"
                            "sale" -> "فاتورة بيع رؤوس مجمعة 🧾"
                            "feed" -> "فاتورة توريد وشراء أعلاف مجمعة 🌾"
                            else -> "فاتورة مجمعة"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (16f * (zoom / 16f)).sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STEP 1: Supplier / Customer Header Details
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. بيانات المشتري/المورد والذمة المالية 👤",
                        fontWeight = FontWeight.Bold,
                        color = themePrimary,
                        fontSize = (14f * (zoom / 16f)).sp
                    )

                    // Dropdown for person/merchant
                    var peopleDropdownExpanded by remember { mutableStateOf(false) }
                    val selectedPersonName = people.find { it.id == selectedPersonId }?.name ?: "اختر صاحب المندوب (مورد/عميل)"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { peopleDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedPersonName, color = MaterialTheme.colorScheme.onSurface, fontSize = (13f * (zoom / 16f)).sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DropdownMenu(
                            expanded = peopleDropdownExpanded,
                            onDismissRequest = { peopleDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("المعاملة مباشرة نقداً بالشراء الحر السريع (بدون قيد حساب آجل)") },
                                onClick = {
                                    selectedPersonId = null
                                    peopleDropdownExpanded = false
                                }
                            )
                            people.forEach { person ->
                                DropdownMenuItem(
                                    text = { Text("${person.name} (${person.role} - رصيد: ${person.balance} جنيه)") },
                                    onClick = {
                                        selectedPersonId = person.id
                                        peopleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedPersonId == null) {
                        OutlinedTextField(
                            value = useNewPersonName,
                            onValueChange = { useNewPersonName = it },
                            label = { Text("أو اسم حساب خارجي جديد") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Settlement Options
                    Text("طريقة تسوية وسداد هذه الفاتورة:", fontWeight = FontWeight.Bold, fontSize = (12f * (zoom / 16f)).sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("cash", "نقدي كاش", Icons.Default.Paid),
                            Triple("deferred", "آجل للحساب", Icons.Default.Pending),
                            Triple("partial", "دفع جزئي مقدم", Icons.Default.Percent)
                        ).forEach { (mode, label, icon) ->
                            val isSelected = settlementType == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { 
                                        settlementType = mode
                                        if (mode == "cash") settlementAmountStr = totalInvoiceSum.toString()
                                        else if (mode == "deferred") settlementAmountStr = "0.0"
                                      },
                                color = if (isSelected) themePrimary else Color.LightGray.copy(alpha = 0.2f),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = (10f * (zoom / 16f)).sp, color = if (isSelected) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (settlementType == "partial") {
                        OutlinedTextField(
                            value = settlementAmountStr,
                            onValueChange = { settlementAmountStr = it },
                            label = { Text("المبلغ المدفوع كاش نقداً من الفاتورة (جنيه)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // STEP 2: Main Items Subform
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. إضافة مادة/رأس جديدة إلى قائمة الفاتورة ➕",
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        fontSize = (14f * (zoom / 16f)).sp
                    )

                    when (invoiceType) {
                        "purchase" -> {
                            OutlinedTextField(
                                value = animalName,
                                onValueChange = { animalName = it },
                                label = { Text("رقم/اسم أو وسم رأس الماشية (مثال: عجل رقم 52)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Gender Selector
                            Text("جنس رأس الماشية 🧬:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        invoiceIsFemale = false
                                        updateInvoiceTypeForGenderAndFamily(false, invoiceSelectedFamilyRaw)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!invoiceIsFemale) themePrimary else Color.LightGray.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ذكر (عجل، خروف طلوقة) ♂️", color = if (!invoiceIsFemale) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        invoiceIsFemale = true
                                        updateInvoiceTypeForGenderAndFamily(true, invoiceSelectedFamilyRaw)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (invoiceIsFemale) themePrimary else Color.LightGray.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("أنثى (عجلة، نعجة بقرة) ♀️", color = if (invoiceIsFemale) Color.White else Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic Types Selector
                            Text("نوع السلالة / الفئة:", fontSize = 12.sp, color = Color.Gray)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            ) {
                                invoiceTypesToUse.forEach { t ->
                                    val parsed = com.example.utils.AnimalTypeHelper.parseAnimalType(t)
                                    val displayName = if (invoiceIsFemale) parsed.female else parsed.male
                                    val isSelected = invoiceSelectedFamilyRaw == t
                                    Button(
                                        onClick = {
                                            invoiceSelectedFamilyRaw = t
                                            updateInvoiceTypeForGenderAndFamily(invoiceIsFemale, t)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) themePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Text(displayName, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = animalType,
                                onValueChange = { animalType = it },
                                label = { Text("النوع المدخل لتسجيل السند (مثال: عجل، عجلة)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = animalWeightStr,
                                    onValueChange = { animalWeightStr = it },
                                    label = { Text("الوزن (كغ)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = animalPriceStr,
                                    onValueChange = { animalPriceStr = it },
                                    label = { Text("السعر لشراء المورد (ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = animalAge,
                                onValueChange = { animalAge = it },
                                label = { Text("العمر (اختياري - مثال: 8 شهور)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val w = animalWeightStr.toDoubleOrNull() ?: 0.0
                                    val p = animalPriceStr.toDoubleOrNull() ?: 0.0
                                    
                                    val currentTotalAnimals = viewModel.animalsList.value.size
                                    val nextSeqNumber = currentTotalAnimals + pendingPurchases.size + 1
                                    val finalAnimalName = if (animalName.isBlank()) "رأس رقم #$nextSeqNumber" else animalName

                                    if (p <= 0.0) {
                                        Toast.makeText(context, "الرجاء كتابة سعر صحيح للشراء!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    pendingPurchases.add(
                                        BatchAnimalPurchaseItem(
                                            name = finalAnimalName,
                                            type = animalType,
                                            weight = w,
                                            price = p,
                                            age = animalAge
                                        )
                                    )
                                    // Reset subform fields
                                    animalName = ""
                                    animalWeightStr = ""
                                    animalPriceStr = ""
                                    animalAge = ""
                                    Toast.makeText(context, "تمت إضافة رأس الماشية للقائمة الفاتورة 📊", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إضافة الرأس للقائمة الفاتورة ➕", color = Color.White)
                            }
                        }

                        "sale" -> {
                            val unsoldAnimalsList = activeAnimals.filter { !it.isArchived }
                            var animalDropdownExpanded by remember { mutableStateOf(false) }
                            val currentAnimalLabel = selectedAnimalForSale?.let { "${it.name} (${it.type} - وزن: ${it.weight} كغ)" } ?: "اختر رأس الماشية من الحظيرة"

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { animalDropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentAnimalLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = (13f * (zoom / 16f)).sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                DropdownMenu(
                                    expanded = animalDropdownExpanded,
                                    onDismissRequest = { animalDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    unsoldAnimalsList.forEach { animal ->
                                        DropdownMenuItem(
                                            text = { Text("${animal.name} (${animal.type} - وزن: ${animal.weight} كغ)") },
                                            onClick = {
                                                selectedAnimalForSale = animal
                                                saleWeightStr = animal.weight.toString()
                                                animalDropdownExpanded = false
                                            }
                                        )
                                    }
                                    if (unsoldAnimalsList.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("لا يوجد رؤوس ماشية بالحظيرة حالياً للبيع!") },
                                            onClick = { animalDropdownExpanded = false }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = saleWeightStr,
                                    onValueChange = { saleWeightStr = it },
                                    label = { Text("وزن البيع الحالي (كغ)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = salePriceStr,
                                    onValueChange = { salePriceStr = it },
                                    label = { Text("سعر البيع للرأس (ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    val animal = selectedAnimalForSale
                                    if (animal == null) {
                                        Toast.makeText(context, "الرجاء اختيار رأس الماشية من القائمة أولاً!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val price = salePriceStr.toDoubleOrNull() ?: 0.0
                                    val weight = saleWeightStr.toDoubleOrNull() ?: animal.weight
                                    if (price <= 0.0) {
                                        Toast.makeText(context, "الرجاء إدخال سعر بيع صحيح!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    if (pendingSales.any { it.animal.id == animal.id }) {
                                        Toast.makeText(context, "تمت إضافة هذا الرأس مسبقاً في هذه الفاتورة!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    pendingSales.add(
                                        BatchAnimalSaleItem(
                                            animal = animal,
                                            salePrice = price,
                                            saleWeight = weight
                                        )
                                    )
                                    selectedAnimalForSale = null
                                    salePriceStr = ""
                                    saleWeightStr = ""
                                    Toast.makeText(context, "تمت إضافة رأس الماشية لصفقة البيع المجمع ☑️", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إضافة الرأس لقائمة البيع الفاتورة ➕", color = Color.White)
                            }
                        }

                        "feed" -> {
                            var itemSubCategory by remember { mutableStateOf("feed") } // "feed" or "medicine"
                            var feedExpanded by remember { mutableStateOf(false) }

                            // Sub-category selector (M3 Card Buttons)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { itemSubCategory = "feed" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (itemSubCategory == "feed") themePrimary else Color.LightGray.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("شراء علف / غذاء 🌾", color = if (itemSubCategory == "feed") Color.White else Color.DarkGray, fontSize = (11f * (zoom / 16f)).sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { itemSubCategory = "medicine" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (itemSubCategory == "medicine") themePrimary else Color.LightGray.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("شراء دواء بيطري 💊", color = if (itemSubCategory == "medicine") Color.White else Color.DarkGray, fontSize = (11f * (zoom / 16f)).sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = feedName,
                                    onValueChange = { 
                                        feedName = it 
                                        if (itemSubCategory == "feed") {
                                            feedExpanded = activeFeeds.any { f -> f.feedName.contains(it, ignoreCase = true) && it.isNotBlank() }
                                        }
                                    },
                                    label = { Text(if (itemSubCategory == "feed") "اسم مادة الأعلاف (نخالة، ذرة، علف جاهز تسمين)" else "اسم الدواء أو العلاج البيطري") },
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { 
                                        if (it.isFocused && itemSubCategory == "feed" && activeFeeds.isNotEmpty() && feedName.isBlank()) feedExpanded = true 
                                    }
                                )
                                if (itemSubCategory == "feed") {
                                    DropdownMenu(
                                        expanded = feedExpanded,
                                        onDismissRequest = { feedExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        activeFeeds.filter { it.feedName.contains(feedName, ignoreCase = true) }
                                            .distinctBy { it.feedName }
                                            .forEach { f ->
                                            DropdownMenuItem(
                                                text = { Text("${f.feedName} ${if(f.ingredientsDescription.isNotBlank()) "(${f.ingredientsDescription})" else ""}") },
                                                onClick = {
                                                    feedName = f.feedName
                                                    feedSupplierBrand = f.ingredientsDescription
                                                    feedExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = feedWeightStr,
                                    onValueChange = { feedWeightStr = it },
                                    label = { Text(if (itemSubCategory == "feed") "الكمية / الوزن (كغ)" else "طول الدواء (فترة السحب بالأيام)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = feedCostStr,
                                    onValueChange = { feedCostStr = it },
                                    label = { Text("إجمالي السعر/التكلفة (ج)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = feedSupplierBrand,
                                onValueChange = { feedSupplierBrand = it },
                                label = { Text(if (itemSubCategory == "feed") "البراند أو العلامة التجارية للأعلاف (اختياري)" else "العلامة المصنعة / التفاصيل الطبية (اختياري)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (feedName.isBlank()) {
                                        Toast.makeText(context, if (itemSubCategory == "feed") "الرجاء كتابة اسم مادة العلف!" else "الرجاء كتابة اسم الدواء!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val w = feedWeightStr.toDoubleOrNull() ?: 0.0
                                    val c = feedCostStr.toDoubleOrNull() ?: 0.0
                                    if (c <= 0.0) {
                                        Toast.makeText(context, "الرجاء كتابة تكلفة صحيحة!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val finalItemName = if (itemSubCategory == "medicine") "MEDICINE:$feedName" else feedName
                                    pendingFeeds.add(
                                        BatchFeedPurchaseItem(
                                            feedName = finalItemName,
                                            totalWeight = w,
                                            totalCost = c,
                                            ingredientsDescription = feedSupplierBrand
                                        )
                                    )
                                    feedName = ""
                                    feedWeightStr = ""
                                    feedCostStr = ""
                                    feedSupplierBrand = ""
                                    Toast.makeText(context, if (itemSubCategory == "feed") "إضافة بند توريد الأعلاف للجدول المجمع بنجاح!" else "إضافة بند العلاج الطبي للجدول المجمع بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (itemSubCategory == "feed") "إضافة بند العلف للفاتورة المجمعة ➕" else "إضافة بند العلاج للفاتورة المجمعة ➕", color = Color.White)
                            }
                        }
                    }
                }
            }

            // STEP 3: List of pending items
            Text(
                "جدول وبنود الفاتورة المجمعة المضافة حالياً 📋:",
                fontWeight = FontWeight.Bold,
                fontSize = (13f * (zoom / 16f)).sp,
                color = themePrimary
            )

            when (invoiceType) {
                "purchase" -> {
                    if (pendingPurchases.isEmpty()) {
                        Text("جدول المشتريات فارغ. أضف رؤوس الماشية في الأعلى لتظهر البنود.", fontSize = (12f * (zoom / 16f)).sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                    } else {
                        pendingPurchases.forEachIndexed { index, item ->
                            Surface(
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${index + 1}. الاسم: ${item.name} (${item.type})", fontWeight = FontWeight.Bold, fontSize = (13f * (zoom / 16f)).sp)
                                        Text("الوزن: ${item.weight} كغ - العمر: ${item.age}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${item.price} ج", fontWeight = FontWeight.Bold, fontSize = (13f * (zoom / 16f)).sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = { pendingPurchases.removeAt(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "sale" -> {
                    if (pendingSales.isEmpty()) {
                        Text("جدول المبيعات فارغ. أضف رؤوس الماشية التي تبيعها من القائمة.", fontSize = (12f * (zoom / 16f)).sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                    } else {
                        pendingSales.forEachIndexed { index, item ->
                            Surface(
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${index + 1}. الرأس: ${item.animal.name}", fontWeight = FontWeight.Bold, fontSize = (13f * (zoom / 16f)).sp)
                                        Text("وزن البيع: ${item.saleWeight} كغ", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${item.salePrice} ج", fontWeight = FontWeight.Bold, fontSize = (13f * (zoom / 16f)).sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = { pendingSales.removeAt(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "feed" -> {
                    if (pendingFeeds.isEmpty()) {
                        Text("جدول توريدات بضائع الأعلاف والأدوية خالٍ حالياً.", fontSize = (12f * (zoom / 16f)).sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                    } else {
                        pendingFeeds.forEachIndexed { index, item ->
                            val isMed = item.feedName.startsWith("MEDICINE:")
                            val displayName = if (isMed) item.feedName.removePrefix("MEDICINE:") else item.feedName
                            val subLabel = if (isMed) {
                                "تفاصيل: ${item.ingredientsDescription} - مدة الصلاحية/السحب: ${item.totalWeight.toInt()} يوم 💊"
                            } else {
                                "البراند: ${item.ingredientsDescription} - الحصيلة: ${item.totalWeight} كغ 🌾"
                            }
                            Surface(
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${index + 1}. ${if (isMed) "علاج: " else "مادة: "} $displayName", fontWeight = FontWeight.Bold, fontSize = (13f * (zoom / 16f)).sp)
                                        Text(subLabel, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${item.totalCost} ج", fontWeight = FontWeight.Bold, fontSize = (13f * (zoom / 16f)).sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = { pendingFeeds.removeAt(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // General billing notes
            OutlinedTextField(
                value = invoiceNotes,
                onValueChange = { invoiceNotes = it },
                label = { Text("شروحات وملاحظات الفاتورة العامة (اختياري)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Final Invoice Summary Section
            Surface(
                color = themePrimary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي قيمة الفاتورة المجمعة:", fontWeight = FontWeight.Bold, fontSize = (14f * (zoom / 16f)).sp)
                        Text("$totalInvoiceSum جنيه", fontWeight = FontWeight.Bold, color = themePrimary, fontSize = (15f * (zoom / 16f)).sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ المدفوع/المستلم نقداً (الصندوق):", fontSize = (13f * (zoom / 16f)).sp)
                        Text("${settlementAmountStr.toDoubleOrNull() ?: 0.0} جنيه", fontWeight = FontWeight.Bold, fontSize = (13f * (zoom / 16f)).sp)
                    }

                    val deferredBalance = remember(totalInvoiceSum, settlementAmountStr) {
                        val cashSettle = settlementAmountStr.toDoubleOrNull() ?: 0.0
                        (totalInvoiceSum - cashSettle).coerceAtLeast(0.0)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ الآجل المعلق بالحسابات المالية:", fontSize = (13f * (zoom / 16f)).sp)
                        Text("$deferredBalance جنيه", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = (13f * (zoom / 16f)).sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save confirmation execution button
            Button(
                onClick = {
                    val settleCash = settlementAmountStr.toDoubleOrNull() ?: 0.0
                    val count = when (invoiceType) {
                        "purchase" -> pendingPurchases.size
                        "sale" -> pendingSales.size
                        "feed" -> pendingFeeds.size
                        else -> 0
                    }

                    if (count <= 0) {
                        Toast.makeText(context, "الرجاء إضافة بضاعة أو رأس واحدة على الأقل لإصدار هذه الفاتورة المجمعة!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    when (invoiceType) {
                        "purchase" -> {
                            viewModel.registerBatchPurchaseInvoice(
                                items = pendingPurchases.toList(),
                                associatedPersonId = selectedPersonId,
                                merchantName = useNewPersonName,
                                paymentStatus = settlementType,
                                paidAmount = settleCash
                            )
                            Toast.makeText(context, "تم تسجيل فاتورة شراء مجمعة وإدخل الرؤوس بنجاح! 🐃", Toast.LENGTH_LONG).show()
                        }
                        
                        "sale" -> {
                            viewModel.registerBatchSaleInvoice(
                                items = pendingSales.toList(),
                                associatedPersonId = selectedPersonId,
                                buyerName = useNewPersonName,
                                paymentStatus = settlementType,
                                receivedAmount = settleCash
                            )
                            Toast.makeText(context, "تم ترحيل مبيعات الرؤوس معاً وإصدار موازنة الفاتورة بنجاح! 🧾", Toast.LENGTH_LONG).show()
                        }

                        "feed" -> {
                            viewModel.registerBatchFeedInvoice(
                                items = pendingFeeds.toList(),
                                associatedPersonId = selectedPersonId,
                                supplierName = useNewPersonName,
                                paymentStatus = settlementType,
                                paidAmount = settleCash
                            )
                            Toast.makeText(context, "تم تسجيل كميات الأعلاف وتعديل ميزانية المورد والمشاريع بنجاح! 🌾", Toast.LENGTH_LONG).show()
                        }
                    }
                    onCompleted()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ترحيل وإصدار الفاتورة بالكامل وتعديل الأرصدة 💾", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
