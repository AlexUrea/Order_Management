package ing.assessment.controller;

import ing.assessment.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ing.assessment.utils.GlobalConstants.Routes.AUTH;
import static ing.assessment.utils.GlobalConstants.Routes.GENERATE;

@RestController
@RequestMapping(AUTH)
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(GENERATE)
    public ResponseEntity<String> generateToken() {
        return authService.generate();
    }
}
