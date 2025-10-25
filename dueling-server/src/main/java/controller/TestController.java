package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.CardRepository;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private CardRepository cardRepository;

    @PostMapping("/stock/set")
    public ResponseEntity<String> setStock(@RequestParam String cardId, @RequestParam int stock) {
        cardRepository.setStock(cardId, stock);
        return ResponseEntity.ok("Stock set for " + cardId + ": " + stock);
    }

    @PostMapping("/stock/clear")
    public ResponseEntity<String> clearStock() {
        cardRepository.clearStock();
        return ResponseEntity.ok("All stock cleared");
    }
}
