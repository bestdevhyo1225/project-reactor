package com.example.projectreactor.fruitBasket

data class FruitDto(
    val distinctFruits: List<String>,
    val countFruits: Map<String, Long>,
)
