package com.carbonlogger.utils

import com.carbonlogger.interceptor.LOGGING_ENABLED_ATTRIBUTE
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

fun Regex.obfuscateValue(input: String): String =
    this.replace(input) {
        "${it.groupValues[1]}XXX"
    }

fun shouldLog(): Boolean {
    val attributes = RequestContextHolder.getRequestAttributes()
    val request =
        (attributes as? ServletRequestAttributes)?.request
            ?: return false

    return request.getAttribute(LOGGING_ENABLED_ATTRIBUTE) != null
}
