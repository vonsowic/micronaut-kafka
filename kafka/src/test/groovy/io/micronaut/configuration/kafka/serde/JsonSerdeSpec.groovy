package io.micronaut.configuration.kafka.serde

import groovy.transform.EqualsAndHashCode
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class JsonSerdeSpec extends Specification {

    void "test json serde"() {
        given:
        ApplicationContext context = ApplicationContext.run()

        when:
        JsonSerde<Book> serde = context.createBean(JsonSerde, Book)
        Book book = new Book(title: "The Stand")
        String json = '{"title":"The Stand"}'

        then:
        new String(serde.serialize("foo", book)) == json
        serde.deserialize("foo", json.bytes) == book

        cleanup:
        context.close()
    }

    void "test json object serde"() {
        given:
        ApplicationContext context = ApplicationContext.run()

        when:
        JsonObjectSerde<Book> serde = context.createBean(JsonObjectSerde, Book)
        Book book = new Book(title: "The Stand")
        String json = '{"title":"The Stand"}'

        then:
        new String(serde.serialize("foo", book)) == json
        serde.deserialize("foo", json.bytes) == book

        cleanup:
        context.close()
    }

    @EqualsAndHashCode
    static class Book {
        String title
    }
}
