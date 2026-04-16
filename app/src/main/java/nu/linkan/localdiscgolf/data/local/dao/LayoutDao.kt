package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.LayoutEntity

@Dao
interface LayoutDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(layout: LayoutEntity): Long

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
}