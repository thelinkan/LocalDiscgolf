package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import nu.linkan.localdiscgolf.data.local.model.LocalResumeRoundListItem

@Dao
interface LocalResumeRoundDao {

    @Query("""
        SELECT
            ps.id,
            ps.server_id AS serverId,
            c.name AS courseName,
            ps.started_at AS startedAt,
            ps.sync_status AS syncStatus,
            ps.current_sequence_number AS currentSequenceNumber,
            EXISTS (
                SELECT 1
                FROM session_player sp
                INNER JOIN session_player_hole sph
                    ON sph.session_player_id = sp.id
                WHERE sp.play_session_id = ps.id
                  AND sph.dirty = 1
            ) AS hasDirtyHoles
        FROM play_session ps
        INNER JOIN course c
            ON c.id = ps.course_id
        WHERE ps.status = 'in_progress'
        ORDER BY ps.started_at DESC
    """)
    suspend fun getLocalInProgressRounds(): List<LocalResumeRoundListItem>

    @Query("""
        SELECT EXISTS (
            SELECT 1
            FROM play_session
            WHERE server_id = :serverRoundId
        )
    """)
    suspend fun hasLocalRoundForServerId(serverRoundId: Long): Boolean

    @Query("""
        SELECT EXISTS (
            SELECT 1
            FROM session_player sp
            INNER JOIN session_player_hole sph
                ON sph.session_player_id = sp.id
            WHERE sp.play_session_id = :playSessionId
              AND sph.dirty = 1
        )
    """)
    suspend fun hasDirtyHoles(playSessionId: Long): Boolean

    @Query("""
        UPDATE play_session
        SET status = 'completed',
            sync_status = 'synced',
            sync_error = NULL,
            updated_at = :updatedAt
        WHERE id = :playSessionId
          AND NOT EXISTS (
                SELECT 1
                FROM session_player sp
                INNER JOIN session_player_hole sph
                    ON sph.session_player_id = sp.id
                WHERE sp.play_session_id = :playSessionId
                  AND sph.dirty = 1
          )
    """)
    suspend fun markLocalRoundCompletedFromServer(
        playSessionId: Long,
        updatedAt: Long
    )
}