package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import nu.linkan.localdiscgolf.data.local.model.CachedCourseForRound
import nu.linkan.localdiscgolf.data.local.model.CachedLayoutForRound
import nu.linkan.localdiscgolf.data.local.model.CachedLayoutHoleForRound
import nu.linkan.localdiscgolf.data.local.model.CachedPlayerForRound

@Dao
interface CachedRoundSetupDao {

    @Query("""
        SELECT
            id,
            server_id AS serverId,
            name,
            permission_level AS permissionLevel,
            is_guest AS isGuest
        FROM player
        WHERE is_active = 1
          AND server_id IS NOT NULL
        ORDER BY
            is_guest ASC,
            name
    """)
    suspend fun getPlayersForRound(): List<CachedPlayerForRound>

    @Query("""
        SELECT
            id,
            server_id AS serverId,
            name
        FROM course
        WHERE is_active = 1
          AND server_id IS NOT NULL
        ORDER BY name
    """)
    suspend fun getCoursesForRound(): List<CachedCourseForRound>

    @Query("""
        SELECT
            l.id,
            l.server_id AS serverId,
            l.course_id AS courseId,
            l.server_course_id AS serverCourseId,
            l.name,
            l.description,
            COUNT(lh.id) AS holeCount,
            COALESCE(SUM(COALESCE(hv.length_meters, h.length_meters)), 0) AS totalLengthMeters,
            COALESCE(SUM(COALESCE(hv.par_value, h.par_value)), 0) AS totalPar
        FROM layout l
        LEFT JOIN layout_hole lh
            ON lh.layout_id = l.id
        LEFT JOIN hole h
            ON h.id = lh.hole_id
        LEFT JOIN hole_variant hv
            ON hv.id = lh.hole_variant_id
        WHERE l.course_id = :courseId
          AND l.is_active = 1
          AND l.server_id IS NOT NULL
        GROUP BY
            l.id,
            l.server_id,
            l.course_id,
            l.server_course_id,
            l.name,
            l.description
        ORDER BY l.name
    """)
    suspend fun getLayoutsForCourse(courseId: Long): List<CachedLayoutForRound>

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
}