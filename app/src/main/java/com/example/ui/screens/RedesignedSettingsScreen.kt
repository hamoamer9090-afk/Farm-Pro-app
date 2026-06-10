package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FarmViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedesignedSettingsScreen(
    viewModel: FarmViewModel,
    accentColor: Color,
    zoomLevel: Float,
    onUpdateLabels: () -> Unit
) {
    val context = LocalContext.current

    val hideWelcomeCard by viewModel.hideWelcomeCard.collectAsStateWithLifecycle()
    val hideNetBalance by viewModel.hideNetBalance.collectAsStateWithLifecycle()
    val hideDashboardQuickActions by viewModel.hideDashboardQuickActions.collectAsStateWithLifecycle()
    val hideDashboardShortcuts by viewModel.hideDashboardShortcuts.collectAsStateWithLifecycle()
    val hideDashboardNotes by viewModel.hideDashboardNotes.collectAsStateWithLifecycle()
    val enableSwipeNavigation by viewModel.enableSwipeNavigation.collectAsStateWithLifecycle()
    val invertSwipeDirection by viewModel.invertSwipeDirection.collectAsStateWithLifecycle()

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val appLockFingerprintEnabled by viewModel.appLockFingerprintEnabled.collectAsStateWithLifecycle()
    val appLockPinEnabled by viewModel.appLockPinEnabled.collectAsStateWithLifecycle()
    val appLockPatternEnabled by viewModel.appLockPatternEnabled.collectAsStateWithLifecycle()
    val appLockPinCode by viewModel.appLockPinCode.collectAsStateWithLifecycle()
    val appLockPatternCode by viewModel.appLockPatternCode.collectAsStateWithLifecycle()

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
    
    val farmName by viewModel.farmName.collectAsStateWithLifecycle()
    val primaryColorHex by viewModel.primaryColorHex.collectAsStateWithLifecycle()
    val cardColorHex by viewModel.cardColorHex.collectAsStateWithLifecycle()
    val textColorHex by viewModel.textColorHex.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val selectedFont by viewModel.selectedFont.collectAsStateWithLifecycle()
    val appCurrency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val hideFinancials by viewModel.hideFinancials.collectAsStateWithLifecycle()

    var showFarmNameDialog by remember { mutableStateOf(false) }
    var editFarmName by remember { mutableStateOf(farmName) }

    val securityQuestion by viewModel.securityQuestion.collectAsStateWithLifecycle()
    val securityAnswer by viewModel.securityAnswer.collectAsStateWithLifecycle()

    var showMandatorySecurityDialog by remember { mutableStateOf(false) }
    var inputSecurityQuestion by remember { mutableStateOf("") }
    var inputSecurityAnswer by remember { mutableStateOf("") }

    var showSecurityAuthDialog by remember { mutableStateOf(false) }
    var securityAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var securityAuthError by remember { mutableStateOf("") }
    var securityAuthInputPin by remember { mutableStateOf("") }
    var securityAuthInputAnswer by remember { mutableStateOf("") }



    if (showFarmNameDialog) {
        AlertDialog(
            onDismissRequest = { showFarmNameDialog = false },
            title = { Text("تعديل اسم المزرعة") },
            text = {
                OutlinedTextField(
                    value = editFarmName,
                    onValueChange = { editFarmName = it },
                    label = { Text("اسم المزرعة") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFarmName(editFarmName)
                    showFarmNameDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showFarmNameDialog = false }) { Text("إلغاء") }
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
        // Main Title
        Text(
            text = "إعدادات وتخصيص المزرعة",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // State for main categories
        var expandedCategory by remember { mutableStateOf<String?>(null) }
        val toggleCategory: (String) -> Unit = { category ->
            expandedCategory = if (expandedCategory == category) null else category
        }

        // Card 1: إعدادات الهيكل والتخصيص
        SettingSection(
            titleText = "إعدادات الهيكل والتخصيص",
            icon = Icons.Default.DashboardCustomize,
            isExpanded = expandedCategory == "layout",
            onToggle = { toggleCategory("layout") }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Farm Name
                Column {
                    Text(
                        text = "إعدادات المزرعة الحالية ($farmName):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        textAlign = TextAlign.Right
                    )
                    Button(
                        onClick = { 
                            editFarmName = farmName
                            showFarmNameDialog = true 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تغيير اسم المزرعة", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Sidebar
                ExpandableSubSection("تخصيص عناصر القائمة الجانبية", accentColor = accentColor) {
                    SwitchSettingItem("لوح التحكم الرئيسية", !hideSidebarDashboard) { viewModel.toggleHideSidebarDashboard(!it) }
                    SwitchSettingItem("الحظيرة", !hideSidebarBarn) { viewModel.toggleHideSidebarBarn(!it) }
                    SwitchSettingItem("الأعلاف", !hideSidebarFeeds) { viewModel.toggleHideSidebarFeeds(!it) }
                    SwitchSettingItem("الحسابات", !hideSidebarAccounts) { viewModel.toggleHideSidebarAccounts(!it) }
                    SwitchSettingItem("الملاحظات والوسائط", !hideSidebarNotes) { viewModel.toggleHideSidebarNotes(!it) }
                    SwitchSettingItem("الأرشيف والبيانات", !hideSidebarArchive) { viewModel.toggleHideSidebarArchive(!it) }
                    SwitchSettingItem("النسخ الاحتياطي", !hideSidebarBackup) { viewModel.toggleHideSidebarBackup(!it) }
                    SwitchSettingItem("إدارة فريق العمل والصلاحيات", !hideSidebarUsers) { viewModel.toggleHideSidebarUsers(!it) }
                    SwitchSettingItem("حاسبة الأعلاف", !hideSidebarFeedCalc) { viewModel.toggleHideSidebarFeedCalc(!it) }
                    SwitchSettingItem("الإحصائيات والتقارير", !hideSidebarReports) { viewModel.toggleHideSidebarReports(!it) }
                    SwitchSettingItem("التنبيهات الذكية", !hideSidebarReminders) { viewModel.toggleHideSidebarReminders(!it) }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Dashboard
                ExpandableSubSection("تخصيص عناصر الشاشة الرئيسية", accentColor = accentColor) {
                    SwitchSettingItem("بطاقات الإحصائيات المالية", !hideNetBalance) { viewModel.toggleHideNetBalance(!it) }
                    SwitchSettingItem("لوحة الإجراءات السريعة", !hideDashboardQuickActions) { viewModel.toggleHideDashboardQuickActions(!it) }
                    SwitchSettingItem("أزرار أقسام المزرعة", !hideDashboardShortcuts) { viewModel.toggleHideDashboardShortcuts(!it) }
                    SwitchSettingItem("الملاحظات والمذكرات", !hideDashboardNotes) { viewModel.toggleHideDashboardNotes(!it) }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Navigation
                ExpandableSubSection("إعدادات التنقل بين الصفحات", accentColor = accentColor) {
                    SwitchSettingItem("تفعيل التنقل بالسحب يمين/يسار", enableSwipeNavigation) { viewModel.toggleEnableSwipeNavigation(it) }
                    SwitchSettingItem("عكس اتجاه السحب", invertSwipeDirection) { viewModel.toggleInvertSwipeDirection(it) }
                }
            }
        }

        // Card 2: السمات والألوان
        SettingSection(
            titleText = "السمات والألوان",
            icon = Icons.Default.Palette,
            isExpanded = expandedCategory == "theme",
            onToggle = { toggleCategory("theme") }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                SubSection("السمة اللونية برو:") {
                    val primaryOptions = listOf(
                        Triple("أخضر زمردي", "#117A65", true),
                        Triple("أزرق ملكي", "#1A5276", false),
                        Triple("ذهبي عسلي", "#B7950B", false),
                        Triple("بنفسجي فاخر", "#6C3483", false)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        primaryOptions.forEach { (label, hex, _) ->
                            ColorButton(
                                label = label,
                                colorHex = hex,
                                isSelected = primaryColorHex == hex,
                                onClick = { viewModel.updateThemePrimaryColor(hex) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                SubSection("نمط المظهر (ليل ونهار):") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OptionButton("تلقائي", Icons.Default.Settings, themeMode == "system", { viewModel.updateThemeMode("system") }, accentColor, Modifier.weight(1f))
                        OptionButton("مضيء (نهار)", Icons.Default.WbSunny, themeMode == "light", { viewModel.updateThemeMode("light") }, accentColor, Modifier.weight(1f))
                        OptionButton("مظلم (ليل)", Icons.Default.DarkMode, themeMode == "dark", { viewModel.updateThemeMode("dark") }, accentColor, Modifier.weight(1f))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                SubSection("لون خلفية الكروت والبطاقات:") {
                    val cardOptions = listOf(
                        Pair("أبيض", "#FFFFFF"),
                        Pair("رمادي", "#F3F4F6"),
                        Pair("وردي", "#FDF2F8"),
                        Pair("داكن", "#1E293B"),
                        Pair("أسود", "#000000")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cardOptions.forEach { (label, hex) ->
                            ColorButton(
                                label = label,
                                colorHex = hex,
                                isSelected = cardColorHex == hex,
                                onClick = { viewModel.updateCardBackgroundColor(hex) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { Toast.makeText(context, "الطبقة المتقدمة لتكوين الألوان قيد التطوير", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("فتح تركيب وتدريج لون مخصص 🎨", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Card 3: الخطوط والتحكم بالنصوص
        SettingSection(
            titleText = "الخطوط والتحكم بالنصوص",
            icon = Icons.Default.TextFields,
            isExpanded = expandedCategory == "fonts",
            onToggle = { toggleCategory("fonts") }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                SubSection("نوع وتصميم الخط العربي:") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OptionButton("تيجوال", null, selectedFont == "tajawal", { viewModel.updateFontFamily("tajawal") }, accentColor, Modifier.weight(1f))
                        OptionButton("أميري", null, selectedFont == "amiri", { viewModel.updateFontFamily("amiri") }, accentColor, Modifier.weight(1f))
                        OptionButton("كايرو", null, selectedFont == "cairo", { viewModel.updateFontFamily("cairo") }, accentColor, Modifier.weight(1f))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                SubSection("لون خطوط ونصوص التطبيق:") {
                    val textOptions = listOf(
                        Pair("تلقائي", "#1E293B"),
                        Pair("داكن", "#334155"),
                        Pair("أسود", "#000000"),
                        Pair("أبيض", "#FFFFFF"),
                        Pair("أصفر", "#FDE047")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        textOptions.forEach { (label, hex) ->
                            ColorButton(
                                label = label,
                                colorHex = hex,
                                isSelected = textColorHex == hex,
                                onClick = { viewModel.updateTypographyColor(hex) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { Toast.makeText(context, "الطبقة المتقدمة لتكوين الألوان قيد التطوير", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("فتح تركيب وتخصيص لون نصوص دقيق 🖌️", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                SubSection("حجم النصوص والواجهة (Zoom & Scale):") {
                    Column {
                        Slider(
                            value = zoomLevel,
                            onValueChange = { viewModel.updateZoomLevel(it) },
                            valueRange = 12f..20f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("صغير (12)", fontSize = 12.sp, color = Color.Gray)
                            Text("المعتدل (16)", fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            Text("ضخم (20)", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                SubSection("لغة التطبيق / App Language:") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OptionButton(
                            label = "العربية 🇸🇦",
                            icon = null,
                            isSelected = appLang == "ar",
                            onClick = { viewModel.updateAppLang("ar") },
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                        OptionButton(
                            label = "English 🇬🇧",
                            icon = null,
                            isSelected = appLang == "en",
                            onClick = { viewModel.updateAppLang("en") },
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
        val appLockType by viewModel.appLockType.collectAsStateWithLifecycle()

        // Card 4: الأمان والخصوصية
        SettingSection(
            titleText = "الأمان والخصوصية",
            icon = Icons.Default.Lock,
            isExpanded = expandedCategory == "security",
            onToggle = { toggleCategory("security") }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SubSection("قفل التطبيق (App Lock):", "التحكم في حماية المزرعة والبيانات") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل قفل التطبيق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (securityQuestion.isBlank() || securityAnswer.isBlank()) {
                                        inputSecurityQuestion = ""
                                        inputSecurityAnswer = ""
                                        showMandatorySecurityDialog = true
                                    } else {
                                        viewModel.toggleAppLock(true)
                                    }
                                } else {
                                    viewModel.toggleAppLock(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.5f))
                        )
                    }

                    AnimatedVisibility(visible = isAppLockEnabled) {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                            Text("اختر طرق الحماية (يمكن تفعيل أكثر من واحدة):", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            
                            var showPinDialog by remember { mutableStateOf(false) }
                            var tempPin by remember { mutableStateOf("") }
                            
                            var showPatternDialog by remember { mutableStateOf(false) }
                            var tempPattern by remember { mutableStateOf("") }

                            SwitchSettingItem("قفل الهوية (بصمة/وجه/قفل الهاتف)", appLockFingerprintEnabled) { viewModel.toggleAppLockFingerprint(it) }
                            
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("رمز (PIN) مخصص", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (appLockPinEnabled && appLockPinCode.isNotEmpty()) Text("تغيير الرمز", fontSize = 11.sp, color = accentColor, modifier = Modifier.clickable { showPinDialog = true })
                                }
                                Switch(
                                    checked = appLockPinEnabled,
                                    onCheckedChange = { 
                                        if (it) { showPinDialog = true } 
                                        else { viewModel.toggleAppLockPin(false) }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.5f))
                                )
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("نقش مخصص", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (appLockPatternEnabled && appLockPatternCode.isNotEmpty()) Text("تغيير النقش", fontSize = 11.sp, color = accentColor, modifier = Modifier.clickable { showPatternDialog = true })
                                }
                                Switch(
                                    checked = appLockPatternEnabled,
                                    onCheckedChange = { 
                                        if (it) { showPatternDialog = true } 
                                        else { viewModel.toggleAppLockPattern(false) }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.5f))
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("سؤال الأمان للحماية وطوارئ المرور", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (securityQuestion.isNotEmpty()) {
                                        Text("السؤال الحالي: $securityQuestion", fontSize = 11.sp, color = Color.Gray)
                                    } else {
                                        Text("لم يتم التعيين - يرجى الإعداد للحماية ⚠️", fontSize = 11.sp, color = Color.Red)
                                    }
                                }
                                Button(
                                    onClick = {
                                        inputSecurityQuestion = securityQuestion
                                        inputSecurityAnswer = securityAnswer
                                        showMandatorySecurityDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f), contentColor = accentColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("تعديل السؤال", fontSize = 12.sp)
                                }
                            }
                            
                            if (showPinDialog) {
                                AlertDialog(
                                    onDismissRequest = { showPinDialog = false },
                                    title = { Text("تعيين رمز PIN مخصص", fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(value = tempPin, onValueChange = { tempPin = it }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword))
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.updateAppLockPinCode(tempPin)
                                            viewModel.toggleAppLockPin(true)
                                            showPinDialog = false
                                        }) { Text("حفظ") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showPinDialog = false }) { Text("إلغاء") }
                                    }
                                )
                            }
                            
                            if (showPatternDialog) {
                                AlertDialog(
                                    onDismissRequest = { showPatternDialog = false },
                                    title = { Text("تعيين نقش مخصص", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column {
                                            Text("للتسهيل، يرجى كتابة كلمة مرور/نقش نصي (مثل: 12369) ليحاكي النقش", fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(value = tempPattern, onValueChange = { tempPattern = it }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password))
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.updateAppLockPatternCode(tempPattern)
                                            viewModel.toggleAppLockPattern(true)
                                            showPatternDialog = false
                                        }) { Text("حفظ") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showPatternDialog = false }) { Text("إلغاء") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        var showClearDataConfirm by remember { mutableStateOf(false) }
        var showDeleteFarmConfirm by remember { mutableStateOf(false) }

        // Card: النسخ الاحتياطي والمزامنة (Firebase)
        SettingSection(
            titleText = "حالة الاتصال والمزامنة (Firebase)",
            icon = Icons.Default.CloudSync,
            isExpanded = expandedCategory == "firebase",
            onToggle = { toggleCategory("firebase") }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val coroutineScope = rememberCoroutineScope()
                var firebaseStatus by remember { mutableStateOf("لم يتم الفحص") }
                
                Text(text = "حالة الاتصال: $firebaseStatus", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                
                Button(
                    onClick = {
                        firebaseStatus = "جاري الفحص..."
                        coroutineScope.launch {
                            val firebaseManager = com.example.data.remote.FirebaseManager()
                            try {
                                val auth = firebaseManager.auth
                                if (auth.currentUser == null) {
                                    val success = firebaseManager.signInAnonymously()
                                    if (success) {
                                        firebaseStatus = "متصل بنجاح ✅ (حساب زائر)"
                                    } else {
                                        firebaseStatus = "فشل في تسجيل الدخول ❌"
                                    }
                                } else {
                                    firebaseStatus = "متصل بنجاح ✅ (معرف: ${auth.currentUser?.uid?.take(8)}...)"
                                }
                            } catch (e: Exception) {
                                firebaseStatus = "فشل الاتصال ❌: ${e.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("أداة فحص وتأكيد الاتصال بـ Firebase", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CloudDone, contentDescription = "اختبار", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Card 5: منطقة الخطر/النظام
        SettingSection(
            titleText = "منطقة الخطر/النظام",
            icon = Icons.Default.Warning,
            isExpanded = expandedCategory == "system",
            onToggle = { toggleCategory("system") }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.factoryResetSettings()
                        Toast.makeText(context, "تمت إعادة استعادة ضبط المصنع للواجهة", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("استعادة ضبط المصنع للتخصيص", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Refresh, contentDescription = "استعادة", tint = MaterialTheme.colorScheme.onSurface)
                }

                Button(
                    onClick = { showClearDataConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("مسح بيانات المزرعة", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.DeleteSweep, contentDescription = "مسح", tint = Color.White)
                }

                Button(
                    onClick = { showDeleteFarmConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("حذف المزرعة نهائياً", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.DeleteForever, contentDescription = "حذف نهائي", tint = Color.White)
                }
            }
        }

        if (showClearDataConfirm) {
            AlertDialog(
                onDismissRequest = { showClearDataConfirm = false },
                title = { Text("تأكيد مسح البيانات", fontWeight = FontWeight.Bold, color = Color(0xFFF97316)) },
                text = { Text("هل أنت متأكد من مسح جميع الحيوانات، الأعلاف، المعاملات والملاحظات؟ هذا الإجراء لا يمكن التراجع عنه.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDataConfirm = false
                        securityAuthAction = {
                            viewModel.clearCurrentFarmData()
                            Toast.makeText(context, "تم مسح بيانات المزرعة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        securityAuthError = ""
                        securityAuthInputPin = ""
                        securityAuthInputAnswer = ""
                        showSecurityAuthDialog = true
                    }) {
                        Text("نعم، امسح البيانات", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataConfirm = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        if (showDeleteFarmConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteFarmConfirm = false },
                title = { Text("حذف المزرعة", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
                text = { Text("سيؤدي هذا إلى حذف المزرعة كاملة بجميع بياناتها وسجلاتها نهائياً من الجهاز! هل ترغب بالاستمرار؟") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteFarmConfirm = false
                        securityAuthAction = {
                            viewModel.deleteCurrentFarm()
                            Toast.makeText(context, "تم حذف المزرعة نهائياً", Toast.LENGTH_SHORT).show()
                        }
                        securityAuthError = ""
                        securityAuthInputPin = ""
                        securityAuthInputAnswer = ""
                        showSecurityAuthDialog = true
                    }) {
                        Text("حذف المزرعة", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteFarmConfirm = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        if (showMandatorySecurityDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showMandatorySecurityDialog = false 
                },
                title = { Text("سؤال أمان الحساب والطوارئ 🔑", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "يرجى تحديد سؤال أمان مخصص وإدخال إجابته الحامية. هذا السؤال سيكون وسيلتك الوحيدة لاسترجاع كود الدخول في حال نسيانه.",
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = inputSecurityQuestion,
                            onValueChange = { inputSecurityQuestion = it },
                            label = { Text("سؤال الأمان (مثال: ما اسم أول مدرسة التحقت بها؟)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputSecurityAnswer,
                            onValueChange = { inputSecurityAnswer = it },
                            label = { Text("إجابة السؤال (تأكد من حفظها بدقة)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputSecurityQuestion.isNotBlank() && inputSecurityAnswer.isNotBlank()) {
                                viewModel.updateSecurityQuestion(inputSecurityQuestion, inputSecurityAnswer)
                                viewModel.toggleAppLock(true)
                                showMandatorySecurityDialog = false
                                Toast.makeText(context, "تم حفظ وسيلة الأمان وتفعيل القفل بنجاح 🛡️", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "الرجاء كتابة السؤال والجواب معاً لاستكمال التفعيل", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("حفظ وتأمين الدخول", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showMandatorySecurityDialog = false 
                    }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        if (showSecurityAuthDialog) {
            val appLockPinCode by viewModel.appLockPinCode.collectAsStateWithLifecycle()
            val appLockPinEnabled by viewModel.appLockPinEnabled.collectAsStateWithLifecycle()

            val checkAndAuthorize = {
                var authorized = false
                if (appLockPinEnabled && appLockPinCode.isNotEmpty()) {
                    if (securityAuthInputPin == appLockPinCode) {
                        authorized = true
                    }
                }
                if (securityQuestion.isNotEmpty() && securityAnswer.isNotEmpty()) {
                    if (securityAuthInputAnswer.trim().equals(securityAnswer.trim(), ignoreCase = true)) {
                        authorized = true
                    }
                }
                if (!appLockPinEnabled && securityQuestion.isEmpty()) {
                    authorized = true
                }

                if (authorized) {
                    securityAuthAction?.invoke()
                    showSecurityAuthDialog = false
                } else {
                    securityAuthError = "الرمز المدخل أو إجابة سؤال الأمان غير صحيحة!"
                }
            }

            AlertDialog(
                onDismissRequest = { showSecurityAuthDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Red, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("التحقق الأمني من الهوية ⚠️", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "أنت بصدد تغيير أو مسح ملفات المزرعة الحساسة. يرجى تأكيد ملكيتك وإثبات هويتك لإكمال المسح.",
                            fontSize = 13.sp
                        )

                        if (securityAuthError.isNotEmpty()) {
                            Text(securityAuthError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (appLockPinEnabled && appLockPinCode.isNotEmpty()) {
                            OutlinedTextField(
                                value = securityAuthInputPin,
                                onValueChange = { securityAuthInputPin = it; securityAuthError = "" },
                                label = { Text("أدخل رمز PIN لقفل التطبيق") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (securityQuestion.isNotEmpty() && securityAnswer.isNotEmpty()) {
                            Text("سؤال الأمان الحالي: $securityQuestion", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = securityAuthInputAnswer,
                                onValueChange = { securityAuthInputAnswer = it; securityAuthError = "" },
                                label = { Text("إجابة سؤال الأمان") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                if (context is androidx.fragment.app.FragmentActivity) {
                                    com.example.utils.BiometricUtils.authenticate(
                                        activity = context,
                                        title = "تأكيد هوية المدير للمسح الحساس",
                                        subtitle = "تأكيد الإجراء عبر النظام لمنع العبث",
                                        onSuccess = {
                                            securityAuthAction?.invoke()
                                            showSecurityAuthDialog = false
                                        },
                                        onError = { 
                                            securityAuthError = "فشل التحقق بالنظام الإفتراضي للموبايل"
                                        }
                                    )
                                } else {
                                    securityAuthError = "المحاكي أو الجهاز لا يدعم بصمة النظام حالياً."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بصمة الإصبع أو قفل الموبايل 📱", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            var pinAnswerMatched = false
                            if (appLockPinEnabled && appLockPinCode.isNotEmpty() && securityAuthInputPin == appLockPinCode) {
                                pinAnswerMatched = true
                            }
                            if (securityQuestion.isNotEmpty() && securityAnswer.isNotEmpty() && securityAuthInputAnswer.trim().equals(securityAnswer.trim(), ignoreCase = true)) {
                                pinAnswerMatched = true
                            }
                            
                            if (pinAnswerMatched) {
                                securityAuthAction?.invoke()
                                showSecurityAuthDialog = false
                            } else {
                                if (context is androidx.fragment.app.FragmentActivity) {
                                    com.example.utils.BiometricUtils.authenticate(
                                        activity = context,
                                        title = "التحقق الأمني من الهوية ⚠️",
                                        subtitle = "أنت بصدد تغيير أو مسح ملفات المزرعة الحساسة.",
                                        onSuccess = {
                                            securityAuthAction?.invoke()
                                            showSecurityAuthDialog = false
                                        },
                                        onError = { err ->
                                            securityAuthError = "فشل التحقق بالنظام: $err"
                                        }
                                    )
                                } else {
                                    securityAuthError = "الرمز أو الإجابة غير صحيحة، أو عذرًا الجهاز لا يدعم البصمة الإفتراضية."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("تأكيد ومسح 🗑️", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSecurityAuthDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SubSection(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun ExpandableSubSection(
    title: String,
    subtitle: String? = null,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.03f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = accentColor
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
        
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingSection(
    titleText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isExpanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val parts = titleText.split("\n", limit = 2)
    val title = parts[0]
    val subtitle = if (parts.size > 1) parts[1] else null

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onToggle != null) it.clip(RoundedCornerShape(12.dp)).clickable { onToggle() } else it }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onToggle != null) {
                    Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Right)
                    if (icon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ColorButton(label: String, colorHex: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colorString = colorHex.replace("#", "")
    val colorInt = try { android.graphics.Color.parseColor("#$colorString") } catch (e: Exception) { android.graphics.Color.GRAY }
    val color = Color(colorInt)
    
    val contentColor = if (colorHex.uppercase() == "#FFFFFF" || colorHex.uppercase() == "#F3F4F6" || colorHex.uppercase() == "#FDE047" || colorHex.uppercase() == "#FDF2F8") Color.Black else Color.White

    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = contentColor, modifier = Modifier.size(16.dp))
            }
            Text(label, color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun OptionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, isSelected: Boolean, onClick: () -> Unit, accentColor: Color, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(
                label, 
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, 
                fontSize = 12.sp, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            if (icon != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SwitchSettingItem(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

