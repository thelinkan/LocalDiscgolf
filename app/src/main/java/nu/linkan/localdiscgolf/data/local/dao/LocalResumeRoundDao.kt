package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Transaction
import nu.linkan.localdiscgolf.data.local.model.CachedLayoutHoleForRound
import nu.linkan.localdiscgolf.data.local.model.LocalResumeRoundListItem
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerHoleEntity

@Dao
interface LocalResumeRoundDao {

    @Insert
    suspend fun insertPlaySession(entity: PlaySessionEntity): Long

    @Insert
    suspend fun insertSessionPlayer(entity: SessionPlayerEntity): Long

    @Insert
    suspend fun insertSessionPlayerHole(entity: SessionPlayerHoleEntity): Long

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

    @Query("""
    SELECT id
    FROM play_session
    WHERE server_id = :serverRoundId
    LIMIT 1
""")
    suspend fun getLocalPlaySessionIdByServerId(serverRoundId: Long): Long?

    @Query("""
    SELECT id
    FROM course
    WHERE server_id = :serverCourseId
    LIMIT 1
""")
    suspend fun getLocalCourseIdByServerId(serverCourseId: Long): Long?

    @Query("""
    SELECT id
    FROM player
    WHERE server_id = :serverPlayerId
    LIMIT 1
""")
    suspend fun getLocalPlayerIdByServerId(serverPlayerId: Long): Long?

    @Query("""
    SELECT id
    FROM layout
    WHERE server_id = :serverLayoutId
    LIMIT 1
""")
    suspend fun getLocalLayoutIdByServerId(serverLayoutId: Long): Long?

    @Query("""
    SELECT id
    FROM hole_variant
    WHERE server_id = :serverHoleVariantId
    LIMIT 1
""")
    suspend fun getLocalHoleVariantIdByServerId(serverHoleVariantId: Long): Long?

    @Query("""
    SELECT
        lh.id AS layoutHoleId,
        lh.layout_id AS layoutId,
        lh.server_layout_id AS serverLayoutId,
        lh.sequence_number AS sequenceNumber,
        lh.hole_id AS holeId,
        lh.server_hole_id AS serverHoleId,
        lh.hole_variant_id AS holeVariantId,
        lh.server_hole_variant_id AS serverHoleVariantId,
        h.hole_number AS holeNumber,
        h.name AS holeName,
        ht.name AS teeName,
        hb.name AS basketName,
        COALESCE(hv.length_meters, h.length_meters) AS lengthMeters,
        COALESCE(hv.par_value, h.par_value) AS parValue
    FROM layout_hole lh
    INNER JOIN hole h
        ON h.id = lh.hole_id
    LEFT JOIN hole_variant hv
        ON hv.id = lh.hole_variant_id
    LEFT JOIN hole_tee ht
        ON ht.id = hv.tee_id
    LEFT JOIN hole_basket hb
        ON hb.id = hv.basket_id
    WHERE lh.layout_id = :layoutId
      AND h.is_active = 1
      AND (hv.id IS NULL OR hv.is_active = 1)
    ORDER BY lh.sequence_number
""")
    suspend fun getLayoutHolesForRound(layoutId: Long): List<CachedLayoutHoleForRound>

    @Insert
    suspend fun insertSessionPlayerHoles(holes: List<SessionPlayerHoleEntity>)

    @Transaction
    suspend fun insertImportedServerRound(
        playSession: PlaySessionEntity,
        sessionPlayersWithHoles: List<Pair<SessionPlayerEntity, List<SessionPlayerHoleEntity>>>
    ): Long {
        val playSessionId = insertPlaySession(playSession)

        for ((sessionPlayer, holes) in sessionPlayersWithHoles) {
            val sessionPlayerId = insertSessionPlayer(
                sessionPlayer.copy(playSessionId = playSessionId)
            )

            insertSessionPlayerHoles(
                holes.map { hole ->
                    hole.copy(sessionPlayerId = sessionPlayerId)
                }
            )
        }

        return playSessionId
    }
}