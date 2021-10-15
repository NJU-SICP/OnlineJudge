package cn.edu.nju.sicp.contests.hog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class HogListener implements MessageListener {

    private final HogCompare hogCompare;
    private final HogRepository hogRepository;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public HogListener(HogCompare hogCompare, HogRepository hogRepository) {
        this.hogCompare = hogCompare;
        this.hogRepository = hogRepository;
    }

    @Override
    public void onMessage(Message message) {
        logger.debug(String.format("Receive AMQP %s", message));
        hogRepository.findById(new String(message.getBody())).ifPresent(hogCompare);
    }

}
