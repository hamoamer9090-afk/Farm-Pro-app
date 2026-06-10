package com.example.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseManager {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Authentication
    fun getCurrentUser() = auth.currentUser

    suspend fun signInAnonymously(): Boolean {
        return try {
            auth.signInAnonymously().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signInWithGoogle(idToken: String): Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // Firestore Integration Example: Syncing Farm Entity
    suspend fun backupData(farmName: String, data: Map<String, Any>) {
        val userId = getCurrentUser()?.uid ?: return
        try {
            firestore.collection("users")
                .document(userId)
                .collection("farms")
                .document(farmName)
                .set(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun restoreData(farmName: String): Map<String, Any>? {
        val userId = getCurrentUser()?.uid ?: return null
        return try {
            // Attempt cache fetch first for offline speed
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("farms")
                .document(farmName)
                .get(com.google.firebase.firestore.Source.CACHE)
                .await()
            snapshot.data
        } catch (e: Exception) {
            // Fallback to standard fetch (checking network with default local persistence rules)
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("farms")
                    .document(farmName)
                    .get(com.google.firebase.firestore.Source.DEFAULT)
                    .await()
                snapshot.data
            } catch (inner: Exception) {
                inner.printStackTrace()
                null
            }
        }
    }

    private fun jsonToMap(json: org.json.JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            var value = json.get(key)
            if (value == org.json.JSONObject.NULL) {
                continue
            }
            if (value is org.json.JSONObject) {
                value = jsonToMap(value)
            } else if (value is org.json.JSONArray) {
                value = jsonToList(value)
            }
            map[key] = value
        }
        return map
    }

    private fun jsonToList(array: org.json.JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until array.length()) {
            var value = array.get(i)
            if (value == org.json.JSONObject.NULL) {
                continue
            }
            if (value is org.json.JSONObject) {
                value = jsonToMap(value)
            } else if (value is org.json.JSONArray) {
                value = jsonToList(value)
            }
            list.add(value)
        }
        return list
    }

    suspend fun syncQueueOperation(
        farmName: String,
        operationType: String,
        collectionName: String,
        documentId: String,
        payloadJson: String
    ): Boolean {
        val userId = getCurrentUser()?.uid ?: return false
        return try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("farms")
                .document(farmName)
                .collection(collectionName)
                .document(documentId)

            if (operationType == "DELETE") {
                docRef.delete().await()
            } else {
                val json = org.json.JSONObject(payloadJson)
                val map = jsonToMap(json)
                docRef.set(map).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
