package com.carbonlogger.config

import com.carbonlogger.aspect.ControllerLoggerAspect
import com.carbonlogger.aspect.RestTemplateLoggerAspect
import com.carbonlogger.interceptor.RequestLoggingFilter
import com.carbonlogger.interceptor.RestTemplateLoggerInterceptor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggingSystem
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@AutoConfiguration
@EnableConfigurationProperties(LogConfig::class)
@ConditionalOnProperty(
    prefix = "logging.http",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class CarbonLoggerAutoConfiguration(
    private val loggingSystem: LoggingSystem,
) : WebMvcConfigurer {
    init {
        loggingSystem.setLogLevel(
            "com.carbonlogger",
            LogLevel.DEBUG,
        )
    }

    @Bean
    fun requestLoggingFilter(logConfig: LogConfig): RequestLoggingFilter {
        return RequestLoggingFilter(logConfig)
    }

    @Bean
    fun restTemplateLoggerCustomizer(logConfig: LogConfig): RestTemplateCustomizer =
        RestTemplateCustomizer { restTemplate ->
            restTemplate.interceptors.add(RestTemplateLoggerInterceptor(logConfig))
        }

    @Bean
    fun restTemplateLoggerAspect(logConfig: LogConfig) = RestTemplateLoggerAspect(logConfig)

    @Bean
    fun controllerLoggerAspect(logConfig: LogConfig) = ControllerLoggerAspect(logConfig)
}
