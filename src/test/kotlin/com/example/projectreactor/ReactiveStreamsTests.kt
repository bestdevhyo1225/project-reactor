package com.example.projectreactor

import io.kotest.core.spec.style.DescribeSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

internal class ReactiveStreamsTests : DescribeSpec({

    describe("just 메서드") {
        it("테스트1") {
            val flux: Flux<String> = Flux.just("A", "B", "C").log()

            flux.subscribe({ consumer -> println("consumer: $consumer") },
                { errorConsumer -> println(errorConsumer) },
                { println("subscription is completed") })
        }

        it("Mono.from(Flux) 테스트") {
            // given
            val flux: Flux<String> = Flux.just("A", "B", "C", "D", "E").log()

            // when
            val mono: Mono<String> = Mono.from(flux).map { data -> data.lowercase() }

            // then
            mono.subscribe { consumer -> println("consumer: $consumer") }
        }
    }
})
