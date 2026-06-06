package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import nu.linkan.localdiscgolf.data.local.model.PendingRoundForSync
import nu.linkan.localdiscgolf.data.local.model.RoundSyncHole
import nu.linkan.localdiscgolf.data.local.model.RoundSyncPlayer

@Dao
interface RoundSyncDao {

    @Query("""
        SELECT
            ps.id,
            ps.server_id AS serverId,
            ps.course_id AS courseId,
            c.server_id AS serverCourseId,
            ps.started_at AS startedAt,
            ps.ended_at AS endedAt,
            ps.status,
            ps.sync_status AS syncStatus,
            ps.current_sequence_number AS currentSequenceNumber
        FROM play_session ps
        INNER JOIN course c
            ON c.id = ps.course_id
        WHERE ps.sync_status IN ('local_only', 'pending_update', 'pending_complete', 'sync_error')
           OR ps.server_id IS NULL
           OR EXISTS (
                SELECT 1
                FROM session_player sp
                INNER JOIN session_player_hole sph
                    ON sph.session_player_id = sp.id
                WHERE sp.play_session_id = ps.id
                  AND sph.dirty = 1
           )
        ORDER BY ps.started_at
    """)
    suspend fun getPendingRoundsForSync(): List<PendingRoundForSync>

    @Query("""
        SELECT
            sp.id,
            sp.play_session_id AS playSessionId,
            sp.player_id AS playerId,
            sp.server_id AS serverId,
            sp.server_player_id AS serverPlayerId,
            sp.layout_id AS layoutId,
            sp.server_layout_id AS serverLayoutId,
            sp.display_name AS displayName,
            sp.start_order AS startOrder,
            sp.approval_required AS approvalRequired,
            sp.approval_state AS approvalState
        FROM session_player sp
        WHERE sp.play_session_id = :playSessionId
        ORDER BY sp.start_order
    """)
    suspend fun getPlayersForRoundSync(playSessionId: Long): List<RoundSyncPlayer>

    @Query("""
        SELECT
            sph.id,
            sph.session_player_id AS sessionPlayerId,
            sph.sequence_number AS sequenceNumber,
            sph.server_id AS serverId,
            sph.server_session_player_id AS serverSessionPlayerId,
            sph.server_hole_id AS serverHoleId,
            sph.server_hole_variant_id AS serverHoleVariantId,
            sph.throws_count AS throwsCount,
            sph.dirty AS dirty
        FROM session_player_hole sph
        INNER JOIN session_player sp
            ON sp.id = sph.session_player_id
        WHERE sp.play_session_id = :playSessionId
        ORDER BY sp.start_order, sph.sequence_number
    """)
    suspend fun getHolesForRoundSync(playSessionId: Long): List<RoundSyncHole>

    @Query("""
        SELECT
            sph.id,
            sph.session_player_id AS sessionPlayerId,
            sph.sequence_number AS sequenceNumber,
            sph.server_id AS serverId,
            sph.server_session_player_id AS serverSessionPlayerId,
            sph.server_hole_id AS serverHoleId,
            sph.server_hole_variant_id AS serverHoleVariantId,
            sph.throws_count AS throwsCount,
            sph.dirty AS dirty
        FROM session_player_hole sph
        INNER JOIN session_player sp
            ON sp.id = sph.session_player_id
        WHERE sp.play_session_id = :playSessionId
          AND sph.dirty = 1
        ORDER BY sp.start_order, sph.sequence_number
    """)
    suspend fun getDirtyHolesForRoundSync(playSessionId: Long): List<RoundSyncHole>

    @Query("""
        UPDATE play_session
        SET server_id = :serverRoundId,
            sync_status = 'pending_update',
            sync_error = NULL,
            last_synced_at = :syncedAt,
            updated_at = :syncedAt
        WHERE id = :localPlaySessionId
    """)
    suspend fun setPlaySessionServerId(
        localPlaySessionId: Long,
        serverRoundId: Long,
        syncedAt: Long
    )

    @Query("""
        UPDATE session_player
        SET server_id = :serverSessionPlayerId,
            updated_at = :updatedAt
        WHERE id = :localSessionPlayerId
    """)
    suspend fun setSessionPlayerServerId(
        localSessionPlayerId: Long,
        serverSessionPlayerId: Long,
        updatedAt: Long
    )

    @Query("""
        UPDATE session_player_hole
        SET server_id = :serverSessionPlayerHoleId,
            server_session_player_id = :serverSessionPlayerId,
            updated_at = :updatedAt
        WHERE id = :localSessionPlayerHoleId
    """)
    suspend fun setSessionPlayerHoleServerId(
        localSessionPlayerHoleId: Long,
        serverSessionPlayerHoleId: Long,
        serverSessionPlayerId: Long,
        updatedAt: Long
    )

    @Query("""
        UPDATE session_player_hole
        SET dirty = 0,
            last_synced_throws_count = throws_count,
            sync_error = NULL,
            updated_at = :updatedAt
        WHERE id IN (:sessionPlayerHoleIds)
    """)
    suspend fun markHolesSynced(
        sessionPlayerHoleIds: List<Long>,
        updatedAt: Long
    )

    @Query("""
        UPDATE play_session
        SET sync_status = 'synced',
            sync_error = NULL,
            last_synced_at = :syncedAt,
            updated_at = :syncedAt
        WHERE id = :playSessionId
          AND NOT EXISTS (
                SELECT 1
                FROM session_player sp
                INNER JOIN session_player_hole sph
                    ON sph.session_player_id = sp.id
                WHERE sp.play_session_id = :playSessionId
                  AND sph.dirty = 1
          )
          AND status != 'in_progress'
    """)
    suspend fun markCompletedPlaySessionSyncedIfClean(
        playSessionId: Long,
        syncedAt: Long
    )

    @Query("""
        UPDATE play_session
        SET sync_status = 'synced',
            sync_error = NULL,
            last_synced_at = :syncedAt,
            updated_at = :syncedAt
        WHERE id = :playSessionId
          AND NOT EXISTS (
                SELECT 1
                FROM session_player sp
                INNER JOIN session_player_hole sph
                    ON sph.session_player_id = sp.id
                WHERE sp.play_session_id = :playSessionId
                  AND sph.dirty = 1
          )
          AND status = 'in_progress'
    """)
    suspend fun markInProgressPlaySessionSyncedIfClean(
        playSessionId: Long,
        syncedAt: Long
    )

    @Query("""
        UPDATE play_session
        SET sync_status = 'sync_error',
            sync_error = :errorMessage,
            last_sync_attempt_at = :attemptedAt,
            updated_at = :attemptedAt
        WHERE id = :playSessionId
    """)
    suspend fun markPlaySessionSyncError(
        playSessionId: Long,
        errorMessage: String?,
        attemptedAt: Long
    )

    @Query("""
        UPDATE play_session
        SET last_sync_attempt_at = :attemptedAt,
            updated_at = :attemptedAt
        WHERE id = :playSessionId
    """)
    suspend fun markPlaySessionSyncAttempt(
        playSessionId: Long,
        attemptedAt: Long
    )

    @Transaction
    suspend fun saveServerIdsAfterRoundCreated(
        localPlaySessionId: Long,
        serverRoundId: Long,
        sessionPlayerMappings: List<Pair<Long, Long>>,
        sessionPlayerHoleMappings: List<Triple<Long, Long, Long>>,
        syncedAt: Long
    ) {
        setPlaySessionServerId(
            localPlaySessionId = localPlaySessionId,
            serverRoundId = serverRoundId,
            syncedAt = syncedAt
        )

        for ((localSessionPlayerId, serverSessionPlayerId) in sessionPlayerMappings) {
            setSessionPlayerServerId(
                localSessionPlayerId = localSessionPlayerId,
                serverSessionPlayerId = serverSessionPlayerId,
                updatedAt = syncedAt
            )
        }

        for ((localSessionPlayerHoleId, serverSessionPlayerHoleId, serverSessionPlayerId) in sessionPlayerHoleMappings) {
            setSessionPlayerHoleServerId(
                localSessionPlayerHoleId = localSessionPlayerHoleId,
                serverSessionPlayerHoleId = serverSessionPlayerHoleId,
                serverSessionPlayerId = serverSessionPlayerId,
                updatedAt = syncedAt
            )
        }
    }
}