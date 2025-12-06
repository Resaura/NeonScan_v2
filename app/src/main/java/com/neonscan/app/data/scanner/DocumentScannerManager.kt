package com.neonscan.app.data.scanner

import androidx.activity.ComponentActivity
import com.websitebeaver.documentscanner.DocumentScanner
import com.websitebeaver.documentscanner.constants.ResponseType
import javax.inject.Inject

/**
 * Fournit une instance de scanner sans appeler startScan().
 * On laisse la registration ActivityResult à l'app hôte pour éviter l'exception
 * "LifecycleOwner ... register while state is RESUMED".
 */
class DocumentScannerManager @Inject constructor() {

    fun buildScanner(
        activity: ComponentActivity,
        allowMultiple: Boolean,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ): DocumentScanner {
        return DocumentScanner(
            activity,
            { results -> onSuccess(results.toList()) },
            { errorMessage -> onError(errorMessage ?: "Erreur lors du scan") },
            { onCancel() },
            ResponseType.IMAGE_FILE_PATH,
            true,
            if (allowMultiple) 20 else 1,
            95
        )
    }
}
