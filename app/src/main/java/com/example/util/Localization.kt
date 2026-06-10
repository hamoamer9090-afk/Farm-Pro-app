package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object Localization {
    private val dictionaryEn = mapOf(
        "تطبيق المزرعة\u060C" to "Farm App, ",
        "تطبيق المزرعة" to "Farm App",
        "مزارعي " to "My Farm ",
        "مزارعي" to "My Farm",
        "مزارعي -" to "My Farm -",
        "الرئيسية" to "Home",
        "الأرشيف" to "Archive",
        "الحظيرة" to "Barn",
        "الأعلاف" to "Feeds",
        "الحسابات" to "Accounts",
        "الكاش الفعلي" to "Actual Cash",
        "حقوقك (لك)" to "Your Rights (Receivable)",
        "مستحقات (عليك)" to "Dues (Payable)",
        "إجراءات تشغيلية سريعة" to "Quick Operations",
        "شراء رأس" to "Buy Livestock",
        "شراء علف" to "Buy Feed",
        "سند قبض" to "Receipt Bond",
        "سند صرف" to "Payment Bond",
        "تصدير ومشاركة تقرير الويب المنسق (HTML/CSS)" to "Export & Share Web PDF Report",
        "الأعلاف والمخزن" to "Feeds & Storage Warehouse",
        "تفاصيل وحصص منفصلة" to "Separate portions & detailed metrics",
        "إدارة الحظيرة" to "Barn Management",
        "0 رأس نشط" to "0 Active Heads",
        "رأس نشط" to "Active Heads",
        "النسخ الاحتياطي" to "Data Backup",
        "استيراد ورفع البيانات" to "Import & cloud upload",
        "حسابات الأشخاص" to "Contact Accounts",
        "إدارة السندات للأسماء" to "Manage bills, accounts & ledger entries",
        "إعدادات" to "Settings",
        "الإعدادات" to "Settings",
        "تسجيل الخروج" to "Sign Out",
        "حساب المزارع" to "Farmer Profile",
        "تأكيد الاتصال بـ Firebase" to "Verify Cloud Sync Connection",
        "مرحباً بك في مدير المزرعة" to "Welcome to FarmManager",
        "اسم المزرعة" to "Farm Name",
        "أدخل اسم المزرعة الجديدة" to "Enter the new farm name",
        "اسم المزرعة مطلوب" to "Farm name is required",
        "ادخل حظيرتك" to "Enter Your Barn",
        "اسم المزرعة الخاص بك" to "Your Farm Name",
        "تغيير اسم المزرعة" to "Change Farm Name",
        "عكس اتجاه السحب" to "Mirror Swipe Direction"
    )

    private val dictionaryAr = mapOf(
        "تطبيق المزرعة\u060C" to "تطبيق المزرعة\u060C",
        "تطبيق المزرعة" to "تطبيق المزرعة",
        "الرئيسية" to "الرئيسية",
        "الأرشيف" to "الأرشيف",
        "الحظيرة" to "الحظيرة",
        "الأعلاف" to "الأعلاف",
        "الحسابات" to "الحسابات",
        "الكاش الفعلي" to "الكاش الفعلي",
        "حقوقك (لك)" to "حقوقك (لك)",
        "مستحقات (عليك)" to "مستحقات (عليك)",
        "إجراءات تشغيلية سريعة" to "إجراءات تشغيلية سريعة",
        "شراء رأس" to "شراء رأس",
        "شراء علف" to "شراء علف",
        "سند قبض" to "سند قبض",
        "سند صرف" to "سند صرف",
        "تصدير ومشاركة تقرير الويب المنسق (HTML/CSS)" to "تصدير ومشاركة تقرير الويب المنسق (HTML/CSS)",
        "الأعلاف والمخزن" to "الأعلاف والمخزن",
        "تفاصيل وحصص منفصلة" to "تفاصيل وحصص منفصلة",
        "إدارة الحظيرة" to "إدارة الحظيرة",
        "النسخ الاحتياطي" to "النسخ الاحتياطي",
        "استيراد ورفع البيانات" to "استيراد ورفع البيانات",
        "حسابات الأشخاص" to "حسابات الأشخاص",
        "إدارة السندات للأسماء" to "إدارة السندات للأسماء",
        "إعدادات" to "الإعدادات",
        "تسجيل الخروج" to "تسجيل الخروج",
        "حساب المزارع" to "حساب المزارع",
        "تأكيد الاتصال بـ Firebase" to "تأكيد الاتصال بـ Firebase"
    )

    fun t(text: String, lang: String): String {
        val trimmed = text.trim()
        if (lang.lowercase() == "en") {
            val mapped = dictionaryEn[trimmed]
            if (mapped != null) return mapped
            // Partial lookup for ending commas or special characters
            for ((key, value) in dictionaryEn) {
                if (trimmed.equals(key, ignoreCase = true)) {
                    return value
                }
                if (trimmed.replace("،", "").trim() == key.replace("،", "").trim()) {
                    return value
                }
            }
            return trimmed
        } else {
            return dictionaryAr[trimmed] ?: trimmed
        }
    }
}

/**
 * Extension helper to gracefully translate any String directly inside Layouts
 */
fun String.translate(lang: String): String {
    return Localization.t(this, lang)
}
