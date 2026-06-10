package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FarmViewModel
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementDialog(
    viewModel: FarmViewModel,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val googleName by viewModel.googleUserName.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val profilePic by viewModel.userProfilePic.collectAsStateWithLifecycle()
    val allFarms by viewModel.allFarms.collectAsStateWithLifecycle()
    val currentFarm by viewModel.currentFarm.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(googleName) }

    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = ImageUtils.uriToBase64(context, it)
            if (base64 != null) {
                viewModel.updateUserProfilePic(base64)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إدارة الحساب", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Section
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePic.isNotEmpty()) {
                            val bmp = ImageUtils.base64ToBitmap(profilePic)
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "صورة الحساب",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "اختر صورة", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (googleName.isNotEmpty()) googleName else "المزارع",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            IconButton(onClick = { showEditNameDialog = true; newName = googleName }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل الاسم", tint = accentColor, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (googleEmail.isNotEmpty()) {
                            Text(text = googleEmail, color = Color.Gray, fontSize = 12.sp)
                        } else {
                            Text(text = "حساب محلي (لم يتم ربط Google)", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text("مزارعك المرتبطة (اضغط للتبديل السريع) 🚜:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(allFarms) { farm ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectFarm(farm.name, "")
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = if (farm.name == currentFarm) accentColor.copy(alpha = 0.11f) else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Agriculture, contentDescription = null, tint = if (farm.name == currentFarm) accentColor else Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(farm.name, fontWeight = if (farm.name == currentFarm) FontWeight.Bold else FontWeight.Normal)
                                }
                                if (farm.name == currentFarm) {
                                    Text("نشط ✅", fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                } else {
                                    IconButton(
                                        onClick = { 
                                            viewModel.deleteFarmByName(farm.name) 
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف المزرعة", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("إضافة مزرعة سريعة جديدة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                
                var newFarmNameInput by remember { mutableStateOf("") }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newFarmNameInput,
                        onValueChange = { newFarmNameInput = it },
                        placeholder = { Text("مثال: حظيرة النخيل 🌴", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).heightIn(max = 56.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newFarmNameInput.isNotBlank()) {
                                viewModel.createFarm(newFarmNameInput, "")
                                newFarmNameInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("إضافة", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("تم")
            }
        }
    )

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("تعديل اسم الحساب/المزارع", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("الاسم") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.updateGoogleUserName(newName)
                        }
                        showEditNameDialog = false
                    }
                ) { Text("حفظ", color = accentColor) }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("إلغاء", color = Color.Gray) }
            }
        )
    }
}
