package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleVariantEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutHoleEntity
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SyncMetadataEntity

@Dao
interface ReferenceSyncDao {

    // -------------------------
    // Hämta lokala rader via server-id
    // -------------------------

    @Query("SELECT * FROM player WHERE server_id = :serverId LIMIT 1")
    suspend fun getPlayerByServerId(serverId: Long): PlayerEntity?

    @Query("SELECT * FROM course WHERE server_id = :serverId LIMIT 1")
    suspend fun getCourseByServerId(serverId: Long): CourseEntity?

    @Query("SELECT * FROM hole WHERE server_id = :serverId LIMIT 1")
    suspend fun getHoleByServerId(serverId: Long): HoleEntity?

    @Query("SELECT * FROM hole_tee WHERE server_id = :serverId LIMIT 1")
    suspend fun getHoleTeeByServerId(serverId: Long): HoleTeeEntity?

    @Query("SELECT * FROM hole_basket WHERE server_id = :serverId LIMIT 1")
    suspend fun getHoleBasketByServerId(serverId: Long): HoleBasketEntity?

    @Query("SELECT * FROM hole_variant WHERE server_id = :serverId LIMIT 1")
    suspend fun getHoleVariantByServerId(serverId: Long): HoleVariantEntity?

    @Query("SELECT * FROM layout WHERE server_id = :serverId LIMIT 1")
    suspend fun getLayoutByServerId(serverId: Long): LayoutEntity?

    // -------------------------
    // Upsert
    // -------------------------

    @Upsert
    suspend fun upsertPlayer(player: PlayerEntity)

    @Upsert
    suspend fun upsertCourse(course: CourseEntity)

    @Upsert
    suspend fun upsertHole(hole: HoleEntity)

    @Upsert
    suspend fun upsertHoleTee(tee: HoleTeeEntity)

    @Upsert
    suspend fun upsertHoleBasket(basket: HoleBasketEntity)

    @Upsert
    suspend fun upsertHoleVariant(variant: HoleVariantEntity)

    @Upsert
    suspend fun upsertLayout(layout: LayoutEntity)

    @Upsert
    suspend fun upsertLayoutHole(layoutHole: LayoutHoleEntity)

    @Upsert
    suspend fun upsertSyncMetadata(metadata: SyncMetadataEntity)

    // -------------------------
    // Layouthål synkas om helt per layout
    // -------------------------

    @Query("DELETE FROM layout_hole WHERE server_layout_id = :serverLayoutId")
    suspend fun deleteLayoutHolesForServerLayout(serverLayoutId: Long)

    // -------------------------
    // Markera gamla cache-rader inaktiva
    // Första versionen: bara spelare.
    // Övriga objekt får sin is_active-status från servern.
    // -------------------------

    @Query("""
        UPDATE player
        SET is_active = 0
        WHERE server_id IS NOT NULL
          AND server_id NOT IN (:activeServerIds)
    """)
    suspend fun markMissingPlayersInactive(activeServerIds: List<Long>)
}