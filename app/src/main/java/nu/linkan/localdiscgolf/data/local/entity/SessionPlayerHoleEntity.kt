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
        )
    ],
    indices = [
        Index(value = ["session_player_id", "sequence_number"], unique = true),
        Index(value = ["session_player_id"]),
        Index(value = ["hole_id"]),
        Index(value = ["course_id"])
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

    @ColumnInfo(name = "hole_number_snapshot")
    val holeNumberSnapshot: Int,

    @ColumnInfo(name = "hole_name_snapshot")
    val holeNameSnapshot: String? = null,

    @ColumnInfo(name = "length_snapshot_meters")
    val lengthSnapshotMeters: Int,

    @ColumnInfo(name = "par_snapshot")
    val parSnapshot: Int,

    @ColumnInfo(name = "throws_count")
    val throwsCount: Int? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)