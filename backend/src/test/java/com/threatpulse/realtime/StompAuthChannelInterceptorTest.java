package com.threatpulse.realtime;

import com.threatpulse.auth.JwtService;
import com.threatpulse.auth.UserDetailsServiceImpl;
import com.threatpulse.user.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StompAuthChannelInterceptorTest {
    @Mock private JwtService jwtService;
    @Mock private UserDetailsServiceImpl userDetailsService;

    private StompAuthChannelInterceptor interceptor;
    private User user;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(jwtService, userDetailsService);
        user = new User("anna", "hash");
        user.setId(7L);
    }

    private Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribe(String destination, User asUser) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (asUser != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(asUser, null, asUser.getAuthorities()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connect_shouldRefuse_whenThereIsNoToken() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), null))
                .isInstanceOf(MessagingException.class).hasMessage("Unauthorized");
    }

    @Test
    void connect_shouldRefuse_whenTheTokenIsInvalid() {
        when(jwtService.extractUserId("bad")).thenThrow(new JwtException("bad"));

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer bad"), null))
                .isInstanceOf(MessagingException.class).hasMessage("Unauthorized");
    }

    @Test
    void connect_shouldRefuse_whenTheTokenVersionIsOld() {
        when(jwtService.extractUserId("old")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(user);
        when(jwtService.isTokenValid("old", user)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer old"), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void connect_shouldSetTheUser_whenTheTokenIsValid() {
        when(jwtService.extractUserId("good")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(user);
        when(jwtService.isTokenValid("good", user)).thenReturn(true);

        Message<?> result = interceptor.preSend(connect("Bearer good"), null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    void subscribe_shouldAllowThreatsTopicForAnAuthenticatedUser() {
        assertThatCode(() -> interceptor.preSend(subscribe("/topic/threats", user), null))
                .doesNotThrowAnyException();
    }

    @Test
    void subscribe_shouldAllowTheUsersOwnAlertTopic() {
        assertThatCode(() -> interceptor.preSend(subscribe("/topic/alerts/7", user), null))
                .doesNotThrowAnyException();
    }

    @Test
    void subscribe_shouldRefuseTheAlertTopicOfAnotherUser() {
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/alerts/8", user), null))
                .isInstanceOf(MessagingException.class).hasMessage("Forbidden");
    }

    @Test
    void subscribe_shouldRefuseAnUnauthenticatedClient() {
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/threats", null), null))
                .isInstanceOf(MessagingException.class).hasMessage("Unauthorized");
    }

    @Test
    void subscribe_shouldRefuseUnknownDestinations() {
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/anything-else", user), null))
                .isInstanceOf(MessagingException.class).hasMessage("Forbidden");
    }
}
