package com.example.cinet.data.remote.canvas

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads and writes Canvas conversations (Canvas's name for messages).
 *
 * Kept in a separate repository from [CanvasRepository] because messaging
 * is request-driven (live fetch when the user opens the inbox) rather than
 * batch-synced like courses/assignments. Different access pattern, different
 * caching story, different lifecycle.
 *
 * The current user's Canvas id is needed to render messages correctly
 * (so we know which side of the thread is "me"). It's resolved once on
 * construction via /users/self and cached.
 */
class CanvasMessagingRepository(
    private val api: CanvasApiClient
) {

    /**
     * Lazily-resolved current user id. Null until the first call to
     * [currentUserId] succeeds. We don't fail constructor on it because
     * a token failure should surface where the user actually does
     * something, not on app start.
     */
    @Volatile
    private var cachedUserId: Long? = null

    /** Returns the current Canvas user id, fetching it lazily on first need. */
    suspend fun currentUserId(): Long? = withContext(Dispatchers.IO) {
        cachedUserId?.let { return@withContext it }
        try {
            val self = api.getJsonObject("users/self")
            val id = self.optLong("id", -1L).takeIf { it > 0 }
            cachedUserId = id
            id
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to resolve current Canvas user id", ex)
            null
        }
    }

    /**
     * Fetches the inbox — the user's recent conversations. Canvas returns
     * these newest-first, so we don't sort.
     *
     * No `scope` param: Canvas's documented scope values are "unread",
     * "starred", "archived", and "sent", none of which match what we want.
     * The default (no scope) returns the regular inbox, which is correct.
     */
    suspend fun fetchInbox(): List<CanvasConversation> = withContext(Dispatchers.IO) {
        try {
            val array = api.getJsonArrayPaginated(
                path = "conversations",
                query = emptyList(),
                // 3 pages of 100 is 300 conversations — well past any realistic inbox.
                maxPages = 3
            )
            array.mapObjects { json ->
                val id = json.optLong("id", -1L)
                if (id <= 0) return@mapObjects null

                // Canvas returns participants minus the current user already, but
                // gives them as an array of {id, name} objects. Concatenate names
                // so the inbox row reads "Prof. Smith, Jane Doe, …".
                val participantsArray = json.optJSONArray("participants")
                val names = mutableListOf<String>()
                if (participantsArray != null) {
                    for (i in 0 until participantsArray.length()) {
                        val p = participantsArray.optJSONObject(i) ?: continue
                        val n = p.optString("name").takeIf { it.isNotBlank() } ?: continue
                        names.add(n)
                    }
                }

                CanvasConversation(
                    id = id,
                    subject = json.optString("subject").ifBlank { "(no subject)" },
                    lastMessagePreview = json.optString("last_message"),
                    lastMessageAtIso = json.optStringOrNull("last_message_at"),
                    participantNames = names.joinToString(", ").ifBlank { "Canvas" },
                    workflowState = json.optString("workflow_state"),
                    messageCount = json.optInt("message_count", 0)
                )
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to load Canvas inbox: ${ex.message}")
            emptyList()
        }
    }

    /**
     * Fetches the full thread for one conversation — all messages, in
     * Canvas's order (newest-first). The UI reverses for chat-style
     * bottom-newest rendering.
     */
    suspend fun fetchConversationDetail(conversationId: Long): CanvasConversationDetail? =
        withContext(Dispatchers.IO) {
            try {
                val obj = api.getJsonObject("conversations/$conversationId")

                // Participants metadata — used to resolve sender names from ids
                // since each message only carries author_id.
                val participantsArray = obj.optJSONArray("participants")
                val nameById = mutableMapOf<Long, String>()
                val displayNames = mutableListOf<String>()
                if (participantsArray != null) {
                    for (i in 0 until participantsArray.length()) {
                        val p = participantsArray.optJSONObject(i) ?: continue
                        val pid = p.optLong("id", -1L)
                        val pname = p.optString("name")
                        if (pid > 0 && pname.isNotBlank()) {
                            nameById[pid] = pname
                            displayNames.add(pname)
                        }
                    }
                }

                val messagesArray = obj.optJSONArray("messages") ?: JSONArray()
                val messages = messagesArray.mapObjects { msg ->
                    val mid = msg.optLong("id", -1L)
                    if (mid <= 0) return@mapObjects null
                    val authorId = msg.optLong("author_id", -1L)

                    // Collect attachment display names if any. We don't
                    // download anything — just inform the user they exist.
                    val attachments = msg.optJSONArray("attachments")
                    val attachmentNames = mutableListOf<String>()
                    if (attachments != null) {
                        for (i in 0 until attachments.length()) {
                            val a = attachments.optJSONObject(i) ?: continue
                            attachmentNames += a.optString("display_name").ifBlank { "attachment" }
                        }
                    }

                    CanvasMessage(
                        id = mid,
                        authorId = authorId,
                        authorName = nameById[authorId] ?: "Unknown",
                        body = msg.optString("body"),
                        createdAtIso = msg.optStringOrNull("created_at"),
                        attachmentNames = attachmentNames
                    )
                }

                CanvasConversationDetail(
                    id = conversationId,
                    subject = obj.optString("subject").ifBlank { "(no subject)" },
                    participantNames = displayNames.joinToString(", ").ifBlank { "Canvas" },
                    messages = messages
                )
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to load Canvas conversation $conversationId", ex)
                null
            }
        }

    /**
     * Sends a reply on an existing conversation. Returns true on success.
     *
     * Canvas's response varies (sometimes a single conversation object,
     * sometimes an array); we don't try to parse out the appended message
     * since the caller refetches the full thread afterward anyway. Simpler
     * than reconciling Canvas-assigned ids with a locally optimistic stub.
     */
    suspend fun sendReply(conversationId: Long, body: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                api.postForm(
                    path = "conversations/$conversationId/add_message",
                    fields = listOf("body" to body)
                )
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to send reply to conversation $conversationId", ex)
                false
            }
        }

    companion object {
        private const val TAG = "CanvasMessagingRepo"
    }
}

// ---- private JSON helpers (duplicated from CanvasRepository for isolation) ----

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T?): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        transform(obj)?.let(out::add)
    }
    return out
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) return null
    val v = optString(name, "")
    return if (v.isBlank()) null else v
}
