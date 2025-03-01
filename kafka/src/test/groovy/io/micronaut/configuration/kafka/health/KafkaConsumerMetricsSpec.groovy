package io.micronaut.configuration.kafka.health

import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.configuration.kafka.AbstractEmbeddedServerSpec
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.configuration.metrics.management.endpoint.MetricsEndpoint
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.messaging.annotation.MessageHeader
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Shared

import static io.micronaut.configuration.kafka.annotation.OffsetReset.EARLIEST
import static io.micronaut.configuration.kafka.config.AbstractKafkaConfiguration.EMBEDDED_TOPICS

class KafkaConsumerMetricsSpec extends AbstractEmbeddedServerSpec {

    @Shared @AutoCleanup HttpClient httpClient
    @Shared MeterRegistry meterRegistry

    protected Map<String, Object> getConfiguration() {
        super.configuration +
                ["micrometer.metrics.enabled" : true,
                 'endpoints.metrics.sensitive': false,
                 (EMBEDDED_TOPICS)            : ["words-metrics", "words", "books", "words-records", "books-records"]]
    }

    void setupSpec() {
        httpClient = context.createBean(
                HttpClient,
                embeddedServer.URL,
                new DefaultHttpClientConfiguration(followRedirects: false)
        )
        meterRegistry = context.getBean(MeterRegistry)
    }

    void "test simple consumer"() {
        given:
        context.containsBean(MeterRegistry)
        context.containsBean(MetricsEndpoint)
        context.containsBean(MyConsumerMetrics)
        context.containsBean(KafkaHealthIndicator)

        expect:
        conditions.eventually {
            HttpResponse<Map> response = Mono.from(httpClient.exchange("/metrics", Map)).block()
            Map result = response.body()
            result.names.contains("kafka.consumer.bytes-consumed-total")
            !result.names.contains("kafka.consumer.record-error-rate")
            !result.names.contains("kafka.producer.count")
            !result.names.contains("kafka.producer.record-error-rate")
            !result.names.contains("kafka.producer.bytes-consumed-total")
            !result.names.contains("kafka.count")
        }
    }

    @Requires(property = 'spec.name', value = 'KafkaConsumerMetricsSpec')
    @KafkaListener(offsetReset = EARLIEST)
    static class MyConsumerMetrics {
        int wordCount
        String lastTopic

        @Topic("words-metrics")
        void countWord(String sentence, @MessageHeader String topic) {
            wordCount += sentence.split(/\s/).size()
            lastTopic = topic
        }
    }
}
