package nu.linkan.localdiscgolf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "player",
    indices = [
        Index(value = ["server_id"], unique = true)
    ]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "is_me")
    val isMe: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "server_id")
    val serverId: Long,

    @ColumnInfo(name = "permission_level")
    val permissionLevel: String? = null,

    @ColumnInfo(name = "is_guest")
    val isGuest: Boolean = false,

    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: Long? = null,

    @ColumnInfo(name = "created_by_user_id")
    val createdByUserId: Long? = null,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null
)