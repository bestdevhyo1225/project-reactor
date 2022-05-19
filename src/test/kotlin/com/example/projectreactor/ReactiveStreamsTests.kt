package com.example.projectreactor

import com.example.projectreactor.mono.Receiver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class ReactiveStreamsTests : DescribeSpec({

    describe("just 메서드") {
        it("테스트1") {
            val array = arrayOf("A", "B", "C")
            val flux: Flux<String> = Flux.just(*array).log()

            flux.subscribe({ consumer -> println("consumer: $consumer") },
                { errorConsumer -> println(errorConsumer) },
                { println("subscription is completed") })
        }
    }

    describe("from 메서드") {
        it("Mono.from(Flux) 테스트") {
            // given
            val array = arrayOf("A", "B", "C", "D", "E")
            val flux: Flux<String> = Flux.just(*array).log()

            // when
            val mono: Mono<String> = Mono.from(flux).map { data -> Receiver.send(data = data) }

            // then
            mono.subscribe { consumer -> println("consumer: $consumer") }
        }

        it("Flux.from(Flux) 테스트") {
            // given
            val array = arrayOf("A", "B", "C", "D", "E")
            val flux: Flux<String> = Flux.just(*array).log()

            // when
            val newFlux: Flux<String> = Flux.from(flux).map { data -> Receiver.send(data = data) }

            // then
            newFlux.subscribe { consumer -> println("consumer: $consumer") }
        }

        it("Flux.from(Mono) 테스트") {
            // given
            val mono: Mono<String> = Mono.just("A").log()

            // when
            val flux: Flux<String> = Flux.from(mono).map { data -> Receiver.send(data = data) }

            // then
            flux.subscribe { consumer -> println("consumer: $consumer") }
        }
    }
})
