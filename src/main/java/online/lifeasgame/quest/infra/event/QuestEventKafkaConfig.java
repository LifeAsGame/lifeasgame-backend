package online.lifeasgame.quest.infra.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.quest.domain.event.QuestEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(QuestEventProperties.class)
@ConditionalOnProperty(prefix = "lifeasgame.quest.events", name = "enabled", havingValue = "true")
class QuestEventKafkaConfig {

    @Bean
    KafkaTemplate<String, QuestEvent> questEventKafkaTemplate(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JsonSerializer<>(objectMapper))
        );
    }
}
