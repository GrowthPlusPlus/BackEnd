// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Set;

/*
    실제로 캘린더 사진을 생성(랜더링) 하는 서비스이다.
 */
@Service
public class CalendarImageRenderService {
    // 최종 이미지 크기이다.
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1350;

    /*
        전체 이미지 생성 흐름을 담당하는 함수이다.
     */
    public BufferedImage render(String challengeTitle, int year, int month, Set<Integer> successDays, Map<Integer, String> proofImageUrlByDay) {
        BufferedImage canvas = createBaseCanvas();
        Graphics2D g = canvas.createGraphics();  // 이미지에 그림을 그리는 객체이다.

        try {
            applyQuality(g);

            drawHeader(g, challengeTitle, year, month);
            drawWeekHeader(g);
            drawCalendarGrid(g, year, month, successDays, proofImageUrlByDay);
        } finally {
            g.dispose();
        }

        return canvas;
    }

    /*
        배경 이미지를 만드는 함수이다.
     */
    private BufferedImage createBaseCanvas() {
        try {
            ClassPathResource resource = new ClassPathResource("share/calendar-template.png");  // 템플릿 배경 사진이 있는지 검색한다.
            if (resource.exists()) {  // 존재하는 경우
                try (InputStream is = resource.getInputStream()) {
                    BufferedImage template = ImageIO.read(is);  // 이미지를 읽는다.
                    if (template != null) {
                        return resizeIfNeeded(template, WIDTH, HEIGHT);  // 미리 정한 사진 크기로 조정한 뒤 반환한다.
                    }
                }
            }
        }
        catch (Exception ignored) {}  // 템플릿이 없어도 아무일도 일어나지 않는다.

        // 템플릿 파일이 없다면 직접 배경을 생성한다.
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            applyQuality(g);
            GradientPaint paint = new GradientPaint(0, 0, new Color(248, 250, 252),
                    WIDTH, HEIGHT, new Color(226, 232, 240));  // 배경을 그린다.
            g.setPaint(paint);  // 그래픽을 그린다.
            g.fillRect(0, 0, WIDTH, HEIGHT);  // x, y 좌표에 지정한 크기만큼 사각형을 채운다.
        } finally {
            g.dispose();  // 객체가 사용한 리소스를 반환한다.
        }
        return image;  // 생성한 배경을 반환한다.
    }

    /*
        이미지의 크기를 미리 정한 크기에 맞게 조정해주는 함수이다.
     */
    private BufferedImage resizeIfNeeded(BufferedImage src, int targetWidth, int targetHeight) {
        if (src.getWidth() == targetWidth && src.getHeight() == targetHeight) {  // 사진 크기가 정한 사이즈와 동일하다면 그대로 반환한다.
            return src;
        }
        // 배경을 생성한다.
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            applyQuality(g);
            g.drawImage(src, 0, 0, targetWidth, targetHeight, null);  // 생성한 배경에 기존 이미지를 그린다.
        } finally {
            g.dispose();  // 객체가 사용한 리소스를 반환한다.
        }
        return resized;  // 조정된 사진을 반환한다.
    }

    /*
        이미지의 품질 향상을 위한 함수이다.
     */
    private void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);  // 계단현상 제거
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);  // 글자 부드럽게
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);  // 고품질 랜더링
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);  // 이미지 리사이즈 품질
    }

    /*
        상단 제목을 그리는 함수이다.
     */
    private void drawHeader(Graphics2D g, String challengeTitle, int year, int month) {
        g.setColor(new Color(15, 23, 42));  // 글자 색 설정
        g.setFont(new Font("Dialog", Font.BOLD, 48));  // 글씨체 설정
        g.drawString(challengeTitle, 70, 100);

        g.setFont(new Font("Dialog", Font.BOLD, 36));
        g.setColor(new Color(51, 65, 85));
        g.drawString(year + "년 " + month + "월", 70, 155);
    }

    /*
        캘린더 상단의 요일을 그리기 위한 함수이다.
     */
    private void drawWeekHeader(Graphics2D g) {
        String[] weeks = {"일", "월", "화", "수", "목", "금", "토"};
        int startX = 70;
        int startY = 250;
        int cellW = 134;

        g.setFont(new Font("Dialog", Font.BOLD, 28));

        for (int i = 0; i < weeks.length; i++) {
            if (i == 0) {  // 일요일은 빨강으로 그린다.
                g.setColor(new Color(220, 38, 38));
            } else if (i == 6) {  // 토요일ㅇ느 파랑으로 그린다.
                g.setColor(new Color(37, 99, 235));
            } else {  // 평일은 회색으로 그린다.
                g.setColor(new Color(71, 85, 105));
            }
            g.drawString(weeks[i], startX + i * cellW + 48, startY);  // 설정한 색깔에 따라 실제로 화면에 그린다.
        }
    }

    /*
        달력의 grid를 생성하는 핵심 함수이다.
     */
    private void drawCalendarGrid(Graphics2D g, int year, int month, Set<Integer> successDays, Map<Integer, String> proofImageUrlByDay) {
        YearMonth yearMonth = YearMonth.of(year, month);  // 월 정보를 만든다. (ex 2026-03)
        LocalDate firstDay = yearMonth.atDay(1);  // 만든 월의 첫 날짜를 계산한다. (ex 2026-03-01)

        int firstColumn = toSundayStartColumn(firstDay.getDayOfWeek());  // 해당 월의 시작 요일을 계산한다.
        int daysInMonth = yearMonth.lengthOfMonth();  // 해당 월의 총 날짜가 며칠인지 계산한다. (ex 30일)

        int startX = 70;
        int startY = 290;
        int cellW = 134;
        int cellH = 150;
        int radius = 24;

        // 실제로 화면에 달력 grid를 그리는 함수이다.
        int day = 1;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                int index = row * 7 + col;
                if (index < firstColumn || day > daysInMonth) {
                    continue;
                }

                int x = startX + col * cellW;
                int y = startY + row * cellH;

                // 생성한 grid에 날짜 칸과 정보를 넣는다.
                drawCell(
                        g, x, y, cellW - 10, cellH - 10, radius,
                        day, successDays.contains(day), proofImageUrlByDay.get(day), col
                );

                day++;
            }
        }
    }

    /*
        drawCalendarGrid함수로 그린 달력 grid에 각 날짜 칸을 그리는 함수이다.
     */
    private void drawCell(Graphics2D g, int x, int y, int w, int h, int radius, int day, boolean success, String proofImageUrl, int column) {
        Shape rect = new RoundRectangle2D.Double(x, y, w, h, radius, radius);  // 둥근 사각형을 만든다.

        g.setColor(Color.WHITE);
        g.fill(rect);  // 지정한 색으로 내부 색깔을 채운다.
        g.setColor(new Color(203, 213, 225));
        g.draw(rect);  // 지정한 색으로 테두리 색깔을 채운다.

        // 해당 날짜에 인증을 성공 했으며, 인증 사진이 있는 경우이다.
        if (success && proofImageUrl != null) {
            BufferedImage proof = loadImageSafely(proofImageUrl, w, h);  // 이미지를 불러온다.
            if (proof != null) {
                g.setClip(rect);  // 인증 사진이 셀 영역 밖으로 나가지 않도록 제한한다.
                g.drawImage(proof, x, y, w, h, null);  // 이미지를 칸 안에 넣는다.
                g.setClip(null);

                g.setColor(new Color(15, 23, 42, 100));
                g.fillRoundRect(x, y, 42, 34, 14, 14);  // 지정한 색깔로 반투명한 overlay를 추가한다.
            }
            else {  // 인증한 날이지만 사진을 읽지 못한 경우 성공 처리만 한다.
                g.setColor(new Color(219, 234, 254));
                g.fill(rect);
            }
        }
        else if (success) {  // 인증했지만 사진이 없는 경우에 성공 처리를 한다.
            g.setColor(new Color(219, 234, 254));
            g.fill(rect);
        }
        else {  // 인증하지 않은 날짜이다.
            g.setColor(new Color(248, 250, 252));
            g.fill(rect);
        }

        // 달력 grid의 각 칸에 들어가는 날짜 숫자 텍스트를 그린다.
        g.setFont(new Font("Dialog", Font.BOLD, 22));
        if (column == 0) {
            g.setColor(new Color(220, 38, 38));  // 일요일의 날짜 숫자 색깔을 지정한다.
        }
        else if (column == 6) {
            g.setColor(new Color(37, 99, 235));  // 토요일의 날짜 숫자 색깔을 지정한다.
        }
        else {
            g.setColor(new Color(15, 23, 42));  // 평일의 날짜 숫자 색깔을 지정한다.
        }
        g.drawString(String.valueOf(day), x + 14, y + 28);  // 지정한 컬러에 따라 날짜를 그린다.

        if (success) {  // 인증에 성공한 날에는 오른쪽 상단에 초록 점을 찍는다.
            g.setColor(new Color(34, 197, 94));
            g.fillOval(x + w - 34, y + 12, 16, 16);
        }
    }

    /*
        이미지 url을 불러오는 함수이다.
     */
    private BufferedImage loadImageSafely(String imageUrl, int w, int h) {
        try {
            String thumbnailUrl = buildThumbnailUrl(imageUrl, w, h);  // 요청할 url을 변경한다.

            return ImageIO.read(new URL(thumbnailUrl));
        }
        catch (Exception e) {
            return null;  // 실패하면 null을 반환한다.
        }
    }

    /*
        클라우디너리 url을 사진 크기를 리사이즈하도록 하는 url로 바꾸는 함수이다.
     */
    private String buildThumbnailUrl(String originalUrl, int w, int h) {
        if (originalUrl == null) {
            return null;
        }

        return originalUrl.replace(
                "/upload/",  // 대체할 문자열이다.
                "/upload/c_fill,w_" + w + ",h_" + h + ",g_auto/"  // 지정한 크기로 클라우디너리에서 조정하여 이미지를 준다.
        );
    }

    /*
        달력의 날짜를 0부터 시작하도록 변환하는 함수이다.
     */
    private int toSundayStartColumn(DayOfWeek dayOfWeek) {
        return dayOfWeek.getValue() % 7;
    }
}