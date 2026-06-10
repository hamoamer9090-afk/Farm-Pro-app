package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

data class UserAccount(
    val name: String,
    val email: String,
    val role: String,
    val permissions: Map<String, Boolean> = emptyMap()
) : Serializable


@Entity(tableName = "farms")
data class FarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "animals")
data class AnimalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val name: String, // Tag or identification name
    val type: String, // e.g., "عجل" (Calf), "أغنام" (Sheep)
    val weight: Double,
    val purchasePrice: Double,
    val salePrice: Double = 0.0,
    val arrivalDate: String, // تاريخ الدخول
    val departureDate: String = "", // تاريخ الخروج
    val age: String, // العمر
    val feedCost: Double = 0.0, // تكلفة الطعام
    val imageBase64: String? = null, // Base64 representation of captured or loaded picture
    val merchantName: String = "سوق", // fallback "سوق" if not entered
    val associatedPersonId: Int? = null, // Linked account person
    val isArchived: Boolean = false
) : Serializable

@Entity(tableName = "feeds")
data class FeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val feedName: String, // Name of feed mix
    val ingredientsDescription: String, // تفصيل المواد المفرومة أو المركبة (JSON formatted or text)
    val totalWeight: Double,
    val totalCost: Double,
    val addedDate: String,
    val associatedPersonId: Int? = null, // Linked account person
    val alertThreshold: Double = 100.0, // Weight warning threshold (default 100kg)
    val remainingWeight: Double = 0.0 // Tracks how much feed remains
) : Serializable

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val name: String,
    val role: String, // "Owner", "Worker", "Merchant", etc.
    val phone: String = "",
    val balance: Double = 0.0, // Debit/Credit index (+ve means creditor [له], -ve means debtor [عليه])
    val initialBalance: Double = 0.0,
    val isArchived: Boolean = false
) : Serializable

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val type: String, // "income" (قبض/دائن), "expense" (دفع/مدين)
    val amount: Double,
    val description: String,
    val date: String,
    val associatedAnimalId: Int? = null,
    val associatedPersonId: Int? = null,
    val category: String = "عام", // Category (عجل، أغنام، راتب، أعلاف، الخ)
    val isArchived: Boolean = false
) : Serializable

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val title: String = "",
    val content: String,
    val imageBase64: String? = null, // base64 representation of note picture
    val colorHex: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "backups")
data class BackupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val dateString: String,
    val backupDataJson: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "google_links")
data class GoogleLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val googleEmail: String,
    val linkedAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val operationType: String, // "INSERT", "UPDATE", "DELETE"
    val collectionName: String, // "animals", "transactions", etc.
    val documentId: String, // ID of the local item or remote item
    val payloadJson: String, // JSON representation of the entity
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val title: String,
    val description: String,
    val reminderDateMillis: Long, // timestamp for trigger
    val isCompleted: Boolean = false,
    val type: String = "general", // "vaccine", "payment", "general"
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val name: String,
    val totalCost: Double,
    val validityDays: Int, // withdrawal period / validity in days
    val addedDate: String = "",
    val isArchived: Boolean = false
) : Serializable

@Entity(tableName = "births")
data class BirthEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmName: String,
    val motherId: Int,
    val gender: String, // "ذكر" or "أنثى"
    val birthType: String, // "فردي" or "توأم"
    val birthDate: String,
    val status: String = "بالحظيرة", // "بالحظيرة" or "تم بيعه"
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
