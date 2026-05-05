package com.devscion.lingualink

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform