package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class FarmRepository(private val db: AppDatabase) {

    // --- JSON serialization helpers inside FarmRepository ---
    private fun animalToJson(it: AnimalEntity): String {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("farmName", it.farmName)
        obj.put("name", it.name)
        obj.put("type", it.type)
        obj.put("weight", it.weight)
        obj.put("purchasePrice", it.purchasePrice)
        obj.put("salePrice", it.salePrice)
        obj.put("arrivalDate", it.arrivalDate)
        obj.put("departureDate", it.departureDate)
        obj.put("age", it.age)
        obj.put("feedCost", it.feedCost)
        obj.put("merchantName", it.merchantName)
        obj.put("imageBase64", it.imageBase64 ?: "")
        obj.put("associatedPersonId", it.associatedPersonId ?: -1)
        obj.put("isArchived", it.isArchived)
        return obj.toString()
    }

    private fun feedToJson(it: FeedEntity): String {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("farmName", it.farmName)
        obj.put("feedName", it.feedName)
        obj.put("ingredientsDescription", it.ingredientsDescription)
        obj.put("totalWeight", it.totalWeight)
        obj.put("totalCost", it.totalCost)
        obj.put("addedDate", it.addedDate)
        obj.put("associatedPersonId", it.associatedPersonId ?: -1)
        obj.put("alertThreshold", it.alertThreshold)
        obj.put("remainingWeight", it.remainingWeight)
        return obj.toString()
    }

    private fun personToJson(it: PersonEntity): String {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("farmName", it.farmName)
        obj.put("name", it.name)
        obj.put("role", it.role)
        obj.put("phone", it.phone)
        obj.put("balance", it.balance)
        obj.put("isArchived", it.isArchived)
        return obj.toString()
    }

    private fun transactionToJson(it: TransactionEntity): String {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("farmName", it.farmName)
        obj.put("type", it.type)
        obj.put("amount", it.amount)
        obj.put("description", it.description)
        obj.put("date", it.date)
        obj.put("associatedAnimalId", it.associatedAnimalId ?: -1)
        obj.put("associatedPersonId", it.associatedPersonId ?: -1)
        obj.put("category", it.category)
        obj.put("isArchived", it.isArchived)
        return obj.toString()
    }

    private fun noteToJson(it: NoteEntity): String {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("farmName", it.farmName)
        obj.put("content", it.content)
        obj.put("imageBase64", it.imageBase64 ?: "")
        obj.put("createdAt", it.createdAt)
        return obj.toString()
    }

    private suspend fun enqueueSync(
        farmName: String,
        operationType: String,
        collectionName: String,
        documentId: String,
        payloadJson: String
    ) {
        val syncItem = SyncQueueEntity(
            farmName = farmName,
            operationType = operationType,
            collectionName = collectionName,
            documentId = documentId,
            payloadJson = payloadJson
        )
        db.syncQueueDao().insertSyncItem(syncItem)
    }

    // --- Farms operations ---
    fun getAllFarms(): Flow<List<FarmEntity>> = db.farmDao().getAllFarms()
    
    suspend fun getFarmByName(name: String): FarmEntity? = db.farmDao().getFarmByName(name)
    
    suspend fun insertFarm(farm: FarmEntity) = db.farmDao().insertFarm(farm)
    
    suspend fun deleteFarmByName(name: String) {
        db.farmDao().deleteFarmByName(name)
        // Clean up everything associated with this farm name
        db.animalDao().deleteAllAnimalsOfFarm(name)
        db.feedDao().deleteAllFeedsOfFarm(name)
        db.personDao().deleteAllPeopleOfFarm(name)
        db.transactionDao().deleteAllTransactionsOfFarm(name)
        db.noteDao().deleteAllNotesOfFarm(name)
    }

    suspend fun renameFarm(oldName: String, newName: String) {
        db.farmDao().updateFarmName(oldName, newName)
        db.animalDao().updateFarmName(oldName, newName)
        db.feedDao().updateFarmName(oldName, newName)
        db.personDao().updateFarmName(oldName, newName)
        db.transactionDao().updateFarmName(oldName, newName)
        db.noteDao().updateFarmName(oldName, newName)
    }

    // --- Animals operations ---
    fun getAnimals(farmName: String): Flow<List<AnimalEntity>> = db.animalDao().getAnimalsByFarm(farmName)
    
    fun getArchiveAnimals(farmName: String): Flow<List<AnimalEntity>> = db.animalDao().getAllAnimalsForArchiveByFarm(farmName)
    
    suspend fun insertAnimal(animal: AnimalEntity): Long {
        val id = db.animalDao().insertAnimal(animal)
        enqueueSync(animal.farmName, "INSERT", "animals", id.toString(), animalToJson(animal.copy(id = id.toInt())))
        return id
    }
    
    suspend fun updateAnimal(animal: AnimalEntity) {
        db.animalDao().updateAnimal(animal)
        enqueueSync(animal.farmName, "UPDATE", "animals", animal.id.toString(), animalToJson(animal))
    }
    
    suspend fun deleteAnimal(animal: AnimalEntity) {
        val archived = animal.copy(isArchived = true)
        db.animalDao().updateAnimal(archived)
        enqueueSync(animal.farmName, "UPDATE", "animals", animal.id.toString(), animalToJson(archived))
    }
    
    suspend fun restoreAnimal(animal: AnimalEntity) {
        val restored = animal.copy(isArchived = false)
        db.animalDao().updateAnimal(restored)
        enqueueSync(animal.farmName, "UPDATE", "animals", animal.id.toString(), animalToJson(restored))
    }
    
    suspend fun hardDeleteAnimal(animal: AnimalEntity) {
        db.animalDao().deleteAnimal(animal)
        enqueueSync(animal.farmName, "DELETE", "animals", animal.id.toString(), animalToJson(animal))
    }

    // --- Feeds operations ---
    fun getFeeds(farmName: String): Flow<List<FeedEntity>> = db.feedDao().getFeedsByFarm(farmName)
    
    suspend fun insertFeed(feed: FeedEntity): Long {
        val id = db.feedDao().insertFeed(feed)
        enqueueSync(feed.farmName, "INSERT", "feeds", id.toString(), feedToJson(feed.copy(id = id.toInt())))
        return id
    }
    
    suspend fun updateFeed(feed: FeedEntity) {
        db.feedDao().updateFeed(feed)
        enqueueSync(feed.farmName, "UPDATE", "feeds", feed.id.toString(), feedToJson(feed))
    }
    
    suspend fun deleteFeed(feed: FeedEntity) {
        db.feedDao().deleteFeed(feed)
        enqueueSync(feed.farmName, "DELETE", "feeds", feed.id.toString(), feedToJson(feed))
    }

    // --- Medicine operations ---
    fun getMedicines(farmName: String): Flow<List<MedicineEntity>> = db.medicineDao().getMedicinesByFarm(farmName)
    
    suspend fun insertMedicine(medicine: MedicineEntity): Long {
        val id = db.medicineDao().insertMedicine(medicine)
        // Ignoring sync queue serialization for medicine for now to save complexity
        return id
    }
    
    suspend fun updateMedicine(medicine: MedicineEntity) {
        db.medicineDao().updateMedicine(medicine)
    }
    
    suspend fun deleteMedicine(medicine: MedicineEntity) {
        val archived = medicine.copy(isArchived = true)
        db.medicineDao().updateMedicine(archived)
    }

    suspend fun restoreMedicine(medicine: MedicineEntity) {
        val restored = medicine.copy(isArchived = false)
        db.medicineDao().updateMedicine(restored)
    }

    suspend fun hardDeleteMedicine(medicine: MedicineEntity) {
        db.medicineDao().deleteMedicine(medicine)
    }

    // --- Births operations ---
    fun getBirths(farmName: String): Flow<List<BirthEntity>> = db.birthDao().getBirthsByFarm(farmName)
    
    suspend fun insertBirth(birth: BirthEntity): Long {
        return db.birthDao().insertBirth(birth)
    }
    
    suspend fun updateBirth(birth: BirthEntity) {
        db.birthDao().updateBirth(birth)
    }
    
    suspend fun deleteBirth(birth: BirthEntity) {
        db.birthDao().deleteBirth(birth)
    }

    // --- Accounts/People operations ---
    fun getPeople(farmName: String): Flow<List<PersonEntity>> = db.personDao().getPeopleByFarm(farmName)
    
    suspend fun insertPerson(person: PersonEntity): Long {
        val id = db.personDao().insertPerson(person)
        enqueueSync(person.farmName, "INSERT", "people", id.toString(), personToJson(person.copy(id = id.toInt())))
        return id
    }
    
    suspend fun updatePerson(person: PersonEntity) {
        db.personDao().updatePerson(person)
        enqueueSync(person.farmName, "UPDATE", "people", person.id.toString(), personToJson(person))
    }
    
    suspend fun deletePerson(person: PersonEntity) {
        val archived = person.copy(isArchived = true)
        db.personDao().updatePerson(archived)
        enqueueSync(person.farmName, "UPDATE", "people", person.id.toString(), personToJson(archived))
    }

    suspend fun restorePerson(person: PersonEntity) {
        val restored = person.copy(isArchived = false)
        db.personDao().updatePerson(restored)
        enqueueSync(person.farmName, "UPDATE", "people", person.id.toString(), personToJson(restored))
    }

    suspend fun hardDeletePerson(person: PersonEntity) {
        db.personDao().deletePerson(person)
        enqueueSync(person.farmName, "DELETE", "people", person.id.toString(), personToJson(person))
    }
    
    suspend fun getPersonById(id: Int): PersonEntity? = db.personDao().getPersonById(id)

    // --- Transactions operations ---
    fun getTransactions(farmName: String): Flow<List<TransactionEntity>> = db.transactionDao().getTransactionsByFarm(farmName)
    
    fun getArchiveTransactions(farmName: String): Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactionsForArchiveByFarm(farmName)

    suspend fun getTransactionsByPersonDirect(personId: Int): List<TransactionEntity> = db.transactionDao().getTransactionsByPersonDirect(personId)
    
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = db.transactionDao().insertTransaction(transaction)
        enqueueSync(transaction.farmName, "INSERT", "transactions", id.toString(), transactionToJson(transaction.copy(id = id.toInt())))
        return id
    }
    
    suspend fun updateTransaction(transaction: TransactionEntity) {
        db.transactionDao().updateTransaction(transaction)
        enqueueSync(transaction.farmName, "UPDATE", "transactions", transaction.id.toString(), transactionToJson(transaction))
    }
    
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val archived = transaction.copy(isArchived = true)
        db.transactionDao().updateTransaction(archived)
        enqueueSync(transaction.farmName, "UPDATE", "transactions", transaction.id.toString(), transactionToJson(archived))
    }

    suspend fun restoreTransaction(transaction: TransactionEntity) {
        val restored = transaction.copy(isArchived = false)
        db.transactionDao().updateTransaction(restored)
        enqueueSync(transaction.farmName, "UPDATE", "transactions", transaction.id.toString(), transactionToJson(restored))
    }

    suspend fun hardDeleteTransaction(transaction: TransactionEntity) {
        db.transactionDao().deleteTransaction(transaction)
        enqueueSync(transaction.farmName, "DELETE", "transactions", transaction.id.toString(), transactionToJson(transaction))
    }

    // --- Notes operations ---
    fun getNotes(farmName: String): Flow<List<NoteEntity>> = db.noteDao().getNotesByFarm(farmName)
    
    suspend fun insertNote(note: NoteEntity): Long {
        val id = db.noteDao().insertNote(note)
        enqueueSync(note.farmName, "INSERT", "notes", id.toString(), noteToJson(note.copy(id = id.toInt())))
        return id
    }
    
    suspend fun updateNote(note: NoteEntity) {
        db.noteDao().updateNote(note)
        enqueueSync(note.farmName, "UPDATE", "notes", note.id.toString(), noteToJson(note))
    }
    
    suspend fun deleteNote(note: NoteEntity) {
        db.noteDao().deleteNote(note)
        enqueueSync(note.farmName, "DELETE", "notes", note.id.toString(), noteToJson(note))
    }
    
    // --- Backups operations ---
    fun getBackups(farmName: String): Flow<List<BackupEntity>> = db.backupDao().getBackupsByFarm(farmName)
    suspend fun getBackupByDate(farmName: String, dateString: String): BackupEntity? = db.backupDao().getBackupByDate(farmName, dateString)
    suspend fun insertBackup(backup: BackupEntity) = db.backupDao().insertBackup(backup)
    suspend fun updateBackup(backup: BackupEntity) = db.backupDao().updateBackup(backup)
    suspend fun deleteBackup(backup: BackupEntity) = db.backupDao().deleteBackup(backup)

    // --- Google links operations ---
    fun getAllLinks(): Flow<List<GoogleLinkEntity>> = db.googleLinkDao().getAllLinks()
    fun getLinksByEmail(email: String): Flow<List<GoogleLinkEntity>> = db.googleLinkDao().getLinksByEmail(email)
    suspend fun insertLink(link: GoogleLinkEntity) = db.googleLinkDao().insertLink(link)
    suspend fun deleteLink(link: GoogleLinkEntity) = db.googleLinkDao().deleteLink(link)

    // --- Bulk Import (Bypasses individual queue syncing implicitly since it is a full state restore) ---
    suspend fun importData(
        farmName: String,
        animals: List<AnimalEntity>,
        feeds: List<FeedEntity>,
        people: List<PersonEntity>,
        transactions: List<TransactionEntity>,
        notes: List<NoteEntity>,
        medicines: List<MedicineEntity>,
        births: List<BirthEntity>
    ) {
        animals.forEach { db.animalDao().insertAnimal(it.copy(farmName = farmName)) }
        feeds.forEach { db.feedDao().insertFeed(it.copy(farmName = farmName)) }
        people.forEach { db.personDao().insertPerson(it.copy(farmName = farmName)) }
        transactions.forEach { db.transactionDao().insertTransaction(it.copy(farmName = farmName)) }
        notes.forEach { db.noteDao().insertNote(it.copy(farmName = farmName)) }
        medicines.forEach { db.medicineDao().insertMedicine(it.copy(farmName = farmName)) }
        births.forEach { db.birthDao().insertBirth(it.copy(farmName = farmName)) }
    }
    
    // --- Clear All Farm Data ---
    suspend fun clearFarmData(farmName: String) {
        db.animalDao().deleteAllAnimalsOfFarm(farmName)
        db.feedDao().deleteAllFeedsOfFarm(farmName)
        db.personDao().deleteAllPeopleOfFarm(farmName)
        db.transactionDao().deleteAllTransactionsOfFarm(farmName)
        db.noteDao().deleteAllNotesOfFarm(farmName)
        db.medicineDao().deleteAllMedicinesOfFarm(farmName)
        db.birthDao().deleteAllBirthsOfFarm(farmName)
    }
}
