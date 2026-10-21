package org.example.ex5

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform