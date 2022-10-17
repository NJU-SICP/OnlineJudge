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

import java.util.Calendar;
import java.util.Date;

@Configuration
public class HogConfig {

    public static final String triggerImage = "sicp-hog-contest-trigger:2022";
    public static final String compareImage = "sicp-hog-contest-compare:2022";
    public static final int compareRounds = (int) 2e6;

    public static final String queueName = "sicp.contests.hog";
    public static final Date deadline;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.NOVEMBER, 12, 23, 59, 0);
        deadline = calendar.getTime();
    }

    @Bean
    Queue hogQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Binding hogQueueBinding(Queue hogQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(hogQueue).to(directExchange).with(queueName);
    }
    @Bean
    SimpleMessageListenerContainer hogListenerContainer(
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
