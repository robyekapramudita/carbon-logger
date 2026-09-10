package com.carbonlogger.interceptor

import com.carbonlogger.config.LogConfig
import com.carbonlogger.utils.shouldLog
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.IOException

class RestTemplateLoggerInterceptor(
    private val logConfig: LogConfig,
) : ClientHttpRequestInterceptor {
    private val logger = KotlinLogging.logger {}

    private fun logOutgoingRequest(request: HttpRequest) {
        logger.debug {
            "Outgoing Request: \n${request.method} ${request.uri}" +
                "\n${request.headers.toMultiLineString()}"
        }
    }

    private fun logIncomingResponse(
        response: ClientHttpResponse,
        duration: Long,
    ) {
        logger.debug {
            "Incoming Response: \nDuration: $duration ms" +
                "\n${response.statusCode} ${response.statusText}" +
                "\n${response.headers.toMultiLineString()}"
        }
    }

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        if (!logger.isDebugEnabled || !shouldLog()) return execution.execute(request, body)

        val startTime = System.currentTimeMillis()

        logOutgoingRequest(request)

        val response = execution.execute(request, body)
        val duration = System.currentTimeMillis() - startTime

        logIncomingResponse(response, duration)

        return response
    }

    fun HttpHeaders.toMultiLineString(): String =
        entries.joinToString(
            separator = "\n",
        ) {
            val headerValue =
                if (logConfig.obfuscate?.sanitizedHeaders?.contains(it.key.trim().lowercase()) == true) {
                    "XXX"
                } else {
                    it.value.joinToString(", ")
                }
            "${it.key}: $headerValue"
        }
}
