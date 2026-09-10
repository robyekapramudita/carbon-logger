package com.carbonlogger.aspect

import com.carbonlogger.config.LogConfig
import com.carbonlogger.utils.obfuscateValue
import com.carbonlogger.utils.shouldLog
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
class ControllerLoggerAspect(
    private val logConfig: LogConfig,
) {
    private val logger = KotlinLogging.logger {}

    private fun logIncomingRequest(
        methodAndPath: String,
        payload: String?,
        headers: String,
    ) {
        val payloadString = if (payload != null) "\n\n${payload.obfuscatePayload()}" else ""
        logger.debug { "Incoming Request: \n$methodAndPath \n$headers$payloadString" }
    }

    private fun logOutgoingResponse(
        methodAndPath: String,
        message: String?,
        duration: Long,
    ) {
        val response =
            (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.response
        val messageString = if (message != null) "\n\n${message.obfuscatePayload()}" else ""
        logger.debug { "Outgoing Response: \n$methodAndPath\nDuration: $duration ms\nStatus: ${response?.status}$messageString" }
    }

    private fun getRequestAttributes(): Pair<String, String> {
        val request =
            (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request ?: return Pair("UNKNOWN", "")
        val headers =
            buildMap {
                request.headerNames.asIterator().forEach { name ->
                    val sanitizedName = name.trim().lowercase()
                    val headerValue =
                        if (logConfig.obfuscate?.sanitizedHeaders?.contains(sanitizedName) == true) {
                            "XXX"
                        } else {
                            request.getHeader(name)
                        }
                    put(name, headerValue)
                }
            }.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }

        return Pair("${request.method} ${request.requestURI}", headers)
    }

    @Around(
        "@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)",
    )
    fun logResponse(joinPoint: ProceedingJoinPoint): Any? {
        if (!shouldLog()) return joinPoint.proceed()

        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val requestBodyIndex =
            method.parameterAnnotations.indexOfFirst { annotations ->
                annotations.any { it is RequestBody }
            }

        val (methodAndPath, headers) = getRequestAttributes()

        val payload =
            if (requestBodyIndex != -1) {
                joinPoint.args[requestBodyIndex].toString()
            } else {
                null
            }

        var message: String? = null
        val startTime = System.currentTimeMillis()

        return try {
            logIncomingRequest(
                methodAndPath = methodAndPath,
                payload = payload,
                headers = headers,
            )

            val result = joinPoint.proceed()

            message = result?.toString()

            result
        } catch (exception: Throwable) {
            message = "${exception.javaClass.simpleName} - ${exception.message}"
            throw exception
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logOutgoingResponse(methodAndPath = methodAndPath, message = message, duration = duration)
        }
    }

    fun String.obfuscatePayload() = logConfig.obfuscate?.jsonBodyFieldsPattern?.obfuscateValue(this) ?: this
}
