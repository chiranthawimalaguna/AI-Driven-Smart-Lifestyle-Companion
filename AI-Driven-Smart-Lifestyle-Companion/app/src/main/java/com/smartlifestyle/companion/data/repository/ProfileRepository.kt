package com.smartlifestyle.companion.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartlifestyle.companion.data.remote.AuthRepository
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val name: String = "",
    val age: Int? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val preferredUnits: String = "metric", // "metric" | "imperial"
    val notificationsEnabled: Boolean = true,
    val stepGoal: Int = 8000,
    val waterGoal: Int = 8
)

/**
 * Personal info, preferences, and goal targets - one document per user, same
 * users/{uid}/... layout as everything else (see RoutineRepository's doc comment
 * on Firestore layout). One-shot read/write rather than a live listener: this
 * data changes rarely and is only needed when the Profile screen is open.
 */
class ProfileRepository(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    private fun profileDoc(uid: String) =
        firestore.collection("users").document(uid).collection("profile").document("info")

    suspend fun getProfile(): UserProfile {
        val uid = authRepository.currentUser?.uid ?: return UserProfile()
        val doc = profileDoc(uid).get().await()
        if (!doc.exists()) return UserProfile()
        return UserProfile(
            name = doc.getString("name") ?: "",
            age = doc.getLong("age")?.toInt(),
            heightCm = doc.getDouble("heightCm")?.toFloat(),
            weightKg = doc.getDouble("weightKg")?.toFloat(),
            preferredUnits = doc.getString("preferredUnits") ?: "metric",
            notificationsEnabled = doc.getBoolean("notificationsEnabled") ?: true,
            stepGoal = doc.getLong("stepGoal")?.toInt() ?: 8000,
            waterGoal = doc.getLong("waterGoal")?.toInt() ?: 8
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        val uid = authRepository.currentUser?.uid ?: return
        profileDoc(uid).set(
            mapOf(
                "name" to profile.name,
                "age" to profile.age,
                "heightCm" to profile.heightCm,
                "weightKg" to profile.weightKg,
                "preferredUnits" to profile.preferredUnits,
                "notificationsEnabled" to profile.notificationsEnabled,
                "stepGoal" to profile.stepGoal,
                "waterGoal" to profile.waterGoal
            )
        ).await()
    }
}
