package nu.linkan.localdiscgolf.data.local.repository

import nu.linkan.localdiscgolf.data.local.dao.LocalRoundCreationDao
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerHoleEntity

class LocalRoundCreationRepository(
    private val localRoundCreationDao: LocalRoundCreationDao
) {
    suspend fun createLocalRoundFromCache(
        courseId: Long,
        layoutId: Long,
        playerIds: List<Long>,
        startedAt: Long
    ): Result<Long> {
        return try {
            if (playerIds.isEmpty()) {
                return Result.failure(IllegalArgumentException("Minst en spelare måste väljas."))
            }

            val now = System.currentTimeMillis()

            val course = localRoundCreationDao.getCourseForRound(courseId)
                ?: return Result.failure(IllegalArgumentException("Banan finns inte i lokal cache."))

            val layout = localRoundCreationDao.getLayoutForRound(layoutId)
                ?: return Result.failure(IllegalArgumentException("Layouten finns inte i lokal cache."))

            if (layout.courseId != course.id) {
                return Result.failure(IllegalArgumentException("Layouten tillhör inte vald bana."))
            }

            val layoutHoles = localRoundCreationDao.getLayoutHolesForRound(layoutId)

            if (layoutHoles.isEmpty()) {
                return Result.failure(IllegalArgumentException("Layouten saknar hål i lokal cache."))
            }

            val players = playerIds.map { playerId ->
                localRoundCreationDao.getPlayerForRound(playerId)
                    ?: return Result.failure(
                        IllegalArgumentException("Spelare saknas i lokal cache: $playerId")
                    )
            }

            val playSession = PlaySessionEntity(
                id = 0,
                courseId = course.id,
                startedAt = startedAt,
                endedAt = null,
                status = "in_progress",

                serverId = null,
                syncStatus = "local_only",
                lastSyncAttemptAt = null,
                lastSyncedAt = null,
                syncError = null,
                createdByUserId = null,
                currentSequenceNumber = 1,

                createdAt = now,
                updatedAt = now
            )

            val sessionPlayersWithHoles = players.mapIndexed { index, player ->
                val approvalState = when (player.permissionLevel) {
                    "propose" -> "pending"
                    else -> "approved"
                }

                val approvalRequired = approvalState == "pending"

                val sessionPlayer = SessionPlayerEntity(
                    id = 0,
                    playSessionId = 0,
                    playerId = player.id,
                    layoutId = layout.id,
                    displayName = player.name,
                    startOrder = index + 1,

                    serverId = null,
                    serverPlayerId = player.serverId,
                    serverLayoutId = layout.serverId,
                    approvalRequired = approvalRequired,
                    approvalState = approvalState,
                    approvedByUserId = null,
                    approvedAt = null,

                    createdAt = now,
                    updatedAt = now
                )

                val holes = layoutHoles.map { layoutHole ->
                    SessionPlayerHoleEntity(
                        id = 0,
                        sessionPlayerId = 0,
                        sequenceNumber = layoutHole.sequenceNumber,
                        courseId = course.id,
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

                        serverId = null,
                        serverSessionPlayerId = null,
                        serverHoleId = layoutHole.serverHoleId
                            ?: return Result.failure(
                                IllegalArgumentException(
                                    "Hål ${layoutHole.holeNumber} saknar serverHoleId."
                                )
                            ),
                        serverHoleVariantId = layoutHole.serverHoleVariantId,
                        dirty = false,
                        lastSyncedThrowsCount = null,
                        syncError = null,

                        createdAt = now,
                        updatedAt = now
                    )
                }

                sessionPlayer to holes
            }

            val playSessionId = localRoundCreationDao.insertLocalRound(
                playSession = playSession,
                sessionPlayersWithHoles = sessionPlayersWithHoles
            )

            Result.success(playSessionId)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}