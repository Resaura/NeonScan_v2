package com.neonscan.app.data.file

import android.content.Context
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
        val timestampDir = File(scansRoot, System.currentTimeMillis().toString())
        if (!timestampDir.exists()) {
            timestampDir.mkdirs()
        }

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
}
