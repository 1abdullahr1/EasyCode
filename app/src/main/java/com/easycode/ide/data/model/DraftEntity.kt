package com.easycode.ide.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: Int = 0,
    val stateJson: String,
    val savedSnapshotJson: String
)
