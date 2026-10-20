package com.setec.hms.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class JacksonConfig {

  @Bean
  SimpleModule hibernateProxyModule() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(
        HibernateProxy.class,
        new JsonSerializer<>() {
          @Override
          public void serialize(
              HibernateProxy proxy, JsonGenerator generator, SerializerProvider serializers)
              throws IOException {
            var initializer = proxy.getHibernateLazyInitializer();
            if (initializer.isUninitialized()) {
              generator.writeStartObject();
              generator.writeObjectField("id", initializer.getInternalIdentifier());
              generator.writeEndObject();
            } else {
              serializers.defaultSerializeValue(initializer.getImplementation(), generator);
            }
          }
        });
    return module;
  }
}
