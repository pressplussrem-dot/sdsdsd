package com.smartcalc.ai.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromType(type: HistoryType): String = type.name

    @TypeConverter
    fun toType(value: String): HistoryType =
        runCatching { HistoryType.valueOf(value) }.getOrDefault(HistoryType.CALCULATOR)
}
