package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.LayoutEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutHoleEntity
import nu.linkan.localdiscgolf.data.local.model.LayoutHoleWithHole

@Dao
interface LayoutDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(layout: LayoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLayoutHole(layoutHole: LayoutHoleEntity): Long

    @Query("""
        SELECT *
        FROM layout
        WHERE course_id = :courseId
          AND is_active = 1
        ORDER BY name
    """)
    fun observeActiveLayoutsForCourse(courseId: Long): Flow<List<LayoutEntity>>

    @Query("SELECT * FROM layout WHERE id = :layoutId")
    suspend fun getById(layoutId: Long): LayoutEntity?

    @Query("""
        SELECT
            lh.id AS layoutHoleId,
            lh.layout_id AS layoutId,
            lh.sequence_number AS sequenceNumber,
            lh.hole_id AS holeId,
            lh.hole_variant_id AS holeVariantId,
            h.hole_number AS holeNumber,
            h.name AS holeName,
            COALESCE(hv.length_meters, h.length_meters) AS lengthMeters,
            COALESCE(hv.par_value, h.par_value) AS parValue,
            h.notes AS holeNotes,
            ht.name AS teeName,
            hb.name AS basketName
        FROM layout_hole lh
        INNER JOIN hole h ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv ON hv.id = lh.hole_variant_id
        LEFT JOIN hole_tee ht ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb ON hb.id = hv.basket_id
        WHERE lh.layout_id = :layoutId
          AND h.is_active = 1
        ORDER BY lh.sequence_number
    """)
    fun observeLayoutHoles(layoutId: Long): Flow<List<LayoutHoleWithHole>>

    @Query("""
        SELECT
            lh.id AS layoutHoleId,
            lh.layout_id AS layoutId,
            lh.sequence_number AS sequenceNumber,
            lh.hole_id AS holeId,
            lh.hole_variant_id AS holeVariantId,
            h.hole_number AS holeNumber,
            h.name AS holeName,
            COALESCE(hv.length_meters, h.length_meters) AS lengthMeters,
            COALESCE(hv.par_value, h.par_value) AS parValue,
            h.notes AS holeNotes,
            ht.name AS teeName,
            hb.name AS basketName
        FROM layout_hole lh
        INNER JOIN hole h ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv ON hv.id = lh.hole_variant_id
        LEFT JOIN hole_tee ht ON ht.id = hv.tee_id
        LEFT JOIN hole_basket hb ON hb.id = hv.basket_id
        WHERE lh.layout_id = :layoutId
          AND h.is_active = 1
        ORDER BY lh.sequence_number
    """)
    suspend fun getLayoutHolesOnce(layoutId: Long): List<LayoutHoleWithHole>

    @Query("""
        SELECT COALESCE(MAX(sequence_number), 0)
        FROM layout_hole
        WHERE layout_id = :layoutId
    """)
    suspend fun getMaxSequenceNumber(layoutId: Long): Int

    @Query("""
        DELETE FROM layout_hole
        WHERE id = :layoutHoleId
    """)
    suspend fun deleteLayoutHoleById(layoutHoleId: Long)

    @Query("""
        UPDATE layout_hole
        SET sequence_number = :newSequenceNumber
        WHERE id = :layoutHoleId
    """)
    suspend fun updateLayoutHoleSequence(layoutHoleId: Long, newSequenceNumber: Int)

    @Query("""
        UPDATE layout_hole
        SET sequence_number = sequence_number - 1
        WHERE layout_id = :layoutId
          AND sequence_number > :deletedSequenceNumber
    """)
    suspend fun closeGapAfterDelete(layoutId: Long, deletedSequenceNumber: Int)

    @Transaction
    suspend fun swapLayoutHoleSequences(
        firstLayoutHoleId: Long,
        firstSequence: Int,
        secondLayoutHoleId: Long,
        secondSequence: Int
    ) {
        updateLayoutHoleSequence(firstLayoutHoleId, -1)
        updateLayoutHoleSequence(secondLayoutHoleId, firstSequence)
        updateLayoutHoleSequence(firstLayoutHoleId, secondSequence)
    }
}