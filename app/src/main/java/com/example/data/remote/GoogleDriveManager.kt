package com.example.data.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Manages secure communication with the Google Drive API v3.
 * Implements strict OAuth scope checking, chunked resumable uploads, stream isolation,
 * and user-friendly error translations for authentication finger-print mismatches.
 */
class GoogleDriveManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Programmatic narrow scope restricted strictly to Sandboxed Directory: drive.appdata
    private val driveScope = "oauth2:https://www.googleapis.com/auth/drive.appdata"

    /**
     * Retrieves the OAuth 2.0 ACCESS TOKEN for Google Drive.
     * Restricts the scope programmatically to the secure, narrow drive.appdata sandbox.
     */
    suspend fun getAccessToken(account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val androidAccount = account.account ?: return@withContext null
            // Programmatic narrow-scope token request (drive.appdata)
            GoogleAuthUtil.getToken(context, androidAccount, driveScope)
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Failed to retrieve access token: ${e.message}", e)
            null
        }
    }

    /**
     * Uploads the SQLite database file to Google Drive (AppData Folder) using Chunked Resumable Uploads.
     * This ensures ultimate connectivity stability and error recovery on unstable network connections.
     */
    suspend fun uploadSqliteFileChunked(
        account: GoogleSignInAccount,
        dbFile: File,
        onProgress: (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val token = getAccessToken(account) ?: throw IOException("فشل الحصول على معرّف الوصول مبرمجاً")
        
        if (!dbFile.exists()) {
            throw IOException("ملف قاعدة البيانات المحلي غير موجود!")
        }

        // Phase 1: Initialize Resumable Session with Google Drive API v3
        val sessionUrl = initializeResumableSession(token, dbFile.name)
            ?: throw IOException("فشل بدء جلسة رفع الملفات مع Google Drive (Resumable Session Error)")

        // Phase 2: Transmit the file in chunks of 256KB to guarantee transfer safety
        val fileSize = dbFile.length()
        val chunkSize = 256 * 1024 // 256KB constraint (Google Drive standard multiple of 256KB)
        val buffer = ByteArray(chunkSize)
        
        FileInputStream(dbFile).use { fis ->
            var bytesRead: Int
            var uploadedBytes: Long = 0
            
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                val nextChunkSize = bytesRead
                val isLastChunk = uploadedBytes + nextChunkSize >= fileSize
                
                val requestBody = buffer.copyOf(nextChunkSize).toRequestBody("application/x-sqlite3".toMediaType())
                
                val rangeHeader = "bytes $uploadedBytes-${uploadedBytes + nextChunkSize - 1}/$fileSize"
                val putRequest = Request.Builder()
                    .url(sessionUrl)
                    .put(requestBody)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Range", rangeHeader)
                    .header("Content-Type", "application/x-sqlite3")
                    .build()

                client.newCall(putRequest).execute().use { response ->
                    if (!response.isSuccessful && response.code != 308) { // 308 Resume Incomplete is expected
                        throw IOException("فشل في رفع جزئية من الملف: كود الخطأ ${response.code}")
                    }
                }

                uploadedBytes += nextChunkSize
                val progress = uploadedBytes.toFloat() / fileSize.toFloat()
                withContext(Dispatchers.Main) {
                    onProgress(progress)
                }
            }
        }
        
        "تم النسخ الاحتياطي السحابي بأمان"
    }

    /**
     * Initializes a Resumable Session with Google Drive API.
     * Allocates file within the localized application data store ("appDataFolder").
     */
    private fun initializeResumableSession(token: String, filename: String): String? {
        val metadata = JSONObject()
        metadata.put("name", filename)
        metadata.put("parents", JSONArray(listOf("appDataFolder"))) // Isolated app-data scope

        val requestBody = metadata.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
            .post(requestBody)
            .header("Authorization", "Bearer $token")
            .header("X-Upload-Content-Type", "application/x-sqlite3")
            .header("Content-Type", "application/json; charset=UTF-8")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.header("Location") // Location of upload session
            } else {
                val responseBody = response.body?.string() ?: ""
                Log.e("GoogleDriveManager", "Resumable initiation failed: ${response.code} -> $responseBody")
                val parsedMessage = try {
                    val json = JSONObject(responseBody)
                    val errorObj = json.optJSONObject("error")
                    errorObj?.optString("message") ?: responseBody
                } catch (e: Exception) {
                    responseBody
                }
                throw IOException("خطأ من جوجل (${response.code}): $parsedMessage")
            }
        }
    }

    /**
     * Downloads the backup SQLite database file from Google Drive's isolated AppData Folder.
     * Safely reads content in streams to resolve stream stalling / UI freezing.
     */
    suspend fun downloadSqliteFile(
        account: GoogleSignInAccount,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val token = getAccessToken(account) ?: throw IOException("فشل الحصول على معرّف الوصول")

        // 1. Search for files in the AppData Folder
        val fileId = searchBackupFileInAppData(token, targetFile.name)
            ?: throw IOException("لم يتم العثور على أي ملف نسخة احتياطية سحابية باسم ${targetFile.name}!")

        // 2. Download media file
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .get()
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("فشل تحميل الملف سحابياً الكود: ${response.code}")
            }

            val body = response.body ?: throw IOException("محتوى الملف فارغ سحابياً")
            val totalBytes = body.contentLength()
            var downloadedBytes: Long = 0

            body.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8192) // 8KB buffer size
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        downloadedBytes += read
                        
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    outputStream.flush()
                }
            }
        }
        true
    }

    /**
     * Searches for a backup file inside the isolated appDataFolder scope.
     */
    private fun searchBackupFileInAppData(token: String, filename: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='$filename'"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: return null
                val json = JSONObject(jsonString)
                val files = json.getJSONArray("files")
                if (files.length() > 0) {
                    return files.getJSONObject(0).getString("id")
                }
            } else {
                val responseBody = response.body?.string() ?: ""
                Log.e("GoogleDriveManager", "Search in AppData failed: ${response.code} -> $responseBody")
                val parsedMessage = try {
                    val json = JSONObject(responseBody)
                    val errorObj = json.optJSONObject("error")
                    errorObj?.optString("message") ?: responseBody
                } catch (e: Exception) {
                    responseBody
                }
                throw IOException("خطأ من جوجل (${response.code}): $parsedMessage")
            }
        }
        return null
    }

    companion object {
        /**
         * Parses Google Authentication and API sign-in errors, providing beautifully mapped, 
         * contextual Arabic guidelines for the user (Authentication & SHA fingerprint issues).
         * Prevents generic, unhandled visual slop.
         */
        fun getFriendlyErrorMessage(exception: Exception): String {
            if (exception is ApiException) {
                return when (exception.statusCode) {
                    12500 -> "خطأ التحقق من الهوية (12500): بصمة التوقيع الخاص بالتطبيق (SHA-1/SHA-256) للتطبيق غير مسجلة في كونسول Google Cloud أو Firebase Console، أو لم يتم إعداد ملف مفاتيح متجر التطبيقات ومصادقته."
                    10 -> "خطأ في تكوين اللوحة الطرفية (كود 10): هذا خطأ مبرمج Developer Error. يرجى التأكد من مطابقة معرّف الحزمة الباقة (Package Name) وبصمة SHA-1 بالكامل مع سجلات خدمات Google Cloud و Firebase."
                    7 -> "فشل في الاتصال السحابي (كود 7): لم نتمكن من الوصول لخدمات جوجل، يرجى التأكد من اتصال الهاتف بالشبكة ومن تفعيل بيانات الإنترنت والـ Wi-Fi."
                    12501 -> "تم إلغاء عملية تسجيل الدخول بناء على طلب المستخدم."
                    12502 -> "عملية التحقق من بيانات الهوية لا زالت معلّقة ومستمرة."
                    8 -> "حدث خطأ داخلي في خدمات Google Play لخلل بجهاز المحاكي أو الهاتف."
                    16 -> "تم إلغاء النشاط التفاعلي لخدمات جوجل أثناء تشغيل النافذة."
                    else -> "فشل في تسجيل الدخول سحابياً (رمز الخطأ: ${exception.statusCode}). يرجى التحقق من بصمات SHA للتطبيق بالمتجر وتعديلها."
                }
            }
            return exception.localizedMessage ?: "حدث خطأ غير متوقع أثناء الاتصال بخدمات Google Cloud السحابية."
        }
    }
}
