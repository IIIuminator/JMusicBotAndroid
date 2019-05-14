/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.jmusicbot.client.api

import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.stateMachine
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import retrofit2.Response
import timber.log.Timber
import timber.log.error
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

private const val GROUP_ADDRESS = "224.0.0.142"
internal const val PORT = 42945
private const val SOCKET_TIMEOUT = 4000

internal fun listenForServerMulticast(): String? {
    return try {
        MulticastSocket(PORT).use { socket ->
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

@ExperimentalCoroutinesApi
@Throws(
    InvalidParametersException::class,
    AuthException::class,
    NotFoundException::class,
    ServerErrorException::class
)
internal suspend inline fun <reified T> Deferred<Response<T>>.process(
    successCodes: List<Int> = listOf(200, 201, 204),
    errorCodes: Map<Int, Exception> = mapOf(),
    notFoundType: NotFoundException.Type = NotFoundException.Type.SONG,
    invalidParamsType: InvalidParametersException.Type = InvalidParametersException.Type.MISSING
): T? {
    val response: Response<T>
    try {
        response = await()
    } catch (e: Exception) {
        JMusicBot.stateMachine.transition(Event.Disconnect(e))
        throw e
    }
    return when (response.code()) {
        in successCodes -> response.body()
        in errorCodes -> throw errorCodes.getValue(response.code())
        400 -> throw InvalidParametersException(
            invalidParamsType,
            response.errorBody()!!.string()
        )
        401 -> throw AuthException(
            AuthException.Reason.NEEDS_AUTH,
            response.errorBody()!!.string()
        )
        403 -> throw AuthException(
            AuthException.Reason.NEEDS_PERMISSION,
            response.errorBody()!!.string()
        )
        404 -> throw NotFoundException(
            notFoundType,
            response.errorBody()!!.string()
        )
        409 -> throw UsernameTakenException()
        else -> {
            Timber.error { "Server Error: ${response.errorBody()!!.string()}, ${response.code()}" }
            throw ServerErrorException(response.code())
        }
    }
}
