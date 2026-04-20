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
            entity = HoleTeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["tee_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = HoleBasketEntity::class,
            parentColumns = ["id"],
            childColumns = ["basket_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["layout_id", "sequence_number"], unique = true),
        Index(value = ["layout_id", "hole_id", "tee_id", "basket_id"], unique = true),
        Index(value = ["layout_id"]),
        Index(value = ["hole_id"]),
        Index(value = ["tee_id"]),
        Index(value = ["basket_id"])
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

    @ColumnInfo(name = "tee_id")
    val teeId: Long? = null,

    @ColumnInfo(name = "basket_id")
    val basketId: Long? = null
)