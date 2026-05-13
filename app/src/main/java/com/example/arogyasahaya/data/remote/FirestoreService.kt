package com.example.arogyasahaya.data.remote

import com.example.arogyasahaya.data.local.entity.AshaEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreService {
    private val db by lazy {
        try {
            com.google.firebase.ktx.Firebase.firestore
        } catch (e: Exception) {
            null
        }
    }
    private val eventsCollection by lazy { db?.collection("health_events") }

    fun getEventsFlow(): Flow<List<AshaEvent>> = callbackFlow {
        val collection = eventsCollection
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val events = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(AshaEvent::class.java)?.copy(id = doc.id.hashCode())
            } ?: emptyList()
            trySend(events)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addEvent(event: AshaEvent) {
        eventsCollection?.add(event)
    }
    
    suspend fun updateEvent(event: AshaEvent) {
        eventsCollection?.whereEqualTo("title", event.title)
            ?.get()
            ?.addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    doc.reference.set(event)
                }
            }
    }
}
