package com.tcms.authentication.service;

import com.tcms.authentication.model.RefreshToken;
import com.tcms.authentication.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {
    @Test void signingInRotatesAnExistingRefreshToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshToken old = new RefreshToken(); old.setUserName("engineer"); old.setToken("previous-token");
        old.setExpiryDate(Instant.now().minusSeconds(5));
        when(repository.findByUserName("engineer")).thenReturn(old);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshTokenService service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "refreshTokenDurationMs", 60000L);
        RefreshToken renewed = service.createRefreshToken(new User("engineer", "hash", List.of()));
        assertThat(renewed.getToken()).isNotEqualTo("previous-token");
        assertThat(renewed.getExpiryDate()).isAfter(Instant.now());
        verify(repository, never()).delete(any());
    }
}
