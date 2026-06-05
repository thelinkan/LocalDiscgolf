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
import nu.linkan.localdiscgolf.data.local.model.PlayerHoleStatsRow
import nu.linkan.localdiscgolf.data.local.model.PlayerLayoutStatsRow
import nu.linkan.localdiscgolf.data.local.model.PlayerHoleDetailRoundRow
import nu.linkan.localdiscgolf.data.local.model.RoundHolePlayerStatsRow
import nu.linkan.localdiscgolf.data.local.model.RoundSummaryHeaderRow

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
        sph.hole_variant_id AS holeVariantId,
        sph.hole_number_snapshot AS holeNumberSnapshot,
        sph.hole_name_snapshot AS holeNameSnapshot,
        sph.tee_name_snapshot AS teeNameSnapshot,
        sph.basket_name_snapshot AS basketNameSnapshot,
        sph.length_snapshot_meters AS lengthSnapshotMeters,
        sph.par_snapshot AS parSnapshot,
        sph.throws_count AS throwsCount,

        COALESCE((
            SELECT SUM(prev.throws_count)
            FROM session_player_hole prev
            WHERE prev.session_player_id = sph.session_player_id
              AND prev.sequence_number < sph.sequence_number
              AND prev.throws_count IS NOT NULL
        ), 0) AS previousThrowsTotal,

        COALESCE((
            SELECT SUM(prev.throws_count - prev.par_snapshot)
            FROM session_player_hole prev
            WHERE prev.session_player_id = sph.session_player_id
              AND prev.sequence_number < sph.sequence_number
              AND prev.throws_count IS NOT NULL
        ), 0) AS previousRelativeToPar

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
        sph.hole_variant_id AS holeVariantId,
        sph.hole_number_snapshot AS holeNumberSnapshot,
        sph.tee_name_snapshot AS teeNameSnapshot,
        sph.basket_name_snapshot AS basketNameSnapshot,
        sph.length_snapshot_meters AS lengthSnapshotMeters,
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
        c.id AS courseId,
        c.name AS courseName,
        l.name AS layoutName,
        COUNT(DISTINCT ps.id) AS roundsPlayed,
        MIN(player_round.totalThrows) AS bestThrows,
        AVG(player_round.totalThrows * 1.0) AS avgThrows,
        MIN(player_round.totalThrows - player_round.totalPar) AS bestRelativeToPar,
        AVG((player_round.totalThrows - player_round.totalPar) * 1.0) AS avgRelativeToPar
    FROM (
        SELECT
            sp.id AS sessionPlayerId,
            sp.player_id AS playerId,
            sp.layout_id AS layoutId,
            sp.play_session_id AS playSessionId,
            SUM(CASE WHEN sph.throws_count IS NOT NULL THEN sph.throws_count ELSE 0 END) AS totalThrows,
            SUM(sph.par_snapshot) AS totalPar,
            COUNT(*) AS holeCount,
            COUNT(sph.throws_count) AS completedHoleCount
        FROM session_player sp
        INNER JOIN session_player_hole sph ON sph.session_player_id = sp.id
        GROUP BY sp.id, sp.player_id, sp.layout_id, sp.play_session_id
    ) AS player_round
    INNER JOIN session_player sp ON sp.id = player_round.sessionPlayerId
    INNER JOIN play_session ps ON ps.id = player_round.playSessionId
    INNER JOIN course c ON c.id = ps.course_id
    INNER JOIN layout l ON l.id = sp.layout_id
    WHERE sp.player_id = :playerId
      AND ps.status = 'completed'
      AND player_round.completedHoleCount = player_round.holeCount
    GROUP BY c.id, c.name, l.name
    ORDER BY c.name, l.name
""")
    fun observeLayoutStatsForPlayer(
        playerId: Long
    ): Flow<List<PlayerLayoutStatsRow>>

    @Query("""
    SELECT
        c.id AS courseId,
        c.name AS courseName,
        sph.hole_number_snapshot AS holeNumber,
        sph.hole_variant_id AS holeVariantId,
        sph.tee_name_snapshot AS teeName,
        sph.basket_name_snapshot AS basketName,
        sph.length_snapshot_meters AS lengthMeters,
        sph.par_snapshot AS parValue,
        COUNT(*) AS timesPlayed,
        MIN(sph.throws_count) AS bestThrows,
        AVG(CAST(sph.throws_count AS REAL)) AS avgThrows,
        SUM(CASE WHEN sph.throws_count - sph.par_snapshot <= -1 THEN 1 ELSE 0 END) AS birdiesOrBetter,
        SUM(CASE WHEN sph.throws_count - sph.par_snapshot = 0 THEN 1 ELSE 0 END) AS pars,
        SUM(CASE WHEN sph.throws_count - sph.par_snapshot = 1 THEN 1 ELSE 0 END) AS bogeys,
        SUM(CASE WHEN sph.throws_count - sph.par_snapshot = 2 THEN 1 ELSE 0 END) AS doubleBogeys,
        SUM(CASE WHEN sph.throws_count - sph.par_snapshot >= 3 THEN 1 ELSE 0 END) AS tripleBogeysOrWorse
    FROM session_player_hole sph
    INNER JOIN session_player sp ON sp.id = sph.session_player_id
    INNER JOIN play_session ps ON ps.id = sp.play_session_id
    INNER JOIN course c ON c.id = ps.course_id
    WHERE sp.player_id = :playerId
      AND ps.status = 'completed'
      AND sph.throws_count IS NOT NULL
    GROUP BY
        c.id,
        c.name,
        sph.hole_number_snapshot,
        sph.hole_variant_id,
        sph.tee_name_snapshot,
        sph.basket_name_snapshot,
        sph.length_snapshot_meters,
        sph.par_snapshot
    ORDER BY
        c.name,
        sph.hole_number_snapshot,
        sph.length_snapshot_meters,
        sph.par_snapshot,
        sph.tee_name_snapshot,
        sph.basket_name_snapshot
""")
    fun observeHoleStatsForPlayer(
        playerId: Long
    ): Flow<List<PlayerHoleStatsRow>>

    @Query("""
    SELECT
        ps.id AS playSessionId,
        ps.started_at AS startedAt,
        c.id AS courseId,
        c.name AS courseName,
        l.name AS layoutName,
        sph.hole_number_snapshot AS holeNumber,
        sph.par_snapshot AS parSnapshot,
        sph.throws_count AS throwsCount
    FROM session_player_hole sph
    INNER JOIN session_player sp ON sp.id = sph.session_player_id
    INNER JOIN play_session ps ON ps.id = sp.play_session_id
    INNER JOIN course c ON c.id = ps.course_id
    INNER JOIN layout l ON l.id = sp.layout_id
    WHERE sp.player_id = :playerId
      AND c.id = :courseId
      AND sph.hole_number_snapshot = :holeNumber
      AND (
            (:holeVariantId IS NOT NULL AND sph.hole_variant_id = :holeVariantId)
         OR (:holeVariantId IS NULL AND sph.hole_variant_id IS NULL)
      )
      AND ps.status = 'completed'
      AND sph.throws_count IS NOT NULL
    ORDER BY ps.started_at DESC
""")
    fun observeHoleDetailForPlayer(
        playerId: Long,
        courseId: Long,
        holeNumber: Int,
        holeVariantId: Long?
    ): Flow<List<PlayerHoleDetailRoundRow>>

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
        c.name AS courseName,
        l.name AS layoutName,
        ps.started_at AS startedAt,
        ps.ended_at AS endedAt,
        ps.status AS status,

        (
            SELECT COUNT(*)
            FROM session_player sp_count
            WHERE sp_count.play_session_id = ps.id
        ) AS playerCount,

        (
            SELECT SUM(sph.throws_count)
            FROM session_player sp_me
            INNER JOIN session_player_hole sph ON sph.session_player_id = sp_me.id
            WHERE sp_me.play_session_id = ps.id
              AND sp_me.player_id = :playerId
              AND sph.throws_count IS NOT NULL
        ) AS totalThrows,

        (
            SELECT SUM(sph.throws_count - sph.par_snapshot)
            FROM session_player sp_me
            INNER JOIN session_player_hole sph ON sph.session_player_id = sp_me.id
            WHERE sp_me.play_session_id = ps.id
              AND sp_me.player_id = :playerId
              AND sph.throws_count IS NOT NULL
        ) AS totalRelativeToPar,

        (
            SELECT COUNT(*)
            FROM session_player sp_me
            INNER JOIN session_player_hole sph ON sph.session_player_id = sp_me.id
            WHERE sp_me.play_session_id = ps.id
              AND sp_me.player_id = :playerId
              AND sph.throws_count IS NOT NULL
        ) AS playedHoleCount,

        (
            SELECT COUNT(*)
            FROM session_player sp_me
            INNER JOIN session_player_hole sph ON sph.session_player_id = sp_me.id
            WHERE sp_me.play_session_id = ps.id
              AND sp_me.player_id = :playerId
        ) AS totalHoleCount

    FROM play_session ps
    INNER JOIN session_player sp ON sp.play_session_id = ps.id
    INNER JOIN course c ON c.id = ps.course_id
    LEFT JOIN layout l ON l.id = sp.layout_id
    WHERE sp.player_id = :playerId
    ORDER BY ps.started_at DESC
""")
    fun observeSessionsForPlayer(
        playerId: Long
    ): Flow<List<PlayerSessionRow>>

    @Query("""
    SELECT
        ps.id AS playSessionId,
        c.name AS courseName,
        (
            SELECT l.name
            FROM session_player sp
            LEFT JOIN layout l ON l.id = sp.layout_id
            WHERE sp.play_session_id = ps.id
            ORDER BY sp.id
            LIMIT 1
        ) AS layoutName,
        ps.started_at AS startedAt
    FROM play_session ps
    INNER JOIN course c ON c.id = ps.course_id
    WHERE ps.id = :playSessionId
""")
    fun observeRoundSummaryHeader(
        playSessionId: Long
    ): Flow<RoundSummaryHeaderRow?>

    @Query("""
    SELECT
        hist_sp.player_id AS playerId,
        hist_sph.hole_id AS holeId,
        COUNT(*) AS timesPlayed,
        MIN(hist_sph.throws_count) AS bestThrows,
        AVG(hist_sph.throws_count * 1.0) AS avgThrows,
        SUM(CASE WHEN hist_sph.throws_count <= hist_sph.par_snapshot - 1 THEN 1 ELSE 0 END) AS birdiesOrBetter,
        SUM(CASE WHEN hist_sph.throws_count = hist_sph.par_snapshot THEN 1 ELSE 0 END) AS pars,
        SUM(CASE WHEN hist_sph.throws_count = hist_sph.par_snapshot + 1 THEN 1 ELSE 0 END) AS bogeys,
        SUM(CASE WHEN hist_sph.throws_count = hist_sph.par_snapshot + 2 THEN 1 ELSE 0 END) AS doubleBogeys,
        SUM(CASE WHEN hist_sph.throws_count >= hist_sph.par_snapshot + 3 THEN 1 ELSE 0 END) AS tripleBogeysOrWorse
    FROM session_player current_sp
    INNER JOIN session_player hist_sp
        ON hist_sp.player_id = current_sp.player_id
    INNER JOIN play_session hist_ps
        ON hist_ps.id = hist_sp.play_session_id
    INNER JOIN session_player_hole hist_sph
        ON hist_sph.session_player_id = hist_sp.id
    WHERE current_sp.play_session_id = :playSessionId
      AND hist_ps.status = 'completed'
      AND hist_sph.throws_count IS NOT NULL
      AND (
            (:holeVariantId IS NOT NULL AND hist_sph.hole_variant_id = :holeVariantId)
         OR (:holeVariantId IS NULL AND hist_sph.hole_id = :holeId AND hist_sph.hole_variant_id IS NULL)
      )
    GROUP BY hist_sp.player_id, hist_sph.hole_id
""")
    fun observeHoleStatsForPlayersInSessionOnHole(
        playSessionId: Long,
        holeId: Long,
        holeVariantId: Long?
    ): Flow<List<RoundHolePlayerStatsRow>>

    @Query("""
    DELETE FROM play_session
    WHERE id = :playSessionId
""")
    suspend fun deletePlaySession(playSessionId: Long)

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
                    holeVariantId = layoutHole.holeVariantId,
                    holeNumberSnapshot = layoutHole.holeNumber,
                    holeNameSnapshot = layoutHole.holeName,
                    teeNameSnapshot = layoutHole.teeName,
                    basketNameSnapshot = layoutHole.basketName,
                    lengthSnapshotMeters = layoutHole.lengthMeters,
                    parSnapshot = layoutHole.parValue,
                    throwsCount = null,
                    isCompleted = false,
                    createdAt = createdAt,
                    updatedAt = createdAt,

                    // Nya synkfälten
                    serverId = null,
                    serverSessionPlayerId = null,
                    serverHoleId = layoutHole.serverHoleId,
                    serverHoleVariantId = layoutHole.serverHoleVariantId,
                    dirty = false,
                    lastSyncedThrowsCount = null,
                    syncError = null
                )
            }

            insertSessionPlayerHoles(holeRows)
        }

        return playSessionId
    }
}