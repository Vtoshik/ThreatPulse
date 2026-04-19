package com.threatpulse.alerts;

import com.threatpulse.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {
    List<AlertHistory> findByUser(User user);
}
