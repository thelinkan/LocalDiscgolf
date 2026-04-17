package nu.linkan.localdiscgolf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_player",
    foreignKeys = [
        ForeignKey(
            entity = PlaySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["play_session_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = LayoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["layout_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["play_session_id", "player_id"], unique = true),
        Index(value = ["play_session_id"]),
        Index(value = ["player_id"]),
        Index(value = ["layout_id"])
    ]
)
data class SessionPlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "play_session_id")
    val playSessionId: Long,

    @ColumnInfo(name = "player_id")
    val playerId: Long,

    @ColumnInfo(name = "layout_id")
    val layoutId: Long,

    @ColumnInfo(name = "is_custom_layout")
    val isCustomLayout: Boolean = false,

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "start_order")
    val startOrder: Int? = null,

    val status: String = "in_progress",

    val notes: String? = null,

    @ColumnInfo(name = "total_throws")
    val totalThrows: Int? = null,

    @ColumnInfo(name = "total_par")
    val totalPar: Int? = null,

    @ColumnInfo(name = "score_relative_to_par")
    val scoreRelativeToPar: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)