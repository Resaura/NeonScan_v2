package com.neonscan.app.data.local.db

import androidx.room.TypeConverter
import com.neonscan.app.domain.model.ScanType

class ScanTypeConverter {
    @TypeConverter
    fun fromString(value: String?): ScanType? = value?.let { enumValueOf<ScanType>(it) }

    @TypeConverter
    fun toString(type: ScanType?): String? = type?.name
}
