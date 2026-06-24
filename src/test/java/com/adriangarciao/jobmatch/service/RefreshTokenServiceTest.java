package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.model.RefreshToken;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    @Mock
    private RefreshTokenRepository repo;
    @Mock
    private User user;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new RefreshTokenService(repo);
    }

    @Test
    void create_createsAndSavesToken() {
        when(user.getId()).thenReturn(1L);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(repo.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshToken token = service.create(user);
        assertNotNull(token.getToken());
        assertNotNull(token.getExpiresAt());
        assertEquals(user, token.getUser());
        assertFalse(token.isRevoked());
        verify(repo).save(captor.capture());
        assertEquals(token, captor.getValue());
    }

    @Test
    void findValid_returnsValidToken() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(1000));
        when(repo.findByToken("abc")).thenReturn(Optional.of(token));
        Optional<RefreshToken> result = service.findValid("abc");
        assertTrue(result.isPresent());
    }

    @Test
    void findValid_filtersRevokedOrExpired() {
        RefreshToken revoked = new RefreshToken();
        revoked.setRevoked(true);
        revoked.setExpiresAt(Instant.now().plusSeconds(1000));
        RefreshToken expired = new RefreshToken();
        expired.setRevoked(false);
        expired.setExpiresAt(Instant.now().minusSeconds(1000));
        when(repo.findByToken("revoked")).thenReturn(Optional.of(revoked));
        when(repo.findByToken("expired")).thenReturn(Optional.of(expired));
        assertTrue(service.findValid("revoked").isEmpty());
        assertTrue(service.findValid("expired").isEmpty());
    }

    @Test
    void rotate_validToken_revokesAndCreatesNew() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(1000));
        token.setUser(user);
        when(repo.findByToken("tok")).thenReturn(Optional.of(token));
        when(repo.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshToken newToken = service.rotate("tok");
        assertNotNull(newToken);
        assertTrue(token.isRevoked());
        verify(repo).save(token);
    }

    @Test
    void rotate_expiredOrRevoked_throws() {
        RefreshToken revoked = new RefreshToken();
        revoked.setRevoked(true);
        revoked.setExpiresAt(Instant.now().plusSeconds(1000));
        RefreshToken expired = new RefreshToken();
        expired.setRevoked(false);
        expired.setExpiresAt(Instant.now().minusSeconds(1000));
        when(repo.findByToken("revoked")).thenReturn(Optional.of(revoked));
        when(repo.findByToken("expired")).thenReturn(Optional.of(expired));
        assertThrows(IllegalArgumentException.class, () -> service.rotate("revoked"));
        assertThrows(IllegalArgumentException.class, () -> service.rotate("expired"));
    }

    @Test
    void rotate_invalidToken_throws() {
        when(repo.findByToken("bad")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.rotate("bad"));
    }

    @Test
    void revokeUserTokens_callsRepo() {
        when(user.getId()).thenReturn(1L);
        service.revokeUserTokens(user);
        verify(repo).revokeAllForUser(1L);
    }

    @Test
    void revoke_setsRevokedAndSaves() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        service.revoke(token);
        assertTrue(token.isRevoked());
        verify(repo).save(token);
    }

    @Test
    void revoke_doesNothingIfAlreadyRevoked() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);
        service.revoke(token);
        verify(repo, never()).save(any());
    }
}
