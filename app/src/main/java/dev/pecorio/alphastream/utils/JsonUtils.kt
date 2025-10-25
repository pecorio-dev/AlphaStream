package dev.pecorio.alphastream.utils

import android.util.Log
import com.google.gson.*
import dev.pecorio.alphastream.data.model.*

/**
 * Utilitaires pour la gestion JSON et la désérialisation sécurisée
 */
object JsonUtils {
    
    const val TAG = "JsonUtils"
    
    /**
     * Crée un Gson configuré avec tous les désérialiseurs personnalisés
     */
    fun createSafeGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(String::class.java, FlexibleStringDeserializer())
            .registerTypeAdapter(Int::class.java, FlexibleIntDeserializer())
            .registerTypeAdapter(Double::class.java, FlexibleDoubleDeserializer())
            .setLenient() // Permet une analyse JSON plus flexible
            .create()
    }
    
    /**
     * Parse un JSON de manière sécurisée avec gestion d'erreur
     */
    fun <T> parseJsonSafely(json: String, clazz: Class<T>): T? {
        return try {
            val gson = createSafeGson()
            gson.fromJson(json, clazz)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Erreur de syntaxe JSON: ${e.message}")
            null
        } catch (e: JsonParseException) {
            Log.e(TAG, "Erreur de parsing JSON: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors du parsing JSON: ${e.message}")
            null
        }
    }
    
    /**
     * Parse un JSON de manière sécurisée avec gestion d'erreur (version inline)
     */
    inline fun <reified T> parseJsonSafelyInline(json: String): T? {
        return parseJsonSafely(json, T::class.java)
    }
    
    /**
     * Valide et nettoie un objet JSON avant désérialisation
     */
    fun cleanJsonString(json: String): String {
        return json
            .replace("\\n", " ") // Remplace les retours à la ligne
            .replace("\\t", " ") // Remplace les tabulations
            .replace("\\r", " ") // Remplace les retours chariot
            .replace(Regex("\\s+"), " ") // Remplace les espaces multiples par un seul
            .trim()
    }
    
    /**
     * Vérifie si une chaîne JSON est valide
     */
    fun isValidJson(json: String): Boolean {
        return try {
            JsonParser().parse(json)
            true
        } catch (e: JsonSyntaxException) {
            false
        } catch (e: JsonParseException) {
            false
        }
    }
    
    /**
     * Extrait une valeur sécurisée d'un JsonElement
     */
    fun extractSafeString(element: JsonElement?): String? {
        return when {
            element == null || element.isJsonNull -> null
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                element.asString.takeIf { it.isNotBlank() }
            }
            element.isJsonArray -> {
                // Si c'est un tableau, on joint les éléments
                val array = element.asJsonArray
                array.mapNotNull { item ->
                    if (item.isJsonPrimitive && item.asJsonPrimitive.isString) {
                        item.asString.takeIf { it.isNotBlank() }
                    } else null
                }.joinToString(", ").takeIf { it.isNotBlank() }
            }
            else -> null
        }
    }
    
    /**
     * Extrait une liste de chaînes sécurisée d'un JsonElement
     */
    fun extractSafeStringList(element: JsonElement?): List<String>? {
        return when {
            element == null || element.isJsonNull -> null
            element.isJsonArray -> {
                val array = element.asJsonArray
                array.mapNotNull { item ->
                    if (item.isJsonPrimitive && item.asJsonPrimitive.isString) {
                        item.asString.takeIf { it.isNotBlank() }
                    } else null
                }.takeIf { it.isNotEmpty() }
            }
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                // Si c'est une chaîne, on la divise par des virgules
                element.asString.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .takeIf { it.isNotEmpty() }
            }
            else -> null
        }
    }
    
    /**
     * Extrait un entier sécurisé d'un JsonElement
     */
    fun extractSafeInt(element: JsonElement?): Int? {
        return when {
            element == null || element.isJsonNull -> null
            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> {
                try {
                    element.asInt.takeIf { it >= 0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                try {
                    element.asString.toIntOrNull()?.takeIf { it >= 0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * Extrait un double sécurisé d'un JsonElement
     */
    fun extractSafeDouble(element: JsonElement?): Double? {
        return when {
            element == null || element.isJsonNull -> null
            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> {
                try {
                    val value = element.asDouble
                    value.takeIf { it >= 0.0 && it <= 10.0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                try {
                    val value = element.asString.toDoubleOrNull()
                    value?.takeIf { it >= 0.0 && it <= 10.0 }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            else -> null
        }
    }
    
    /**
     * Log les erreurs de désérialisation pour le debugging
     */
    fun logDeserializationError(fieldName: String, expectedType: String, actualValue: Any?, exception: Exception? = null) {
        val message = "Erreur de désérialisation pour le champ '$fieldName': " +
                "attendu $expectedType, reçu ${actualValue?.javaClass?.simpleName ?: "null"} " +
                "avec la valeur: $actualValue"
        
        Log.w(TAG, message)
        exception?.let { Log.w(TAG, "Exception: ${it.message}") }
    }
    
    /**
     * Crée un objet Series sécurisé à partir d'un JsonObject
     */
    fun createSafeSeriesFromJson(jsonObject: JsonObject): Series? {
        return try {
            val gson = createSafeGson()
            gson.fromJson(jsonObject, Series::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la création d'une série à partir du JSON: ${e.message}")
            // Créer un objet Series minimal avec les données disponibles
            createFallbackSeries(jsonObject)
        }
    }
    
    /**
     * Crée un objet Series de fallback avec les données minimales disponibles
     */
    private fun createFallbackSeries(jsonObject: JsonObject): Series? {
        return try {
            val title = extractSafeString(jsonObject.get("title")) ?: return null
            val id = extractSafeString(jsonObject.get("id")) ?: title.hashCode().toString()
            
            Series(
                id = id,
                title = title,
                imageUrl = extractSafeString(jsonObject.get("image_url")),
                remoteImageUrl = extractSafeString(jsonObject.get("remote_image_url")),
                synopsis = extractSafeString(jsonObject.get("synopsis")),
                genres = extractSafeStringList(jsonObject.get("genres")),
                rating = extractSafeDouble(jsonObject.get("rating")),
                releaseDate = extractSafeString(jsonObject.get("release_date")),
                status = extractSafeString(jsonObject.get("status")),
                details = extractSafeString(jsonObject.get("details"))
            )
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de créer un objet Series de fallback: ${e.message}")
            null
        }
    }
}