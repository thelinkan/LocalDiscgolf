package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nu.linkan.localdiscgolf.data.local.entity.PlayerEntity
import nu.linkan.localdiscgolf.data.local.model.PlayerListRow

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(player: PlayerEntity): Long

    @Query("SELECT * FROM player WHERE is_active = 1 ORDER BY name")
    fun observeActivePlayers(): Flow<List<PlayerEntity>>

    @Query("""
        SELECT
            p.id AS playerId,
            p.name AS playerName,
            COUNT(DISTINCT sp.play_session_id) AS roundCount
        FROM player p
        LEFT JOIN session_player sp ON sp.player_id = p.id
        WHERE p.is_active = 1
        GROUP BY p.id, p.name
        ORDER BY p.name
    """)
    fun observePlayerListRows(): Flow<List<PlayerListRow>>
}