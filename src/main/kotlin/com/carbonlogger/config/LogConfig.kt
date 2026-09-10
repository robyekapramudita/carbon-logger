package com.carbonlogger.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("logbook")
data class LogConfig(
    val enabled: Boolean = true,
    val predicate: Predicate?,
    val obfuscate: Obfuscate?,
) {
    data class Predicate(
        val exclude: List<PredicateValue>?,
        val include: List<PredicateValue>?,
    )

    data class PredicateValue(
        val path: String,
        val methods: Set<String>?,
    ) {
        val sanitizedMethods: Set<String>? = methods?.map { it.trim().uppercase() }?.toSet()
    }

    data class Obfuscate(
        val headers: Set<String>?,
        val jsonBodyFields: Set<String>?,
    ) {
        val sanitizedHeaders: Set<String>? = headers?.map { it.trim().lowercase() }?.toSet()
        val jsonBodyFieldsPattern: Regex? =
            jsonBodyFields?.let { keys ->
                val keyPattern = keys.joinToString("|") { Regex.escape(it) }
                """(("?(?:$keyPattern)"?\s*(?:=|:\s*")))([^",)}]+|[^"]*)(?="|[,)}])"""
            }?.toRegex()
    }
}
