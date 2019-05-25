package com.ivoberger.jmusicbot.client

import timber.log.Timber
import timber.log.Tree

class PrintTree(private val minPriority: Int = Timber.DEBUG) : Tree() {

    override fun isLoggable(priority: Int, tag: String?): Boolean = priority >= minPriority

    override fun performLog(priority: Int, tag: String?, throwable: Throwable?, message: String?) {
        println("$priority/$tag/$message")
        throwable?.printStackTrace()
    }
}
