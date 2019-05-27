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
