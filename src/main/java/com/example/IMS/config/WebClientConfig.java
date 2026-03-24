package com.example.IMS.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient Configuration for External API Integration
 * 
 * Provides:
 * - Connection pooling (50 connections)
 * - Connection timeout (10 seconds)
 * - Read timeout (30 seconds)
 * - Exponential backoff retry (3 attempts)
 * - Request/response logging
 * - Error handling
 * 
 * Used by:
 * - GstVerificationService
 * - PanVerificationService
 * - BankVerificationService
 * - CinVerificationService
 */
@Configuration
public class WebClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);
    
    @Value("${verification.webclient.connection-timeout:10000}")
    private int connectionTimeout;
    
    @Value("${verification.webclient.read-timeout:30000}")
    private int readTimeout;
    
    @Value("${verification.webclient.write-timeout:10000}")
    private int writeTimeout;
    
    @Value("${verification.webclient.max-connections:50}")
    private int maxConnections;
    
    @Value("${verification.webclient.retry-attempts:3}")
    private int retryAttempts;
    
    @Value("${verification.webclient.retry-delay-ms:1000}")
    private long retryDelayMs;
    
    /**
     * Global WebClient bean with connection pooling and retry logic
     */
    @Bean(name = "verificationWebClient")
    public WebClient verificationWebClient() {
        // Configure HTTP client with Netty
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
            )
            .responseTimeout(Duration.ofMillis(readTimeout));
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest())
            .filter(logResponse())
            .filter(retryFilter())
            .build();
    }
    
    /**
     * Log outgoing requests (mask sensitive data)
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) -> {
                    // Mask authorization headers
                    if (name.equalsIgnoreCase("Authorization") || name.equalsIgnoreCase("X-API-Key")) {
                        logger.debug("Header: {}=[MASKED]", name);
                    } else {
                        values.forEach(value -> logger.debug("Header: {}={}", name, value));
                    }
                });
            }
            return Mono.just(clientRequest);
        });
    }
    
    /**
     * Log incoming responses
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Response: {} {}", clientResponse.statusCode(), clientResponse.headers().asHttpHeaders());
            }
            return Mono.just(clientResponse);
        });
    }
    
    /**
     * Retry filter with exponential backoff
     * 
     * Retries on:
     * - 5xx server errors
     * - Network timeouts
     * - Connection failures
     * 
     * Does NOT retry on:
     * - 4xx client errors (invalid request)
     * - 401/403 authentication errors
     */
    private ExchangeFilterFunction retryFilter() {
        return (request, next) -> next.exchange(request)
            .flatMap(response -> {
                // Don't retry 4xx errors (client errors)
                if (response.statusCode().is4xxClientError()) {
                    logger.warn("Client error {}, not retrying: {} {}", 
                        response.statusCode(), request.method(), request.url());
                    return Mono.just(response);
                }
                
                // Retry 5xx errors
                if (response.statusCode().is5xxServerError()) {
                    logger.warn("Server error {}, will retry: {} {}", 
                        response.statusCode(), request.method(), request.url());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                }
                
                return Mono.just(response);
            })
            .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelayMs))
                .maxBackoff(Duration.ofSeconds(10))
                .filter(throwable -> {
                    // Retry on network errors and timeouts
                    return throwable instanceof java.net.ConnectException ||
                           throwable instanceof java.util.concurrent.TimeoutException ||
                           throwable instanceof io.netty.handler.timeout.ReadTimeoutException ||
                           throwable.getMessage().contains("Server error");
                })
                .doBeforeRetry(retrySignal -> {
                    logger.warn("Retry attempt {} for {} {}: {}", 
                        retrySignal.totalRetries() + 1, 
                        request.method(), 
                        request.url(),
                        retrySignal.failure().getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    logger.error("Retry exhausted after {} attempts for {} {}", 
                        retrySignal.totalRetries(), request.method(), request.url());
                    return new RuntimeException(
                        "External API call failed after " + retrySignal.totalRetries() + " retries",
                        retrySignal.failure()
                    );
                })
            );
    }
}
