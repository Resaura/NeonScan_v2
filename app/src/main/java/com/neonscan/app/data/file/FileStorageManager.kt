package com.neonscan.app.data.file

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

data class StoredScanResult(
    val primaryPath: String,
    val pageCount: Int
)

class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val scansRoot: File
        get() = File(context.filesDir, "scans")

    suspend fun saveImages(sourcePaths: List<String>): StoredScanResult = withContext(Dispatchers.IO) {
        if (sourcePaths.isEmpty()) throw IllegalArgumentException("No images to save")
        val timestampDir = createTimestampDir()

        var primary: String? = null
        val resolver = context.contentResolver
        sourcePaths.forEachIndexed { index, path ->
            val destFile = File(timestampDir, "page_${index + 1}.jpg")
            val uri = Uri.parse(path)
            copyUriOrFile(uri, destFile, resolver)
            if (primary == null) {
                primary = destFile.absolutePath
            }
        }

        StoredScanResult(
            primaryPath = primary ?: "",
            pageCount = sourcePaths.size
        )
    }

    suspend fun saveBitmap(bitmap: Bitmap, extension: String, quality: Int = 90): StoredScanResult =
        withContext(Dispatchers.IO) {
            val dir = createTimestampDir()
            val dest = File(dir, "document.$extension")
            val format = if (extension.lowercase() == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            FileOutputStream(dest).use { output ->
                bitmap.compress(format, quality, output)
            }
            StoredScanResult(primaryPath = dest.absolutePath, pageCount = 1)
        }

    suspend fun saveBytes(bytes: ByteArray, extension: String): StoredScanResult =
        withContext(Dispatchers.IO) {
            val dir = createTimestampDir()
            val dest = File(dir, "document.$extension")
            FileOutputStream(dest).use { output ->
                output.write(bytes)
            }
            StoredScanResult(primaryPath = dest.absolutePath, pageCount = 1)
        }

    suspend fun createEmptyFile(extension: String): File = withContext(Dispatchers.IO) {
        val dir = createTimestampDir()
        File(dir, "document.$extension")
    }

    suspend fun deleteDocument(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) {
            val parent = file.parentFile
            if (parent != null && parent.exists()) {
                parent.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }

    private fun copyUriOrFile(src: Uri, dest: File, resolver: android.content.ContentResolver) {
        val inputStream: InputStream? = when (src.scheme) {
            "content" -> resolver.openInputStream(src)
            "file", null -> FileInputStream(File(src.path ?: ""))
            else -> resolver.openInputStream(src)
        }
        requireNotNull(inputStream) { "Impossible de lire le fichier scanné" }
        inputStream.use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun createTimestampDir(): File {
        val dir = File(scansRoot, System.currentTimeMillis().toString())
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
