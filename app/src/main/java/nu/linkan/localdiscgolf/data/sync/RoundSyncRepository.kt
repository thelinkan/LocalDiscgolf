package nu.linkan.localdiscgolf.data.sync

import nu.linkan.localdiscgolf.data.local.dao.RoundSyncDao
import nu.linkan.localdiscgolf.data.local.model.PendingRoundForSync
import nu.linkan.localdiscgolf.data.local.model.RoundSyncHole
import nu.linkan.localdiscgolf.data.local.model.RoundSyncPlayer
import nu.linkan.localdiscgolf.network.ApiClient
import nu.linkan.localdiscgolf.network.CreateRoundApiRequest
import nu.linkan.localdiscgolf.network.CreateRoundPlayerApiRequest
import nu.linkan.localdiscgolf.network.RoundDetailApiResponse
import nu.linkan.localdiscgolf.network.RoundHoleScoreUpdateApiRequest
import nu.linkan.localdiscgolf.network.UpdateRoundApiRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RoundSyncRepository(
    private val roundSyncDao: RoundSyncDao
) {
    fun syncPendingRounds(
        baseUrl: String,
        token: String
    ): Result<Int> {
        return try {
            val pendingRounds = runBlockingDao {
                roundSyncDao.getPendingRoundsForSync()
            }

            var syncedCount = 0

            for (round in pendingRounds) {
                val result = syncSingleRound(
                    baseUrl = baseUrl,
                    token = token,
                    round = round
                )

                if (result.isSuccess) {
                    syncedCount += 1
                }
            }

            Result.success(syncedCount)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun syncSingleRound(
        baseUrl: String,
        token: String,
        round: PendingRoundForSync
    ): Result<Unit> {
        return try {
            val attemptTime = System.currentTimeMillis()

            runBlockingDao {
                roundSyncDao.markPlaySessionSyncAttempt(
                    playSessionId = round.id,
                    attemptedAt = attemptTime
                )
            }

            val serverRoundId = if (round.serverId == null) {
                createRoundOnServer(
                    baseUrl = baseUrl,
                    token = token,
                    round = round
                )
            } else {
                round.serverId
            }

            syncDirtyScores(
                baseUrl = baseUrl,
                token = token,
                localPlaySessionId = round.id,
                serverRoundId = serverRoundId
            )

            syncCompletionIfNeeded(
                baseUrl = baseUrl,
                token = token,
                round = round.copy(serverId = serverRoundId),
                serverRoundId = serverRoundId
            )

            val syncedAt = System.currentTimeMillis()

            runBlockingDao {
                if (round.status == "in_progress") {
                    roundSyncDao.markInProgressPlaySessionSyncedIfClean(
                        playSessionId = round.id,
                        syncedAt = syncedAt
                    )
                } else {
                    roundSyncDao.markCompletedPlaySessionSyncedIfClean(
                        playSessionId = round.id,
                        syncedAt = syncedAt
                    )
                }
            }

            Result.success(Unit)
        } catch (error: Exception) {
            val now = System.currentTimeMillis()

            runBlockingDao {
                roundSyncDao.markPlaySessionSyncError(
                    playSessionId = round.id,
                    errorMessage = error.message,
                    attemptedAt = now
                )
            }

            Result.failure(error)
        }
    }

    private fun createRoundOnServer(
        baseUrl: String,
        token: String,
        round: PendingRoundForSync
    ): Long {
        val serverCourseId = round.serverCourseId
            ?: error("Rundan saknar serverCourseId.")

        val players = runBlockingDao {
            roundSyncDao.getPlayersForRoundSync(round.id)
        }

        if (players.isEmpty()) {
            error("Rundan saknar spelare.")
        }

        val request = CreateRoundApiRequest(
            course_id = serverCourseId,
            started_at = formatServerDateTime(round.startedAt),
            players = players.map { player ->
                CreateRoundPlayerApiRequest(
                    player_id = player.serverPlayerId,
                    layout_id = player.serverLayoutId
                )
            }
        )

        val createdRound = ApiClient.createRound(
            baseUrl = baseUrl,
            token = token,
            requestBody = request
        ).getOrThrow()

        saveServerIdsFromCreatedRound(
            localRound = round,
            localPlayers = players,
            serverRound = createdRound
        )

        return createdRound.id
    }

    private fun saveServerIdsFromCreatedRound(
        localRound: PendingRoundForSync,
        localPlayers: List<RoundSyncPlayer>,
        serverRound: RoundDetailApiResponse
    ) {
        val localPlayersByServerPlayerId = localPlayers.associateBy { it.serverPlayerId }

        val sessionPlayerMappings = mutableListOf<Pair<Long, Long>>()
        val sessionPlayerHoleMappings = mutableListOf<Triple<Long, Long, Long>>()

        val localHoles = runBlockingDao {
            roundSyncDao.getHolesForRoundSync(localRound.id)
        }

        val localHolesByPlayerAndSequence = localHoles.groupBy { localHole ->
            val localPlayer = localPlayers.firstOrNull { it.id == localHole.sessionPlayerId }
            "${localPlayer?.serverPlayerId}:${localHole.sequenceNumber}:${localHole.serverHoleVariantId}"
        }

        for (serverPlayer in serverRound.players) {
            val localPlayer = localPlayersByServerPlayerId[serverPlayer.player_id]
                ?: continue

            sessionPlayerMappings += localPlayer.id to serverPlayer.id

            for (serverHole in serverPlayer.holes) {
                val key = "${serverPlayer.player_id}:${serverHole.sequence_number}:${serverHole.hole_variant_id}"
                val localHole = localHolesByPlayerAndSequence[key]?.firstOrNull()
                    ?: continue

                sessionPlayerHoleMappings += Triple(
                    localHole.id,
                    serverHole.id,
                    serverPlayer.id
                )
            }
        }

        val now = System.currentTimeMillis()

        runBlockingDao {
            roundSyncDao.saveServerIdsAfterRoundCreated(
                localPlaySessionId = localRound.id,
                serverRoundId = serverRound.id,
                sessionPlayerMappings = sessionPlayerMappings,
                sessionPlayerHoleMappings = sessionPlayerHoleMappings,
                syncedAt = now
            )
        }
    }

    private fun syncDirtyScores(
        baseUrl: String,
        token: String,
        localPlaySessionId: Long,
        serverRoundId: Long
    ) {
        val dirtyHoles = runBlockingDao {
            roundSyncDao.getDirtyHolesForRoundSync(localPlaySessionId)
        }

        val dirtyHolesWithServerIds = dirtyHoles.filter { it.serverId != null }

        if (dirtyHolesWithServerIds.isEmpty()) {
            return
        }

        val request = UpdateRoundApiRequest(
            scores = dirtyHolesWithServerIds.map { hole ->
                RoundHoleScoreUpdateApiRequest(
                    session_player_hole_id = hole.serverId!!,
                    throws_count = hole.throwsCount
                )
            }
        )

        ApiClient.updateRound(
            baseUrl = baseUrl,
            token = token,
            roundId = serverRoundId,
            requestBody = request
        ).getOrThrow()

        val now = System.currentTimeMillis()

        runBlockingDao {
            roundSyncDao.markHolesSynced(
                sessionPlayerHoleIds = dirtyHolesWithServerIds.map { it.id },
                updatedAt = now
            )
        }
    }

    private fun syncCompletionIfNeeded(
        baseUrl: String,
        token: String,
        round: PendingRoundForSync,
        serverRoundId: Long
    ) {
        if (round.status == "in_progress") {
            return
        }

        val request = UpdateRoundApiRequest(
            ended_at = round.endedAt?.let { formatServerDateTime(it) },
            status = round.status
        )

        ApiClient.updateRound(
            baseUrl = baseUrl,
            token = token,
            roundId = serverRoundId,
            requestBody = request
        ).getOrThrow()
    }

    private fun formatServerDateTime(value: Long): String {
        return Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun <T> runBlockingDao(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking {
            block()
        }
    }
}