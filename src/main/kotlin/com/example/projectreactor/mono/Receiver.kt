package com.example.projectreactor.mono

object Receiver {
    fun send(data: String): String {
        println("data = $data")
        val convertedData = data.lowercase()
        println("convertedData = $convertedData")
        return convertedData
    }
}
