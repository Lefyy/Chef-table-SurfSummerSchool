package ru.cheftable.web.page

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomePageController {
    @GetMapping("/") fun home() = "redirect:/login"
    @GetMapping("/login") fun login() = "login"
}
