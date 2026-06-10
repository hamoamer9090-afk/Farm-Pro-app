package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
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

@Composable
fun RecycleBinScreen(viewModel: FarmViewModel, themePrimary: Color, zoom: Float) {
    val items by viewModel.recycleBinItems.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("سجل المحذوفات (سلة المهملات) \uD83D\uDDD1️", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
        Text("يتم حفط العناصر المحذوفة هنا، وسيتم إزالتها نهائياً بعد مرور 30 يوماً.", fontSize = 12.sp, color = Color.Gray)

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد عناصر محذوفة", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    val displayInfo = remember(item) {
                        try {
                            val obj = org.json.JSONObject(item.itemJson)
                            when (item.itemType) {
                                "transaction" -> {
                                    val typeAr = if (obj.optString("type") == "income") "إيرادات" else "مصروفات"
                                    val desc = obj.optString("description")
                                    val amount = obj.optDouble("amount")
                                    Pair("قيد مالي ($typeAr) 💵", "$desc\nالقيمة: $amount جنيه")
                                }
                                "animal" -> {
                                    val name = obj.optString("name")
                                    val type = obj.optString("type")
                                    val price = obj.optDouble("purchasePrice")
                                    Pair("رأس ماشية (حظيرة) 🐂", "الاسم/الوسم: $name\nالسلالة: $type - قيمة الشراء: $price ج")
                                }
                                "feed" -> {
                                    val name = obj.optString("feedName")
                                    val cost = obj.optDouble("totalCost")
                                    val weight = obj.optDouble("totalWeight")
                                    Pair("سجل تغذية/أعلاف 🌾", "العلف: $name\nالوزن: $weight كغ - التكلفة: $cost ج")
                                }
                                "medicine" -> {
                                    val name = obj.optString("name")
                                    val cost = obj.optDouble("totalCost")
                                    Pair("سجل رعاية ولقاحات طبي 💊", "العلاج: $name\nالتكلفة الإجمالية: $cost جنيه")
                                }
                                "person" -> {
                                    val name = obj.optString("name")
                                    val role = obj.optString("role")
                                    Pair("حساب جهة/شخص 👥", "الاسم: $name\nالصفة/الدور: $role")
                                }
                                "note" -> {
                                    val content = obj.optString("content")
                                    Pair("ملاحظة أو روشتة مدونة 📝", content)
                                }
                                else -> {
                                    Pair("عنصر محذوف (${item.itemType})", item.itemJson.take(60) + "...")
                                }
                            }
                        } catch (e: Exception) {
                            Pair("عنصر محذوف (${item.itemType})", item.itemJson.take(60) + "...")
                        }
                    }

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayInfo.first, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(displayInfo.second, color = Color.LightGray, fontSize = 11.sp, maxLines = 3)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { 
                                        viewModel.restoreTransactionRecord(item)
                                        Toast.makeText(context, "تمت الاستعادة بنجاح وإرجاع العنصر لموقعه! ✅", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("إستعادة ⤴️", color = Color.White, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.hardDeleteRecycleBinItem(item) }) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "حذف نهائي", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
