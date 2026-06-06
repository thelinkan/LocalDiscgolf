package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerHoleEntity
import nu.linkan.localdiscgolf.data.local.model.CachedCourseForRound
import nu.linkan.localdiscgolf.data.local.model.CachedLayoutForRound
import nu.linkan.localdiscgolf.data.local.model.CachedLayoutHoleForRound
import nu.linkan.localdiscgolf.data.local.model.CachedPlayerForRound

@Dao
interface LocalRoundCreationDao {

    @Query("""
        SELECT
            id,
            server_id AS serverId,
            name,
            permission_level AS permissionLevel,
            is_guest AS isGuest
        FROM player
        WHERE id = :playerId
          AND is_active = 1
          AND server_id IS NOT NULL
        LIMIT 1
    """)
    suspend fun getPlayerForRound(playerId: Long): CachedPlayerForRound?

    @Query("""
        SELECT
            id,
            server_id AS serverId,
            name
        FROM course
        WHERE id = :courseId
          AND is_active = 1
          AND server_id IS NOT NULL
        LIMIT 1
    """)
    suspend fun getCourseForRound(courseId: Long): CachedCourseForRound?

    @Query("""
        SELECT
            l.id,
            l.server_id AS serverId,
            l.course_id AS courseId,
            l.server_course_id AS serverCourseId,
            l.name,
            l.description,
            COUNT(lh.id) AS holeCount,
            COALESCE(SUM(COALESCE(hv.par_value, h.par_value)), 0) AS totalPar,
            COALESCE(SUM(COALESCE(hv.length_meters, h.length_meters)), 0) AS totalLengthMeters
        FROM layout l
        LEFT JOIN layout_hole lh
            ON lh.layout_id = l.id
        LEFT JOIN hole h
            ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv
            ON hv.id = lh.hole_variant_id
        WHERE l.id = :layoutId
          AND l.is_active = 1
          AND l.server_id IS NOT NULL
        GROUP BY
            l.id,
            l.server_id,
            l.course_id,
            l.server_course_id,
            l.name,
            l.description
        LIMIT 1
    """)
    suspend fun getLayoutForRound(layoutId: Long): CachedLayoutForRound?

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
    suspend fun insertPlaySession(playSession: PlaySessionEntity): Long

    @Insert
    suspend fun insertSessionPlayer(sessionPlayer: SessionPlayerEntity): Long

    @Insert
    suspend fun insertSessionPlayerHoles(holes: List<SessionPlayerHoleEntity>)

    @Transaction
    suspend fun insertLocalRound(
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