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
            h.id AS holeId,
            h.hole_number AS holeNumber,
            h.name AS holeName,
            h.length_meters AS lengthMeters,
            h.par_value AS parValue,
            h.notes AS holeNotes
        FROM layout_hole lh
        INNER JOIN hole h ON h.id = lh.hole_id
        WHERE lh.layout_id = :layoutId
          AND h.is_active = 1
        ORDER BY lh.sequence_number
    """)
    fun observeLayoutHoles(layoutId: Long): Flow<List<LayoutHoleWithHole>>

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