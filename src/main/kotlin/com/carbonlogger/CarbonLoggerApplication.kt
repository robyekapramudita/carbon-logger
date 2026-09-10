package com.carbonlogger

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CarbonLoggerApplication

fun main(args: Array<String>) {
    runApplication<CarbonLoggerApplication>(*args)
}
