package com.example.data

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class AuthRepository {
    private val firebaseAuth: FirebaseAuth?
        get() = try { 
            FirebaseAuth.getInstance() 
        } catch (e: Exception) { 
            e.printStackTrace() 
            null 
        }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            firebaseAuth?.currentUser
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onComplete: (AuthResult?, Exception?) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(null, Exception("Firebase authentication is not initialized or unavailable in this environment."))
            return
        }
        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(task.result, null)
                    } else {
                        onComplete(null, task.exception)
                    }
                }
        } catch (e: Exception) {
            onComplete(null, e)
        }
    }

    fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        onComplete: (AuthResult?, Exception?) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(null, Exception("Firebase authentication is not initialized or unavailable in this environment."))
            return
        }
        try {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(task.result, null)
                    } else {
                        onComplete(null, task.exception)
                    }
                }
        } catch (e: Exception) {
            onComplete(null, e)
        }
    }

    fun signInWithGoogleToken(
        idToken: String,
        onComplete: (AuthResult?, Exception?) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            onComplete(null, Exception("Firebase authentication is not initialized or unavailable in this environment."))
            return
        }
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(task.result, null)
                    } else {
                        onComplete(null, task.exception)
                    }
                }
        } catch (e: Exception) {
            onComplete(null, e)
        }
    }
}
