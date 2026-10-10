package com.threatpulse.feed.dto;

/**
 * Counts for the dashboard. Only analyzed threats are counted, the same set the feed shows.
 *
 * @param last24h threats published in the last 24 hours
 */
public record ThreatStatsResponse(
        long critical,
        long high,
        long medium,
        long low,
        long info,
        long total,
        long last24h
) {}
