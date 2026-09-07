package com.smartlifestyle.companion.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.smartlifestyle.companion.data.remote.AuthRepository
import com.smartlifestyle.companion.domain.model.RoutineTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Routine tasks are stored directly in Firestore rather than mirrored into Room.
 * Firestore's Android SDK ships with offline persistence enabled by default -
 * reads and writes work with no network and sync automatically once connectivity
 * returns - so a hand-rolled local cache would just duplicate what the SDK already
 * does for this kind of low-frequency, user-authored data.
 *
 * Contrast with HealthRepository: high-frequency sensor readings are buffered in
 * Room first and only periodically pushed to Firestore, to avoid a cloud write on
 * every single sensor event.
 *
 * Firestore layout: users/{uid}/routine_tasks/{taskId}
 * Access is restricted per-user via Firestore security rules (see firestore.rules).
 */
class RoutineRepository(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {

    private val userIdProvider: () -> String? = { authRepository.currentUser?.uid }

    private fun tasksCollection(uid: String) =
        firestore.collection("users").document(uid).collection("routine_tasks")

    /**
     * Re-attaches its Firestore listener every time auth state changes, instead of
     * resolving the uid once at subscription time. Earlier versions closed this Flow
     * permanently if collected before sign-in (which happens routinely, since
     * DashboardViewModel is created at app launch, before the user has signed in) -
     * meaning tasks added after signing in were written to Firestore correctly but
     * never reached a UI that had stopped listening. This version stays open and
     * swaps its Firestore subscription on every sign-in/sign-out instead.
     */
    fun observeTasks(): Flow<List<RoutineTask>> = callbackFlow {
        var firestoreRegistration: ListenerRegistration? = null

        fun attach(uid: String?) {
            firestoreRegistration?.remove()
            firestoreRegistration = null
            if (uid == null) {
                trySend(emptyList())
                return
            }
            firestoreRegistration = tasksCollection(uid).addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val tasks = snapshot.documents.map { doc ->
                    RoutineTask(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        scheduledTimeMillis = doc.getLong("scheduledTimeMillis") ?: 0L,
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        priorityScore = doc.getDouble("priorityScore")?.toFloat() ?: 0f
                    )
                }.sortedByDescending { it.priorityScore }
                trySend(tasks)
            }
        }

        val authListener = authRepository.observeAuthState { user -> attach(user?.uid) }

        awaitClose {
            firestoreRegistration?.remove()
            authRepository.removeAuthStateListener(authListener)
        }
    }

    suspend fun addTask(title: String, scheduledTimeMillis: Long) {
        val uid = userIdProvider() ?: return
        val data = hashMapOf(
            "title" to title,
            "scheduledTimeMillis" to scheduledTimeMillis,
            "isCompleted" to false,
            "priorityScore" to 0f,
            "lastUpdated" to System.currentTimeMillis()
        )
        tasksCollection(uid).add(data).await()
    }

    suspend fun updatePriority(task: RoutineTask, newScore: Float) {
        val uid = userIdProvider() ?: return
        tasksCollection(uid).document(task.id)
            .update(
                mapOf(
                    "priorityScore" to newScore,
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()
    }

    suspend fun setCompleted(task: RoutineTask, isCompleted: Boolean) {
        val uid = userIdProvider() ?: return
        tasksCollection(uid).document(task.id)
            .update(
                mapOf(
                    "isCompleted" to isCompleted,
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()
    }

    suspend fun editTask(task: RoutineTask, newTitle: String, newScheduledTimeMillis: Long) {
        val uid = userIdProvider() ?: return
        tasksCollection(uid).document(task.id)
            .update(
                mapOf(
                    "title" to newTitle,
                    "scheduledTimeMillis" to newScheduledTimeMillis,
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()
    }

    suspend fun deleteTask(task: RoutineTask) {
        val uid = userIdProvider() ?: return
        tasksCollection(uid).document(task.id).delete().await()
    }

    /** One-shot read for background contexts (e.g. the notification worker) that
     * shouldn't hold a live listener open. */
    suspend fun getOverdueCount(): Int {
        val uid = userIdProvider() ?: return 0
        val snapshot = tasksCollection(uid).get().await()
        val now = System.currentTimeMillis()
        return snapshot.documents.count { doc ->
            val completed = doc.getBoolean("isCompleted") ?: false
            val scheduled = doc.getLong("scheduledTimeMillis") ?: 0L
            !completed && scheduled < now
        }
    }
}
