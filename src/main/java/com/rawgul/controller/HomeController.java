package com.rawgul.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for handling home page requests
 */
@Controller
public class HomeController {

    /**
     * Redirect root path to index.html
     * @return redirect to index.html
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }
}
