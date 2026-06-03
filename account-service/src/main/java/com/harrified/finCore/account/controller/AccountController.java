package com.harrified.finCore.account.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    @GetMapping("/hello")
    public String greetings() {
        return "Hello Bolanla of IB";
    }

    @GetMapping("/hello2")
    public String greetings2() {
        return "Hello Bolanla 3 Goats...............";
    }
}
