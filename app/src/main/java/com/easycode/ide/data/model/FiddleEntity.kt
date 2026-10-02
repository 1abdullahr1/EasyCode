package com.easycode.ide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fiddles")
data class FiddleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val htmlCode: String = "",
    val cssCode: String = "",
    val jsCode: String = "",
    val resourcesJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false
)
