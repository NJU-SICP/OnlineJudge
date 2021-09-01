package cn.edu.nju.sicp.configs;

import java.util.UUID;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import cn.edu.nju.sicp.listeners.BuildImageListener;
import cn.edu.nju.sicp.listeners.GradeSubmissionListener;
import cn.edu.nju.sicp.listeners.RemoveImageListener;

@Configuration
public class AmqpConfig {

    public static final String directExchangeName = "sicp.direct";
    public static final String fanoutExchangeName = "sicp.fanout";

    public static final String buildImageQueueName = "sicp.buildImage";
    public static final String removeImageQueueName = "sicp.removeImage";
    public static final String gradeSubmissionQueueName = "sicp.gradeSubmission";

    @Bean
    Queue buildImageQueue() {
        return QueueBuilder.durable(buildImageQueueName).build();
    }

    @Bean
    Queue removeImageQueue() {
        return QueueBuilder.nonDurable(removeImageQueueName + "." + UUID.randomUUID().toString())
                .autoDelete().build();
    }

    @Bean
    Queue gradeSubmissionQueue() {
        return QueueBuilder.durable(gradeSubmissionQueueName).build();
    }

    @Bean
    DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(directExchangeName).durable(true).build();
    }

    @Bean
    FanoutExchange fanoutExchange() {
        return ExchangeBuilder.fanoutExchange(fanoutExchangeName).durable(true).build();
    }

    @Bean
    Binding buildImageBinding(Queue buildImageQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(buildImageQueue).to(directExchange).with(buildImageQueueName);
    }

    @Bean
    Binding removeImageBinding(Queue removeImageQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(removeImageQueue).to(fanoutExchange);
    }

    @Bean
    Binding gradeSubmissionBinding(Queue gradeSubmissionQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(gradeSubmissionQueue).to(directExchange)
                .with(gradeSubmissionQueueName);
    }

    @Bean
    SimpleMessageListenerContainer buildImageListenerContainer(ConnectionFactory connectionFactory,
            Queue buildImageQueue, BuildImageListener buildImageListener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueues(buildImageQueue);
        container.setMessageListener(buildImageListener);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setPrefetchCount(1);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer removeImageListenerContainer(ConnectionFactory connectionFactory,
            Queue removeImageQueue, RemoveImageListener removeImageListener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueues(removeImageQueue);
        container.setMessageListener(removeImageListener);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer gradeSubmissionListenerContainer(
            ConnectionFactory connectionFactory, Queue gradeSubmissionQueue,
            GradeSubmissionListener gradeSubmissionListener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueues(gradeSubmissionQueue);
        container.setMessageListener(gradeSubmissionListener);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setPrefetchCount(1);
        return container;
    }

}
