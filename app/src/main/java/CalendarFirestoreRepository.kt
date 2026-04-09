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
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user found.")

        Log.d("FirestoreDebug", "Current UID: $uid")
        return uid
    }

    suspend fun loadClasses(): List<ClassItem> {
        val uid = getUid()

        Log.d("FirestoreDebug", "Loading classes from users/$uid/classes")

        val snapshot = db.collection("users")
            .document(uid)
            .collection("classes")
            .get()
            .await()

        Log.d("FirestoreDebug", "Class documents found: ${snapshot.documents.size}")

        return snapshot.documents.mapNotNull { doc ->
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
                id = doc.id,
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
            val date = doc.getString("date") ?: return@mapNotNull null
            val classId = doc.getString("classId") ?: return@mapNotNull null
            val className = doc.getString("className") ?: return@mapNotNull null
            val assignmentName = doc.getString("assignmentName") ?: return@mapNotNull null
            val dueTime = doc.getString("dueTime") ?: return@mapNotNull null

            ScheduleItem(
                id = doc.id,
                date = date,
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
            "date" to date,
            "classId" to classId,
            "className" to className,
            "assignmentName" to assignmentName,
            "dueTime" to dueTime
        )

        db.collection("users")
            .document(uid)
            .collection("assignments")
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
            "name" to name,
            "meetingDays" to meetingDays,
            "startTime" to startTime,
            "endTime" to endTime,
            "location" to location
        )

        val docRef = db.collection("users")
            .document(uid)
            .collection("classes")
            .add(classData)
            .await()

        val newClass = ClassItem(
            id = docRef.id,
            name = name,
            meetingDays = meetingDays,
            startTime = startTime,
            endTime = endTime
        )

        Log.d("FirestoreDebug", "Added class successfully: ${newClass.name}, id=${newClass.id}")

        return newClass
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
            .set(classData)
            .await()

        Log.d("FirestoreDebug", "Updated class successfully: $name, id=$classId")
    }

    suspend fun deleteClass(classId: String) {
        val uid = getUid()

        db.collection("users")
            .document(uid)
            .collection("classes")
            .document(classId)
            .delete()
            .await()

        Log.d("FirestoreDebug", "Deleted class successfully: id=$classId")
    }
}