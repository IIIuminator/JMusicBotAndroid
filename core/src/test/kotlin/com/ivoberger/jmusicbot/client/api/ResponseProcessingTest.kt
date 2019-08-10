package com.ivoberger.jmusicbot.client.api

import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.VersionInfo
import com.ivoberger.jmusicbot.client.utils.process
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import retrofit2.Response

@ExperimentalCoroutinesApi
internal class ResponseProcessingTest {

    private val testMsg = "testMsg"
    private val versionInfo = VersionInfo(
        "0.1.0", VersionInfo.ImplementationInfo("testImpl", "v1.0.0")
    )

    @Test
    fun processResponseNotNull() {
        val response = Response.success(versionInfo)
        assertEquals(versionInfo, response.process())
    }

    @Test
    fun processCustomCodes() = runBlockingTest {
        var statusCode = 200
        var response = Response.success(versionInfo)
        assertThrows(IllegalStateException::class.java) {
            response.process(listOf(), mapOf(statusCode to IllegalStateException()))
        }
        statusCode = 400
        response = Response.error<VersionInfo>(statusCode, "".toResponseBody())

        assertNull(response.process(listOf(statusCode)))
    }

    @Test
    fun processResponseNull() {
        val response = Response.success<VersionInfo>(null)
        assertEquals(null, response.process())
    }

    @Test
    fun processError400() {
        var response = Response.error<VersionInfo>(400, testMsg.toResponseBody())

        with(assertThrows(InvalidParametersException::class.java) { response.process() }) {
            assertEquals(InvalidParametersException.Type.MISSING, type)
            assertEquals(testMsg, message)
        }

        response = Response.error<VersionInfo>(400, testMsg.toResponseBody())

        with(assertThrows(InvalidParametersException::class.java) {
            response.process(invalidParamsType = InvalidParametersException.Type.INVALID_PASSWORD)
        }) {
            assertEquals(InvalidParametersException.Type.INVALID_PASSWORD, type)
            assertEquals(testMsg, message)
        }
    }

    @Test
    fun processError401() {
        val response = Response.error<VersionInfo>(401, testMsg.toResponseBody())

        with(assertThrows(AuthException::class.java) { response.process() }) {
            assertEquals(AuthException.Reason.NEEDS_AUTH, reason)
            assertEquals(testMsg, message)
        }
    }

    @Test
    fun processError403() {
        val response = Response.error<VersionInfo>(403, testMsg.toResponseBody())

        with(assertThrows(AuthException::class.java) { response.process() }) {
            assertEquals(AuthException.Reason.NEEDS_PERMISSION, reason)
            assertEquals(testMsg, message)
        }
    }

    @Test
    fun processError404() {
        val statusCode = 404
        var response = Response.error<VersionInfo>(statusCode, testMsg.toResponseBody())

        with(assertThrows(NotFoundException::class.java) { response.process() }) {
            assertEquals(NotFoundException.Type.SONG, type)
            assertEquals(testMsg, message)
        }

        response = Response.error<VersionInfo>(
            statusCode,
            testMsg.toResponseBody()
        )

        with(assertThrows(NotFoundException::class.java) { response.process(notFoundType = NotFoundException.Type.USER) }) {
            assertEquals(NotFoundException.Type.USER, type)
            assertEquals(testMsg, message)
        }
    }

    @Test
    fun processError409() {
        val response = Response.error<VersionInfo>(409, "".toResponseBody())

        with(assertThrows(UsernameTakenException::class.java) { response.process() })
        { assertEquals("Username already in use", message) }
    }

    @Test
    fun processErrorOther() {
        var statusCode = 410
        var response = Response.error<VersionInfo>(statusCode, "".toResponseBody())

        with(assertThrows(ServerErrorException::class.java) { response.process() }) {
            assertEquals(statusCode, code)
            assertEquals("Server error $statusCode", message)
        }
        statusCode = 500
        response = Response.error<VersionInfo>(statusCode, "".toResponseBody())

        with(assertThrows(ServerErrorException::class.java) { response.process() }) {
            assertEquals(statusCode, code)
            assertEquals("Server error $statusCode", message)
        }
    }
}
