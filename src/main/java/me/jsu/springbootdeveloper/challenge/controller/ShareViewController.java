// 정성우

package me.jsu.springbootdeveloper.challenge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/*
    서버 내부에서 캘린더 공유 캡처를 위한 html을 호출하는 컨트롤러이다.
 */
@Controller
public class ShareViewController {
    @GetMapping("/share")
    public String share() {
        return "haenaem_share";
    }
}