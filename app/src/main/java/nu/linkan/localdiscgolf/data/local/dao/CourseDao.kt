package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.model.CourseListRow

@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(course: CourseEntity): Long

    @Query("SELECT * FROM course WHERE is_active = 1 ORDER BY name")
    fun observeActiveCourses(): Flow<List<CourseEntity>>

    @Query("""
    SELECT
        c.id AS courseId,
        c.name AS courseName,
        (
            SELECT COUNT(*)
            FROM hole h
            WHERE h.course_id = c.id
              AND h.is_active = 1
        ) AS holeCount,
        (
            SELECT COUNT(*)
            FROM layout l
            WHERE l.course_id = c.id
              AND l.is_active = 1
        ) AS layoutCount
    FROM course c
    WHERE c.is_active = 1
    ORDER BY c.name
""")
    fun observeCourseListRows(): Flow<List<CourseListRow>>
}

