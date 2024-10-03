package com.reactive.demo.filter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.reactive.demo.service.ReactiveService
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StreamUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.*

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
        private var logger = LoggerFactory.getLogger(ReactiveWebFilter::class.java)
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
       return if (!isExcludeUrl(exchange.request.path.value(), EXCLUDE_URL_DEFAULT)) {
                val modifiedRequest: ServerHttpRequest =
                    object : ServerHttpRequestDecorator(exchange.request) {
                        override fun getBody(): Flux<DataBuffer> {
                            return super.getBody().handle { dataBuffer, sink ->
                                ByteArrayOutputStream().also { outputStream ->
                                    try {
                                        val bytes = ByteArray(dataBuffer.readableByteCount())
                                        val byteBuffer: ByteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
                                        dataBuffer.toByteBuffer(byteBuffer)
                                        Channels.newChannel(outputStream).write(byteBuffer)
                                        val rawRequestBody = StreamUtils.copyToString(outputStream, Charsets.UTF_8)
                                       logger.info(rawRequestBody)
                                        sink.next( dataBuffer)
                                    } catch (e: Exception) {
                                        sink.error(Exception())
                                    } finally {
                                        outputStream.close()
                                    }
                                }
                            }
                        }
                    }
                val mutatedWebExchange = exchange.mutate().request(modifiedRequest).build()
                 chain.filter(mutatedWebExchange)

        } else {
             chain.filter(exchange)
        }
    }

    fun isExcludeUrl(requestPath: String, excludeUrls: List<String>) =
        excludeUrls.find { urlString ->
            antPathMatcher.match(urlString, requestPath)
        }?.isNotEmpty() ?: false
}
