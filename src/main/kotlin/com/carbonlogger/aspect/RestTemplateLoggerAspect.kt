package com.carbonlogger.aspect

import com.carbonlogger.config.LogConfig
import com.carbonlogger.utils.obfuscateValue
import com.carbonlogger.utils.shouldLog
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.http.HttpEntity
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity

@Aspect
class RestTemplateLoggerAspect(private val logConfig: LogConfig) {
    private val logger = KotlinLogging.logger {}

    private fun constructResponseData(result: Any?): String {
        val resultString =
            when (result) {
                is ResponseEntity<*> -> (result.body?.toString())?.let { "| Returned Data: $it" } ?: ""
                null -> ""
                else -> "| Returned Data: $result"
            }

        return logConfig.obfuscate?.jsonBodyFieldsPattern?.obfuscateValue(resultString) ?: resultString
    }

    private fun extractRequestBody(args: Array<Any?>): String =
        args
            .filterNotNull()
            .firstOrNull { it is HttpEntity<*> }
            ?.let { (it as HttpEntity<*>).body }
            .let { formatBody(it) }

    private fun formatBody(body: Any?): String =
        when (body) {
            null -> ""
            is String -> "| payload: $body"
            else -> "| payload: $body"
        }.let { value ->
            logConfig.obfuscate
                ?.jsonBodyFieldsPattern
                ?.obfuscateValue(value)
                ?: value
        }

    @Around("within(org.springframework.web.client.RestTemplate)")
    fun logOutboundCall(joinPoint: ProceedingJoinPoint): Any? {
        if (!shouldLog()) return joinPoint.proceed()

        val signature = joinPoint.signature as MethodSignature
        val methodName = signature.method.name

        val parameterNames = signature.parameterNames
        val argumentValues = joinPoint.args

        val inputParams =
            parameterNames?.zip(argumentValues)?.toMap() ?: emptyMap()

        val requestEntity = inputParams["requestEntity"] as HttpEntity<*>?
        val targetUrl = inputParams["url"] ?: (requestEntity as? RequestEntity<*>)?.url

        val requestBody = extractRequestBody(argumentValues)

        return try {
            logger.debug { "Request to: $methodName | URL: $targetUrl $requestBody" }

            val result = joinPoint.proceed()

            val logOutput = constructResponseData(result)

            logger.debug { "Response from: $methodName | URL: $targetUrl $logOutput" }

            result
        } catch (exception: Throwable) {
            logger.debug {
                "Response from: $methodName | URL: $targetUrl " +
                    "| Error: ${exception.javaClass.simpleName} - ${exception.message}"
            }
            throw exception
        }
    }
}
