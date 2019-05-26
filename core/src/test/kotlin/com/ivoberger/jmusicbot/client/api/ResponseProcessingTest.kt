package com.ivoberger.jmusicbot.client.api

import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.VersionInfo
import com.ivoberger.jmusicbot.client.utils.process
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.Response
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.io.IOException

@ExperimentalCoroutinesApi
internal class ResponseProcessingTest {

    private val testMsg = "testMsg"
    private val versionInfo = VersionInfo(
        "0.1.0", VersionInfo.ImplementationInfo("testImpl", "v1.0.0")
    )

    @Test
    fun processResponseNotNull() = runBlockingTest {
        val response = async { Response.success(versionInfo) }
        expectThat(response.process()).isEqualTo(versionInfo)
    }

    @Test
    fun processCustomCodes() = runBlockingTest {
        var statusCode = 200
        var response = async { Response.success(versionInfo) }
        expectThrows<IllegalStateException> {
            response.process(listOf(), mapOf(statusCode to IllegalStateException()))
        }
        statusCode = 400
        response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, ""))
        }
        expectThat(response.process(listOf(statusCode))).isNull()
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
        expectThrows<InvalidParametersException> { response.process() }.and {
            get { type }.isEqualTo(InvalidParametersException.Type.MISSING)
            get { localizedMessage }.isEqualTo(testMsg)
        }

        response = async {
            Response.error<VersionInfo>(400, ResponseBody.create(null, testMsg))
        }
        expectThrows<InvalidParametersException> {
            response.process(invalidParamsType = InvalidParametersException.Type.INVALID_PASSWORD)
        }.and {
            get { type }.isEqualTo(InvalidParametersException.Type.INVALID_PASSWORD)
            get { localizedMessage }.isEqualTo(testMsg)
        }
    }

    @Test
    fun processError401() = runBlockingTest {
        val response = async {
            Response.error<VersionInfo>(401, ResponseBody.create(null, testMsg))
        }
        expectThrows<AuthException> { response.process() }.and {
            get { reason }.isEqualTo(AuthException.Reason.NEEDS_AUTH)
            get { localizedMessage }.isEqualTo(testMsg)
        }
    }

    @Test
    fun processError403() = runBlockingTest {
        val response = async {
            Response.error<VersionInfo>(403, ResponseBody.create(null, testMsg))
        }
        expectThrows<AuthException> { response.process() }.and {
            get { reason }.isEqualTo(AuthException.Reason.NEEDS_PERMISSION)
            get { localizedMessage }.isEqualTo(testMsg)
        }
    }

    @Test
    fun processError404() = runBlockingTest {
        val statusCode = 404
        var response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, testMsg))
        }
        expectThrows<NotFoundException> { response.process() }.and {
            get { type }.isEqualTo(NotFoundException.Type.SONG)
            get { localizedMessage }.isEqualTo(testMsg)
        }

        response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, testMsg))
        }
        expectThrows<NotFoundException> { response.process(notFoundType = NotFoundException.Type.USER) }.and {
            get { type }.isEqualTo(NotFoundException.Type.USER)
            get { localizedMessage }.isEqualTo(testMsg)
        }
    }

    @Test
    fun processError409() = runBlockingTest {
        val response = async {
            Response.error<VersionInfo>(409, ResponseBody.create(null, ""))
        }
        expectThrows<UsernameTakenException> { response.process() }
            .get { localizedMessage }.isEqualTo("Username already in use")
    }

    @Test
    fun processErrorOther() = runBlockingTest {
        var statusCode = 410
        var response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, ""))
        }
        expectThrows<ServerErrorException> { response.process() }.and {
            get { code }.isEqualTo(statusCode)
            get { localizedMessage }.isEqualTo("Server error $statusCode")
        }
        statusCode = 500
        response = async {
            Response.error<VersionInfo>(statusCode, ResponseBody.create(null, ""))
        }
        expectThrows<ServerErrorException> { response.process() }.and {
            get { code }.isEqualTo(statusCode)
            get { localizedMessage }.isEqualTo("Server error $statusCode")
        }
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun processNetworkError() = runBlockingTest {
        var response = async {
            throw IOException()
            Response.success<VersionInfo>(null)
        }
        expectThrows<IOException> { response.process() }
        response = async {
            throw UsernameTakenException()
            Response.success<VersionInfo>(null)
        }
        expectThrows<UsernameTakenException> { response.process() }
    }
}
