package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerHoleEntity
import nu.linkan.localdiscgolf.data.local.model.LayoutHoleWithHole
import nu.linkan.localdiscgolf.data.local.model.RoundHolePlayerRow
import nu.linkan.localdiscgolf.data.local.model.RoundSummaryHoleRow
import nu.linkan.localdiscgolf.data.local.model.InProgressSessionRow
import nu.linkan.localdiscgolf.data.local.model.PlayerSessionRow

@Dao
interface PlaySessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaySession(playSession: PlaySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionPlayer(sessionPlayer: SessionPlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionPlayerHoles(sessionPlayerHoles: List<SessionPlayerHoleEntity>)

    @Query("""
        SELECT
            sph.id AS sessionPlayerHoleId,
            sp.id AS sessionPlayerId,
            sp.player_id AS playerId,
            sp.display_name AS playerName,
            sph.sequence_number AS sequenceNumber,
            sph.hole_id AS holeId,
            sph.hole_number_snapshot AS holeNumberSnapshot,
            sph.hole_name_snapshot AS holeNameSnapshot,
            sph.length_snapshot_meters AS lengthSnapshotMeters,
            sph.par_snapshot AS parSnapshot,
            sph.throws_count AS throwsCount
        FROM session_player_hole sph
        INNER JOIN session_player sp ON sp.id = sph.session_player_id
        WHERE sp.play_session_id = :playSessionId
          AND sph.sequence_number = :sequenceNumber
        ORDER BY sp.start_order, sp.id
    """)
    fun observeRoundHoleRows(
        playSessionId: Long,
        sequenceNumber: Int
    ): Flow<List<RoundHolePlayerRow>>

    @Query("""
        SELECT COUNT(*)
        FROM session_player_hole sph
        INNER JOIN session_player sp ON sp.id = sph.session_player_id
        WHERE sp.play_session_id = :playSessionId
          AND sp.id = (
              SELECT MIN(id) FROM session_player WHERE play_session_id = :playSessionId
          )
    """)
    suspend fun getHoleCountForSession(playSessionId: Long): Int

    @Query("""
        UPDATE session_player_hole
        SET throws_count = :throwsCount,
            is_completed = 1,
            updated_at = :updatedAt
        WHERE id = :sessionPlayerHoleId
    """)
    suspend fun updateThrowsForSessionPlayerHole(
        sessionPlayerHoleId: Long,
        throwsCount: Int,
        updatedAt: Long
    )

    @Query("""
    UPDATE play_session
    SET ended_at = :endedAt,
        status = :status,
        updated_at = :updatedAt
    WHERE id = :playSessionId
""")
    suspend fun finishPlaySession(
        playSessionId: Long,
        endedAt: Long,
        status: String,
        updatedAt: Long
    )

    @Query("""
    SELECT
        sp.player_id AS playerId,
        sp.display_name AS playerName,
        sp.start_order AS startOrder,
        sph.sequence_number AS sequenceNumber,
        sph.hole_number_snapshot AS holeNumberSnapshot,
        sph.par_snapshot AS parSnapshot,
        sph.throws_count AS throwsCount
    FROM session_player_hole sph
    INNER JOIN session_player sp ON sp.id = sph.session_player_id
    WHERE sp.play_session_id = :playSessionId
    ORDER BY sp.start_order, sp.id, sph.sequence_number
""")
    fun observeRoundSummaryHoleRows(
        playSessionId: Long
    ): Flow<List<RoundSummaryHoleRow>>

    @Query("""
    SELECT
        ps.id AS playSessionId,
        ps.course_id AS courseId,
        c.name AS courseName,
        ps.started_at AS startedAt
    FROM play_session ps
    INNER JOIN course c ON c.id = ps.course_id
    WHERE ps.status = 'in_progress'
      AND ps.ended_at IS NULL
    ORDER BY ps.started_at DESC
""")
    fun observeInProgressSessions(): Flow<List<InProgressSessionRow>>

    @Query("""
    SELECT COALESCE(
        (
            SELECT MIN(sph.sequence_number)
            FROM session_player_hole sph
            INNER JOIN session_player sp ON sp.id = sph.session_player_id
            WHERE sp.play_session_id = :playSessionId
              AND sph.throws_count IS NULL
        ),
        1
    )
""")
    suspend fun getResumeSequenceNumber(playSessionId: Long): Int

    @Query("""
    SELECT
        ps.id AS playSessionId,
        sp.player_id AS playerId,
        sp.display_name AS playerName,
        c.name AS courseName,
        l.name AS layoutName,
        ps.started_at AS startedAt,
        ps.ended_at AS endedAt,
        ps.status AS status
    FROM session_player sp
    INNER JOIN play_session ps ON ps.id = sp.play_session_id
    INNER JOIN course c ON c.id = ps.course_id
    INNER JOIN layout l ON l.id = sp.layout_id
    WHERE sp.player_id = :playerId
    ORDER BY ps.started_at DESC
""")
    fun observeSessionsForPlayer(
        playerId: Long
    ): Flow<List<PlayerSessionRow>>

    @Transaction
    suspend fun createPlaySessionWithPlayersAndHoles(
        playSession: PlaySessionEntity,
        sessionPlayers: List<SessionPlayerEntity>,
        layoutHoles: List<LayoutHoleWithHole>,
        courseId: Long,
        createdAt: Long
    ): Long {
        val playSessionId = insertPlaySession(playSession)

        sessionPlayers.forEach { sessionPlayer ->
            val sessionPlayerId = insertSessionPlayer(
                sessionPlayer.copy(playSessionId = playSessionId)
            )

            val holeRows = layoutHoles.map { layoutHole ->
                SessionPlayerHoleEntity(
                    sessionPlayerId = sessionPlayerId,
                    sequenceNumber = layoutHole.sequenceNumber,
                    courseId = courseId,
                    holeId = layoutHole.holeId,
                    holeNumberSnapshot = layoutHole.holeNumber,
                    holeNameSnapshot = layoutHole.holeName,
                    lengthSnapshotMeters = layoutHole.lengthMeters,
                    parSnapshot = layoutHole.parValue,
                    createdAt = createdAt,
                    updatedAt = createdAt
                )
            }

            insertSessionPlayerHoles(holeRows)
        }

        return playSessionId
    }
}