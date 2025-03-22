package ing.assessment.service.impl;

import ing.assessment.security.JwtProvider;
import ing.assessment.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final JwtProvider jwtProvider;

    public AuthServiceImpl(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public ResponseEntity<String> generate() {
        String token = jwtProvider.generateToken();
        return ResponseEntity.ok(token);
    }
}
