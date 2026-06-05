package nu.linkan.localdiscgolf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_player_hole",
    foreignKeys = [
        ForeignKey(
            entity = SessionPlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_player_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = HoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["hole_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = HoleVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["hole_variant_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["session_player_id", "sequence_number"], unique = true),
        Index(value = ["session_player_id"]),
        Index(value = ["course_id"]),
        Index(value = ["hole_id"]),
        Index(value = ["hole_variant_id"])
    ]
)
data class SessionPlayerHoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_player_id")
    val sessionPlayerId: Long,

    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,

    @ColumnInfo(name = "course_id")
    val courseId: Long,

    @ColumnInfo(name = "hole_id")
    val holeId: Long,

    @ColumnInfo(name = "hole_variant_id")
    val holeVariantId: Long? = null,

    @ColumnInfo(name = "hole_number_snapshot")
    val holeNumberSnapshot: Int,

    @ColumnInfo(name = "hole_name_snapshot")
    val holeNameSnapshot: String? = null,

    @ColumnInfo(name = "tee_name_snapshot")
    val teeNameSnapshot: String? = null,

    @ColumnInfo(name = "basket_name_snapshot")
    val basketNameSnapshot: String? = null,

    @ColumnInfo(name = "length_snapshot_meters")
    val lengthSnapshotMeters: Int,

    @ColumnInfo(name = "par_snapshot")
    val parSnapshot: Int,

    @ColumnInfo(name = "throws_count")
    val throwsCount: Int? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    @ColumnInfo(name = "server_session_player_id")
    val serverSessionPlayerId: Long? = null,

    @ColumnInfo(name = "server_hole_id")
    val serverHoleId: Long,

    @ColumnInfo(name = "server_hole_variant_id")
    val serverHoleVariantId: Long?,

    @ColumnInfo(name = "dirty")
    val dirty: Boolean = false,

    @ColumnInfo(name = "last_synced_throws_count")
    val lastSyncedThrowsCount: Int? = null,

    @ColumnInfo(name = "sync_error")
    val syncError: String? = null
)