package com.threatpulse.realtime;

import com.threatpulse.alerts.AlertTriggerEvent;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.feed.ThreatRepository;
import com.threatpulse.feed.ThreatResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Pushes live updates to connected browsers.
 * <p>
 * With Kafka, {@link RealtimeConsumer} calls these methods for every message. Without Kafka
 * there is no message bus, so the same two things are published as in-process Spring events
 * and received here. Only one of the two ways is active in a given pipeline mode.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeService {
    static final String THREATS_TOPIC = "/topic/threats";
    static final String ALERTS_TOPIC_PREFIX = "/topic/alerts/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ThreatRepository threatRepository;
    private final ThreatResponseMapper threatResponseMapper;

    /**
     * Loads the stored threat and sends it as a DTO. The entity is never sent: its lazy
     * collections cannot be read outside a transaction.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void publishThreat(String externalId) {
        sendThreat(externalId);
    }

    // A call from inside this class does not go through the transactional proxy, so each
    // entry point that needs a session has its own @Transactional and calls this method.
    private void sendThreat(String externalId) {
        threatRepository.findByExternalId(externalId)
                .map(threatResponseMapper::toThreatResponse)
                .ifPresentOrElse(
                        response -> messagingTemplate.convertAndSend(THREATS_TOPIC, response),
                        () -> log.warn("The threat with externalId: {} "
                                + "wasn't found in the database", externalId));
    }

    public void publishAlert(AlertTriggerEvent event) {
        messagingTemplate.convertAndSend(ALERTS_TOPIC_PREFIX + event.userId(), event);
    }

    // After the commit, so a browser that reacts to the message finds the threat in the database
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onAnalyzedThreat(AnalyzedThreatEvent event) {
        sendThreat(event.externalId());
    }

    @EventListener
    public void onAlertTrigger(AlertTriggerEvent event) {
        publishAlert(event);
    }
}
