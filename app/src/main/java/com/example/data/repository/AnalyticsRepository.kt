package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class FinanceSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netProfit: Double
)

data class ExpenseCategoryInfo(
    val category: String,
    val totalAmount: Double,
    val percentage: Float
)

class AnalyticsRepository(private val farmRepository: FarmRepository) {

    // جلب الإحصائيات المالية الكلية للمزرعة
    fun getFinanceSummary(farmName: String): Flow<FinanceSummary> {
        return farmRepository.getTransactions(farmName).map { transactions ->
            val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
            FinanceSummary(
                totalIncome = income,
                totalExpense = expense,
                netProfit = income - expense
            )
        }
    }

    // جلب توزيع المصروفات كنسب مئوية لاستخدامها في الرسم البياني الدائري (Pie Chart)
    fun getExpensesDistribution(farmName: String): Flow<List<ExpenseCategoryInfo>> {
        return farmRepository.getTransactions(farmName).map { transactions ->
            val expenses = transactions.filter { it.type == "expense" }
            val totalExpense = expenses.sumOf { it.amount }
            if (totalExpense == 0.0) return@map emptyList()

            // تجميع المصاريف حسب الفئة (Category)
            val categoryMap = expenses.groupBy { it.category }
            categoryMap.map { (category, txList) ->
                val catSum = txList.sumOf { it.amount }
                ExpenseCategoryInfo(
                    category = category,
                    totalAmount = catSum,
                    percentage = (catSum / totalExpense).toFloat() * 100f
                )
            }.sortedByDescending { it.totalAmount }
        }
    }

    // هنا يمكن إضافة منطق جلب المصروفات والأرباح شهرياً لخدمة الرسم البياني الخطي
}
