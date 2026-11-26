package com.syed.slate.core

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * A sync failure in words the user can act on. A dropped or switching connection surfaces as a raw
 * "Unable to resolve host …: No address associated with hostname" (DNS) or a timeout; those are not errors in the
 * app, the change stays queued on the device and goes out on the next retry, so say exactly that instead.
 * Anything else (a server reply such as a 4xx/5xx) keeps its own message.
 */
fun friendlySyncError(t: Throwable?, fallback: String = "Couldn't reach the server"): String {
    val root = generateSequence(t) { it.cause }.lastOrNull() ?: t
    return when {
        t == null -> fallback
        t is UnknownHostException || root is UnknownHostException -> "No internet connection — saved on this device, will sync automatically"
        t is SocketTimeoutException || root is SocketTimeoutException -> "The connection timed out — will retry automatically"
        t is ConnectException || root is ConnectException || t is SSLException -> "Couldn't connect to the server — will retry automatically"
        t is IOException && (t.message?.contains("Unable to resolve host") == true) -> "No internet connection — saved on this device, will sync automatically"
        else -> t.message ?: fallback
    }
}
