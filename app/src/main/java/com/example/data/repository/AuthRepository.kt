package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.w("AuthRepository", "Firebase not initialized. Make sure google-services.json is present.")
        null
    }

    val currentUser: Flow<User?> = callbackFlow {
        if (auth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                trySend(User(user.uid, user.displayName, user.email, user.photoUrl?.toString()))
            } else {
                trySend(null)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun getUserId(): String? = auth?.currentUser?.uid

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        if (auth == null) return Result.failure(Exception("Firebase is not initialized. Please configure google-services.json."))
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("AuthRepository", "Sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<Unit> {
        if (auth == null) return Result.failure(Exception("Firebase is not initialized. Please configure google-services.json."))
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("AuthRepository", "Sign up failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
    }
}
