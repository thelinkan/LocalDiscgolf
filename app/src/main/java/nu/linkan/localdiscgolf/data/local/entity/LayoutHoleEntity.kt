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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["hole_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("layout_id"),
        Index("hole_id"),
        Index("hole_variant_id"),
        Index("server_layout_id"),
        Index("server_hole_id"),
        Index("server_hole_variant_id")
    ]
)
data class LayoutHoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "layout_id")
    val layoutId: Long,

    @ColumnInfo(name = "hole_id")
    val holeId: Long,

    @ColumnInfo(name = "hole_variant_id")
    val holeVariantId: Long?,

    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,

    @ColumnInfo(name = "server_layout_id")
    val serverLayoutId: Long? = null,

    @ColumnInfo(name = "server_hole_id")
    val serverHoleId: Long? = null,

    @ColumnInfo(name = "server_hole_variant_id")
    val serverHoleVariantId: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)