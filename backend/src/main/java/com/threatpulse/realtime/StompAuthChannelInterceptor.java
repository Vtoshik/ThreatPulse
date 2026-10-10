package com.threatpulse.realtime;

import com.threatpulse.auth.JwtService;
import com.threatpulse.auth.UserDetailsServiceImpl;
import com.threatpulse.user.User;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP connections and decides who may subscribe to what.
 * <p>
 * The HTTP security filter does not see messages that travel over a WebSocket, so the same
 * JWT check is repeated here:
 * <ul>
 *   <li>CONNECT must carry a valid "Authorization: Bearer ..." header.</li>
 *   <li>/topic/threats is open to every authenticated user.</li>
 *   <li>/topic/alerts/{id} is open only to the user with that id.</li>
 *   <li>Everything else is refused.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ALERTS_PREFIX = "/topic/alerts/";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Unauthorized");
        }
        String token = header.substring(BEARER_PREFIX.length());
        try {
            Long id = jwtService.extractUserId(token);
            User user = userDetailsService.loadUserById(id);
            if (!jwtService.isTokenValid(token, user)) {
                throw new MessagingException("Unauthorized");
            }
            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        } catch (JwtException | UsernameNotFoundException e) {
            log.debug("WebSocket connection refused: {}", e.getMessage());
            throw new MessagingException("Unauthorized");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new MessagingException("Unauthorized");
        }

        String destination = accessor.getDestination();
        if (RealtimeService.THREATS_TOPIC.equals(destination)) {
            return;
        }
        if (destination != null && destination.equals(ALERTS_PREFIX + user.getId())) {
            return;
        }
        throw new MessagingException("Forbidden");
    }
}
