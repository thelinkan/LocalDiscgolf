package nu.linkan.localdiscgolf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import nu.linkan.localdiscgolf.data.local.dao.CourseDao
import nu.linkan.localdiscgolf.data.local.dao.PlayerDao
import nu.linkan.localdiscgolf.data.local.entity.CourseEntity
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity

@Database(
    entities = [
        PlayerEntity::class,
        CourseEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun courseDao(): CourseDao
}