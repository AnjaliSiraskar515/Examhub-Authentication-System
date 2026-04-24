package com.example.examauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards the root URL "/" to the frontend entry point (index.html).
 *
 * Spring Boot's default static resource handling will resolve "forward:/index.html"
 * to src/main/resources/static/index.html automatically.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }
}
