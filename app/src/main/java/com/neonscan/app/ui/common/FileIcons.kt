package com.neonscan.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun fileTypeIconForExtension(extension: String?): ImageVector {
    return when (extension?.lowercase()) {
        "pdf" -> Icons.Filled.PictureAsPdf
        "jpg", "jpeg", "png", "webp" -> Icons.Filled.Image
        "doc", "docx" -> Icons.Filled.Description
        "xls", "xlsx", "csv" -> Icons.Filled.TableChart
        "txt" -> Icons.Filled.TextSnippet
        else -> Icons.Filled.Description
    }
}

fun extractExtensionFromPath(path: String?): String? {
    if (path.isNullOrBlank()) return null
    val lastSegment = path.substringAfterLast("/", path)
    return lastSegment.substringAfterLast('.', missingDelimiterValue = "")
}
