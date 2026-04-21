package nu.linkan.localdiscgolf.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hole_variant",
    foreignKeys = [
        ForeignKey(
            entity = HoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["hole_id"],
            onDelete = ForeignKey.CASCADE,
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
        Index(value = ["hole_id", "tee_id", "basket_id"], unique = true),
        Index(value = ["hole_id"]),
        Index(value = ["tee_id"]),
        Index(value = ["basket_id"])
    ]
)
data class HoleVariantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "hole_id")
    val holeId: Long,

    @ColumnInfo(name = "tee_id")
    val teeId: Long,

    @ColumnInfo(name = "basket_id")
    val basketId: Long,

    @ColumnInfo(name = "length_meters")
    val lengthMeters: Int,

    @ColumnInfo(name = "par_value")
    val parValue: Int,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)