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
package com.ivoberger.jmusicbot.client.di

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class BaseModule(private val logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC) {

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder().build()

    @Provides
    fun okHttpClient(): OkHttpClient.Builder = OkHttpClient.Builder().cache(null).addInterceptor(
        HttpLoggingInterceptor().apply { level = logLevel }
    ).retryOnConnectionFailure(true).connectTimeout(30, TimeUnit.SECONDS)

    @Provides
    @Named(NameKeys.BUILDER_RETROFIT_BASE)
    fun retrofitBuilder(okHttpClientBuilder: OkHttpClient.Builder, moshi: Moshi): Retrofit.Builder =
        Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .client(okHttpClientBuilder.build())
}
