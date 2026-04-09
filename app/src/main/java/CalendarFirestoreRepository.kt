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
        android.util.Log.d("FirestoreDebug", "Loading classes from users/$uid/classes")
        val snapshot = db.collection("users").document(uid).collection("classes").get().await()
        android.util.Log.d("FirestoreDebug", "Class documents found: ${snapshot.documents.size}")
        return snapshot.documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            val meetingDays = doc.get("meetingDays") as? List<String> ?: emptyList()
            val startTime = doc.getString("startTime") ?: return@mapNotNull null
            val endTime = doc.getString("endTime") ?: return@mapNotNull null
            ClassItem(id = doc.id, name = name, meetingDays = meetingDays, startTime = startTime, endTime = endTime)
        }
    }

    suspend fun loadAssignments(): List<ScheduleItem> {
        val uid = getUid()
        val snapshot = db.collection("users").document(uid).collection("assignments").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val date = doc.getString("date") ?: return@mapNotNull null
            val classId = doc.getString("classId") ?: return@mapNotNull null
            val className = doc.getString("className") ?: return@mapNotNull null
            val assignmentName = doc.getString("assignmentName") ?: return@mapNotNull null
            val dueTime = doc.getString("dueTime") ?: return@mapNotNull null
            ScheduleItem(id = doc.id, date = date, classId = classId, className = className, assignmentName = assignmentName, dueTime = dueTime)
        }
    }

    suspend fun loadStudySessions(): List<StudySession> {
        val uid = getUid()
        val snapshot = db.collection("users").document(uid).collection("studySessions").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val date = doc.getString("date") ?: return@mapNotNull null
            val className = doc.getString("className") ?: return@mapNotNull null
            val topic = doc.getString("topic") ?: return@mapNotNull null
            val startTime = doc.getString("startTime") ?: return@mapNotNull null
            val location = doc.getString("location") ?: ""
            StudySession(id = doc.id, date = date, className = className, topic = topic, startTime = startTime, location = location)
        }
    }

    suspend fun loadEvents(): List<EventItem> {
        val uid = getUid()
        val snapshot = db.collection("users").document(uid).collection("events").get().await()
        return snapshot.documents.mapNotNull { doc ->
            val date = doc.getString("date") ?: return@mapNotNull null
            val name = doc.getString("name") ?: return@mapNotNull null
            val time = doc.getString("time") ?: return@mapNotNull null
            val location = doc.getString("location") ?: ""
            EventItem(id = doc.id, date = date, name = name, time = time, location = location)
        }
    }

    suspend fun addAssignment(date: String, classId: String, className: String, assignmentName: String, dueTime: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("assignments")
            .add(mapOf("date" to date, "classId" to classId, "className" to className, "assignmentName" to assignmentName, "dueTime" to dueTime))
            .await()
    }

    suspend fun updateAssignment(assignmentId: String, date: String, classId: String, className: String, assignmentName: String, dueTime: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("assignments").document(assignmentId)
            .set(mapOf("date" to date, "classId" to classId, "className" to className, "assignmentName" to assignmentName, "dueTime" to dueTime))
            .await()
    }

    suspend fun deleteAssignment(assignmentId: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("assignments").document(assignmentId).delete().await()
    }

    suspend fun addStudySession(date: String, className: String, topic: String, startTime: String, location: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("studySessions")
            .add(mapOf("date" to date, "className" to className, "topic" to topic, "startTime" to startTime, "location" to location))
            .await()
    }

    suspend fun updateStudySession(sessionId: String, date: String, className: String, topic: String, startTime: String, location: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("studySessions").document(sessionId)
            .set(mapOf("date" to date, "className" to className, "topic" to topic, "startTime" to startTime, "location" to location))
            .await()
    }

    suspend fun deleteStudySession(sessionId: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("studySessions").document(sessionId).delete().await()
    }

    suspend fun addEvent(date: String, name: String, time: String, location: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("events")
            .add(mapOf("date" to date, "name" to name, "time" to time, "location" to location))
            .await()
    }

    suspend fun updateEvent(eventId: String, date: String, name: String, time: String, location: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("events").document(eventId)
            .set(mapOf("date" to date, "name" to name, "time" to time, "location" to location))
            .await()
    }

    suspend fun deleteEvent(eventId: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("events").document(eventId).delete().await()
    }

    suspend fun addClass(name: String, meetingDays: List<String>, startTime: String, endTime: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("classes")
            .add(mapOf("name" to name, "meetingDays" to meetingDays, "startTime" to startTime, "endTime" to endTime))
            .await()
        android.util.Log.d("FirestoreDebug", "Added class successfully: $name")
        Log.d("FirestoreDebug", "Added class: $name")
    }

    suspend fun updateClass(classId: String, name: String, meetingDays: List<String>, startTime: String, endTime: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("classes").document(classId)
            .set(mapOf("name" to name, "meetingDays" to meetingDays, "startTime" to startTime, "endTime" to endTime))
            .await()
    }

    suspend fun deleteClass(classId: String) {
        val uid = getUid()
        db.collection("users").document(uid).collection("classes").document(classId).delete().await()
    }
}