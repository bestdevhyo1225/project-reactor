package com.example.projectreactor.fruitBasket

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FruitBasketTests : DescribeSpec({

    describe("바구니 속 과일을 중복 없이 종류로 나누고, 각 종류별 개수를 나눈다.") {
        val fruits1: List<String> = listOf("kiwi", "orange", "lemon", "orange", "lemon", "kiwi")
        val fruits2: List<String> = listOf("banana", "lemon", "lemon", "kiwi")
        val fruits3: List<String> = listOf("strawberry", "orange", "lemon", "grape", "strawberry")
        val fruits: List<List<String>> = listOf(fruits1, fruits2, fruits3)
        val fluxFruits: Flux<List<String>> = Flux.fromIterable(fruits)

        context("첫 번째 로직 구현") {
            it("동기 / 블록킹 방식으로 결과를 구한다.") {
                fluxFruits.concatMap { fruits ->
                    val monoDistinctFruits: Mono<List<String>> = Flux.fromIterable(fruits)
                        .log()
                        .distinct()
                        .collectList()
                    val monoCountFruits: Mono<Map<String, Long>> = Flux.fromIterable(fruits)
                        .log()
                        .groupBy { fruit -> fruit }
                        .concatMap { groupedFruits ->
                            groupedFruits
                                .count()
                                .map { count ->
                                    val fruitCountsMap = mutableMapOf<String, Long>()
                                    fruitCountsMap[groupedFruits.key()] = count
                                    fruitCountsMap.toMap()
                                }// 각 과일별로 개수를 Map으로 반환
                        } // concatMap으로 순서 보장
                        .reduce { accumulatedMap, currentMap ->
                            val currentAccumulatedMap: MutableMap<String, Long> = accumulatedMap.toMutableMap()
                            currentAccumulatedMap.putAll(currentMap)
                            currentAccumulatedMap
                        }

                    Flux.zip(monoDistinctFruits, monoCountFruits) { distinctFruits, countFruits ->
                        FruitDto(
                            distinctFruits = distinctFruits,
                            countFruits = countFruits
                        )
                    }
                }.subscribe { consumer -> println("consumer: $consumer") }
            }
        }

        context("두 번째 로직 구현 - 첫 번째 로직 개선") {
            it("subscribeOn(Schedulers.parallel())를 사용하여, 비동기 방식으로 결과를 구한다") {
                withContext(Dispatchers.IO) {
                    val threadCount = 2
                    val countDownLatch = CountDownLatch(threadCount)
                    fluxFruits.concatMap { fruits ->
                        val monoDistinctFruits: Mono<List<String>> = Flux.fromIterable(fruits)
                            .log()
                            .distinct()
                            .collectList()
                            .subscribeOn(Schedulers.parallel())
                        val monoCountFruits: Mono<Map<String, Long>> = Flux.fromIterable(fruits)
                            .log()
                            .groupBy { fruit -> fruit }
                            .concatMap { groupedFruits ->
                                groupedFruits
                                    .count()
                                    .map { count ->
                                        val fruitCountsMap = mutableMapOf<String, Long>()
                                        fruitCountsMap[groupedFruits.key()] = count
                                        fruitCountsMap.toMap()
                                    }// 각 과일별로 개수를 Map으로 반환
                            } // concatMap으로 순서 보장
                            .reduce { accumulatedMap, currentMap ->
                                val currentAccumulatedMap: MutableMap<String, Long> = accumulatedMap.toMutableMap()
                                currentAccumulatedMap.putAll(currentMap)
                                currentAccumulatedMap
                            }
                            .subscribeOn(Schedulers.parallel())

                        Flux.zip(monoDistinctFruits, monoCountFruits) { distinctFruits, countFruits ->
                            FruitDto(
                                distinctFruits = distinctFruits,
                                countFruits = countFruits
                            )
                        }
                    }.subscribe(
                        { consumer -> println("consumer: $consumer") },
                        { errorConsumer ->
                            println(errorConsumer)
                            countDownLatch.countDown()
                        },
                        {
                            println("subscription is completed")
                            countDownLatch.countDown()
                        }
                    )
                    countDownLatch.await(1, TimeUnit.SECONDS)
                }
            }
        }

        context("세 번째 로직 구현 - 두 번째 로직 개선") {
            context("Connectable Flux 변환 과정을 통해 Cold Flux 에서 Hot Flux로 변경하는데") {
                it("publish().autoConnect(2)에 의해서 통해 2개 구독자가 구독하면, 자동으로 Hot Flux를 만든다.") {
                    fluxFruits.concatMap { fruits ->
                        val baseMonoFruits: Flux<String> = Flux.fromIterable(fruits)
                            .log()
                            .publish()
                            .autoConnect(2)
                        val monoDistinctFruits: Mono<List<String>> = baseMonoFruits
                            .distinct()
                            .collectList()
                        val monoCountFruits: Mono<Map<String, Long>> = baseMonoFruits
                            .groupBy { fruit -> fruit }
                            .concatMap { groupedFruits ->
                                groupedFruits
                                    .count()
                                    .map { count ->
                                        val fruitCountsMap = mutableMapOf<String, Long>()
                                        fruitCountsMap[groupedFruits.key()] = count
                                        fruitCountsMap.toMap()
                                    }// 각 과일별로 개수를 Map으로 반환
                            } // concatMap으로 순서 보장
                            .reduce { accumulatedMap, currentMap ->
                                val currentAccumulatedMap: MutableMap<String, Long> = accumulatedMap.toMutableMap()
                                currentAccumulatedMap.putAll(currentMap)
                                currentAccumulatedMap
                            }

                        Flux.zip(monoDistinctFruits, monoCountFruits) { distinctFruits, countFruits ->
                            FruitDto(
                                distinctFruits = distinctFruits,
                                countFruits = countFruits
                            )
                        }
                    }.subscribe { consumer -> println("consumer: $consumer") }
                }
            }
        }

        context("네 번째 로직 구현 - 세 번째 로직 개선") {
            it("Hot Flux 이후에 각 스트림만 비동기로 처리한다.") {
                withContext(Dispatchers.IO) {
                    val threadCount = 2
                    val countDownLatch = CountDownLatch(threadCount)
                    fluxFruits.concatMap { fruits ->
                        val baseMonoFruits: Flux<String> = Flux.fromIterable(fruits)
                            .log()
                            .publish()
                            .autoConnect(2)
                        val monoDistinctFruits: Mono<List<String>> = baseMonoFruits
                            .distinct()
                            .collectList()
                            .log()
                            .subscribeOn(Schedulers.parallel())
                        val monoCountFruits: Mono<Map<String, Long>> = baseMonoFruits
                            .groupBy { fruit -> fruit }
                            .concatMap { groupedFruits ->
                                groupedFruits
                                    .count()
                                    .map { count ->
                                        val fruitCountsMap = mutableMapOf<String, Long>()
                                        fruitCountsMap[groupedFruits.key()] = count
                                        fruitCountsMap.toMap()
                                    }// 각 과일별로 개수를 Map으로 반환
                            } // concatMap으로 순서 보장
                            .reduce { accumulatedMap, currentMap ->
                                val currentAccumulatedMap: MutableMap<String, Long> = accumulatedMap.toMutableMap()
                                currentAccumulatedMap.putAll(currentMap)
                                currentAccumulatedMap
                            }
                            .log()
                            .subscribeOn(Schedulers.parallel())

                        Flux.zip(monoDistinctFruits, monoCountFruits) { distinctFruits, countFruits ->
                            FruitDto(
                                distinctFruits = distinctFruits,
                                countFruits = countFruits
                            )
                        }
                    }.subscribe(
                        { consumer -> println("consumer: $consumer") },
                        { errorConsumer ->
                            println(errorConsumer)
                            countDownLatch.countDown()
                        },
                        {
                            println("subscription is completed")
                            countDownLatch.countDown()
                        }
                    )
                    countDownLatch.await(1, TimeUnit.SECONDS)
                }
            }
        }

        context("다섯 번째 로직 구현 - 네 번째 로직 개선") {
            it("publishOn(Schedulers.parallel()) 적용하기") {
                withContext(Dispatchers.IO) {
                    val threadCount = 2
                    val countDownLatch = CountDownLatch(threadCount)
                    fluxFruits.concatMap { fruits ->
                        val baseMonoFruits: Flux<String> = Flux.fromIterable(fruits)
                            .log()
                            .publish()
                            .autoConnect(2)
                        val monoDistinctFruits: Mono<List<String>> = baseMonoFruits
                            .publishOn(Schedulers.parallel())
                            .distinct()
                            .collectList()
                            .log()
                        val monoCountFruits: Mono<Map<String, Long>> = baseMonoFruits
                            .publishOn(Schedulers.parallel())
                            .groupBy { fruit -> fruit }
                            .concatMap { groupedFruits ->
                                groupedFruits
                                    .count()
                                    .map { count ->
                                        val fruitCountsMap = mutableMapOf<String, Long>()
                                        fruitCountsMap[groupedFruits.key()] = count
                                        fruitCountsMap.toMap()
                                    }// 각 과일별로 개수를 Map으로 반환
                            } // concatMap으로 순서 보장
                            .reduce { accumulatedMap, currentMap ->
                                val currentAccumulatedMap: MutableMap<String, Long> = accumulatedMap.toMutableMap()
                                currentAccumulatedMap.putAll(currentMap)
                                currentAccumulatedMap
                            }
                            .log()

                        Flux.zip(monoDistinctFruits, monoCountFruits) { distinctFruits, countFruits ->
                            FruitDto(
                                distinctFruits = distinctFruits,
                                countFruits = countFruits
                            )
                        }
                    }.subscribe(
                        { consumer -> println("consumer: $consumer") },
                        { errorConsumer ->
                            println(errorConsumer)
                            countDownLatch.countDown()
                        },
                        {
                            println("subscription is completed")
                            countDownLatch.countDown()
                        }
                    )
                    countDownLatch.await(1, TimeUnit.SECONDS)
                }
            }
        }

        context("여섯 번째 로직 구현 - 다섯 번째 로직 개선") {
            it("baseFruitsMono를 가져올때, subscribeOn(Schedulers.single()) 적용하기") {
                withContext(Dispatchers.IO) {
                    val threadCount = 2
                    val countDownLatch = CountDownLatch(threadCount)
                    fluxFruits.concatMap { fruits ->
                        val baseMonoFruits: Flux<String> = Flux.fromIterable(fruits)
                            .log()
                            .publish()
                            .autoConnect(2)
                            .subscribeOn(Schedulers.single())
                        val monoDistinctFruits: Mono<List<String>> = baseMonoFruits
                            .publishOn(Schedulers.parallel())
                            .distinct()
                            .collectList()
                            .log()
                        val monoCountFruits: Mono<Map<String, Long>> = baseMonoFruits
                            .publishOn(Schedulers.parallel())
                            .groupBy { fruit -> fruit }
                            .concatMap { groupedFruits ->
                                groupedFruits
                                    .count()
                                    .map { count ->
                                        val fruitCountsMap = mutableMapOf<String, Long>()
                                        fruitCountsMap[groupedFruits.key()] = count
                                        fruitCountsMap.toMap()
                                    }// 각 과일별로 개수를 Map으로 반환
                            } // concatMap으로 순서 보장
                            .reduce { accumulatedMap, currentMap ->
                                val currentAccumulatedMap: MutableMap<String, Long> = accumulatedMap.toMutableMap()
                                currentAccumulatedMap.putAll(currentMap)
                                currentAccumulatedMap
                            }
                            .log()

                        Flux.zip(monoDistinctFruits, monoCountFruits) { distinctFruits, countFruits ->
                            FruitDto(
                                distinctFruits = distinctFruits,
                                countFruits = countFruits
                            )
                        }
                    }.subscribe(
                        { consumer -> println("consumer: $consumer") },
                        { errorConsumer ->
                            println(errorConsumer)
                            countDownLatch.countDown()
                        },
                        {
                            println("subscription is completed")
                            countDownLatch.countDown()
                        }
                    )
                    countDownLatch.await(1, TimeUnit.SECONDS)
                }
            }
        }
    }
})
