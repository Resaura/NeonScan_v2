package com.neonscan.app.data.local.db

import androidx.room.TypeConverter
import com.neonscan.app.domain.model.DocumentKind

class DocumentKindConverter {
    @TypeConverter
    fun fromString(value: String?): DocumentKind? = value?.let { enumValueOf<DocumentKind>(it) }

    @TypeConverter
    fun toString(kind: DocumentKind?): String? = kind?.name
}
