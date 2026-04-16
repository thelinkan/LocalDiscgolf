package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity

@Dao
interface HoleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(hole: HoleEntity): Long

    @Query("""
        SELECT * 
        FROM hole 
        WHERE course_id = :courseId
          AND is_active = 1
        ORDER BY hole_number
    """)
    fun observeActiveHolesForCourse(courseId: Long): Flow<List<HoleEntity>>

    @Query("SELECT * FROM hole WHERE id = :holeId")
    suspend fun getById(holeId: Long): HoleEntity?
}