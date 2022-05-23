package com.example.projectreactor

import io.kotest.core.spec.style.DescribeSpec
import reactor.core.publisher.Flux
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

        it("문자열 변환 테스트") {
            Flux.just("a", "bc", "def", "ghij")
                .log()
                .map { it.length }
                .subscribe { consumer -> println("consumer = $consumer") }
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

        it("Flux<Int> -> Flux<Int> 변환 테스트") {
            Flux.just(1, 2, 3)
                .flatMap {
                    println("----- flatMap -----")
                    Flux.range(1, it)
                }
                .subscribe { consumer -> println("consumer = $consumer") }
        }

        it("Flux<Int> -> Mono<Int> 변환 테스트") {
            Flux.just(1, 2, 3)
                .flatMap {
                    println("----- flatMap -----")
                    Mono.just(it * 2)
                }
                .subscribe { consumer -> println("consumer = $consumer") }
        }
    }
})
