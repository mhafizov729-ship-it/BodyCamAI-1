package com.example.bodycamai.p2p

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Serverless encrypted peer channel. ECDH establishes a per-connection key;
 * AES-GCM protects framed application payloads. The room code is an additional
 * admission secret and must be verified by the Host before accepting a peer.
 */
class SecurePeerSession private constructor(private val socket: Socket, private val key: ByteArray) {
    private val input = DataInputStream(socket.getInputStream())
    private val output = DataOutputStream(socket.getOutputStream())

    @Synchronized
    fun send(payload: ByteArray) {
        val nonce = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        val encrypted = cipher.doFinal(payload)
        output.writeInt(nonce.size + encrypted.size)
        output.write(nonce)
        output.write(encrypted)
        output.flush()
    }

    fun receive(): ByteArray {
        val size = input.readInt()
        require(size in 28..(16 * 1024 * 1024)) { "Недопустимый размер пакета" }
        val packet = ByteArray(size)
        input.readFully(packet)
        val nonce = packet.copyOfRange(0, 12)
        val encrypted = packet.copyOfRange(12, packet.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(encrypted)
    }

    fun close() = runCatching { socket.close() }

    companion object {
        private fun keyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        private fun derive(privateKey: java.security.PrivateKey, remote: PublicKey, roomCode: String): ByteArray {
            val agreement = KeyAgreement.getInstance("ECDH")
            agreement.init(privateKey)
            agreement.doPhase(remote, true)
            val shared = agreement.generateSecret()
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(shared, "HmacSHA256"))
            return mac.doFinal(MessageDigest.getInstance("SHA-256").digest(roomCode.trim().toByteArray()))
        }

        private fun sendPublic(out: DataOutputStream, keyPair: KeyPair) {
            val encoded = keyPair.public.encoded
            out.writeInt(encoded.size)
            out.write(encoded)
            out.flush()
        }

        private fun readPublic(input: DataInputStream): PublicKey {
            val size = input.readInt()
            require(size in 50..4096)
            val bytes = ByteArray(size)
            input.readFully(bytes)
            return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
        }

        fun client(socket: Socket, roomCode: String): SecurePeerSession {
            val pair = keyPair()
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            sendPublic(output, pair)
            val remote = readPublic(input)
            return SecurePeerSession(socket, derive(pair.private, remote, roomCode))
        }

        fun host(socket: Socket, roomCode: String): SecurePeerSession {
            val pair = keyPair()
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())
            val remote = readPublic(input)
            sendPublic(output, pair)
            return SecurePeerSession(socket, derive(pair.private, remote, roomCode))
        }
    }
}

class SecureP2PHost(private val roomCode: String) {
    private var server: ServerSocket? = null

    fun start(port: Int = 0): Int {
        val s = ServerSocket(port)
        server = s
        return s.localPort
    }

    fun accept(): Result<SecurePeerSession> = runCatching {
        val s = server ?: error("Host не запущен")
        SecurePeerSession.host(s.accept(), roomCode)
    }

    fun stop() = runCatching { server?.close() }
}

object SecureP2PClient {
    fun connect(host: String, port: Int, roomCode: String): Result<SecurePeerSession> = runCatching {
        SecurePeerSession.client(Socket(host, port), roomCode)
    }
}
