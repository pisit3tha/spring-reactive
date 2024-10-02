package com.reactive.demo.filter

import com.reactive.demo.service.ReactiveService
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels

@Component
class ReactiveWebFilter(private val reactiveService: ReactiveService) : WebFilter {
    companion object {
        val EXCLUDE_URL_DEFAULT = listOf(
            "/**/health",
            "/**/info",
            "/**/metrics",
            "/**/prometheus",
            "/actuator*/**"
        )
        private val antPathMatcher: AntPathMatcher = AntPathMatcher()
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return if (!isExcludeUrl(exchange.request.path.value(), EXCLUDE_URL_DEFAULT)) {
            reactiveService.getService().flatMap {
                exchange.request.body.map { requestDataBuffer ->
                    var body = ""
                    ByteArrayOutputStream().also { outputStream ->
                        val bytes = ByteArray(requestDataBuffer.readableByteCount())
                        val byteBuffer: ByteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
                        requestDataBuffer.toByteBuffer(byteBuffer)
                        Channels.newChannel(outputStream).write(byteBuffer)
                        body = StreamUtils.copyToString(outputStream, Charsets.UTF_8)
                        outputStream.close()
                    }
                    body
                }.doOnNext {requestBody ->
                    println(requestBody)
                }.then(chain.filter(exchange))

            }
        } else {
            chain.filter(exchange)
        }
    }

    fun isExcludeUrl(requestPath: String, excludeUrls: List<String>) =
        excludeUrls.find { urlString ->
            antPathMatcher.match(urlString, requestPath)
        }?.isNotEmpty() ?: false
}
