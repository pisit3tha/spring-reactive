package com.reactive.demo.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class ReactiveController {
    @PostMapping("/get")
    fun get(@RequestBody req : String ):Mono<String>{
        return Mono.just(req)
    }
}