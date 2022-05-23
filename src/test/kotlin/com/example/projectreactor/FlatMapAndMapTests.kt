package com.example.projectreactor

import io.kotest.core.spec.style.DescribeSpec
import reactor.core.publisher.Mono

internal class FlatMapAndMapTests : DescribeSpec({

    describe("map 메서드는") {
        context("A -> B 함수를 적용하면") {
            it("Mono<A> -> Mono<B> 결과가 나온다.") {
                val monoResult: Mono<String> = Mono.just("apple")
                    .log()
                    .map { it.uppercase() }

                monoResult.subscribe { consumer -> println("consumer = $consumer") }
            }
        }

        context("A -> Mono<B> 함수를 적용하면") {
            it("Mono<A> -> Mono<Mono<B>> 결과가 나온다.") {
                val monoResult: Mono<Mono<String>> = Mono.just("apple")
                    .log()
                    .map { Mono.just(it.uppercase()) }

                monoResult.subscribe { consumer ->
                    consumer.subscribe { innerConsumer -> println("consumer = $innerConsumer") }
                }
            }
        }
    }

    describe("flatMap 메서드는") {
        context("A -> Mono<B> 함수를 적용하면") {
            it("Mono<A> -> Mono<B> 결과가 나온다.") {
                val monoResult: Mono<String> = Mono.just("apple")
                    .log()
                    .flatMap { Mono.just(it.uppercase()) }

                monoResult.subscribe { consumer -> println("consumer = $consumer") }
            }
        }
    }
})
