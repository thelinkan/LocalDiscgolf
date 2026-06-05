package nu.linkan.localdiscgolf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null,

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    companion object {
        const val KEY_REFERENCE_DATA = "reference_data"
        const val KEY_PLAYERS = "players"
        const val KEY_COURSES = "courses"
        const val KEY_LAYOUTS = "layouts"
        const val KEY_HOLES = "holes"
        const val KEY_TEES = "tees"
        const val KEY_BASKETS = "baskets"
        const val KEY_VARIANTS = "variants"
    }
}
