package com.smartnotes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Console controller that provides convenient URL routes to the static console pages.
 * SecurityConfig permits /console/** and /static/** without authentication.
 * The console pages themselves handle auth via JavaScript by storing JWT tokens.
 */
@Controller
@RequestMapping("/console")
public class ConsoleController {

    @GetMapping
    public String consoleRoot() {
        return "forward:/static/console/index.html";
    }

    @GetMapping("/index")
    public String index() {
        return "forward:/static/console/index.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "forward:/static/console/admin.html";
    }
}
