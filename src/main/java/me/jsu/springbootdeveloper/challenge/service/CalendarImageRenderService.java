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

    // 이미지 크기
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1350;

    // 전체 여백
    private static final int PADDING = 60;

    // 달력 시작 위치
    private static final int CALENDAR_TOP = 250;

    // 셀 크기
    private static final int CELL_SIZE = 120;

    // 셀 간격
    private static final int CELL_GAP = 16;

    // 셀 모서리
    private static final int CELL_RADIUS = 24;

    // 색상
    private static final Color BG = Color.WHITE;
    private static final Color SUCCESS = new Color(18,183,106);
    private static final Color FAIL = new Color(237,237,233);
    private static final Color BORDER = new Color(220,220,220);
    private static final Color TEXT = new Color(40,40,40);

    /*
        이미지 생성을 시작하는 함수이다.
     */
    public BufferedImage render(String challengeTitle,
                                int year,
                                int month,
                                Set<Integer> successDays,
                                Map<Integer,String> proofImageUrlByDay,
                                LocalDate joinDate,
                                int totalSuccessDays
    ){

        BufferedImage image =
                new BufferedImage(
                        WIDTH,
                        HEIGHT,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D g = image.createGraphics();

        try{

            applyQuality(g);

            drawBackground(g);

            drawHeader(
                    g,
                    challengeTitle,
                    year,
                    month,
                    successDays.size(),
                    YearMonth.of(year,month).lengthOfMonth()
            );

            drawWeekHeader(g);

            drawCalendar(
                    g,
                    year,
                    month,
                    successDays,
                    proofImageUrlByDay
            );

            drawSummary(g, joinDate, totalSuccessDays);

            drawLogo(g);

        }finally{

            g.dispose();

        }

        return image;
    }

    /*
        고품질 렌더링 옵션을 적용한다.
     */
    private void applyQuality(Graphics2D g){

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );
    }

    /*
        배경을 그리는 함수이다.
     */
    private void drawBackground(Graphics2D g){

        GradientPaint paint =
                new GradientPaint(
                        0,
                        0,
                        new Color(252,252,249),
                        0,
                        HEIGHT,
                        BG
                );

        g.setPaint(paint);

        g.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );
    }

    /*
        상단 제목을 출력하는 함수이다.
     */
    private void drawHeader(Graphics2D g,
                            String title,
                            int year,
                            int month,
                            int successCount,
                            int totalDays
    ) {
        int headerY = 115;

        // 년월 (맨 위)
        String[] months = {
                "JAN", "FEB", "MAR", "APR",
                "MAY", "JUN", "JUL", "AUG",
                "SEP", "OCT", "NOV", "DEC"
        };

        String monthText = months[month - 1];

        g.setFont(loadFont("/fonts/Pretendard-Regular.otf", 18f));
        g.setColor(new Color(16, 185, 129));

        g.drawString(
                "• " + monthText + " · " + year + " · CHALLENGE",
                PADDING,
                45
        );

        // 챌린지 제목
        g.setFont(loadFont("/fonts/Pretendard-Bold.otf", 54f));
        g.setColor(TEXT);

        g.drawString(
                title,
                PADDING,
                headerY
        );

        // 설명
        g.setFont(loadFont("/fonts/Pretendard-Regular.otf", 22f));
        g.setColor(new Color(120,120,120));

        g.drawString("이번달 해낸 횟수", 750, headerY);

        // 숫자
        g.setFont(loadFont("/fonts/Pretendard-Bold.otf", 54f));
        g.setColor(SUCCESS);
        g.drawString(String.valueOf(successCount), 910, headerY);

        g.setFont(loadFont("/fonts/Pretendard-Bold.otf", 54f));
        g.setColor(Color.GRAY);
        g.drawString("/" + totalDays, 955, headerY);

        // 구분선
        g.setColor(new Color(232,232,232));

        g.drawLine(
                PADDING,
                170,
                WIDTH-PADDING,
                170
        );
    }

    /*
        달력 상단의 요일을 출력하는 함수이다.
     */
    private void drawWeekHeader(Graphics2D g) {

        String[] weeks = {
                "일",
                "월",
                "화",
                "수",
                "목",
                "금",
                "토"
        };

        int startX = PADDING;
        int startY = CALENDAR_TOP - 30;

        g.setFont(loadFont("/fonts/Pretendard-Regular.otf", 22f));

        for (int i = 0; i < 7; i++) {

            g.setColor(new Color(150,150,150));

            FontMetrics fm = g.getFontMetrics();

            int x =
                    startX +
                            i * (CELL_SIZE + CELL_GAP) +
                            (CELL_SIZE - fm.stringWidth(weeks[i])) / 2;

            g.drawString(
                    weeks[i],
                    x,
                    startY
            );
        }
    }

    /*
        달력을 출력하는 핵심 함수이다.
     */
    private void drawCalendar(Graphics2D g,
                              int year,
                              int month,
                              Set<Integer> successDays,
                              Map<Integer,String> proofImageUrlByDay){

        YearMonth ym =
                YearMonth.of(year, month);

        LocalDate firstDay =
                ym.atDay(1);

        int firstColumn =
                toSundayStartColumn(
                        firstDay.getDayOfWeek()
                );

        int totalDays =
                ym.lengthOfMonth();

        int day = 1;

        for(int row = 0; row < 6; row++){

            for(int col = 0; col < 7; col++){

                int index =
                        row * 7 + col;

                if(index < firstColumn){
                    continue;
                }

                if(day > totalDays){
                    break;
                }

                int x =
                        PADDING +
                                col * (CELL_SIZE + CELL_GAP);

                int y =
                        CALENDAR_TOP +
                                row * (CELL_SIZE + CELL_GAP);

                drawDayCell(
                        g,
                        x,
                        y,
                        CELL_SIZE,
                        day,
                        successDays.contains(day),
                        proofImageUrlByDay.get(day),
                        col
                );

                day++;

            }

        }

    }

    /*
          달력의 하루를 출력하는 함수이다.
       */
    private void drawDayCell(Graphics2D g,
                             int x,
                             int y,
                             int size,
                             int day,
                             boolean success,
                             String proofImageUrl,
                             int column) {

        Shape cell =
                new RoundRectangle2D.Double(
                        x,
                        y,
                        size,
                        size,
                        CELL_RADIUS,
                        CELL_RADIUS
                );

        /*
            먼저 셀 배경을 그린다.
         */

        // 기본 배경
        g.setColor(FAIL);
        g.fill(cell);

        // 성공했고 사진이 없는 경우
        if (success && proofImageUrl == null) {

            g.setColor(SUCCESS);
            g.fill(cell);

        }

        /*
            인증사진이 존재하면 셀을 사진으로 채운다.
         */
        if (success && proofImageUrl != null) {

            BufferedImage image =
                    loadImageSafely(
                            proofImageUrl,
                            size,
                            size
                    );

            if (image != null) {

                Shape oldClip = g.getClip();

                g.setClip(cell);

                // Cover 방식으로 사진을 꽉 채운다.
                double scale =
                        Math.max(
                                (double) size / image.getWidth(),
                                (double) size / image.getHeight()
                        );

                int drawW =
                        (int) (image.getWidth() * scale);

                int drawH =
                        (int) (image.getHeight() * scale);

                int drawX =
                        x - (drawW - size) / 2;

                int drawY =
                        y - (drawH - size) / 2;

                g.drawImage(
                        image,
                        drawX,
                        drawY,
                        drawW,
                        drawH,
                        null
                );

                g.setClip(oldClip);
            }
        }

        /*
            셀 테두리를 출력한다.
         */
        g.setColor(BORDER);
        g.draw(cell);

        if (success) {
            // 초록색 셀의 숫자는 흰색
            g.setColor(Color.WHITE);

        }
        else {

            // 실패한 날짜는 검정
            g.setColor(TEXT);
        }

        /*
            날짜를 출력한다.
         */
        g.setFont(loadFont("/fonts/Pretendard-Regular.otf", 30f));

        FontMetrics fm = g.getFontMetrics();
        String text = String.valueOf(day);

        int tx = x + (size - fm.stringWidth(text)) / 2;
        // 세로도 가운데 정렬
        int ty = y + ((size - fm.getHeight()) / 2) + fm.getAscent();

        g.drawString(text, tx, ty);
    }

    /*
        하단의 요약 정보를 출력하는 함수이다.
     */
    private void drawSummary(Graphics2D g, LocalDate joinDate, int successCount) {
        Font dateFont = loadFont("/fonts/Pretendard-Regular.otf", 24f);  // 날짜
        Font bigFont = loadFont("/fonts/Pretendard-Bold.otf", 42f);

        String dateText = String.format(
                "%d년 %d월 %d일부터 ",
                joinDate.getYear(),
                joinDate.getMonthValue(),
                joinDate.getDayOfMonth()
        );
        String totalText = "총 ";
        String number = String.valueOf(successCount);
        String suffix = "일 해냈어요!";

        FontMetrics fmDate = g.getFontMetrics(dateFont);
        FontMetrics fmBig = g.getFontMetrics(bigFont);

        int totalWidth =
                fmDate.stringWidth(dateText)
                        + fmBig.stringWidth(totalText)
                        + fmBig.stringWidth(number)
                        + fmBig.stringWidth(suffix);

        int startX = (WIDTH - totalWidth) / 2;
        int y = 1110;

        // 날짜
        g.setFont(dateFont);
        g.setColor(TEXT);
        g.drawString(dateText, startX, y);

        startX += fmDate.stringWidth(dateText);

        // 총
        g.setFont(bigFont);
        g.drawString(totalText, startX, y);

        startX += fmBig.stringWidth(totalText);

        // 숫자
        g.setColor(SUCCESS);
        g.drawString(number, startX, y);

        startX += fmBig.stringWidth(number);

        // 일 해냈어요!
        g.setColor(TEXT);
        g.drawString(suffix, startX, y);
    }

    /*
        하단 로고를 출력하는 함수이다.
        로고가 없는 경우에는 텍스트를 출력한다.
     */
    private void drawLogo(Graphics2D g) {
        // 구분선을 그린다.
        g.setColor(new Color(230,230,230));

        g.drawLine(
                PADDING,
                1140,
                WIDTH - PADDING,
                1140
        );

        try {

            BufferedImage logo = ImageIO.read(
                    getClass().getResourceAsStream("/share/haenaem-logo.png")
            );

            if (logo != null) {

                int width = 170;
                int height = logo.getHeight() * width / logo.getWidth();

                g.drawImage(
                        logo,
                        WIDTH / 2 - width / 2,
                        1145,
                        width,
                        height,
                        null
                );

                return;
            }

        }
        catch (Exception ignored) {}

        // 로고가 없으면 텍스트 출력
        g.setFont(loadFont("/fonts/Pretendard-Regular.otf", 22f));
        g.setColor(new Color(160,160,160));

        FontMetrics fm = g.getFontMetrics();

        String text = "해냄";

        g.drawString(
                text,
                (WIDTH - fm.stringWidth(text)) / 2,
                1145
        );
    }

    /*
        이미지를 안전하게 불러오는 함수이다.
     */
    private BufferedImage loadImageSafely(String imageUrl,
                                          int width,
                                          int height) {

        try {

            String url =
                    buildThumbnailUrl(
                            imageUrl,
                            width,
                            height
                    );

            return ImageIO.read(
                    new URL(url)
            );

        } catch (Exception e) {

            return null;

        }
    }

    /*
        Cloudinary 썸네일 URL을 생성하는 함수이다.
     */
    private String buildThumbnailUrl(String originalUrl,
                                     int width,
                                     int height) {

        if (originalUrl == null) {
            return null;
        }

        return originalUrl.replace(
                "/upload/",
                "/upload/c_fill,w_"
                        + width
                        + ",h_"
                        + height
                        + ",g_auto,q_auto,f_auto/"
        );
    }

    /*
        Java의 월요일 시작을
        일요일 시작으로 변경하는 함수이다.
     */
    private int toSundayStartColumn(DayOfWeek dayOfWeek) {

        return dayOfWeek.getValue() % 7;

    }

    /*
        폰트를 로드하는 함수이다.
     */
    private Font loadFont(String path, float size) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            return font.deriveFont(size);
        } catch (Exception e) {
            return new Font("Malgun Gothic", Font.PLAIN, (int) size);
        }
    }
}
