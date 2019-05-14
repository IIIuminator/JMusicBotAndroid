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
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.AuthExpectation
import com.ivoberger.jmusicbot.client.model.AuthType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import timber.log.debug

@ExperimentalCoroutinesApi
class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? =
        runBlocking(Dispatchers.IO) {
            Timber.debug { "Re-authorizing" }
            var auth: String? = null
            response.body()?.let { body ->
                val authExpectation =
                    JMusicBot.mBaseComponent.moshi.adapter(AuthExpectation::class.java)
                        .fromJson(String(body.bytes()))
                Timber.debug { "AuthExpectation: $authExpectation" }
                auth = when (authExpectation?.format) {
                    AuthType.BASIC -> {
                        Timber.debug { "BASIC Auth" }
                        JMusicBot.user?.let { Auth.Basic(it).toAuthHeader() }
                    }
                    AuthType.TOKEN -> {
                        Timber.debug { "TOKEN Auth" }
                        if (!JMusicBot.state.hasServer) {
                            JMusicBot.discoverHost()
                        }
                        JMusicBot.user?.let { JMusicBot.authorize(it) }
                        JMusicBot.authToken?.toAuthHeader()
                    }
                    else -> null
                }
            }
            val origRequest = response.request()

            return@runBlocking if (origRequest.header(KEY_AUTHORIZATION) == auth) null else origRequest.newBuilder()
                .header(KEY_AUTHORIZATION, auth ?: "")
                .build()
        }
}
