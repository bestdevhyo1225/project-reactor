package com.example.projectreactor

import com.example.projectreactor.mono.Receiver
import io.kotest.core.spec.style.DescribeSpec
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

    describe("defer 메서드") {
        var data = 1

        fun getData(caller: String): Int {
            println("caller = $caller")
            return data
        }

        it("Mono.defer 와 Mono.just 의 차이점 테스트") {
            // 인스턴스화 되면, caller = just 값을 출력한다.
            val monoJust: Mono<Int> = Mono.just(getData(caller = "just"))
            // 구독시에 caller = defer 값을 출력한다.
            val monoDefer: Mono<Int> = Mono.defer { Mono.just(getData(caller = "defer")) }

            println("------- start -------")
            monoJust.subscribe { consumer -> println("consumer: $consumer") }
            /**
             * 구독시에 캡쳐된다는 특징이 있다. 따라서 caller 값의 출력문이 구독시에 캡쳐되며,
             * consumer 값도 구독시에 캡쳐되기 때문에 1를 출력하게 된다.
             */
            monoDefer.subscribe { consumer -> println("consumer: $consumer") }

            println("------- data change -------")
            data = 2
            /*
            * 인스턴스화 시점에 이미 consumer 값은 1이 였기 때문에 data 값이 2로 변경되어도 영향 없이 1의 값을 출력한다.
            * */
            monoJust.subscribe { consumer -> println("consumer: $consumer") }
            /**
             * 구독시에 캡쳐된다는 특징이 있다. 따라서 caller 값의 출력문이 구독시에 캡쳐되며,
             * consumer 값도 구독시에 캡쳐되기 때문에 변경된 값인 2를 출력하게 된다.
             */
            monoDefer.subscribe { consumer -> println("consumer: $consumer") }
        }
    }
})
