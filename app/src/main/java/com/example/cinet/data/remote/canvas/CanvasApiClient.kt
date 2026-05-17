package com.example.cinet.data.remote.canvas

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Low-level HTTP wrapper for the Instructure Canvas REST API.
 *
 * Responsibilities:
 *   - Attach the `Authorization: Bearer <token>` header on every call.
 *   - Follow Canvas's Link-header pagination (`Link: <...>; rel="next"`) so
 *     callers always get a complete list back, not just the first page.
 *   - Surface clear, typed errors for the common failure modes (no token,
 *     bad token, rate limit, network).
 *
 * Sibling pattern: this is intentionally shaped like ClubsRepository — same
 * OkHttp client, same suspend + Dispatchers.IO usage — but extracted to its
 * own class because Canvas needs auth, pagination, and multiple endpoints.
 */
class CanvasApiClient(
    private val tokenStore: CanvasTokenStore,
    /** Override for testing or future multi-tenant support. */
    val baseUrl: String = DEFAULT_BASE_URL
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Performs a GET that returns a JSON object (e.g. /users/self).
     * Throws [CanvasApiException] on failure.
     */
    fun getJsonObject(path: String, query: List<Pair<String, String>> = emptyList()): JSONObject {
        val url = buildUrl(path, query)
        val body = executeForBody(url)
        return JSONObject(body)
    }

    /**
     * Performs a GET against a paginated list endpoint (e.g. /courses) and
     * follows `Link: rel="next"` until the server stops sending one.
     *
     * Pagination is capped at [maxPages] to avoid runaway loops if Canvas
     * ever returns a malformed cycle. With per_page=100 and a typical
     * student load, one page usually suffices.
     *
     * [query] is a list (not a map) because Canvas's array-style params
     * require repeated keys like `context_codes[]=course_1&context_codes[]=course_2`
     * that a Map can't represent.
     */
    fun getJsonArrayPaginated(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        maxPages: Int = 10
    ): JSONArray {
        // Always request the biggest page size Canvas allows to minimize round trips.
        // Only add per_page if the caller didn't supply one.
        val hasPerPage = query.any { it.first == "per_page" }
        val initialQuery = if (hasPerPage) query else query + ("per_page" to "100")
        var nextUrl: HttpUrl? = buildUrl(path, initialQuery)
        val combined = JSONArray()
        var pagesFetched = 0

        while (nextUrl != null && pagesFetched < maxPages) {
            // Capture to a local val so Kotlin smart-casts to non-null inside the loop.
            val pageUrl = nextUrl!!
            val response = execute(pageUrl)
            response.use { res ->
                val pageJson = JSONArray(res.body?.string() ?: "[]")
                for (i in 0 until pageJson.length()) combined.put(pageJson.get(i))
                nextUrl = parseNextLink(res.header("Link"))
            }
            pagesFetched++
        }

        if (pagesFetched == maxPages && nextUrl != null) {
            Log.w(TAG, "Hit pagination cap ($maxPages pages) for $path — some items may be missing.")
        }

        return combined
    }

    /**
     * Probes the saved token by calling /users/self.
     * Returns a typed result so the UI can show a friendly message.
     */
    fun probeAuth(): CanvasAuthResult {
        return try {
            val user = getJsonObject("users/self")
            CanvasAuthResult.Success(user.optString("name", "Canvas user"))
        } catch (ex: CanvasApiException) {
            when (ex.code) {
                401 -> CanvasAuthResult.Failure("Token rejected by Canvas. Generate a new one and try again.")
                403 -> CanvasAuthResult.Failure("This token doesn't have permission to read your profile.")
                else -> CanvasAuthResult.Failure(ex.message ?: "Canvas request failed")
            }
        } catch (ex: IOException) {
            CanvasAuthResult.Failure("Network error: ${ex.message ?: "unable to reach Canvas"}")
        }
    }

    // ---- internals --------------------------------------------------------

    private fun buildUrl(path: String, query: List<Pair<String, String>>): HttpUrl {
        // Strip a leading slash so callers can write "courses" or "/courses" interchangeably.
        val normalized = path.trimStart('/')
        val urlBuilder = "$baseUrl$normalized".toHttpUrl().newBuilder()
        for ((k, v) in query) {
            // Canvas uses Rails-style array params like `context_codes[]=course_1`.
            // We must add these with the literal `[]` in the key. OkHttp's
            // `addQueryParameter` would percent-encode the brackets, which Rails
            // then parses as a Hash key rather than an Array entry. Use the
            // encoded variant for any key that already contains the array marker;
            // for plain keys, the regular method handles encoding the *value*.
            if (k.endsWith("[]")) {
                urlBuilder.addEncodedQueryParameter(k, v)
            } else {
                urlBuilder.addQueryParameter(k, v)
            }
        }
        return urlBuilder.build()
    }

    /** Executes a request and returns the body string, closing the response. */
    private fun executeForBody(url: HttpUrl): String {
        execute(url).use { res ->
            return res.body?.string().orEmpty()
        }
    }

    /** Builds the bearer-authenticated request and runs it; throws on non-2xx. */
    private fun execute(url: HttpUrl): Response {
        val token = tokenStore.getToken()
            ?: throw CanvasApiException(0, "No Canvas token saved — connect Canvas first.")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (ex: IOException) {
            // Re-thrown so the caller can distinguish network vs. HTTP errors.
            throw ex
        }

        if (!response.isSuccessful) {
            // We need to consume the body here or OkHttp leaks the connection.
            val errorBody = response.body?.string().orEmpty()
            response.close()
            throw CanvasApiException(
                response.code,
                "Canvas ${response.code}: ${errorBody.take(200)}"
            )
        }
        return response
    }

    /**
     * Parses the next-page URL from a Canvas Link header.
     * Header format example:
     *   <https://csuci.instructure.com/api/v1/courses?page=2&per_page=100>; rel="next",
     *   <https://csuci.instructure.com/api/v1/courses?page=1&per_page=100>; rel="first",
     *   <https://csuci.instructure.com/api/v1/courses?page=3&per_page=100>; rel="last"
     */
    private fun parseNextLink(header: String?): HttpUrl? {
        if (header.isNullOrBlank()) return null
        return header.split(",")
            .map { it.trim() }
            .firstOrNull { it.contains("rel=\"next\"") }
            ?.let { entry ->
                val start = entry.indexOf('<')
                val end = entry.indexOf('>')
                if (start >= 0 && end > start) {
                    runCatching { entry.substring(start + 1, end).toHttpUrl() }.getOrNull()
                } else null
            }
    }

    companion object {
        private const val TAG = "CanvasApiClient"

        /**
         * CSUCI's Canvas tenant. If CINet ever needs to support other campuses,
         * make this configurable via settings and pass into the constructor.
         */
        const val DEFAULT_BASE_URL = "https://csuci.instructure.com/api/v1/"
    }
}

/** Thrown when Canvas returns a non-2xx HTTP status or auth state is missing. */
class CanvasApiException(val code: Int, message: String) : RuntimeException(message)
