package app.snips.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * §8 notes the DAO is the only seam that changes if sync ever arrives —
 * so everything above it talks in Snips and Flows, never in SQL.
 */
@Dao
interface SnipDao {

    @Query("SELECT * FROM snips ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<Snip>>

    @Query("SELECT * FROM snips WHERE id = :id")
    suspend fun byId(id: String): Snip?

    /**
     * Recovery path 3 in §4b: PROCESS_TEXT hands over a passage with no URL,
     * so offer the article the last snip came from if it was saved moments ago.
     */
    @Query("SELECT * FROM snips WHERE url != '' AND savedAt >= :since ORDER BY savedAt DESC LIMIT 1")
    suspend fun mostRecentWithUrlSince(since: Long): Snip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snip: Snip)

    @Update
    suspend fun update(snip: Snip)

    @Delete
    suspend fun delete(snip: Snip)
}
