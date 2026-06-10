package com.example.utils

data class AnimalType(val male: String, val female: String) {
    fun getName(isFemale: Boolean): String = if (isFemale) female else male
    override fun toString(): String = "$male|$female"
}

object AnimalTypeHelper {
    fun parseAnimalType(typeStr: String): AnimalType {
        if (typeStr.contains("|")) {
            val parts = typeStr.split("|")
            if (parts.size >= 2) {
                return AnimalType(parts[0].trim(), parts[1].trim())
            }
        }
        return when (typeStr.trim()) {
            "عجل" -> AnimalType("عجل", "عجلة")
            "أغنام" -> AnimalType("خروف", "نعجة")
            "خروف" -> AnimalType("خروف", "نعجة")
            "ماعز" -> AnimalType("جدي", "عنزة")
            "جمال", "جمل" -> AnimalType("جمل", "ناقة")
            "جاموس" -> AnimalType("جاموس", "جاموسة")
            "بقر", "بقرة" -> AnimalType("ثور", "بقرة")
            else -> {
                val male = typeStr.trim()
                val female = if (male.endsWith("ة")) male else male + "ة"
                AnimalType(male, female)
            }
        }
    }
}
