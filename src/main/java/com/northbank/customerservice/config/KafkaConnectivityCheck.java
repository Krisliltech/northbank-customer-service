package com.northbank.customerservice.config;

import com.northbank.customerservice.event.EventTopics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "kafka.connectivity-check.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConnectivityCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KafkaConnectivityCheck.class);
    private static final List<String> TOPICS = List.of(
            EventTopics.CUSTOMER_REGISTERED, EventTopics.CUSTOMER_STATUS_CHANGED);
    private final KafkaAdmin kafkaAdmin;

    public KafkaConnectivityCheck(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
//        Map<String, Object> config = kafkaAdmin.getConfigurationProperties();
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.put("metadata.recovery.strategy", "none");
        config.put("reconnect.backoff.ms", 500);
        config.put("reconnect.backoff.max.ms", 1000);
        try (AdminClient client = AdminClient.create(config)) {
            Map<String, TopicDescription> found = client
                    .describeTopics(TOPICS, new DescribeTopicsOptions().timeoutMs(5_000))
                    .allTopicNames()
                    .get(6, TimeUnit.SECONDS);
            log.info("Kafka reachable at {}; authenticated and allowed to describe {}",
                    config.get("bootstrap.servers"), found.keySet());
//            String clusterId = client
//                    .describeCluster(new DescribeClusterOptions().timeoutMs(5_000))
//                    .clusterId()
//                    .get(6, TimeUnit.SECONDS);
//            log.info("Kafka reachable at {}, cluster id {}", config.get("bootstrap.servers"), clusterId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Kafka not reachable at {}: {}", config.get("bootstrap.servers"), e.toString());
        }
    }
}
