package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.SyncMetadataEntity
import nu.linkan.localdiscgolf.data.local.entity.SyncQueueEntity

@Dao
interface SyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE `key` = :key")
    suspend fun getMetadata(key: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE `key` = :key")
    fun observeMetadata(key: String): Flow<SyncMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQueueItem(item: SyncQueueEntity): Long

    @Query("""
        SELECT *
        FROM sync_queue
        WHERE status IN (:statuses)
          AND (:now IS NULL OR next_attempt_at IS NULL OR next_attempt_at <= :now)
        ORDER BY created_at, id
        LIMIT :limit
    """)
    suspend fun getQueueItemsReadyForSync(
        statuses: List<String> = listOf(
            SyncQueueEntity.STATUS_PENDING,
            SyncQueueEntity.STATUS_ERROR
        ),
        now: Long? = null,
        limit: Int = 20
    ): List<SyncQueueEntity>

    @Query("""
        SELECT *
        FROM sync_queue
        WHERE entity_type = :entityType
          AND entity_id = :entityId
          AND operation = :operation
          AND status IN ('pending', 'running', 'error')
        LIMIT 1
    """)
    suspend fun findOpenQueueItem(
        entityType: String,
        entityId: Long,
        operation: String
    ): SyncQueueEntity?

    @Query("""
        UPDATE sync_queue
        SET status = :status,
            attempt_count = attempt_count + :attemptIncrement,
            last_attempt_at = :lastAttemptAt,
            next_attempt_at = :nextAttemptAt,
            last_error = :lastError,
            updated_at = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateQueueItemStatus(
        id: Long,
        status: String,
        attemptIncrement: Int,
        lastAttemptAt: Long?,
        nextAttemptAt: Long?,
        lastError: String?,
        updatedAt: Long
    )

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteQueueItem(id: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'done'")
    suspend fun deleteCompletedQueueItems()

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('pending', 'running', 'error')")
    fun observeOpenQueueItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'error'")
    fun observeErrorQueueItemCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*)
        FROM sync_queue
        WHERE entity_type = :entityType
          AND entity_id = :entityId
          AND operation = :operation
          AND status IN ('pending', 'running', 'error')
    """)
    suspend fun countOpenQueueItemsForEntity(
        entityType: String,
        entityId: Long,
        operation: String
    ): Int
}
