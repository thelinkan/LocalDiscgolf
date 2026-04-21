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
import nu.linkan.localdiscgolf.data.local.entity.HoleVariantEntity
import nu.linkan.localdiscgolf.data.local.model.HoleVariantWithNames

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHoleVariant(holeVariant: HoleVariantEntity): Long

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
        SELECT
            hv.id AS id,
            hv.hole_id AS holeId,
            hv.tee_id AS teeId,
            hv.basket_id AS basketId,
            ht.name AS teeName,
            hb.name AS basketName,
            hv.length_meters AS lengthMeters,
            hv.par_value AS parValue
        FROM hole_variant hv
        INNER JOIN hole_tee ht ON ht.id = hv.tee_id
        INNER JOIN hole_basket hb ON hb.id = hv.basket_id
        WHERE hv.hole_id = :holeId
          AND hv.is_active = 1
        ORDER BY ht.sort_order, ht.name, hb.sort_order, hb.name
    """)
    fun observeActiveVariantsForHole(holeId: Long): Flow<List<HoleVariantWithNames>>

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