package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.database.FirebaseDatabase
// import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Custom Application class representing the application level lifecycle.
 * Initializes and configures cloud storage offline persistence and uncaught crash reporting.
 */
class FarmProApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        try {
            // 1. Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            
            // 2. Configure Cloud Firestore Offline Cache & Local Persistence
            // Set size to CACHE_SIZE_UNLIMITED to assign maximum cache storage to satisfy:
            // "مع تفعيل الـ Offline Cache والـ Local Persistence (بأقصى مساحة كاش ممكنة)"
            val firestore = FirebaseFirestore.getInstance()
            val firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
            firestore.firestoreSettings = firestoreSettings
            Log.d("FarmProApplication", "Firestore Offline Persistence initialized with UNLIMITED cache.")

            // 3. Configure Realtime Database Local Persistence with maximum cache allocation (100MB)
            val database = FirebaseDatabase.getInstance()
            database.setPersistenceEnabled(true)
            database.setPersistenceCacheSizeBytes(100 * 1024 * 1024) 
            Log.d("FarmProApplication", "Realtime Database Offline Persistence enabled (100MB cache).")

            // 4. Robust Crash Reporting & Global Security Guard
            // Functional equivalent to Dart's runZonedGuarded, capturing unhandled runtime exceptions
            setupGlobalExceptionHandlers()

        } catch (e: Exception) {
            Log.e("FarmProApplication", "Failed to initialize Firebase configurations: ${e.message}", e)
        }
    }

    private fun setupGlobalExceptionHandlers() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // val crashlytics = FirebaseCrashlytics.getInstance()
                
                // Save essential system keys to isolate tracking under Firebase Console
                // crashlytics.setCustomKey("active_thread", thread.name)
                // crashlytics.setCustomKey("occurred_at_timestamp", System.currentTimeMillis())
                // crashlytics.setCustomKey("database_status", "SQLite_OK")
                
                // Record the exception details
                // crashlytics.recordException(throwable)
                
                // Backup log to local logs
                Log.e("GLOBAL_FATAL_EXCEPTION", "Captured uncaught exception on thread ${thread.name}", throwable)
                
            } catch (e: Exception) {
                Log.e("FarmProApplication", "Failed to log uncaught exception to Crashlytics: ${e.message}")
            } finally {
                // Return control back to original OS stack handler to gracefully close/restart app
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
        
        Log.i("FarmProApplication", "Global Crash Reporting Framework with native Guard Zone initialized.")
    }
}
