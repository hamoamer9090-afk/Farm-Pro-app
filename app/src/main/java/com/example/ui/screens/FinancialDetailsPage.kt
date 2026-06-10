package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.FarmViewModel
import com.example.data.model.TransactionEntity
import com.example.data.model.PersonEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDetailsPage(
    viewModel: FarmViewModel,
    type: String, // "cash", "credits", "debts"
    accentColor: Color,
    onBack: () -> Unit
) {
    val persons by viewModel.peopleList.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsList.collectAsStateWithLifecycle()

    val title = when (type) {
        "cash" -> "تحليلات وتفاصيل الكاش الفعلي"
        "credits" -> "تفاصيل ديونك المستحقة (لك)"
        "debts" -> "تفاصيل مستحقات الغير (عليك)"
        else -> "التحليل المالي التفصيلي"
    }

    var searchQuery by remember { mutableStateOf("") }
    
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
    
    val actualCash = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        title, 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Metrics Summary Header (Dashboard style inside details screen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (type) {
                        "cash" -> {
                            Text(
                                "ملخص حركة الخزينة والصندوق:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Income Metric
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.TrendingUp, "وارد", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("إجمالي المقبوضات", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        Text("${totalIncome} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                                    }
                                }
                                // Expense Metric
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.TrendingDown, "مصروف", tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("إجمالي المدفوعات", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                        Text("${totalExpense} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الرصيد الفعلي المتوفر بالخزينة:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "${actualCash} ج.م", 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Black,
                                    color = if (actualCash >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                        "credits" -> {
                            val activeCreditors = persons.filter { it.balance > 0 }
                            val totalCreditsAmt = activeCreditors.sumOf { it.balance }
                            Text(
                                "ملخص مستحقات المزرعة المتبقية بالخارج:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1E3A8A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("عدد المتعاملين المدينين لك:", fontSize = 12.sp, color = Color.Gray)
                                    Text("${activeCreditors.size} أشخاص / تجار", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("إجمالي المبالغ المستحقة لك", fontSize = 11.sp, color = Color(0xFF1D4ED8))
                                        Text("${totalCreditsAmt} ج.م", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF1D4ED8))
                                    }
                                }
                            }
                        }
                        "debts" -> {
                            val activeDebtors = persons.filter { it.balance < 0 }
                            val totalDebtsAmt = activeDebtors.sumOf { kotlin.math.abs(it.balance) }
                            Text(
                                "ملخص ديون ومستحقات الغير على المزرعة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF7F1D1D)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("عدد الدائنين (تطالب بسدادها):", fontSize = 12.sp, color = Color.Gray)
                                    Text("${activeDebtors.size} جهات / تجار", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("إجمالي الديون المطلوبة", fontSize = 11.sp, color = Color(0xFFB91C1C))
                                        Text("${totalDebtsAmt} ج.م", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFB91C1C))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar for transactions or persons
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("بحث بالاسم، الوصف، أو الفئة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Body List
            if (type == "cash") {
                val cashTransactions = remember(transactions, searchQuery) {
                    transactions.filter {
                        (it.type == "income" || it.type == "expense") &&
                        (it.description.contains(searchQuery, ignoreCase = true) ||
                         it.category.contains(searchQuery, ignoreCase = true) ||
                         (persons.find { p -> p.id == it.associatedPersonId }?.name?.contains(searchQuery, ignoreCase = true) == true) ||
                         it.amount.toString().contains(searchQuery))
                    }
                }

                if (cashTransactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا توجد سجلات معاملات مطابقة للبحث.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    Text(
                        "تفاصيل سجل الخزينة والحركات المباشرة (${cashTransactions.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(cashTransactions) { t ->
                            val assocPersonName = remember(persons, t.associatedPersonId) {
                                persons.find { it.id == t.associatedPersonId }?.name ?: "رصيد عام / غير محدد"
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (t.type == "income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (t.type == "income") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (t.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = t.description.ifEmpty { "معاملة بدون وصف" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(t.category, fontSize = 10.sp) },
                                                    modifier = Modifier.height(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "لـ: $assocPersonName",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (t.type == "income") "+" else "-"}${t.amount} جنيه",
                                            color = if (t.type == "income") Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(t.date, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // credits or debts
                val filteredPeople = remember(persons, searchQuery) {
                    val matchingPart = persons.filter {
                        (type == "credits" && it.balance > 0) || (type == "debts" && it.balance < 0)
                    }
                    if (searchQuery.isBlank()) matchingPart
                    else {
                        matchingPart.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.role.contains(searchQuery, ignoreCase = true) ||
                            it.phone.contains(searchQuery) ||
                            kotlin.math.abs(it.balance).toString().contains(searchQuery)
                        }
                    }
                }

                if (filteredPeople.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("لا توجد أسماء تطابق البحث حالياً.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    Text(
                        if (type == "credits") "قائمة الأشخاص والتجار المستحقين مبالغ ومستحقات لك:" else "قائمة الدائنين والالتزامات المستحقة لغيرك من المزرعة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    var showSettlementDialog by remember { mutableStateOf<PersonEntity?>(null) }
                    var settlementAmount by remember { mutableStateOf("") }
                    val context = androidx.compose.ui.platform.LocalContext.current

                    if (showSettlementDialog != null) {
                        AlertDialog(
                            onDismissRequest = { showSettlementDialog = null },
                            title = { Text("تسوية حساب (Debt Settlement)") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("تسوية الرصيد لصالح/على: ${showSettlementDialog?.name}", fontSize = 14.sp)
                                    OutlinedTextField(
                                        value = settlementAmount,
                                        onValueChange = { settlementAmount = it },
                                        label = { Text("المبلغ المراد تسويته (SDG/EGP)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val amt = settlementAmount.toDoubleOrNull() ?: 0.0
                                        if (amt > 0.0) {
                                            val person = showSettlementDialog!!
                                            val isWeArePaying = person.balance < 0
                                            val newBalance = if (isWeArePaying) person.balance + amt else person.balance - amt
                                            
                                            viewModel.updatePersonRecord(person.copy(balance = newBalance))
                                            viewModel.registerManualTransaction(
                                                type = if (isWeArePaying) "expense" else "income",
                                                amount = amt,
                                                description = "تسوية مديونية / حساب مع ${person.name}",
                                                category = "تسوية حسابات",
                                                personId = person.id
                                            )
                                            android.widget.Toast.makeText(context, "تمت التسوية بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                                            showSettlementDialog = null
                                            settlementAmount = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("تأكيد التسوية", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSettlementDialog = null }) { Text("إلغاء") }
                            }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredPeople) { p ->
                            val absBalance = kotlin.math.abs(p.balance)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSettlementDialog = p },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (p.balance > 0) Color(0xFFEFF6FF) else Color(0xFFFEF2F2)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (p.balance > 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (p.balance > 0) Color(0xFF1D4ED8) else Color(0xFFB91C1C),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(p.role, fontSize = 10.sp) },
                                                    modifier = Modifier.height(20.dp)
                                                )
                                                if (p.phone.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(p.phone, fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$absBalance جنيه",
                                            color = if (p.balance > 0) Color(0xFF1D4ED8) else Color(0xFFB91C1C),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (p.balance > 0) "تطالبه بالمبلغ 📂" else "يطالبك بالمبلغ ⚠️",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (p.balance > 0) Color(0xFF1D4ED8) else Color(0xFFB91C1C)
                                        )
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
