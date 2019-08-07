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

import com.ivoberger.jmusicbot.client.KEY_AUTHORIZATION
import com.ivoberger.jmusicbot.client.KEY_PROVIDER_ID
import com.ivoberger.jmusicbot.client.KEY_QUERY
import com.ivoberger.jmusicbot.client.KEY_SONG_ID
import com.ivoberger.jmusicbot.client.KEY_SUGGESTER_ID
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.MusicBotPlugin
import com.ivoberger.jmusicbot.client.model.PlayerAction
import com.ivoberger.jmusicbot.client.model.PlayerState
import com.ivoberger.jmusicbot.client.model.PlayerStateChange
import com.ivoberger.jmusicbot.client.model.QueueEntry
import com.ivoberger.jmusicbot.client.model.Song
import com.ivoberger.jmusicbot.client.model.UserInfo
import com.ivoberger.jmusicbot.client.model.VersionInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface MusicBotService {
    companion object {
        private const val URL_USER = "user"
        private const val URL_PLAYER = "player"
        private const val URL_SUGGEST = "suggester"
        private const val URL_PROVIDER = "provider"
        private const val URL_QUEUE = "queue"
    }

    // User operations
    @PUT(URL_USER)
    suspend fun changePassword(@Body newPassword: Auth.PasswordChange): Response<String>

    @DELETE(URL_USER)
    suspend fun deleteUser(): Response<Unit>

    @POST(URL_USER)
    suspend fun registerUser(@Body credentials: Auth.Register): Response<String>

    @GET("token")
    suspend fun loginUser(@Header(KEY_AUTHORIZATION) loginCredentials: String): Response<String>

    @GET(URL_USER)
    suspend fun testToken(
        @Header(KEY_AUTHORIZATION) authToken: String
    ): Response<UserInfo>

    // Song operations

    @GET("$URL_PROVIDER/{$KEY_PROVIDER_ID}/{$KEY_SONG_ID}")
    suspend fun lookupSong(
        @Path(KEY_PROVIDER_ID) providerId: String,
        @Path(KEY_SONG_ID) songId: String
    ): Response<Song>

    @GET("$URL_PROVIDER/{$KEY_PROVIDER_ID}")
    suspend fun searchForSong(
        @Path(KEY_PROVIDER_ID) providerId: String,
        @Query(KEY_QUERY) query: String
    ): Response<List<Song>>

    // Queue operations

    @DELETE("$URL_PLAYER/$URL_QUEUE")
    suspend fun dequeue(@Query(KEY_SONG_ID) songId: String, @Query(KEY_PROVIDER_ID) providerId: String): Response<List<QueueEntry>>

    @PUT("$URL_PLAYER/$URL_QUEUE")
    suspend fun enqueue(@Query(KEY_SONG_ID) songId: String, @Query(KEY_PROVIDER_ID) providerId: String): Response<List<QueueEntry>>

    @PUT("$URL_PLAYER/$URL_QUEUE/order")
    suspend fun moveEntry(
        @Body entry: QueueEntry,
        @Query("providerId") providerId: String,
        @Query("songId") songId: String,
        @Query("index") index: Int
    ): Response<List<QueueEntry>>

    @GET("$URL_PLAYER/$URL_QUEUE")
    suspend fun getQueue(): Response<List<QueueEntry>>

    @GET("$URL_PLAYER/$URL_QUEUE/history")
    suspend fun getHistory(): Response<List<QueueEntry>>

    // Suggest operations

    @GET(URL_SUGGEST)
    suspend fun getSuggesters(): Response<List<MusicBotPlugin>>

    @DELETE("$URL_SUGGEST/{$KEY_SUGGESTER_ID}")
    suspend fun deleteSuggestion(
        @Path(KEY_SUGGESTER_ID) suggesterId: String,
        @Query(KEY_SONG_ID) songId: String,
        @Query(KEY_PROVIDER_ID) providerId: String
    ): Response<Unit>

    @GET("$URL_SUGGEST/{$KEY_SUGGESTER_ID}")
    suspend fun getSuggestions(@Path(KEY_SUGGESTER_ID) suggesterId: String, @Query("max") limit: Int = 32): Response<List<Song>>

    // Provider operations

    @GET(URL_PROVIDER)
    suspend fun getProvider(): Response<List<MusicBotPlugin>>

    // Player operations

    @GET(URL_PLAYER)
    suspend fun getPlayerState(): Response<PlayerState>

//    @PUT(URL_PLAYER)
//    suspend fun setPlayerState(@Body playerStateChange: PlayerStateChange): Response<PlayerState>

    @PUT(URL_PLAYER)
    suspend fun pause(
        @Body playerStateChange: PlayerStateChange = PlayerStateChange(
            PlayerAction.PAUSE
        )
    ): Response<PlayerState>

    @PUT(URL_PLAYER)
    suspend fun play(
        @Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.PLAY)
    ): Response<PlayerState>

    @PUT(URL_PLAYER)
    suspend fun skip(
        @Body playerStateChange: PlayerStateChange = PlayerStateChange(PlayerAction.SKIP)
    ): Response<PlayerState>

    @GET("version")
    suspend fun getVersionInfo(): Response<VersionInfo>
}
