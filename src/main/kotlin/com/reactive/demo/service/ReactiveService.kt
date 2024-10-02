package com.reactive.demo.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReactiveService {

    fun getService():Mono<Void>{
        return Mono.empty()
    }
}