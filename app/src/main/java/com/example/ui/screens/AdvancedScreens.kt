package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCalculatorScreen(
    themePrimary: Color,
    cardBgColor: Color
) {
    var ingredients by remember { mutableStateOf(listOf(FeedIngredientInput("", "", ""))) }
    var totalCostKg by remember { mutableStateOf(0.0) }
    var totalCostTon by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Transparent)
    ) {
        Text(
            text = "حاسبة تكلفة الأعلاف الدقيقة",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = themePrimary
        )
        Text("أدخل اسم المكون، نسبة المكون في الخلطة، وسعر الكيلو", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ingredients.indices.toList()) { index ->
                val item = ingredients[index]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("المكون رقم ${index + 1}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val newList = ingredients.toMutableList()
                                newList.removeAt(index)
                                ingredients = newList
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                            }
                        }
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = {
                                val newList = ingredients.toMutableList()
                                newList[index] = item.copy(name = it)
                                ingredients = newList
                            },
                            label = { Text("اسم المكون (مثال: ذرة)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = item.ratio,
                                onValueChange = {
                                    val newList = ingredients.toMutableList()
                                    newList[index] = item.copy(ratio = it)
                                    ingredients = newList
                                },
                                label = { Text("النسبة المئوية %") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = item.pricePerKg,
                                onValueChange = {
                                    val newList = ingredients.toMutableList()
                                    newList[index] = item.copy(pricePerKg = it)
                                    ingredients = newList
                                },
                                label = { Text("سعر الكيلو") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val newList = ingredients.toMutableList()
                newList.add(FeedIngredientInput("", "", ""))
                ingredients = newList
            },
            colors = ButtonDefaults.buttonColors(containerColor = themePrimary.copy(alpha = 0.2f), contentColor = themePrimary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("إضافة مكون آخر للخلطة", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                var kgCost = 0.0
                ingredients.forEach { 
                    val r = it.ratio.toDoubleOrNull() ?: 0.0
                    val p = it.pricePerKg.toDoubleOrNull() ?: 0.0
                    kgCost += (r / 100.0) * p
                }
                totalCostKg = kgCost
                totalCostTon = kgCost * 1000.0
            },
            colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حساب تكلفة الخلطة", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = themePrimary.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("التكلفة الإجمالية", color = themePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سعر الكيلو", color = Color.Gray, fontSize = 12.sp)
                        Text(String.format("%.2f جنيه", totalCostKg), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = themePrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سعر الطن", color = Color.Gray, fontSize = 12.sp)
                        Text(String.format("%.2f جنيه", totalCostTon), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = themePrimary)
                    }
                }
            }
        }
    }
}

data class FeedIngredientInput(
    val name: String,
    val ratio: String,
    val pricePerKg: String
)

@Composable
fun SyncQueueScreen(
    themePrimary: Color,
    cardBgColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CloudSync, contentDescription = null, tint = themePrimary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "هذا النظام يعمل في الخلفية تلقائياً",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = themePrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "عند انقطاع الإنترنت، يتم تجميع جميع التعديلات والإضافات التي قمت بها وحفظها في قاعدة البيانات المحلية. فور عودة الاتصال، سيقوم طابور المزامنة برفع جميع التغييرات لـ Firestore لتحديث البيانات السحابية.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun AnalyticsDashboardScreen(
    themePrimary: Color,
    cardBgColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PieChart, contentDescription = null, tint = themePrimary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "لوحة الإحصائيات والتقارير",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = themePrimary
        )
        Text("يتم عرض المؤشرات والبيانات البيانية هنا", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBgColor)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("توزيع المصروفات (مجسم تقريبي)", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // Simulated Chart List
                listOf("تغذية - 60%" to Color(0xFFEAB308), "أدوية - 15%" to Color(0xFFEF4444), "أجور - 25%" to Color(0xFF3B82F6)).forEach { (name, color) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    themePrimary: Color,
    cardBgColor: Color
) {
    var reminders by remember { mutableStateOf(listOf("تطعيم الحمى القلاعية", "موعد دفع قسط التاجر", "وزن العجول الدوري")) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Transparent)
    ) {
        Text(
            text = "جدول التنبيهات الذكية",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = themePrimary
        )
        Text("تذكيرات بأهم المواعيد في המזרعة", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reminders) { reminder ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = themePrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(reminder, fontWeight = FontWeight.Bold)
                        }
                        Checkbox(checked = false, onCheckedChange = {})
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AddAlarm, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إضافة موعد/تنبيه جديد", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
