package nu.linkan.localdiscgolf.data.sync

import nu.linkan.localdiscgolf.data.local.dao.ReferenceSyncDao
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleVariantEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutHoleEntity
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SyncMetadataEntity
import nu.linkan.localdiscgolf.network.ApiClient
import nu.linkan.localdiscgolf.network.CourseApiResponse
import nu.linkan.localdiscgolf.network.HoleApiResponse
import nu.linkan.localdiscgolf.network.HoleBasketApiResponse
import nu.linkan.localdiscgolf.network.HoleTeeApiResponse
import nu.linkan.localdiscgolf.network.HoleVariantApiResponse
import nu.linkan.localdiscgolf.network.LayoutApiResponse
import nu.linkan.localdiscgolf.network.UserPlayersResponse

class ReferenceSyncRepository(
    private val referenceSyncDao: ReferenceSyncDao
) {
    suspend fun syncReferenceData(
        baseUrl: String,
        token: String,
        username: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            val userPlayers = ApiClient.getUserPlayers(
                baseUrl = baseUrl,
                token = token,
                username = username
            ).getOrThrow()

            syncPlayers(userPlayers, now)

            val courses = ApiClient.getCourses(
                baseUrl = baseUrl,
                token = token,
                includeInactive = true
            ).getOrThrow()

            for (course in courses) {
                val localCourseId = upsertCourse(course, now)

                val holes = ApiClient.getCourseHoles(
                    baseUrl = baseUrl,
                    token = token,
                    courseId = course.id,
                    includeInactive = true
                ).getOrThrow()

                for (hole in holes) {
                    val localHoleId = upsertHole(
                        hole = hole,
                        localCourseId = localCourseId,
                        serverCourseId = course.id,
                        now = now
                    )

                    val tees = ApiClient.getHoleTees(
                        baseUrl = baseUrl,
                        token = token,
                        holeId = hole.id,
                        includeInactive = true
                    ).getOrThrow()

                    for (tee in tees) {
                        upsertHoleTee(
                            tee = tee,
                            localHoleId = localHoleId,
                            now = now
                        )
                    }

                    val baskets = ApiClient.getHoleBaskets(
                        baseUrl = baseUrl,
                        token = token,
                        holeId = hole.id,
                        includeInactive = true
                    ).getOrThrow()

                    for (basket in baskets) {
                        upsertHoleBasket(
                            basket = basket,
                            localHoleId = localHoleId,
                            now = now
                        )
                    }

                    val variants = ApiClient.getHoleVariants(
                        baseUrl = baseUrl,
                        token = token,
                        holeId = hole.id,
                        includeInactive = true
                    ).getOrThrow()

                    for (variant in variants) {
                        upsertHoleVariant(
                            variant = variant,
                            localHoleId = localHoleId,
                            now = now
                        )
                    }
                }

                val layouts = ApiClient.getCourseLayouts(
                    baseUrl = baseUrl,
                    token = token,
                    courseId = course.id,
                    includeInactive = true
                ).getOrThrow()

                for (layout in layouts) {
                    val localLayoutId = upsertLayout(
                        layout = layout,
                        localCourseId = localCourseId,
                        serverCourseId = course.id,
                        now = now
                    )

                    val layoutHoles = ApiClient.getLayoutHoles(
                        baseUrl = baseUrl,
                        token = token,
                        layoutId = layout.id
                    ).getOrThrow()

                    referenceSyncDao.deleteLayoutHolesForServerLayout(layout.id)

                    for (layoutHole in layoutHoles) {
                        val localHole = referenceSyncDao.getHoleByServerId(layoutHole.hole_id)
                        val localVariant = layoutHole.hole_variant_id?.let { variantServerId ->
                            referenceSyncDao.getHoleVariantByServerId(variantServerId)
                        }

                        if (localHole != null) {
                            referenceSyncDao.upsertLayoutHole(
                                LayoutHoleEntity(
                                    id = 0,
                                    layoutId = localLayoutId,
                                    holeId = localHole.id,
                                    holeVariantId = localVariant?.id,
                                    sequenceNumber = layoutHole.sequence_number,
                                    serverLayoutId = layout.id,
                                    serverHoleId = layoutHole.hole_id,
                                    serverHoleVariantId = layoutHole.hole_variant_id,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    }
                }
            }

            referenceSyncDao.upsertSyncMetadata(
                SyncMetadataEntity(
                    key = SyncMetadataEntity.KEY_REFERENCE_DATA,
                    lastSyncedAt = now,
                    lastAttemptAt = now,
                    lastError = null,
                    updatedAt = now
                )
            )

            Result.success(Unit)
        } catch (error: Exception) {
            val errorTime = System.currentTimeMillis()

            referenceSyncDao.upsertSyncMetadata(
                SyncMetadataEntity(
                    key = SyncMetadataEntity.KEY_REFERENCE_DATA,
                    lastSyncedAt = null,
                    lastAttemptAt = errorTime,
                    lastError = error.message,
                    updatedAt = errorTime
                )
            )

            Result.failure(error)
        }
    }

    private suspend fun syncPlayers(
        userPlayers: UserPlayersResponse,
        now: Long
    ) {
        val activeServerIds = mutableSetOf<Long>()

        userPlayers.own_player?.let { player ->
            activeServerIds += player.id

            upsertPlayer(
                serverId = player.id,
                name = player.name,
                permissionLevel = "owner",
                isGuest = false,
                now = now
            )
        }

        for (player in userPlayers.guest_players) {
            activeServerIds += player.id

            upsertPlayer(
                serverId = player.id,
                name = player.name,
                permissionLevel = "guest",
                isGuest = true,
                now = now
            )
        }

        for (player in userPlayers.scoreable_players) {
            if (activeServerIds.contains(player.id)) {
                continue
            }

            activeServerIds += player.id

            upsertPlayer(
                serverId = player.id,
                name = player.name,
                permissionLevel = player.permission_level,
                isGuest = false,
                now = now
            )
        }

        if (activeServerIds.isNotEmpty()) {
            referenceSyncDao.markMissingPlayersInactive(activeServerIds.toList())
        }
    }

    private suspend fun upsertPlayer(
        serverId: Long,
        name: String,
        permissionLevel: String?,
        isGuest: Boolean,
        now: Long
    ) {
        val existing = referenceSyncDao.getPlayerByServerId(serverId)

        referenceSyncDao.upsertPlayer(
            PlayerEntity(
                id = existing?.id ?: 0,
                name = name,
                serverId = serverId,
                permissionLevel = permissionLevel,
                isGuest = isGuest,
                ownerUserId = existing?.ownerUserId,
                createdByUserId = existing?.createdByUserId,
                isActive = true,
                lastSyncedAt = now,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    private suspend fun upsertCourse(
        course: CourseApiResponse,
        now: Long
    ): Long {
        val existing = referenceSyncDao.getCourseByServerId(course.id)

        referenceSyncDao.upsertCourse(
            CourseEntity(
                id = existing?.id ?: 0,
                name = course.name,
                serverId = course.id,
                isActive = course.is_active != 0,
                lastSyncedAt = now,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        return referenceSyncDao.getCourseByServerId(course.id)?.id
            ?: error("Course was not saved locally: ${course.id}")
    }

    private suspend fun upsertHole(
        hole: HoleApiResponse,
        localCourseId: Long,
        serverCourseId: Long,
        now: Long
    ): Long {
        val existing = referenceSyncDao.getHoleByServerId(hole.id)

        referenceSyncDao.upsertHole(
            HoleEntity(
                id = existing?.id ?: 0,
                courseId = localCourseId,
                serverId = hole.id,
                serverCourseId = serverCourseId,
                holeNumber = hole.hole_number,
                name = hole.name,
                lengthMeters = hole.length_meters,
                parValue = hole.par_value,
                notes = hole.notes,
                isActive = hole.is_active != 0,
                lastSyncedAt = now,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        return referenceSyncDao.getHoleByServerId(hole.id)?.id
            ?: error("Hole was not saved locally: ${hole.id}")
    }

    private suspend fun upsertHoleTee(
        tee: HoleTeeApiResponse,
        localHoleId: Long,
        now: Long
    ) {
        val existing = referenceSyncDao.getHoleTeeByServerId(tee.id)

        referenceSyncDao.upsertHoleTee(
            HoleTeeEntity(
                id = existing?.id ?: 0,
                holeId = localHoleId,
                serverId = tee.id,
                name = tee.name,
                sortOrder = tee.sort_order,
                isActive = tee.is_active != 0,
                lastSyncedAt = now,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    private suspend fun upsertHoleBasket(
        basket: HoleBasketApiResponse,
        localHoleId: Long,
        now: Long
    ) {
        val existing = referenceSyncDao.getHoleBasketByServerId(basket.id)

        referenceSyncDao.upsertHoleBasket(
            HoleBasketEntity(
                id = existing?.id ?: 0,
                holeId = localHoleId,
                serverId = basket.id,
                name = basket.name,
                sortOrder = basket.sort_order,
                isActive = basket.is_active != 0,
                lastSyncedAt = now,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    private suspend fun upsertHoleVariant(
        variant: HoleVariantApiResponse,
        localHoleId: Long,
        now: Long
    ) {
        val existing = referenceSyncDao.getHoleVariantByServerId(variant.id)

        val localTee = referenceSyncDao.getHoleTeeByServerId(variant.tee_id)
        val localBasket = referenceSyncDao.getHoleBasketByServerId(variant.basket_id)

        if (localTee == null || localBasket == null) {
            return
        }

        referenceSyncDao.upsertHoleVariant(
            HoleVariantEntity(
                id = existing?.id ?: 0,
                holeId = localHoleId,
                serverId = variant.id,
                teeId = localTee.id,
                basketId = localBasket.id,
                lengthMeters = variant.length_meters,
                parValue = variant.par_value,
                isActive = variant.is_active != 0,
                lastSyncedAt = now,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    private suspend fun upsertLayout(
        layout: LayoutApiResponse,
        localCourseId: Long,
        serverCourseId: Long,
        now: Long
    ): Long {
        val existing = referenceSyncDao.getLayoutByServerId(layout.id)

        referenceSyncDao.upsertLayout(
            LayoutEntity(
                id = existing?.id ?: 0,
                courseId = localCourseId,
                serverId = layout.id,
                serverCourseId = serverCourseId,
                name = layout.name,
                description = layout.description,
                isActive = layout.is_active != 0,
                lastSyncedAt = now,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        return referenceSyncDao.getLayoutByServerId(layout.id)?.id
            ?: error("Layout was not saved locally: ${layout.id}")
    }
}