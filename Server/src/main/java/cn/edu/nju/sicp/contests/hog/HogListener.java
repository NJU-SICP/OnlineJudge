package cn.edu.nju.sicp.contests.hog;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class HogListener implements ChannelAwareMessageListener {

    private final HogCompare compare;
    private final MongoOperations mongo;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public HogListener(HogCompare compare, MongoTemplate mongo) {
        this.compare = compare;
        this.mongo = mongo;
    }

    @Override
    public void onMessage(Message message, Channel channel) {
        logger.debug(String.format("Receive AMQP %s", message));
        try {
            HogEntry entry = mongo
                    .findOne(Query.query(Criteria.where("id").is(new String(message.getBody()))), HogEntry.class);
            if (entry != null) {
                compare.accept(entry);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            logger.error(String.format("HogListener failed: %s", e.getMessage()), e);
        }
    }

}
