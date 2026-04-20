package com.threatpulse.alerts;

import com.threatpulse.common.domain.Threat;
import com.threatpulse.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alert_history")
@EntityListeners(AuditingEntityListener.class)
public class AlertHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "threat_id", nullable = false, unique = false)
    private Threat threat;

    @CreatedDate
    @Column(name = "sent_at", nullable = false, updatable = false)
    private OffsetDateTime sentAt;

    @Column(name = "channel")
    private String channel;
}
