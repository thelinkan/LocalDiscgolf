package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity

@Dao
interface HoleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(hole: HoleEntity): Long

    @Update
    suspend fun update(hole: HoleEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHoleTee(holeTee: HoleTeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHoleBasket(holeBasket: HoleBasketEntity): Long

    @Query("""
        SELECT * 
        FROM hole 
        WHERE course_id = :courseId
          AND is_active = 1
        ORDER BY hole_number
    """)
    fun observeActiveHolesForCourse(courseId: Long): Flow<List<HoleEntity>>

    @Query("""
        SELECT *
        FROM hole_tee
        WHERE hole_id = :holeId
          AND is_active = 1
        ORDER BY sort_order, name
    """)
    fun observeActiveTeesForHole(holeId: Long): Flow<List<HoleTeeEntity>>

    @Query("""
        SELECT *
        FROM hole_basket
        WHERE hole_id = :holeId
          AND is_active = 1
        ORDER BY sort_order, name
    """)
    fun observeActiveBasketsForHole(holeId: Long): Flow<List<HoleBasketEntity>>

    @Query("""
        SELECT COALESCE(MAX(sort_order), 0)
        FROM hole_tee
        WHERE hole_id = :holeId
    """)
    suspend fun getMaxTeeSortOrder(holeId: Long): Int

    @Query("""
        SELECT COALESCE(MAX(sort_order), 0)
        FROM hole_basket
        WHERE hole_id = :holeId
    """)
    suspend fun getMaxBasketSortOrder(holeId: Long): Int

    @Query("SELECT * FROM hole WHERE id = :holeId")
    suspend fun getById(holeId: Long): HoleEntity?
}