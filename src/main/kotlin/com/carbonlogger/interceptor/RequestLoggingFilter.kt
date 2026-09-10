package com.carbonlogger.interceptor

import com.carbonlogger.config.LogConfig
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

const val LOGGING_ENABLED_ATTRIBUTE = "logging.enabled"

class RequestLoggingFilter(
    private val logConfig: LogConfig,
) : OncePerRequestFilter() {
    private val pathMatcher = AntPathMatcher()

    private fun matchesRule(
        rule: LogConfig.PredicateValue,
        currentPath: String,
        currentMethod: String,
    ): Boolean {
        val allowedMethods = rule.sanitizedMethods
        val isMethodMatch = allowedMethods.isNullOrEmpty() || allowedMethods.contains(currentMethod.trim().uppercase())

        if (!isMethodMatch) return false

        return pathMatcher.match(rule.path, currentPath)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        val method = request.method

        logConfig.predicate?.exclude?.let { excludeRule ->
            if (excludeRule.any {
                    matchesRule(it, path, method)
                }
            ) {
                return true
            }
        }

        logConfig.predicate?.include?.let { includeRule ->
            if (includeRule.none {
                    matchesRule(it, path, method)
                }
            ) {
                return true
            }
        }

        return false
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        request.setAttribute(LOGGING_ENABLED_ATTRIBUTE, Unit)
        filterChain.doFilter(request, response)
    }
}
