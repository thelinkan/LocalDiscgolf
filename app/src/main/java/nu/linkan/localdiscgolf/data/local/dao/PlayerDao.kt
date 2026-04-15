package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(player: PlayerEntity): Long

    @Query("SELECT * FROM player WHERE is_active = 1 ORDER BY name")
    fun observeActivePlayers(): Flow<List<PlayerEntity>>
}