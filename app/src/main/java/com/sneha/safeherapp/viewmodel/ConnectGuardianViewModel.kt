package com.sneha.safeherapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.sneha.safeherapp.data.model.GuardianConnection
import com.sneha.safeherapp.data.model.GuardianInvite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

sealed class ConnectState {
    object Idle : ConnectState()
    object Loading : ConnectState()
    data class Preview(val invite: GuardianInvite) : ConnectState()
    object Success : ConnectState()
    data class Error(val message: String) : ConnectState()
}

data class ConnectedGuardianUI(
    val guardianUid: String,
    val guardianName: String,
    val relationship: String,
    val connectedSince: String
)

class ConnectGuardianViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SafeHerConnect"

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.Idle)
    val connectState: StateFlow<ConnectState> = _connectState

    private val _guardians = MutableStateFlow<List<ConnectedGuardianUI>>(emptyList())
    val guardians: StateFlow<List<ConnectedGuardianUI>> = _guardians

    private var listenerRegistration: ListenerRegistration? = null

    init {
        startListeningForGuardians()
    }

    private fun startListeningForGuardians() {
        val userId = auth.currentUser?.uid ?: return
        listenerRegistration?.remove()
        
        listenerRegistration = db.collection("user_connections")
            .whereEqualTo("childUid", userId)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("connectedGuardianName") ?: doc.getString("guardianName") ?: "Guardian"
                        val relationship = doc.getString("relationship") ?: "Guardian"
                        val timestamp = doc.getTimestamp("connectedAt")
                        val dateStr = if (timestamp != null) {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(timestamp.toDate())
                        } else {
                            "Recently"
                        }
                        ConnectedGuardianUI(
                            guardianUid = doc.getString("guardianUid") ?: "",
                            guardianName = name,
                            relationship = relationship,
                            connectedSince = dateStr
                        )
                    }
                    _guardians.value = list
                }
            }
    }

    fun verifyInviteCode(code: String) {
        viewModelScope.launch {
            _connectState.value = ConnectState.Loading
            try {
                Log.d(TAG, "Verifying code: $code")
                val doc = db.collection("guardian_invites").document(code).get().await()
                
                if (doc.exists()) {
                    val invite = doc.toObject(GuardianInvite::class.java)
                    if (invite != null && invite.status == "pending") {
                        _connectState.value = ConnectState.Preview(invite)
                    } else {
                        _connectState.value = ConnectState.Error("Invite already used or expired")
                    }
                } else {
                    _connectState.value = ConnectState.Error("Invalid invite code")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Verification error", e)
                _connectState.value = ConnectState.Error(e.localizedMessage ?: "Connection error")
            }
        }
    }

    fun acceptConnection(invite: GuardianInvite) {
        viewModelScope.launch {
            _connectState.value = ConnectState.Loading
            try {
                val user = auth.currentUser ?: throw Exception("Not logged in")
                val childUid = user.uid

                val userDoc = db.collection("users").document(childUid).get().await()
                val fullName = userDoc.getString("fullName") ?: invite.childName
                val email = userDoc.getString("email") ?: ""

                val batch = db.batch()

                val inviteRef = db.collection("guardian_invites").document(invite.inviteCode)
                batch.update(inviteRef, "status", "accepted")

                // Use unique doc ID to support multiple guardians
                val connectionId = "${invite.guardianUid}_$childUid"
                val childConnRef = db.collection("user_connections").document(connectionId)
                val now = Timestamp.now()
                val childConnData = mapOf(
                    "childUid" to childUid,
                    "connectedGuardianUid" to invite.guardianUid,
                    "connectedGuardianName" to invite.guardianName,
                    "guardianUid" to invite.guardianUid,
                    "guardianName" to invite.guardianName,
                    "status" to "accepted",
                    "connectedAt" to now,
                    "relationship" to invite.relationship
                )
                batch.set(childConnRef, childConnData)

                val guardianChildrenRef = db.collection("guardian_users").document(invite.guardianUid)
                    .collection("children")
                
                val childDocs = guardianChildrenRef.whereEqualTo("inviteCode", invite.inviteCode).get().await()
                for (doc in childDocs.documents) {
                    batch.update(doc.reference, mapOf(
                        "status" to "accepted",
                        "connectedChildUid" to childUid,
                        "connectedChildName" to fullName,
                        "childUid" to childUid,
                        "fullName" to fullName,
                        "email" to email,
                        "lastUpdated" to now
                    ))
                }

                batch.commit().await()
                _connectState.value = ConnectState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Accept error", e)
                _connectState.value = ConnectState.Error(e.localizedMessage ?: "Failed to connect")
            }
        }
    }

    fun removeGuardian(guardianUid: String) {
        viewModelScope.launch {
            try {
                val childUid = auth.currentUser?.uid ?: return@launch
                val connectionId = "${guardianUid}_$childUid"
                
                val batch = db.batch()
                
                // 1. Delete from user_connections
                val connectionRef = db.collection("user_connections").document(connectionId)
                batch.delete(connectionRef)
                
                // 2. Update status in guardian's children list
                val guardianChildrenRef = db.collection("guardian_users").document(guardianUid)
                    .collection("children")
                val childDocs = guardianChildrenRef.whereEqualTo("childUid", childUid).get().await()
                for (doc in childDocs.documents) {
                    batch.update(doc.reference, "status", "Removed by Child")
                }
                
                batch.commit().await()
                Log.d(TAG, "Guardian $guardianUid removed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Remove guardian error", e)
            }
        }
    }

    fun resetState() {
        _connectState.value = ConnectState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
