package com.example.data

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class AuthRepository {
    private val firebaseAuth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onComplete: (AuthResult?, Exception?) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(task.result, null)
                } else {
                    onComplete(null, task.exception)
                }
            }
    }

    fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        onComplete: (AuthResult?, Exception?) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(task.result, null)
                } else {
                    onComplete(null, task.exception)
                }
            }
    }

    fun signInWithGoogleToken(
        idToken: String,
        onComplete: (AuthResult?, Exception?) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(task.result, null)
                } else {
                    onComplete(null, task.exception)
                }
            }
    }
}
