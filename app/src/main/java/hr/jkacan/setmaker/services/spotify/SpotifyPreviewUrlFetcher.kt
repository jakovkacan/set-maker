package hr.jkacan.setmaker.services.spotify

import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

// Source - https://stackoverflow.com/a/79238027
// Posted by Diego Perez, modified by community. See post 'Timeline' for change history
// Retrieved 2026-02-05, License - CC BY-SA 4.0

suspend fun fetchPreviewUrl(trackId: String): String? = withContext(Dispatchers.IO) {
    val embedUrl = "https://open.spotify.com/embed/track/$trackId"
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(embedUrl)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            println("Failed to fetch embed page: ${response.code}")
            return@withContext null
        }

        val html = response.body?.string() ?: return@withContext null
        val document = Jsoup.parse(html)
        val scriptElements = document.getElementsByTag("script")

        for (element in scriptElements) {
            val scriptContent = element.html()
            if (scriptContent.isNotEmpty()) {
                /*val jsonObject = JSONObject(scriptContent)
                val audioPreview1 = findNodeValue1(jsonObject, "audioPreview")*/
                return@withContext findNodeValueWithJsonPath(scriptContent, "audioPreview")
            }
        }
    }
    return@withContext null
}

private fun findNodeValueWithJsonPath(jsonString: String, targetNode: String): String? {
    return try {
        // Construct the JsonPath query
        val query = "$..$targetNode.url"
        println("Using JsonPath Query: $query") // Debug query

        // Perform the query
        // Handle cases where the result is a list (e.g., multiple matches)
        when (val result = JsonPath.read<Any>(jsonString, query)) {
            is List<*> -> {
                if (result.isNotEmpty()) result[0].toString() else null // Return the first match
            }

            is String -> result // Directly return the string if it's not a list
            else -> null
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        null
    }
}
