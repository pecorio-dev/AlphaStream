package dev.pecorio.alphastream.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import dev.pecorio.alphastream.data.dao.WatchProgressDao
import dev.pecorio.alphastream.data.model.Favorite
import dev.pecorio.alphastream.data.model.WatchProgress

@Database(
    entities = [Favorite::class, WatchProgress::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AlphaStreamDatabase : RoomDatabase() {
    
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchProgressDao(): WatchProgressDao
    
    companion object {
        @Volatile
        private var INSTANCE: AlphaStreamDatabase? = null
        
        fun getDatabase(context: Context): AlphaStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlphaStreamDatabase::class.java,
                    "alphastream_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}