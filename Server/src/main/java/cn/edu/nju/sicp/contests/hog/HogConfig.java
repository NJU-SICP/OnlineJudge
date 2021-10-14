package cn.edu.nju.sicp.contests.hog;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HogConfig {

    public static final String hogImage1 = "hog_1"; // run strategy and save dict
    public static final String hogImage2 = "hog_2"; // compare two strategies

    public static final String hogQueueName = "sicp.updateHogContest";

    @Bean
    Queue hogQueue() {
        return QueueBuilder.durable(hogQueueName).build();
    }


    @Bean
    Binding updateHogContestBinding(Queue hogQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(hogQueue).to(directExchange).with(hogQueueName);
    }

    @Bean
    SimpleMessageListenerContainer updateHogContestListenerContainer(
            ConnectionFactory connectionFactory, Queue hogQueue, HogListener hogListener) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueues(hogQueue);
        container.setMessageListener(hogListener);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setPrefetchCount(1);
        container.setConcurrency("1-4");
        return container;
    }

}
