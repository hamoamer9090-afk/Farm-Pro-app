package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

class RemindersRepository(private val db: AppDatabase) {

    // جلب جميع التنبيهات المرتبطة بمزرعة معينة
    fun getReminders(farmName: String): Flow<List<ReminderEntity>> = db.reminderDao().getRemindersByFarm(farmName)

    // جدولة وإضافة تنبيه جديد
    suspend fun addReminder(farmName: String, title: String, description: String, reminderDateMillis: Long, type: String = "general") {
        val reminder = ReminderEntity(
            farmName = farmName,
            title = title,
            description = description,
            reminderDateMillis = reminderDateMillis,
            type = type
        )
        db.reminderDao().insertReminder(reminder)
    }

    // تحديد التنبيه كمنجز أو إلغاء الإنجاز
    suspend fun toggleReminderCompletion(reminder: ReminderEntity) {
        val updated = reminder.copy(isCompleted = !reminder.isCompleted)
        db.reminderDao().updateReminder(updated)
    }

    // مسح التنبيه نهائياً
    suspend fun deleteReminder(reminder: ReminderEntity) {
        db.reminderDao().deleteReminder(reminder)
    }
}
