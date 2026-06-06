package nu.linkan.localdiscgolf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import nu.linkan.localdiscgolf.data.local.dao.CourseDao
import nu.linkan.localdiscgolf.data.local.dao.HoleDao
import nu.linkan.localdiscgolf.data.local.dao.LayoutDao
import nu.linkan.localdiscgolf.data.local.dao.PlaySessionDao
import nu.linkan.localdiscgolf.data.local.dao.PlayerDao
import nu.linkan.localdiscgolf.data.local.dao.SyncDao
import nu.linkan.localdiscgolf.data.local.dao.ReferenceSyncDao
import nu.linkan.localdiscgolf.data.local.dao.CachedRoundSetupDao
import nu.linkan.localdiscgolf.data.local.dao.LocalRoundCreationDao
import nu.linkan.localdiscgolf.data.local.dao.RoundSyncDao
import nu.linkan.localdiscgolf.data.local.dao.LocalResumeRoundDao
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleBasketEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleTeeEntity
import nu.linkan.localdiscgolf.data.local.entity.HoleVariantEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutEntity
import nu.linkan.localdiscgolf.data.local.entity.LayoutHoleEntity
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerHoleEntity
import nu.linkan.localdiscgolf.data.local.entity.SyncMetadataEntity
import nu.linkan.localdiscgolf.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        PlayerEntity::class,
        CourseEntity::class,
        HoleEntity::class,
        HoleTeeEntity::class,
        HoleBasketEntity::class,
        HoleVariantEntity::class,
        LayoutEntity::class,
        LayoutHoleEntity::class,
        PlaySessionEntity::class,
        SessionPlayerEntity::class,
        SessionPlayerHoleEntity::class,
        SyncQueueEntity::class,
        SyncMetadataEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun courseDao(): CourseDao
    abstract fun holeDao(): HoleDao
    abstract fun layoutDao(): LayoutDao
    abstract fun playSessionDao(): PlaySessionDao
    abstract fun referenceSyncDao(): ReferenceSyncDao
    abstract fun syncDao(): SyncDao
    abstract fun cachedRoundSetupDao(): CachedRoundSetupDao
    abstract fun localRoundCreationDao(): LocalRoundCreationDao
    abstract fun roundSyncDao(): RoundSyncDao
    abstract fun localResumeRoundDao(): LocalResumeRoundDao
}