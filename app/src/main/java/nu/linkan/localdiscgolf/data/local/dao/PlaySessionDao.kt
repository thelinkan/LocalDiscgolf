package nu.linkan.localdiscgolf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import nu.linkan.localdiscgolf.data.local.entity.PlaySessionEntity
import nu.linkan.localdiscgolf.data.local.entity.SessionPlayerEntity

@Dao
interface PlaySessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaySession(playSession: PlaySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionPlayers(sessionPlayers: List<SessionPlayerEntity>)

    @Transaction
    suspend fun createPlaySessionWithPlayers(
        playSession: PlaySessionEntity,
        sessionPlayers: List<SessionPlayerEntity>
    ): Long {
        val playSessionId = insertPlaySession(playSession)
        insertSessionPlayers(
            sessionPlayers.map { it.copy(playSessionId = playSessionId) }
        )
        return playSessionId
    }
}