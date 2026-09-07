package com.smartlifestyle.companion.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around FirebaseAuth so the ViewModel never touches the Firebase SDK
 * directly - keeps the data layer as the single point of contact with external
 * services, matching the architecture diagram.
 */
class AuthRepository(private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Sign-in succeeded but no user was returned")
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> = runCatching {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Sign-up succeeded but no user was returned")
    }

    fun signOut() = firebaseAuth.signOut()

    /**
     * Registers a listener that fires immediately with the current user (or null),
     * then again on every sign-in/sign-out - regardless of which code path triggered
     * it. Passing the FirebaseUser itself (rather than just a boolean) lets callers
     * that need the uid - like RoutineRepository's live task listener - derive it
     * directly instead of needing a second, separately-wired listener.
     */
    fun observeAuthState(onChange: (FirebaseUser?) -> Unit): FirebaseAuth.AuthStateListener {
        val listener = FirebaseAuth.AuthStateListener { auth -> onChange(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        return listener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        firebaseAuth.removeAuthStateListener(listener)
    }
}
