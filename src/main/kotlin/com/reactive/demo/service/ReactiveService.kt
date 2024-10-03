package com.reactive.demo.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactiveService {

    fun getService():Mono<String>{
        return Mono.just("test")
    }
}