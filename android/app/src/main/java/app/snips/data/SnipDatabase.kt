package app.snips.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Snip::class], version = 1, exportSchema = true)
abstract class SnipDatabase : RoomDatabase() {

    abstract fun snips(): SnipDao

    companion object {
        @Volatile private var instance: SnipDatabase? = null

        /**
         * §3 rules out a DI framework, so the database is a plain lazy
         * singleton. Room's own builder is already thread-safe to call; the
         * lock is only here so two capture intents landing at once don't
         * each open their own handle.
         */
        fun get(context: Context): SnipDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SnipDatabase::class.java,
                    "snips.db",
                ).build().also { instance = it }
            }
    }
}
