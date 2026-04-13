package com.threatpulse.collector;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import com.threatpulse.feed.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssCollector {
    private final KafkaTemplate<String, RawThreatEvent> kafkaTemplate;
    private final ThreatRepository threatRepository;

    private static final List<String> RSS_FEEDS = List.of(
            "https://feeds.feedburner.com/TheHackersNews",
            "https://www.bleepingcomputer.com/feed/",
            "https://krebsonsecurity.com/feed/",
            "https://threatpost.com/feed/"
    );

    public void collect() {
        for (String feedUrl : RSS_FEEDS) {
            collectFeed(feedUrl);
        }
    }

    private void collectFeed(String feedUrl) {
        try {
            URL url = new URL(feedUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));
            String feedTitle = feed.getTitle();

            for (SyndEntry entry : feed.getEntries()) {
                String externalId = entry.getUri() != null ? entry.getUri() : entry.getLink();

                if (threatRepository.existsByExternalId(externalId)) {
                    continue;
                }

                String title = entry.getTitle();
                // getDescription() returns a SyndContent object, not String — must call getValue()
                String description = entry.getDescription() != null
                        ? entry.getDescription().getValue() : "";
                String sourceUrl = entry.getLink();
                // getSource() is for re-syndicated feeds and is almost always null — use feed title
                String sourceName = feedTitle != null ? feedTitle : feedUrl;
                // getPublishedDate() can be null for some feeds — fall back to now
                OffsetDateTime published = entry.getPublishedDate() != null
                        ? entry.getPublishedDate().toInstant().atOffset(ZoneOffset.UTC)
                        : OffsetDateTime.now(ZoneOffset.UTC);

                RawThreatEvent event = new RawThreatEvent(
                        externalId, title, description, sourceUrl, sourceName, published, "RSS"
                );

                kafkaTemplate.send(KafkaConfig.RAW_THREATS_TOPIC, externalId, event);
            }
        } catch (Exception e) {
            log.error("Failed to collect RSS feed: {}", feedUrl, e);
        }
    }
}
