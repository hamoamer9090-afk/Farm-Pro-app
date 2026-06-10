package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailsScreen(
    viewModel: FarmViewModel,
    animalId: Int,
    accentColor: Color,
    onBack: () -> Unit,
    onSell: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (com.example.data.model.AnimalEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val animals by viewModel.animalsList.collectAsStateWithLifecycle()
    val archivedAnimals by viewModel.archiveAnimalsList.collectAsStateWithLifecycle()
    val animal = remember(animals, archivedAnimals, animalId) {
        animals.find { it.id == animalId } ?: archivedAnimals.find { it.id == animalId }
    }
    var showCascadeDeleteDialog by remember { mutableStateOf(false) }

    if (animal == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم يتم العثور على الحيوان", fontSize = 16.sp, color = Color.Gray)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل ${animal.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
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
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المعلومات الأساسية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("الاسم: ${animal.name} (${animal.type})")
                    Text("النوع: ${animal.type}")
                    Text("اشتريت من التاجر: ${animal.merchantName}")
                    Text("السعر: ${animal.purchasePrice} جنيه")
                    Text("الوزن الدفتري: ${animal.weight} كغ")
                    Text("تاريخ الدخول: ${animal.arrivalDate}")
                    Text("العمر الحالي: ${animal.age}")
                    Text("تكلفة طعام هذا الرأس المخصص: ${animal.feedCost} جنيه", color = Color(0xFFD97706))
                }
            }

            if (animal.isArchived) {
                // Archived layout choices
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (animal.salePrice > 0.0) {
                            Text("محفوظات المبيعات والأرباح 💰", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("حالة القيد: مبيع مكتمل وأرشيف مالي", color = Color.White, fontSize = 14.sp)
                            Text("سعر البيع الإجمالي: ${animal.salePrice} جنيه", color = Color.LightGray, fontSize = 13.sp)
                            Text("المشتري/التاجر: ${animal.merchantName.ifBlank { "غير محدد" }}", color = Color.LightGray, fontSize = 13.sp)
                            Text("تاريخ مغادرة الحظيرة: ${animal.departureDate.ifBlank { "غير محدد" }}", color = Color.LightGray, fontSize = 13.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            var showRefundDialog by remember { mutableStateOf(false) }
                            
                            Button(
                                onClick = { showRefundDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إسترجاع المبيعات وإلغاء الأرشفة 🔄", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            
                            if (showRefundDialog) {
                                var refundSettlementType by remember { mutableStateOf(if (animal.associatedPersonId != null) "adjust_deferred" else "none") } // "adjust_deferred", "cash_out", "none"
                                var refundAmountStr by remember { mutableStateOf(animal.salePrice.toString()) }
                                var refundReason by remember { mutableStateOf("") }
                                
                                AlertDialog(
                                    onDismissRequest = { showRefundDialog = false },
                                    title = { Text("تسوية استرجاع مبيعات رأس الماشية 🔄") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("سيتم التراجع عن البيع، وإعادة الرأس كحيوان نشط بالحظيرة. الرجاء اختيار طريقة تسوية مبالغ الاسترجاع:", fontSize = 12.sp, color = Color.Gray)
                                            
                                            Text("طريقة التسوية المالية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = refundSettlementType == "cash_out", onClick = { refundSettlementType = "cash_out" })
                                                Text("إرجاع نقداً (دفع كاش من مصروفات المزرعة)", fontSize = 12.sp)
                                            }
                                            if (animal.associatedPersonId != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = refundSettlementType == "adjust_deferred", onClick = { refundSettlementType = "adjust_deferred" })
                                                    Text("تسوية الحساب الآجل (تعديل دين الشريك/المشتري)", fontSize = 12.sp)
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = refundSettlementType == "none", onClick = { refundSettlementType = "none" })
                                                Text("بدون تسوية مالية (إلغاء البيع فقط)", fontSize = 12.sp)
                                            }
                                            
                                            OutlinedTextField(
                                                value = refundAmountStr,
                                                onValueChange = { refundAmountStr = it },
                                                label = { Text("قيمة الاسترجاع المالي (جنيه)") },
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            OutlinedTextField(
                                                value = refundReason,
                                                onValueChange = { refundReason = it },
                                                label = { Text("سبب الاسترجاع (اختياري)") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                val refundAmount = refundAmountStr.toDoubleOrNull() ?: 0.0
                                                viewModel.refundSoldAnimal(animal, refundSettlementType, refundAmount, refundReason)
                                                Toast.makeText(context, "تمت عملية استرجاع الرأس وإدراجها كنشط بالحظيرة بنجاح! ✅", Toast.LENGTH_LONG).show()
                                                showRefundDialog = false
                                                onBack()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                        ) {
                                            Text("تأكيد الاسترجاع والعود")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRefundDialog = false }) {
                                            Text("إلغاء")
                                        }
                                    }
                                )
                            }
                        } else {
                            Text("سجل حالة نفوق (Deceased) ⚠️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("السبب والتوثيق المكتوب: ${animal.departureDate.ifBlank { "لم يذكر سبب" }}", color = Color.LightGray, fontSize = 13.sp)
                            Text("الحالة: غير نشط في الحظيرة ومصنف كنقوق.", color = Color.White, fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    val revived = animal.copy(
                                        departureDate = "",
                                        isArchived = false
                                    )
                                    viewModel.updateAnimalDetails(revived)
                                    Toast.makeText(context, "تم إلغاء حالة النفوق وإدراج الرأس كنشطة بالحظيرة من جديد! ✅", Toast.LENGTH_LONG).show()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إلغاء النفوق وسحب الرأس كنشط 🐄")
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        var showSellDialog by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                if (!viewModel.hasPermission("add_transaction")) {
                                    Toast.makeText(context, "عذراً، ليس لديك صلاحية للبيع ❌", Toast.LENGTH_LONG).show()
                                } else {
                                    showSellDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بيع الرأس", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        
                        if (showSellDialog) {
                            var sellPriceStr by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showSellDialog = false },
                                title = { Text("بيع الحيوان 🐄") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = sellPriceStr,
                                            onValueChange = { sellPriceStr = it },
                                            label = { Text("سعر البيع (جنيه)") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        val sp = sellPriceStr.toDoubleOrNull() ?: 0.0
                                        if (sp > 0) {
                                            val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                                            viewModel.sellAnimal(animal, sp, formattedDate, animal.associatedPersonId)
                                            showSellDialog = false
                                            onBack() // go back to list
                                            Toast.makeText(context, "تم بيع الحيوان وأرشفته وتسجيل الإيراد", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "أدخل سعر بيع صحيح", Toast.LENGTH_SHORT).show()
                                        }
                                    }) { Text("تأكيد البيع") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSellDialog = false }) { Text("إلغاء") }
                                }
                            )
                        }

                        var showMortalityLocalDialog by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                if (!viewModel.hasPermission("add_transaction")) {
                                    Toast.makeText(context, "عذراً، ليس لديك صلاحية ❌", Toast.LENGTH_LONG).show()
                                } else {
                                    showMortalityLocalDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تسجيل نفوق", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        
                        if (showMortalityLocalDialog) {
                            var reason by remember { mutableStateOf("") }
                            var affectFinancials by remember { mutableStateOf(true) }
                            AlertDialog(
                                onDismissRequest = { showMortalityLocalDialog = false },
                                title = { Text("تسجيل حالة نفوق ⚠️", color = Color.Red) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = reason,
                                            onValueChange = { reason = it },
                                            label = { Text("سبب النفوق (اختياري)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = affectFinancials, onCheckedChange = { affectFinancials = it })
                                            Text("تسجيل كخسارة مالية في السجلات", fontSize = 12.sp)
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                                            viewModel.logMortality(animal, reason, formattedDate, affectFinancials)
                                            showMortalityLocalDialog = false
                                            onBack()
                                            Toast.makeText(context, "تم أرشفة الحيوان وتسجيل النفوق", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) { Text("تأكيد النفوق", color = Color.White) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showMortalityLocalDialog = false }) { Text("إلغاء") }
                                }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (!viewModel.hasPermission("edit_animal")) {
                                    Toast.makeText(context, "عذراً، ليس لديك صلاحية التعديل ❌", Toast.LENGTH_LONG).show()
                                } else {
                                    onEdit(animal)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Button(
                            onClick = {
                                showCascadeDeleteDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }

                var showMortalityDialog by remember { mutableStateOf(false) }
                var mortalityReason by remember { mutableStateOf("") }
                
                Button(
                    onClick = { showMortalityDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل كنقوق (نفوق)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (showMortalityDialog) {
                    AlertDialog(
                        onDismissRequest = { showMortalityDialog = false },
                        title = { Text("تسجيل حالة نفوق (Deceased)") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("سيتم نقل الحيوان إلى الأرشيف وتوثيق الخسارة المالية.", fontSize = 14.sp)
                                OutlinedTextField(
                                    value = mortalityReason,
                                    onValueChange = { mortalityReason = it },
                                    label = { Text("سبب النفوق") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.archiveAnimalDueToMortality(animal, mortalityReason)
                                    Toast.makeText(context, "تم تسجيل حالة النفوق بقواعد البيانات", Toast.LENGTH_SHORT).show()
                                    showMortalityDialog = false
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("تأكيد النفوق")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMortalityDialog = false }) { Text("إلغاء") }
                        }
                    )
                }
            }
        }
    }

    if (showCascadeDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showCascadeDeleteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنبيه بالنتائج المترتبة ⚠️", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "إن حذف رأس الماشية (${animal.name}) سيترتب عليه نقل أو حذف جميع البيانات المرتبطة تلقائياً، بما في ذلك:\n" +
                    "• سجلات اللقاح والتحصينات الطبية المترتبة.\n" +
                    "• سجلات وجداول التغذية والأعلاف المستهلكة.\n" +
                    "• سجلات الولادة والنسب والجيل المرتبط بها لضمان دقة الإحصائيات.\n\n" +
                    "هل أنت متأكد من رغبتك بالاستمرار في الحذف ونقل البيانات للأرشيف؟",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAnimalRecord(animal)
                        Toast.makeText(context, "تم نقل رأس الماشية للأرشيف بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                        showCascadeDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، تابع الحذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCascadeDeleteDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}
