package ru.cheftable

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChefTableApplication

fun main(args: Array<String>) {
    runApplication<ChefTableApplication>(*args)
}
