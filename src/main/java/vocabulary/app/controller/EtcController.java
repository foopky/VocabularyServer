package vocabulary.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EtcController {
    @GetMapping("/heathCheck")
    public ResponseEntity<String> healthCheck(){
        return ResponseEntity.ok("OK");
    }
}
