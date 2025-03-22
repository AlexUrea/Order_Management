package ing.assessment.service;

import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<String> generate();
}
