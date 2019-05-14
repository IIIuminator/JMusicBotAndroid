package com.ivoberger.jmusicbot.client.api

import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.VersionInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import retrofit2.Response
import java.io.IOException

@ExperimentalCoroutinesApi
internal class UtilsKtTest {

    private val testMsg = "testMsg"
    private val versionInfo = VersionInfo(
        "0.1.0", VersionInfo.ImplementationInfo("testImpl", "v1.0.0")
    )

    @Test
    fun processResponseNotNull() = runBlockingTest {
        val response = async { Response.success(versionInfo) }
        assertEquals(versionInfo, response.process())
    }

    @Test
    fun processCustomCodes() = runBlockingTest {
        var statusCode = 200
        var response = async { Response.success(versionInfo) }
        assertThrows(IllegalStateException::class.java) {
            runBlockingTest {
                response.process(listOf(), mapOf(statusCode to IllegalStateException()))
            }
        }
        statusCode = 400
        response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, ""))
        }
        assertEquals(null, response.process(listOf(statusCode)))
    }

    @Test
    fun processResponseNull() = runBlockingTest {
        val response = async { Response.success<VersionInfo>(null) }
        assertEquals(null, response.process())
    }

    @Test
    fun processError400() = runBlockingTest {
        var response = async {
            Response.error<VersionInfo>(400, ResponseBody.create(null, testMsg))
        }
        assertThrows(InvalidParametersException::class.java) {
            runBlockingTest { response.process() }
        }.apply {
            assertEquals(InvalidParametersException.Type.MISSING, type)
            assertEquals(testMsg, localizedMessage)
        }

        response = async {
            Response.error<VersionInfo>(400, ResponseBody.create(null, testMsg))
        }
        assertThrows(InvalidParametersException::class.java) {
            runBlockingTest { response.process(invalidParamsType = InvalidParametersException.Type.INVALID_PASSWORD) }
        }.apply {
            assertEquals(InvalidParametersException.Type.INVALID_PASSWORD, type)
            assertEquals(testMsg, localizedMessage)
        }
    }

    @Test
    fun processError401() = runBlockingTest {
        val response = async {
            Response.error<VersionInfo>(401, ResponseBody.create(null, testMsg))
        }
        assertThrows(AuthException::class.java) {
            runBlockingTest { response.process() }
        }.apply {
            assertEquals(AuthException.Reason.NEEDS_AUTH, reason)
            assertEquals(testMsg, localizedMessage)
        }
    }

    @Test
    fun processError403() = runBlockingTest {
        val response = async {
            Response.error<VersionInfo>(403, ResponseBody.create(null, testMsg))
        }
        assertThrows(AuthException::class.java) {
            runBlockingTest { response.process() }
        }.apply {
            assertEquals(AuthException.Reason.NEEDS_PERMISSION, reason)
            assertEquals(testMsg, localizedMessage)
        }
    }

    @Test
    fun processError404() = runBlockingTest {
        val statusCode = 404
        var response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, testMsg))
        }
        assertThrows(NotFoundException::class.java) {
            runBlockingTest { response.process() }
        }.apply {
            assertEquals(NotFoundException.Type.SONG, type)
            assertEquals(testMsg, localizedMessage)
        }

        response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, testMsg))
        }
        assertThrows(NotFoundException::class.java) {
            runBlockingTest { response.process(notFoundType = NotFoundException.Type.USER) }
        }.apply {
            assertEquals(NotFoundException.Type.USER, type)
            assertEquals(testMsg, localizedMessage)
        }
    }

    @Test
    fun processError409() = runBlockingTest {
        val response = async {
            Response.error<VersionInfo>(409, ResponseBody.create(null, ""))
        }
        assertThrows(UsernameTakenException::class.java) {
            runBlockingTest { response.process() }
        }.apply {
            assertEquals("Username already in use", localizedMessage)
        }
    }

    @Test
    fun processErrorOther() = runBlockingTest {
        var statusCode = 410
        var response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, ""))
        }
        assertThrows(ServerErrorException::class.java) {
            runBlockingTest { response.process() }
        }.apply {
            assertEquals(statusCode, code)
            assertEquals("Server error $statusCode", localizedMessage)
        }
        statusCode = 500
        response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, ""))
        }
        assertThrows(ServerErrorException::class.java) {
            runBlockingTest { response.process() }
        }.apply {
            assertEquals(statusCode, code)
            assertEquals("Server error $statusCode", localizedMessage)
        }
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun processNetworkError() = runBlockingTest {
        var response = async {
            throw IOException()
            Response.success<VersionInfo>(null)
        }
        assertThrows(IOException::class.java) { runBlockingTest { response.process() } }
        response = async {
            throw UsernameTakenException()
            Response.success<VersionInfo>(null)
        }
        assertThrows(UsernameTakenException::class.java) { runBlockingTest { response.process() } }
    }
}
