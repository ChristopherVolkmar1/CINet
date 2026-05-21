package com.example.cinet.data.remote.canvas

import android.util.Log
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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
 *   - Provide a form-encoded POST helper for the small number of endpoints
 *     CINet writes to (currently just sending conversation replies).
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
        val body = executeForBody(buildGetRequest(url))
        return JSONObject(body)
    }

    /**
     * Performs a GET against a paginated list endpoint and follows
     * `Link: rel="next"` until the server stops sending one.
     *
     * Pagination is capped at [maxPages] to avoid runaway loops if Canvas
     * ever returns a malformed cycle.
     */
    fun getJsonArrayPaginated(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        maxPages: Int = 10
    ): JSONArray {
        val hasPerPage = query.any { it.first == "per_page" }
        val initialQuery = if (hasPerPage) query else query + ("per_page" to "100")
        var nextUrl: HttpUrl? = buildUrl(path, initialQuery)
        val combined = JSONArray()
        var pagesFetched = 0

        while (nextUrl != null && pagesFetched < maxPages) {
            val pageUrl = nextUrl!!
            val response = execute(buildGetRequest(pageUrl))
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
     * Form-encoded POST. Canvas accepts both JSON and form bodies on most
     * endpoints; form is simpler for our needs (text fields only, no
     * nesting) and avoids any content-type configuration drift.
     *
     * Returns the parsed JSON response as a [JSONObject]. Throws
     * [CanvasApiException] on non-2xx.
     */
    fun postForm(path: String, fields: List<Pair<String, String>>): JSONObject {
        val url = buildUrl(path, emptyList())
        val formBuilder = FormBody.Builder()
        for ((k, v) in fields) formBuilder.add(k, v)
        val body: RequestBody = formBuilder.build()

        val request = baseRequestBuilder(url)
            .post(body)
            .build()

        val responseBody = executeForBody(request)
        // Canvas occasionally returns an array on POSTs (e.g. add_message
        // returns the updated conversation array). Wrap so callers can deal
        // with either shape: try object first, fall back to wrapping array.
        return if (responseBody.trimStart().startsWith("[")) {
            val arr = JSONArray(responseBody)
            val obj = JSONObject()
            obj.put("array", arr)
            obj
        } else {
            JSONObject(responseBody)
        }
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
        val normalized = path.trimStart('/')
        val urlBuilder = "$baseUrl$normalized".toHttpUrl().newBuilder()
        for ((k, v) in query) {
            // Canvas uses Rails-style array params like `context_codes[]=course_1`.
            if (k.endsWith("[]")) {
                urlBuilder.addEncodedQueryParameter(k, v)
            } else {
                urlBuilder.addQueryParameter(k, v)
            }
        }
        return urlBuilder.build()
    }

    /** Shared header setup for any request. */
    private fun baseRequestBuilder(url: HttpUrl): Request.Builder {
        val token = tokenStore.getToken()
            ?: throw CanvasApiException(0, "No Canvas token saved — connect Canvas first.")
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
    }

    private fun buildGetRequest(url: HttpUrl): Request = baseRequestBuilder(url).get().build()

    /** Executes a request and returns the body string, closing the response. */
    private fun executeForBody(request: Request): String {
        execute(request).use { res ->
            return res.body?.string().orEmpty()
        }
    }

    /** Builds the bearer-authenticated request and runs it; throws on non-2xx. */
    private fun execute(request: Request): Response {
        val response = try {
            client.newCall(request).execute()
        } catch (ex: IOException) {
            throw ex
        }

        if (!response.isSuccessful) {
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
        const val DEFAULT_BASE_URL = "https://csuci.instructure.com/api/v1/"
    }
}

/** Thrown when Canvas returns a non-2xx HTTP status or auth state is missing. */
class CanvasApiException(val code: Int, message: String) : RuntimeException(message)
