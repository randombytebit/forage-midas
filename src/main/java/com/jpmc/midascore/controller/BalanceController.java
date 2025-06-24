package com.jpmc.midascore.controller;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BalanceController {
    private final UserRepository userRepository;

    public BalanceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/balance")
    ResponseEntity<Balance> getBalance(@RequestParam(value = "userId", required = false) Long id){
        UserRecord userRecord = userRepository.findById(id).orElse(null);
        if(userRecord == null){
            return ResponseEntity.ok(new Balance(0));
        }
        return ResponseEntity.ok(new Balance(userRecord.getBalance()));
    }
}
