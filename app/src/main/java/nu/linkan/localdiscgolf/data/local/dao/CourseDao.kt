package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity

@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(course: CourseEntity): Long

    @Query("SELECT * FROM course WHERE is_active = 1 ORDER BY name")
    fun observeActiveCourses(): Flow<List<CourseEntity>>
}