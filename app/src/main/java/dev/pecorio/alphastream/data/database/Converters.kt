package dev.pecorio.alphastream.data.database

import androidx.room.TypeConverter
import dev.pecorio.alphastream.data.model.FavoriteType

class Converters {
    
    @TypeConverter
    fun fromFavoriteType(type: FavoriteType): String {
        return type.name
    }
    
    @TypeConverter
    fun toFavoriteType(type: String): FavoriteType {
        return FavoriteType.valueOf(type)
    }
}