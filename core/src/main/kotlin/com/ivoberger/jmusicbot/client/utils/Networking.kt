package com.ivoberger.jmusicbot.client.utils

import com.ivoberger.jmusicbot.client.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.client.api.TokenAuthenticator
import com.ivoberger.jmusicbot.client.model.Auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

private const val GROUP_ADDRESS = "224.0.0.142"
const val DEFAULT_PORT = 42945
private const val SOCKET_TIMEOUT = 4000

internal fun listenForServerMulticast(port: Int): String? {
    return try {
        MulticastSocket(port).use { socket ->
            val groupAddress = InetAddress.getByName(GROUP_ADDRESS)
            socket.joinGroup(groupAddress)
            socket.soTimeout = SOCKET_TIMEOUT
            val buffer = ByteArray(8)
            val packet = DatagramPacket(buffer, buffer.size)
            socket.broadcast = true
            socket.receive(packet)
            socket.leaveGroup(groupAddress)
            packet.address.hostAddress
        }
    } catch (e: IOException) {
        null
    }
}

@ExperimentalCoroutinesApi
internal fun OkHttpClient.Builder.withToken(token: Auth.Token) = addInterceptor { chain ->
    chain.proceed(
        chain.request().newBuilder().header(KEY_AUTHORIZATION, token.toAuthHeader()).build()
    )
}.authenticator(TokenAuthenticator()).build()
