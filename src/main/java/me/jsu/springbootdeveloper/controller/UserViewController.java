package me.jsu.springbootdeveloper.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserViewController {
    @GetMapping("/login")
    public String login() {
        return "oauthLogin";  //해당 html로 이동
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }
}
