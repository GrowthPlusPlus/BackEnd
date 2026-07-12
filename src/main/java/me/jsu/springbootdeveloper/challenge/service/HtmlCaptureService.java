// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import me.jsu.springbootdeveloper.challenge.dto.share.ShareImageRenderRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;

/*
    html을 띄운 뒤, 해당 화면을 캡처하여 리턴하는 서비스이다.
 */
@Service
public class HtmlCaptureService {
    public byte[] capture(ShareImageRenderRequest request) throws IOException {
        try (Playwright playwright = Playwright.create()) {  // Playwright를 실행한다.

            // Chromium 브라우저를 실행한다.
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)  // 화면 없이 실행한다.
            );

            // 브라우저 프로필을 생성한다.
            BrowserContext context = browser.newContext();

            // 새 탭을 하나 연다. 크롬에서 새 탭을 누르는 것과 같다.
            Page page = context.newPage();

            // html을 호출한다.
            page.navigate(
                    "http://localhost:8080/share",
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
            );

            // 데이터를 json 형태로 변환한다.
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(request);

            page.evaluate("""
            async (json) => {
                await window.renderShareImage(JSON.parse(json));
            }
            """, json);

            // 현재 보이는 화면을 png 형식으로 캡처한다.
            byte[] image = page.screenshot(
                    new Page.ScreenshotOptions()
                            .setFullPage(true)
            );

            // 크롬을 닫는다.
            browser.close();

            return image;  // 따로 이미지를 만들지 않고 데이터만 반환한다.
        }
    }
}
