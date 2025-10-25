package dev.pecorio.alphastream.data.extractor

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class ExtractedStreamInfo(
    val url: String,
    val title: String?,
    val quality: String?,
    val format: String?,
    val duration: String?,
    val thumbnail: String?,
    val headers: Map<String, String>
)

class VideoNotFoundException(message: String) : Exception(message)

@Singleton
class FastDirectExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FastDirectExtractor"
        private const val TIMEOUT_MS = 15000
    }

    /**
     * Extracts video info and metadata from Uqload embed URL, using parallel fetching.
     * Forces the use of uqload_old_url if available, otherwise tries uqload_new_url
     */
    suspend fun extractStreamInfo(embedUrl: String, maxRetries: Int = 3): Result<ExtractedStreamInfo> = withContext(Dispatchers.IO) {
        var attempt = 0
        var cookies = ""
        
        // Force old URL if it's a new URL format
        val finalEmbedUrl = if (embedUrl.contains("uqload.to") || embedUrl.contains("uqload.com")) {
            embedUrl.replace("uqload.to", "uqload.cx").replace("uqload.com", "uqload.cx")
        } else {
            embedUrl
        }
        
        while (attempt < maxRetries) {
            try {
                Log.d(TAG, "🚀 EXTRACTION STREAM INFO pour: $finalEmbedUrl (Attempt ${attempt + 1}/$maxRetries)")
                val startTime = System.currentTimeMillis()

                // Parallel fetch: embed and non-embed URLs
                val nonEmbedUrl = finalEmbedUrl.replace("embed-", "").replace("/embed/", "/")
                val (embedContent, nonEmbedContent, fetchedCookies) = fetchParallel(finalEmbedUrl, nonEmbedUrl)
                cookies = fetchedCookies

                if (embedContent == null && nonEmbedContent == null) {
                    throw Exception("No content fetched from either URL")
                }

                if (embedContent?.contains("File was deleted") == true || 
                    nonEmbedContent?.contains("File was deleted") == true ||
                    embedContent?.contains("File not found") == true || 
                    nonEmbedContent?.contains("File not found") == true) {
                    throw VideoNotFoundException("The video has been deleted or does not exist")
                }

                // Extract direct URL from embed content (primary source)
                val videoUrlWithFormat = parseVideoUrl(embedContent ?: nonEmbedContent ?: "")
                if (videoUrlWithFormat == null) {
                    Log.w(TAG, "❌ Aucune URL vidéo trouvée")
                    debugSearchPatterns(embedContent ?: nonEmbedContent ?: "")
                    
                    // Try Uqload specific extraction (based on Python implementation)
                    val uqloadUrl = extractUqloadSpecific(embedContent ?: nonEmbedContent ?: "")
                    if (uqloadUrl != null) {
                        Log.d(TAG, "🎉 Uqload specific URL found: $uqloadUrl")
                        val streamInfo = createStreamInfo(uqloadUrl, "mp4", embedContent ?: nonEmbedContent ?: "", cookies, finalEmbedUrl)
                        return@withContext Result.success(streamInfo)
                    }
                    
                    // Try alternative extraction methods
                    val alternativeUrl = extractAlternativeVideoUrl(embedContent ?: nonEmbedContent ?: "")
                    if (alternativeUrl != null) {
                        Log.d(TAG, "🎉 Alternative URL found: $alternativeUrl")
                        val (videoUrl, format) = alternativeUrl
                        val streamInfo = createStreamInfo(videoUrl, format ?: "mp4", embedContent ?: nonEmbedContent ?: "", cookies, finalEmbedUrl)
                        return@withContext Result.success(streamInfo)
                    }
                    
                    // Try brute force URL extraction
                    val bruteForceUrl = extractUrlsBruteForce(embedContent ?: nonEmbedContent ?: "")
                    if (bruteForceUrl != null) {
                        Log.d(TAG, "🔨 Brute force URL found: $bruteForceUrl")
                        val (videoUrl, format) = bruteForceUrl
                        val streamInfo = createStreamInfo(videoUrl, format, embedContent ?: nonEmbedContent ?: "", cookies, finalEmbedUrl)
                        return@withContext Result.success(streamInfo)
                    }
                    
                    throw VideoNotFoundException("No video stream found")
                }

                val (videoUrl, format) = videoUrlWithFormat
                
                // Ensure we have a valid URL
                if (videoUrl.isNullOrEmpty()) {
                    throw VideoNotFoundException("Invalid video URL extracted")
                }

                // Validate direct URL with headers
                val referer = getRefererFromUrl(finalEmbedUrl)
                val headers = buildHeaders(cookies, referer, format)
                val accessResult = accessDirectUrl(videoUrl, headers, bypassSSL = attempt > 0)

                if (accessResult.isSuccess) {
                    val streamInfo = createStreamInfo(videoUrl, format ?: "mp4", nonEmbedContent ?: embedContent ?: "", cookies, finalEmbedUrl)
                    val totalTime = System.currentTimeMillis() - startTime
                    Log.d(TAG, "✅ SUCCÈS en ${totalTime}ms - Stream info: $streamInfo")
                    return@withContext Result.success(streamInfo)
                } else {
                    val errorMsg = accessResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "❌ Direct URL inaccessible: $errorMsg")
                    if (errorMsg.contains("error_wrong_ip") || errorMsg.contains("403")) {
                        Log.d(TAG, "🔄 IP/Access error detected, retrying with different approach...")
                        attempt++
                        continue
                    }
                    return@withContext Result.failure(accessResult.exceptionOrNull() ?: Exception("Direct URL inaccessible"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 ERREUR: ${e.message}", e)
                if (e is VideoNotFoundException) {
                    return@withContext Result.failure(e)
                }
                attempt++
                if (attempt >= maxRetries) {
                    return@withContext Result.failure(Exception("Failed after $maxRetries attempts: ${e.message}"))
                }
            }
        }
        return@withContext Result.failure(Exception("Failed after $maxRetries attempts"))
    }

    /**
     * Fetches embed and non-embed URLs in parallel, capturing cookies.
     */
    private suspend fun fetchParallel(embedUrl: String, nonEmbedUrl: String): Triple<String?, String?, String> = withContext(Dispatchers.IO) {
        val jobs = listOf(
            async { fetchWithHeaders(embedUrl) },
            async { fetchWithHeaders(nonEmbedUrl) }
        )
        val (embedResult, nonEmbedResult) = jobs.awaitAll()
        val cookies = embedResult.second.takeIf { it.isNotEmpty() } ?: nonEmbedResult.second
        Triple(embedResult.first, nonEmbedResult.first, cookies)
    }

    /**
     * Fetches content with headers and captures cookies.
     */
    private suspend fun fetchWithHeaders(url: String): Pair<String?, String> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpsURLConnection
            
            // Bypass SSL for problematic connections
            bypassSSL(connection)
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true

            val referer = getRefererFromUrl(url)
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0",
                "Referer" to referer,
                "Origin" to referer,
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.5",
                "Accept-Encoding" to "identity",
                "Connection" to "keep-alive",
                "Upgrade-Insecure-Requests" to "1",
                "Sec-Fetch-Dest" to "document",
                "Sec-Fetch-Mode" to "navigate",
                "Sec-Fetch-Site" to "same-origin",
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache"
            )
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Handle different content encodings
                val inputStream = when (connection.contentEncoding) {
                    "gzip" -> java.util.zip.GZIPInputStream(connection.inputStream)
                    "deflate" -> java.util.zip.InflaterInputStream(connection.inputStream)
                    else -> connection.inputStream
                }
                
                val content = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { it.readText() }
                val cookies = connection.headerFields["Set-Cookie"]?.joinToString("; ") { it.split(";")[0] } ?: ""
                
                Log.d(TAG, "📝 Content encoding: ${connection.contentEncoding}")
                Log.d(TAG, "📝 Content type: ${connection.contentType}")
                Log.d(TAG, "📝 Content length: ${content.length}")
                
                return@withContext Pair(content, cookies)
            } else {
                Log.w(TAG, "⚠️ Fetch failed for $url: HTTP $responseCode")
                return@withContext Pair(null, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch error for $url: ${e.message}")
            return@withContext Pair(null, "")
        }
    }

    /**
     * Enhanced video URL parsing with better format detection
     */
    private fun parseVideoUrl(content: String?): Pair<String?, String?> {
        if (content == null) return null to null
        
        Log.d(TAG, "🔍 Parsing content length: ${content.length}")
        Log.d(TAG, "🔍 Content preview: ${content.take(500)}...")
        
        // Enhanced patterns for Uqload specifically
        val patterns = listOf(
            // Uqload specific patterns (most common)
            Pattern.compile("sources\\s*:\\s*\\[\\s*\\{[^}]*file\\s*:\\s*[\"']([^\"']+)[\"']") to "auto",
            Pattern.compile("file\\s*:\\s*[\"']([^\"']+\\.(?:mp4|m3u8|mpd))[\"']") to "auto",
            Pattern.compile("src\\s*:\\s*[\"']([^\"']+\\.(?:mp4|m3u8|mpd))[\"']") to "auto",
            Pattern.compile("url\\s*:\\s*[\"']([^\"']+\\.(?:mp4|m3u8|mpd))[\"']") to "auto",
            
            // JavaScript variable patterns for Uqload
            Pattern.compile("var\\s+\\w+\\s*=\\s*[\"']([^\"']*(?:mp4|m3u8|mpd)[^\"']*)[\"']") to "auto",
            Pattern.compile("\\w+\\s*=\\s*[\"']([^\"']*(?:mp4|m3u8|mpd)[^\"']*)[\"']") to "auto",
            
            // Direct video URLs
            Pattern.compile("https?://[^\"'\\s]+\\.m3u8(?:\\?[^\"'\\s]*)?") to "hls",
            Pattern.compile("https?://[^\"'\\s]+\\.mpd(?:\\?[^\"'\\s]*)?") to "dash",
            Pattern.compile("https?://[^\"'\\s]+\\.mp4(?:\\?[^\"'\\s]*)?") to "mp4",
            
            // Uqload legacy patterns
            Pattern.compile("https?://[^/]+/[^/]+/v\\.mp4") to "mp4",
            Pattern.compile("\"(https?://[^\"]+\\.(?:mp4|m3u8|mpd))\"") to "auto",
            
            // Generic patterns
            Pattern.compile("src\\s*=\\s*[\"'](https?://[^\"']+\\.(?:mp4|m3u8|mpd))[\"']") to "auto"
        )

        patterns.forEach { (pattern, format) ->
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val url = matcher.group().replace("\"", "").replace("'", "")
                val detectedFormat = if (format == "auto") {
                    when {
                        url.contains(".m3u8") -> "hls"
                        url.contains(".mpd") -> "dash"
                        url.contains(".mp4") -> "mp4"
                        else -> "mp4"
                    }
                } else format
                
                Log.d(TAG, "🎉 Video URL found: $url (Format: $detectedFormat)")
                return url to detectedFormat
            }
        }

        Log.w(TAG, "⚠️ No video URL found with standard patterns")
        return null to null
    }

    /**
     * Uqload specific extraction based on Python implementation
     */
    private fun extractUqloadSpecific(content: String): String? {
        Log.d(TAG, "🎆 Starting Uqload specific extraction")
        
        // Check if file was deleted
        if (content.contains("File was deleted", ignoreCase = true)) {
            Log.w(TAG, "File was deleted")
            return null
        }
        
        // Look for the specific pattern: https?://.+/v\.mp4
        val uqloadPattern = Regex("https?://[^\\s\"'<>]+/v\\.mp4")
        val matches = uqloadPattern.findAll(content)
        
        matches.forEach { match ->
            val videoUrl = match.value
            Log.d(TAG, "🎆 Found Uqload pattern: $videoUrl")
            return videoUrl
        }
        
        Log.d(TAG, "🎆 No Uqload specific pattern found")
        return null
    }

    /**
     * Alternative extraction method for edge cases
     */
    private fun extractAlternativeVideoUrl(content: String): Pair<String, String>? {
        // Look for JavaScript variables containing video URLs
        val jsPatterns = listOf(
            Pattern.compile("var\\s+\\w+\\s*=\\s*[\"'](https?://[^\"']+\\.(?:mp4|m3u8|mpd))[\"']"),
            Pattern.compile("\\w+\\s*=\\s*[\"'](https?://[^\"']+\\.(?:mp4|m3u8|mpd))[\"']"),
            Pattern.compile("url\\s*:\\s*[\"'](https?://[^\"']+)[\"']"),
            Pattern.compile("source\\s*:\\s*[\"'](https?://[^\"']+)[\"']")
        )

        jsPatterns.forEach { pattern ->
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val url = matcher.group(1)
                val format = when {
                    url.contains(".m3u8") -> "hls"
                    url.contains(".mpd") -> "dash"
                    else -> "mp4"
                }
                Log.d(TAG, "🔍 Alternative URL found: $url (Format: $format)")
                return url to format
            }
        }
        return null
    }

    /**
     * Brute force URL extraction - looks for any video-like URLs
     */
    private fun extractUrlsBruteForce(content: String): Pair<String, String>? {
        Log.d(TAG, "🔨 Starting brute force URL extraction")
        
        // Look for any URLs that might be video files
        val urlPatterns = listOf(
            // Any URL ending with video extensions
            Regex("(https?://[^\\s\"'<>]+\\.(?:mp4|m3u8|mpd|avi|mkv|webm)(?:\\?[^\\s\"'<>]*)?)"),
            // URLs containing video-like paths
            Regex("(https?://[^\\s\"'<>]*(?:video|stream|media|file)[^\\s\"'<>]*)"),
            // Uqload specific URL patterns
            Regex("(https?://[^\\s\"'<>]*uqload[^\\s\"'<>]*\\.(?:mp4|m3u8|mpd))"),
            // Any URL with common video hosting patterns
            Regex("(https?://[^\\s\"'<>]*(?:cdn|storage|media)[^\\s\"'<>]*\\.(?:mp4|m3u8|mpd))")
        )
        
        urlPatterns.forEach { pattern ->
            pattern.findAll(content).forEach { match ->
                val url = match.groupValues[1]
                val format = when {
                    url.contains(".m3u8") -> "hls"
                    url.contains(".mpd") -> "dash"
                    else -> "mp4"
                }
                Log.d(TAG, "🔨 Potential video URL: $url")
                
                // Basic validation - URL should be reasonable length and contain expected patterns
                if (url.length > 20 && url.length < 500 && 
                    (url.contains("uqload") || url.contains("cdn") || url.contains("stream"))) {
                    return url to format
                }
            }
        }
        
        return null
    }

    /**
     * Build appropriate headers based on format and context
     */
    private fun buildHeaders(cookies: String, referer: String, format: String?): Map<String, String> {
        return mapOf(
            "Cookie" to cookies,
            "Referer" to referer,
            "Origin" to referer,
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0",
            "Accept" to when (format) {
                "hls" -> "application/vnd.apple.mpegurl,application/x-mpegURL,application/octet-stream,*/*"
                "dash" -> "application/dash+xml,application/octet-stream,*/*"
                else -> "video/mp4,video/*,*/*"
            },
            "Accept-Language" to "en-US,en;q=0.5",
            "Accept-Encoding" to "identity",
            "Range" to "bytes=0-",
            "Sec-Fetch-Dest" to if (format == "hls" || format == "dash") "video" else "document",
            "Sec-Fetch-Mode" to if (format == "hls" || format == "dash") "cors" else "navigate",
            "Sec-Fetch-Site" to "same-site",
            "Connection" to "keep-alive"
        )
    }

    /**
     * Create ExtractedStreamInfo with all metadata
     */
    private fun createStreamInfo(videoUrl: String, format: String, content: String, cookies: String, originalUrl: String): ExtractedStreamInfo {
        val title = parseTitle(content)
        val thumbnail = parseThumbnail(content)
        val (quality, duration) = parseQualityAndDuration(content)
        val referer = getRefererFromUrl(originalUrl)
        val headers = buildHeaders(cookies, referer, format)

        return ExtractedStreamInfo(
            url = videoUrl,
            title = title,
            quality = quality,
            format = format,
            duration = duration,
            thumbnail = thumbnail,
            headers = headers
        )
    }

    /**
     * Validates direct URL with headers and SSL bypass
     */
    private suspend fun accessDirectUrl(videoUrl: String, headers: Map<String, String>, bypassSSL: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(videoUrl).openConnection() as HttpsURLConnection

            if (bypassSSL) {
                bypassSSL(connection)
            }

            connection.requestMethod = "HEAD"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                Log.d(TAG, "✅ Direct URL accessible: $videoUrl")
                return@withContext Result.success(videoUrl)
            } else {
                val errorContent = connection.errorStream?.let { InputStreamReader(it).use { reader -> reader.readText() } } ?: ""
                Log.e(TAG, "❌ Access failed: HTTP $responseCode - $errorContent")
                if (responseCode == 403 && (errorContent.contains("error_wrong_ip") || errorContent.contains("wrong ip"))) {
                    return@withContext Result.failure(Exception("error_wrong_ip"))
                }
                return@withContext Result.failure(Exception("HTTP $responseCode: $errorContent"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error accessing URL: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Bypass SSL verification for problematic connections
     */
    private fun bypassSSL(connection: HttpsURLConnection) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            connection.sslSocketFactory = sslContext.socketFactory
            connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            Log.d(TAG, "🔓 SSL verification bypassed")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bypass SSL: ${e.message}")
        }
    }

    /**
     * Parse title from content
     */
    private fun parseTitle(content: String?): String? {
        if (content == null) return null
        val patterns = listOf(
            Regex("<title[^>]*>(.*?)</title>", RegexOption.DOT_MATCHES_ALL),
            Regex("<h1[^>]*>(.*?)</h1>", RegexOption.DOT_MATCHES_ALL),
            Regex("title\\s*:\\s*[\"'](.*?)[\"']"),
            Regex("name\\s*:\\s*[\"'](.*?)[\"']")
        )
        
        patterns.forEach { pattern ->
            pattern.find(content)?.groupValues?.getOrNull(1)?.let { rawTitle ->
                val cleanTitle = rawTitle.trim()
                    .replace(Regex("<[^>]*>"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (cleanTitle.isNotEmpty() && !cleanTitle.contains("uqload", true)) {
                    return cleanTitle
                }
            }
        }
        return null
    }

    /**
     * Parse thumbnail from content
     */
    private fun parseThumbnail(content: String?): String? {
        if (content == null) return null
        val patterns = listOf(
            Regex("poster\\s*:\\s*[\"'](https?://[^\"']+\\.(?:jpg|jpeg|png|webp))[\"']"),
            Regex("thumbnail\\s*:\\s*[\"'](https?://[^\"']+\\.(?:jpg|jpeg|png|webp))[\"']"),
            Regex("image\\s*:\\s*[\"'](https?://[^\"']+\\.(?:jpg|jpeg|png|webp))[\"']"),
            Regex("(https?://[^\"'\\s]+\\.(?:jpg|jpeg|png|webp))")
        )
        
        patterns.forEach { pattern ->
            pattern.find(content)?.groupValues?.getOrNull(1)?.let { thumbnail ->
                if (thumbnail.startsWith("http")) {
                    return thumbnail
                }
            }
        }
        return null
    }

    /**
     * Parse quality and duration from content
     */
    private fun parseQualityAndDuration(content: String?): Pair<String?, String?> {
        if (content == null) return null to null
        
        // Quality patterns
        val qualityPatterns = listOf(
            Regex("\\[(\\d+x\\d+)"),
            Regex("(\\d+p)"),
            Regex("quality\\s*:\\s*[\"']([^\"']+)[\"']")
        )
        
        var quality: String? = null
        run qualityLoop@ {
            qualityPatterns.forEach { pattern ->
                pattern.find(content)?.groupValues?.getOrNull(1)?.let {
                    quality = it
                    return@qualityLoop
                }
            }
        }
        
        // Duration patterns
        val durationPatterns = listOf(
            Regex("duration\\s*:\\s*[\"']([^\"']+)[\"']"),
            Regex("\\[\\d+x\\d+,\\s*((\\d+:)*\\d+)\\]"),
            Regex("(\\d+:\\d+:\\d+)"),
            Regex("(\\d+:\\d+)")
        )
        
        var duration: String? = null
        run durationLoop@ {
            durationPatterns.forEach { pattern ->
                pattern.find(content)?.groupValues?.getOrNull(1)?.let {
                    duration = it
                    return@durationLoop
                }
            }
        }
        
        return quality to duration
    }

    /**
     * Debug search patterns in content
     */
    private fun debugSearchPatterns(content: String) {
        Log.d(TAG, "🔍 DEBUG - Recherche de mots-clés dans le contenu:")
        Log.d(TAG, "🔍 Content length: ${content.length}")
        
        // Look for script tags that might contain video URLs
        val scriptPattern = Regex("<script[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
        val scripts = scriptPattern.findAll(content)
        scripts.forEachIndexed { index, script ->
            val scriptContent = script.groupValues[1]
            if (scriptContent.contains("mp4", true) || 
                scriptContent.contains("m3u8", true) || 
                scriptContent.contains("sources", true) ||
                scriptContent.contains("file", true)) {
                Log.d(TAG, "📜 Script $index contains video keywords:")
                Log.d(TAG, "📜 ${scriptContent.take(200)}...")
            }
        }
        
        val keywords = listOf("sources", "file", "video", "mp4", "m3u8", "mpd", "url", "src", "stream", "player", "jwplayer", "videojs")
        for (keyword in keywords) {
            val count = content.split(keyword, ignoreCase = true).size - 1
            if (count > 0) {
                Log.d(TAG, "🔍 '$keyword' trouvé $count fois")
                val index = content.indexOf(keyword, ignoreCase = true)
                if (index != -1) {
                    val start = maxOf(0, index - 100)
                    val end = minOf(content.length, index + 200)
                    val context = content.substring(start, end)
                    Log.d(TAG, "📝 Contexte: ...$context...")
                }
            }
        }
        
        // Look for any URLs in the content
        val urlPattern = Regex("https?://[^\\s\"'<>]+")
        val urls = urlPattern.findAll(content).take(10)
        urls.forEach { url ->
            Log.d(TAG, "🔗 URL found: ${url.value}")
        }
    }

    /**
     * Get referer from URL
     */
    private fun getRefererFromUrl(embedUrl: String): String {
        return try {
            val url = URL(embedUrl)
            "${url.protocol}://${url.host}"
        } catch (e: Exception) {
            "https://uqload.cx"
        }
    }
}