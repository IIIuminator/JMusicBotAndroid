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
package com.ivoberger.jmusicbot.client.testUtils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ivoberger.jmusicbot.client.model.Permissions
import com.ivoberger.jmusicbot.client.model.User
import java.time.Duration
import java.time.Instant
import java.util.Date

private const val testSigningKey = "totally secret"
private val testSigningAlgo = Algorithm.HMAC512(testSigningKey)

const val testUserName = "newTestUser"
const val testUserId = "testUserId"
val newTestUser = User(
    testUserName,
    id = testUserId,
    permissions = listOf(Permissions.PAUSE, Permissions.ENQUEUE, Permissions.ALTER_SUGGESTIONS)
)

val existingTestUser = newTestUser.copy().apply { password = "password" }

fun User.toToken(): String = JWT.create()
    .withSubject(name)
    .withIssuedAt(Date())
    .withExpiresAt(Date.from(Instant.now().plus(Duration.ofMinutes(10))))
    .withArrayClaim("permissions", permissions.map { it.label }.toTypedArray())
    .sign(testSigningAlgo)

fun User.toExpiredToken(): String = JWT.create()
    .withSubject(name)
    .withIssuedAt(Date.from(Instant.now() - (Duration.ofMinutes(15))))
    .withExpiresAt(Date.from(Instant.now() - (Duration.ofMinutes(10))))
    .withArrayClaim("permissions", permissions.map { it.label }.toTypedArray())
    .sign(testSigningAlgo)

fun String.userFromToken(): User {
    val decodedToken = JWT.require(testSigningAlgo).build().verify(this)
    val name = decodedToken.subject
    val permissions = Permissions.fromClaims(decodedToken.claims)
    return User(name, id = testUserId, permissions = permissions)
}
