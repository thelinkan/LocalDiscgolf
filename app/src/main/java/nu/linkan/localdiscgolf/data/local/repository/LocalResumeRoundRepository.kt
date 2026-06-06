package nu.linkan.localdiscgolf.data.local.repository

import nu.linkan.localdiscgolf.data.local.dao.LocalResumeRoundDao
import nu.linkan.localdiscgolf.data.local.model.LocalResumeRoundListItem
import nu.linkan.localdiscgolf.network.ApiClient

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

            val refreshedLocalRounds = localResumeRoundDao.getLocalInProgressRounds()

            Result.success(refreshedLocalRounds)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}