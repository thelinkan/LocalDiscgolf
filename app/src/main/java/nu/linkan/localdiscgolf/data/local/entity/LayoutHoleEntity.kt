package nu.linkan.localdiscgolf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "layout_hole",
    foreignKeys = [
        ForeignKey(
            entity = LayoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["layout_id"],
            onDelete = ForeignKey.CASCADE,
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
        Index(value = ["layout_id", "sequence_number"], unique = true),
        Index(value = ["layout_id", "hole_variant_id"], unique = true),
        Index(value = ["layout_id"]),
        Index(value = ["hole_id"]),
        Index(value = ["hole_variant_id"])
    ]
)
data class LayoutHoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "layout_id")
    val layoutId: Long,

    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,

    @ColumnInfo(name = "hole_id")
    val holeId: Long,

    @ColumnInfo(name = "hole_variant_id")
    val holeVariantId: Long? = null
)