package com.smartcalc.ai.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** History can hold two different kinds of entries. */
enum class HistoryType { CALCULATOR, AI }

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** CALCULATOR or AI. Stored as text. */
    @ColumnInfo(name = "type") val type: HistoryType,

    /** Calculator: the typed expression. AI: the recognised problem. */
    @ColumnInfo(name = "expression") val expression: String,

    /** Calculator: the numeric result. AI: the final solution. */
    @ColumnInfo(name = "result") val result: String,

    /** AI only: solution steps, one per line. */
    @ColumnInfo(name = "steps") val steps: String? = null,

    /** AI only: absolute path of the stored image copy, if it could be kept. */
    @ColumnInfo(name = "image_path") val imagePath: String? = null,

    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
