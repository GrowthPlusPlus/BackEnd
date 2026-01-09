//package me.jsu.springbootdeveloper.controller;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import me.jsu.springbootdeveloper.dto.AddUserRequest;
//import me.jsu.springbootdeveloper.service.UserService;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//
//// OAuth 방식에선 사용하지 않는 form 로그인 방식으로 내 서비스 자체의 로그인, 회원가입 기능을 지원하는 클래스이다. 세션 기반으로 토큰을 사용하지 않는다.
//@RequiredArgsConstructor
//@Controller
//public class UserApiController {
//
//    private final UserService userService;
//
//    @PostMapping("/user")
//    public String signup(AddUserRequest request) {
//        userService.save(request);  // 회원가입 메서드 호출
//        return "redirect:/login";  // 회원가입이 완료된 이후에 로그인 페이지로 이동
//    }
//
//    @GetMapping("/logout")
//    public String logout(HttpServletRequest request, HttpServletResponse response) {
//        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
//        return "redirect:/login";
//    }
//}
