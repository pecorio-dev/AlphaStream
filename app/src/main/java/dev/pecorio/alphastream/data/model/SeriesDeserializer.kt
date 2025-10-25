package dev.pecorio.alphastream.data.model

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Désérialiseur personnalisé pour gérer les champs flexibles dans les réponses API
 */
class FlexibleStringDeserializer : JsonDeserializer<String?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): String? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> json.asString
            json.isJsonArray -> {
                // Si c'est un tableau, on joint les éléments avec des virgules
                val array = json.asJsonArray
                array.mapNotNull { element ->
                    when {
                        element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString
                        element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asString
                        else -> null
                    }
                }.joinToString(", ").takeIf { it.isNotBlank() }
            }
            json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> json.asString
            json.isJsonPrimitive && json.asJsonPrimitive.isBoolean -> json.asString
            else -> null
        }
    }
}

/**
 * Désérialiseur personnalisé pour les listes de chaînes flexibles
 */
class FlexibleStringListDeserializer : JsonDeserializer<List<String>?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): List<String>? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonArray -> {
                val array = json.asJsonArray
                array.mapNotNull { element ->
                    when {
                        element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                            element.asString.takeIf { it.isNotBlank() }
                        }
                        element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asString
                        else -> null
                    }
                }.takeIf { it.isNotEmpty() }
            }
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                // Si c'est une chaîne, on la divise par des virgules
                json.asString.split(",").map { it.trim() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
            }
            else -> null
        }
    }
}

/**
 * Désérialiseur personnalisé pour les entiers flexibles
 */
class FlexibleIntDeserializer : JsonDeserializer<Int?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Int? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                try {
                    json.asInt.takeIf { it >= 0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                try {
                    json.asString.toIntOrNull()?.takeIf { it >= 0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            else -> null
        }
    }
}

/**
 * Désérialiseur personnalisé pour les doubles flexibles
 */
class FlexibleDoubleDeserializer : JsonDeserializer<Double?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Double? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                try {
                    val value = json.asDouble
                    value.takeIf { it >= 0.0 && it <= 10.0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                try {
                    val value = json.asString.toDoubleOrNull()
                    value?.takeIf { it >= 0.0 && it <= 10.0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            else -> null
        }
    }
}

/**
 * Désérialiseur personnalisé pour les saisons qui peuvent être dans différents formats
 */
class FlexibleSeasonListDeserializer : JsonDeserializer<List<Season>?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): List<Season>? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonArray -> {
                val array = json.asJsonArray
                array.mapNotNull { element ->
                    try {
                        context?.deserialize<Season>(element, Season::class.java)
                    } catch (e: Exception) {
                        // Si la désérialisation échoue, on ignore cet élément
                        null
                    }
                }.takeIf { it.isNotEmpty() }
            }
            json.isJsonObject -> {
                // Si c'est un objet unique, on le traite comme une liste d'un élément
                try {
                    val season = context?.deserialize<Season>(json, Season::class.java)
                    season?.let { listOf(it) }
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
}

/**
 * Désérialiseur personnalisé pour les épisodes qui peuvent être dans différents formats
 */
class FlexibleEpisodeListDeserializer : JsonDeserializer<List<Episode>?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): List<Episode>? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonArray -> {
                val array = json.asJsonArray
                array.mapNotNull { element ->
                    try {
                        context?.deserialize<Episode>(element, Episode::class.java)
                    } catch (e: Exception) {
                        // Si la désérialisation échoue, on ignore cet élément
                        null
                    }
                }.takeIf { it.isNotEmpty() }
            }
            json.isJsonObject -> {
                // Si c'est un objet unique, on le traite comme une liste d'un élément
                try {
                    val episode = context?.deserialize<Episode>(json, Episode::class.java)
                    episode?.let { listOf(it) }
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
}