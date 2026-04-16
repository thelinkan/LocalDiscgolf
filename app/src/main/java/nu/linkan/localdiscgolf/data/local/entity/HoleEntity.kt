package nu.linkan.localdiscgolf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hole",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["course_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["course_id", "hole_number"], unique = true),
        Index(value = ["course_id"])
    ]
)
data class HoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "course_id")
    val courseId: Long,

    @ColumnInfo(name = "hole_number")
    val holeNumber: Int,

    val name: String? = null,

    @ColumnInfo(name = "length_meters")
    val lengthMeters: Int,

    @ColumnInfo(name = "par_value")
    val parValue: Int,

    val notes: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)