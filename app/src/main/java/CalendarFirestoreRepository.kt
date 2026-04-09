package com.example.cinet

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CalendarFirestoreRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun getUid(): String {
        // All data is stored under users/{uid}/..., so every operation depends
        // on having a currently authenticated Firebase user.
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user found.")

        Log.d("FirestoreDebug", "Current UID: $uid")
        return uid
    }

    suspend fun loadClasses(): List<ClassItem> {
        val uid = getUid()

        android.util.Log.d("FirestoreDebug", "Loading classes from users/$uid/classes")

        val snapshot = db.collection("users")
            .document(uid)
            .collection("classes")
            .get()
            .await() // Converts Firebase Task → coroutine suspend call

        android.util.Log.d("FirestoreDebug", "Class documents found: ${snapshot.documents.size}")

        return snapshot.documents.mapNotNull { doc ->
            // If any required field is missing, that document is skipped entirely.
            val name = doc.getString("name") ?: return@mapNotNull null
            val meetingDays = doc.get("meetingDays") as? List<String> ?: emptyList()
            val startTime = doc.getString("startTime") ?: return@mapNotNull null
            val endTime = doc.getString("endTime") ?: return@mapNotNull null
            val location = doc.getString("location") ?: ""
            android.util.Log.d(
                "FirestoreDebug",
                "Loaded class -> id=${doc.id}, name=$name, meetingDays=$meetingDays, start=$startTime, end=$endTime"
            )

            ClassItem(
                id = doc.id, // Firestore document ID becomes the app's class ID
                name = name,
                meetingDays = meetingDays,
                startTime = startTime,
                endTime = endTime,
                location = location
            )
        }
    }

    suspend fun loadAssignments(): List<ScheduleItem> {
        val uid = getUid()

        val snapshot = db.collection("users")
            .document(uid)
            .collection("assignments")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            // Same pattern: skip any malformed or incomplete documents.
            val date = doc.getString("date") ?: return@mapNotNull null
            val classId = doc.getString("classId") ?: return@mapNotNull null
            val className = doc.getString("className") ?: return@mapNotNull null
            val assignmentName = doc.getString("assignmentName") ?: return@mapNotNull null
            val dueTime = doc.getString("dueTime") ?: return@mapNotNull null

            ScheduleItem(
                id = doc.id,
                date = date, // Must match the format used elsewhere (e.g., CalendarGrid)
                classId = classId,
                className = className,
                assignmentName = assignmentName,
                dueTime = dueTime
            )
        }
    }

    suspend fun addAssignment(
        date: String,
        classId: String,
        className: String,
        assignmentName: String,
        dueTime: String
    ) {
        val uid = getUid()

        val assignmentData = mapOf(
            // Field names must match what loadAssignments() expects.
            "date" to date,
            "classId" to classId,
            "className" to className,
            "assignmentName" to assignmentName,
            "dueTime" to dueTime
        )

        db.collection("users")
            .document(uid)
            .collection("assignments")
            // .add() generates a new document ID automatically.
            .add(assignmentData)
            .await()
    }

    suspend fun updateAssignment(
        assignmentId: String,
        date: String,
        classId: String,
        className: String,
        assignmentName: String,
        dueTime: String
    ) {
        val uid = getUid()

        val assignmentData = mapOf(
            "date" to date,
            "classId" to classId,
            "className" to className,
            "assignmentName" to assignmentName,
            "dueTime" to dueTime
        )

        db.collection("users")
            .document(uid)
            .collection("assignments")
            .document(assignmentId)
            // .set() overwrites the entire document (not partial update).
            .set(assignmentData)
            .await()
    }

    suspend fun deleteAssignment(assignmentId: String) {
        val uid = getUid()

        db.collection("users")
            .document(uid)
            .collection("assignments")
            .document(assignmentId)
            .delete()
            .await()
    }

    suspend fun addClass(
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String,
        location: String
    ) {
        val uid = getUid()

        val classData = mapOf(
            // Must match loadClasses() field expectations.
            "name" to name,
            "meetingDays" to meetingDays,
            "startTime" to startTime,
            "endTime" to endTime,
            "location" to location
        )

        db.collection("users")
            .document(uid)
            .collection("classes")
            .add(classData)
            .await()

        android.util.Log.d("FirestoreDebug", "Added class successfully: $name")
        Log.d("FirestoreDebug", "Added class: $name")
    }

    suspend fun updateClass(
        classId: String,
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String,
        location: String
    ) {
        val uid = getUid()

        val classData = mapOf(
            "name" to name,
            "meetingDays" to meetingDays,
            "startTime" to startTime,
            "endTime" to endTime,
            "location" to location
        )

        db.collection("users")
            .document(uid)
            .collection("classes")
            .document(classId)
            // Overwrites entire class document.
            .set(classData)
            .await()
    }

    suspend fun deleteClass(classId: String) {
        val uid = getUid()

        db.collection("users")
            .document(uid)
            .collection("classes")
            .document(classId)
            .delete()
            .await()
    }
}