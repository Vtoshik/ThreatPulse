package com.threatpulse.alerts.dto;

import java.time.OffsetDateTime;

public record AlertHistoryResponse(
   Long id,
   Long threatId,
   String threatTitle,
   String channel,
   OffsetDateTime sentAt
) {}
