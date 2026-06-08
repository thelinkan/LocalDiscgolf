package nu.linkan.localdiscgolf.data.local.repository

import nu.linkan.localdiscgolf.data.local.dao.LocalResumeRoundDao
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerHoleEntity
import nu.linkan.localdiscgolf.data.local.model.LocalResumeRoundListItem
import nu.linkan.localdiscgolf.network.ApiClient
import nu.linkan.localdiscgolf.network.CurrentRoundApiResponse
import nu.linkan.localdiscgolf.network.RoundDetailApiResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

class LocalResumeRoundRepository(
    private val localResumeRoundDao: LocalResumeRoundDao
) {
    suspend fun getResumeRounds(
        baseUrl: String?,
        token: String?
    ): Result<List<LocalResumeRoundListItem>> {
        return try {
            val localRounds = localResumeRoundDao.getLocalInProgressRounds()

            if (baseUrl.isNullOrBlank() || token.isNullOrBlank()) {
                return Result.success(localRounds)
            }

            val serverResult = ApiClient.getMyInProgressRounds(
                baseUrl = baseUrl,
                token = token
            )

            if (serverResult.isFailure) {
                return Result.success(localRounds)
            }

            val serverRounds = serverResult.getOrThrow()
            val serverInProgressIds = serverRounds.map { it.id }.toSet()

            val now = System.currentTimeMillis()

            for (localRound in localRounds) {
                if (localRound.serverId == null) {
                    continue
                }

                if (localRound.hasDirtyHoles) {
                    continue
                }

                if (!serverInProgressIds.contains(localRound.serverId)) {
                    localResumeRoundDao.markLocalRoundCompletedFromServer(
                        playSessionId = localRound.id,
                        updatedAt = now
                    )
                }
            }

            for (serverRound in serverRounds) {
                val existsLocally = localResumeRoundDao
                    .getLocalPlaySessionIdByServerId(serverRound.id) != null

                if (existsLocally) {
                    continue
                }

                val currentResult = ApiClient.getCurrentRound(
                    baseUrl = baseUrl,
                    token = token,
                    roundId = serverRound.id
                )

                val currentRound = currentResult.getOrNull()
                if (currentRound == null) {
                    println(
                        "Kunde inte hämta aktuell serverrunda ${serverRound.id}: " +
                                currentResult.exceptionOrNull()?.message
                    )
                    continue
                }

                if (currentRound.round.status != "in_progress") {
                    continue
                }

                if (currentRound.progress.completed_holes > 0) {
                    println(
                        "Hoppar över import av serverrunda ${serverRound.id}: " +
                                "rundan har redan ${currentRound.progress.completed_holes} spelade hål."
                    )
                    continue
                }

                val detailResult = ApiClient.getRoundDetail(
                    baseUrl = baseUrl,
                    token = token,
                    roundId = serverRound.id
                )

                val roundDetail = detailResult.getOrNull()
                if (roundDetail == null) {
                    println(
                        "Kunde inte hämta runddetalj för serverrunda ${serverRound.id}: " +
                                detailResult.exceptionOrNull()?.message
                    )
                    continue
                }

                val importResult = importEmptyServerRound(
                    currentRound = currentRound,
                    roundDetail = roundDetail
                )

                importResult.onFailure { error ->
                    println(
                        "Kunde inte importera serverrunda ${serverRound.id}: ${error.message}"
                    )
                }
            }

            val refreshedLocalRounds = localResumeRoundDao.getLocalInProgressRounds()

            Result.success(refreshedLocalRounds)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun importEmptyServerRound(
        currentRound: CurrentRoundApiResponse,
        roundDetail: RoundDetailApiResponse
    ): Result<Long> {
        return try {
            if (currentRound.progress.completed_holes > 0) {
                return Result.failure(
                    IllegalArgumentException("Endast tomma serverrundor kan importeras i första versionen.")
                )
            }

            if (currentRound.round.id != roundDetail.id) {
                return Result.failure(
                    IllegalArgumentException("Current round och round detail avser olika rundor.")
                )
            }

            val now = System.currentTimeMillis()

            val localCourseId = localResumeRoundDao.getLocalCourseIdByServerId(
                currentRound.round.course_id
            ) ?: return Result.failure(
                IllegalArgumentException(
                    "Banan saknas i lokal cache. Server course_id=${currentRound.round.course_id}"
                )
            )

            val startedAt = parseServerDateTimeToMillis(currentRound.round.started_at)
                ?: now

            val playSession = PlaySessionEntity(
                id = 0,
                courseId = localCourseId,
                startedAt = startedAt,
                endedAt = null,
                notes = null,
                status = "in_progress",

                serverId = currentRound.round.id,
                syncStatus = "synced",
                lastSyncAttemptAt = now,
                lastSyncedAt = now,
                syncError = null,
                createdByUserId = currentRound.round.created_by_user_id,
                currentSequenceNumber = currentRound.progress.current_sequence_number,

                createdAt = now,
                updatedAt = now
            )

            if (roundDetail.players.isEmpty()) {
                return Result.failure(
                    IllegalArgumentException("Serverrundan saknar spelare.")
                )
            }

            val sessionPlayersWithHoles =
                roundDetail.players.sortedBy { it.start_order }.map { serverPlayer ->
                    val serverLayoutId = serverPlayer.layout_id
                        ?: return Result.failure(
                            IllegalArgumentException(
                                "Spelare ${serverPlayer.player_name} saknar layout_id."
                            )
                        )

                    val localPlayerId = localResumeRoundDao.getLocalPlayerIdByServerId(
                        serverPlayer.player_id
                    ) ?: return Result.failure(
                        IllegalArgumentException(
                            "Spelare saknas i lokal cache. Server player_id=${serverPlayer.player_id}"
                        )
                    )

                    val localLayoutId = localResumeRoundDao.getLocalLayoutIdByServerId(
                        serverLayoutId
                    ) ?: return Result.failure(
                        IllegalArgumentException(
                            "Layout saknas i lokal cache. Server layout_id=$serverLayoutId"
                        )
                    )

                    val layoutHoles = localResumeRoundDao.getLayoutHolesForRound(localLayoutId)

                    if (layoutHoles.isEmpty()) {
                        return Result.failure(
                            IllegalArgumentException(
                                "Lokal layout saknar hål. Local layout id=$localLayoutId"
                            )
                        )
                    }

                    val serverHolesBySequence =
                        serverPlayer.holes.associateBy { it.sequence_number }

                    val sessionPlayer = SessionPlayerEntity(
                        id = 0,
                        playSessionId = 0,
                        playerId = localPlayerId,
                        layoutId = localLayoutId,
                        isCustomLayout = false,
                        displayName = serverPlayer.player_name,
                        startOrder = serverPlayer.start_order,
                        status = "in_progress",
                        notes = null,
                        totalThrows = null,
                        totalPar = null,
                        scoreRelativeToPar = null,

                        serverId = serverPlayer.id,
                        serverPlayerId = serverPlayer.player_id,
                        serverLayoutId = serverLayoutId,
                        approvalRequired = serverPlayer.approval_required != 0,
                        approvalState = serverPlayer.approval_state,
                        approvedByUserId = null,
                        approvedAt = parseServerDateTimeToMillis(serverPlayer.approved_at),

                        createdAt = now,
                        updatedAt = now
                    )

                    val holes = layoutHoles.map { layoutHole ->
                        val serverHole = serverHolesBySequence[layoutHole.sequenceNumber]
                            ?: return Result.failure(
                                IllegalArgumentException(
                                    "Saknar serverhål för sekvens ${layoutHole.sequenceNumber}."
                                )
                            )

                        if (layoutHole.serverHoleId != null &&
                            layoutHole.serverHoleId != serverHole.hole_id
                        ) {
                            return Result.failure(
                                IllegalArgumentException(
                                    "Hålmatchning misslyckades för sekvens ${layoutHole.sequenceNumber}. " +
                                            "Lokal serverHoleId=${layoutHole.serverHoleId}, " +
                                            "server hole_id=${serverHole.hole_id}."
                                )
                            )
                        }

                        if (layoutHole.serverHoleVariantId != serverHole.hole_variant_id) {
                            return Result.failure(
                                IllegalArgumentException(
                                    "Hålvariant matchar inte för sekvens ${layoutHole.sequenceNumber}. " +
                                            "Lokal serverHoleVariantId=${layoutHole.serverHoleVariantId}, " +
                                            "server hole_variant_id=${serverHole.hole_variant_id}."
                                )
                            )
                        }

                        SessionPlayerHoleEntity(
                            id = 0,
                            sessionPlayerId = 0,
                            sequenceNumber = layoutHole.sequenceNumber,
                            courseId = localCourseId,
                            holeId = layoutHole.holeId,
                            holeVariantId = layoutHole.holeVariantId,
                            holeNumberSnapshot = layoutHole.holeNumber,
                            holeNameSnapshot = layoutHole.holeName,
                            teeNameSnapshot = layoutHole.teeName,
                            basketNameSnapshot = layoutHole.basketName,
                            lengthSnapshotMeters = layoutHole.lengthMeters,
                            parSnapshot = layoutHole.parValue,
                            throwsCount = serverHole.throws_count,
                            isCompleted = serverHole.is_completed != 0,

                            serverId = serverHole.id,
                            serverSessionPlayerId = serverPlayer.id,
                            serverHoleId = serverHole.hole_id,
                            serverHoleVariantId = serverHole.hole_variant_id,
                            dirty = false,
                            lastSyncedThrowsCount = serverHole.throws_count,
                            syncError = null,

                            createdAt = now,
                            updatedAt = now
                        )
                    }

                    sessionPlayer to holes
                }

            val playSessionId = localResumeRoundDao.insertImportedServerRound(
                playSession = playSession,
                sessionPlayersWithHoles = sessionPlayersWithHoles
            )

            Result.success(playSessionId)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun parseServerDateTimeToMillis(value: String?): Long? {
        if (value.isNullOrBlank()) {
            return null
        }

        return try {
            OffsetDateTime.parse(value)
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}