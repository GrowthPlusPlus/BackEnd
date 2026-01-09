//// 정성우
//
//package me.jsu.springbootdeveloper.service;
//
//import lombok.RequiredArgsConstructor;
//import me.jsu.springbootdeveloper.domain.User;
//import me.jsu.springbootdeveloper.repository.UserRepository;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.stereotype.Service;
//
///*
//    스프링 시큐리티에서 사용자 정보를 가져오는 인터페이스로 jwt를 쓰는 환경에선 사용되지 않는다.
// */
//@RequiredArgsConstructor
//@Service
//public class UserDetailService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//
//    // 사용자 이름인 이메일로 사용자의 정보를 가져오는 메서드이다
//    @Override
//    public User loadUserByUsername(String email) {
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND_FOR_EMAIL"));
//    }
//}
