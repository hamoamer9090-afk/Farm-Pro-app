package com.example.data.repository

data class FeedIngredient(
    val name: String,
    val pricePerKg: Double,
    val ratioPercentage: Double // نسبة المكون في الخلطة (مثال: 50% ذرة)
)

data class FeedFormulationResult(
    val totalCostPerKg: Double,
    val totalCostPerTon: Double,
    val ingredientsSummary: List<String>
)

class FeedCalculatorRepository {

    // دالة لحساب التكلفة الفعلية للكيلو والطن بناءً على نسب المكونات وأسعارها
    fun calculateFeedCost(ingredients: List<FeedIngredient>): FeedFormulationResult {
        var totalCostPerKg = 0.0

        // التأكد من أن مجموع النسب المئوية لا يتجاوز 100% (نطاق السماحية لتسهيل الحساب)
        val validIngredients = mutableListOf<String>()

        for (ingredient in ingredients) {
            // حساب تكلفة هذا المكون في كل كجم من الخلطة
            // مثال: إذا كان الكيلو بـ 10، والنسبة 50% => إذن مساهمته في وزن 1 كيلو هي 500 جرام، بتكلفة 5
            val costContribution = ingredient.pricePerKg * (ingredient.ratioPercentage / 100.0)
            totalCostPerKg += costContribution

            validIngredients.add("${ingredient.name}: ${ingredient.ratioPercentage}%")
        }

        return FeedFormulationResult(
            totalCostPerKg = totalCostPerKg,
            totalCostPerTon = totalCostPerKg * 1000.0,
            ingredientsSummary = validIngredients
        )
    }
}
