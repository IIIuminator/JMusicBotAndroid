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
package com.ivoberger.jmusicbot.client.utils

import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.Response
import timber.log.Timber
import timber.log.error

@ExperimentalCoroutinesApi
@Throws(
    InvalidParametersException::class,
    AuthException::class,
    NotFoundException::class,
    ServerErrorException::class
)
internal inline fun <reified T> Response<T>.process(
    successCodes: List<Int> = listOf(200, 201, 204),
    errorCodes: Map<Int, Exception> = mapOf(),
    notFoundType: NotFoundException.Type = NotFoundException.Type.SONG,
    invalidParamsType: InvalidParametersException.Type = InvalidParametersException.Type.MISSING
): T? {
    return when (code()) {
        in successCodes -> body()
        in errorCodes -> throw errorCodes.getValue(code())
        400 -> throw InvalidParametersException(invalidParamsType, errorBody()!!.string())
        401 -> throw AuthException(AuthException.Reason.NEEDS_AUTH, errorBody()!!.string())
        403 -> throw AuthException(
            AuthException.Reason.NEEDS_PERMISSION, errorBody()!!.string()
        )
        404 -> throw NotFoundException(notFoundType, errorBody()!!.string())
        409 -> throw UsernameTakenException()
        else -> {
            Timber.error { "Server Error: ${errorBody()?.string()}, ${code()}" }
            throw ServerErrorException(code())
        }
    }
}
