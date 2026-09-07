package com.smartlifestyle.companion.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.smartlifestyle.companion.data.remote.AuthRepository
import com.smartlifestyle.companion.domain.model.Goal
import com.smartlifestyle.companion.domain.model.Habit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Goals and habits, same architecture as RoutineRepository: stored directly in
 * Firestore under users/{uid}/goals and users/{uid}/habits, relying on the SDK's
 * built-in offline persistence rather than a hand-rolled local cache. Low-frequency,
 * user-authored data - the same reasoning documented in RoutineRepository.
 */
class GoalsRepository(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    private val userIdProvider: () -> String? = { authRepository.currentUser?.uid }
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun goalsCollection(uid: String) = firestore.collection("users").document(uid).collection("goals")
    private fun habitsCollection(uid: String) = firestore.collection("users").document(uid).collection("habits")

    fun observeGoals(): Flow<List<Goal>> = observeAuthScoped(
        collectionFor = { uid -> goalsCollection(uid) },
        mapper = { doc ->
            Goal(
                id = doc.id,
                title = doc.getString("title") ?: "",
                targetValue = doc.getDouble("targetValue")?.toFloat() ?: 0f,
                currentValue = doc.getDouble("currentValue")?.toFloat() ?: 0f,
                unit = doc.getString("unit") ?: "",
                createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
            )
        }
    )

    fun observeHabits(): Flow<List<Habit>> = observeAuthScoped(
        collectionFor = { uid -> habitsCollection(uid) },
        mapper = { doc ->
            @Suppress("UNCHECKED_CAST")
            Habit(
                id = doc.id,
                title = doc.getString("title") ?: "",
                completedDates = (doc.get("completedDates") as? List<String>) ?: emptyList(),
                createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
            )
        }
    )

    suspend fun addGoal(title: String, targetValue: Float, unit: String) {
        val uid = userIdProvider() ?: return
        goalsCollection(uid).add(
            hashMapOf(
                "title" to title,
                "targetValue" to targetValue,
                "currentValue" to 0f,
                "unit" to unit,
                "createdAtMillis" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun updateGoalProgress(goal: Goal, newValue: Float) {
        val uid = userIdProvider() ?: return
        goalsCollection(uid).document(goal.id).update("currentValue", newValue).await()
    }

    suspend fun deleteGoal(goal: Goal) {
        val uid = userIdProvider() ?: return
        goalsCollection(uid).document(goal.id).delete().await()
    }

    suspend fun addHabit(title: String) {
        val uid = userIdProvider() ?: return
        habitsCollection(uid).add(
            hashMapOf(
                "title" to title,
                "completedDates" to emptyList<String>(),
                "createdAtMillis" to System.currentTimeMillis()
            )
        ).await()
    }

    /** Toggles today's completion for a habit - marking it done extends the streak,
     * un-marking removes today only (yesterday's streak history is untouched). */
    suspend fun toggleHabitToday(habit: Habit) {
        val uid = userIdProvider() ?: return
        val today = dayFormat.format(Calendar.getInstance().time)
        val updated = if (habit.completedDates.contains(today)) {
            habit.completedDates - today
        } else {
            habit.completedDates + today
        }
        habitsCollection(uid).document(habit.id).update("completedDates", updated).await()
    }

    suspend fun deleteHabit(habit: Habit) {
        val uid = userIdProvider() ?: return
        habitsCollection(uid).document(habit.id).delete().await()
    }

    fun todayKey(): String = dayFormat.format(Calendar.getInstance().time)
    fun yesterdayKey(): String = dayFormat.format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
    )

    private fun <T> observeAuthScoped(
        collectionFor: (String) -> com.google.firebase.firestore.CollectionReference,
        mapper: (com.google.firebase.firestore.DocumentSnapshot) -> T
    ): Flow<List<T>> = callbackFlow {
        var registration: ListenerRegistration? = null

        fun attach(uid: String?) {
            registration?.remove()
            registration = null
            if (uid == null) {
                trySend(emptyList())
                return
            }
            registration = collectionFor(uid).addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                trySend(snapshot.documents.map(mapper))
            }
        }

        val authListener = authRepository.observeAuthState { user -> attach(user?.uid) }

        awaitClose {
            registration?.remove()
            authRepository.removeAuthStateListener(authListener)
        }
    }
}
