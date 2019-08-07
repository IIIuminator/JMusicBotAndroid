package com.ivoberger.jmusicbot.client.testUtils

import timber.log.Timber
import timber.log.Tree
import java.util.Date

class PrintTree(private val minPriority: Int = Timber.DEBUG) : Tree() {

    override fun isLoggable(priority: Int, tag: String?): Boolean = priority >= minPriority

    override fun performLog(priority: Int, tag: String?, throwable: Throwable?, message: String?) {
        println("$priority/${Date().time}/$message")
        throwable?.printStackTrace()
    }
}
