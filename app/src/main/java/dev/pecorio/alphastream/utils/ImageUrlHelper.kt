package dev.pecorio.alphastream.utils

object ImageUrlHelper {
    
    // URL de base du serveur pour les images
    private const val BASE_SERVER_URL = "http://78.197.211.192:25315"
    
    /**
     * Construit l'URL complète de l'image à partir de l'image_url relative
     */
    fun buildImageUrl(imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        
        return when {
            // Si c'est déjà une URL complète, la retourner telle quelle
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            
            // Si c'est un chemin relatif commençant par /images/, construire l'URL complète
            imageUrl.startsWith("/images/") -> "$BASE_SERVER_URL$imageUrl"
            
            // Si c'est juste un nom de fichier, ajouter le préfixe complet
            else -> "$BASE_SERVER_URL/images/$imageUrl"
        }
    }
    
    /**
     * Valide si une URL d'image est utilisable
     */
    fun isValidImageUrl(imageUrl: String?): Boolean {
        if (imageUrl.isNullOrBlank()) return false
        
        return when {
            // URLs complètes valides
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
                !imageUrl.contains("localhost") && !imageUrl.contains("127.0.0.1")
            }
            
            // Chemins relatifs valides
            imageUrl.startsWith("/images/") -> true
            
            // Noms de fichiers simples
            else -> imageUrl.isNotBlank()
        }
    }
    
    /**
     * Obtient l'URL d'image optimale pour un contenu
     * Priorise image_url (serveur local) sur remote_image_url
     */
    fun getOptimalImageUrl(imageUrl: String?, remoteImageUrl: String?): String? {
        // Prioriser image_url du serveur local
        val localUrl = buildImageUrl(imageUrl)
        if (isValidImageUrl(localUrl)) {
            return localUrl
        }
        
        // Fallback sur remote_image_url si disponible et valide
        if (isValidImageUrl(remoteImageUrl)) {
            return remoteImageUrl
        }
        
        return null
    }
}