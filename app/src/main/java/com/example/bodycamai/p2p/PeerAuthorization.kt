package com.example.bodycamai.p2p

import java.security.MessageDigest
import java.util.UUID

/** Local authorization primitives for a serverless Host/Client group. */
data class PeerIdentity(
    val deviceId: String = UUID.randomUUID().toString(),
    val displayName: String,
    val role: PeerRole = PeerRole.CLIENT
)

data class JoinRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val displayName: String,
    val roomCodeHash: String
)

data class AuthorizedPeer(
    val deviceId: String,
    val displayName: String,
    val role: PeerRole,
    val approvedAt: Long = System.currentTimeMillis()
)

object RoomCodeVerifier {
    fun digest(code: String): String = MessageDigest.getInstance("SHA-256")
        .digest(code.trim().toByteArray())
        .joinToString("") { "%02x".format(it) }

    fun matches(code: String, expectedDigest: String): Boolean = digest(code) == expectedDigest
}
