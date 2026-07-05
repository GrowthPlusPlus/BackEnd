// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
    Figma 디자인 스펙에 맞춰 캘린더 공유 이미지를 생성(렌더링)하는 서비스이다.
 */
@Service
public class CalendarImageRenderService {
    private static final int WIDTH = 1080;
    private static final int HEIGHT = 1640;

    private static final int PADDING_LEFT = 80;
    private static final int PADDING_TOP = 96;
    private static final int CONTENT_WIDTH = 920;

    private static final int SECTION_GAP = 27;
    private static final int CARD_RADIUS = 40;

    private static final double CELL_SIZE = 120.79;
    private static final double CELL_GAP = 12.08;
    private static final int CELL_RADIUS = 15;

    private static final Color COLOR_BACKGROUND = new Color(0xFBFBF8);
    private static final Color COLOR_GREEN = new Color(0x4FB179);
    private static final Color COLOR_TEXT_PRIMARY = new Color(0x111418);
    private static final Color COLOR_TEXT_SECONDARY = new Color(0x2A2F36);
    private static final Color COLOR_TEXT_MUTED = new Color(0x9BA3AD);
    private static final Color COLOR_WEEKDAY = new Color(0x8C8C8C);
    private static final Color COLOR_CELL_INCOMPLETE = new Color(224, 226, 220, 128);
    private static final Color COLOR_CELL_INCOMPLETE_TEXT = new Color(0x616161);
    private static final Color COLOR_FOOTER_BORDER = new Color(0xD9D9D9);
    private static final Color COLOR_IMAGE_OVERLAY = new Color(0, 0, 0, 51);

    private static final String FONT_REGULAR = "fonts/Pretendard-Regular.otf";
    private static final String FONT_MEDIUM = "fonts/Pretendard-Medium.otf";
    private static final String FONT_SEMI_BOLD = "fonts/Pretendard-SemiBold.otf";
    private static final String FONT_BOLD = "fonts/Pretendard-Bold.otf";
    private static final String FONT_EXTRA_BOLD = "fonts/Pretendard-ExtraBold.otf";
    private static final String FONT_BLACK = "fonts/Pretendard-Black.otf";

    private static final String[] MONTH_ABBREV = {
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };

    private static final String[] MONTH_FULL_EN = {
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    };

    private static final DateTimeFormatter JOIN_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    private final Map<String, Font> fontCache = new HashMap<>();

    /*
        전체 이미지 생성 흐름을 담당하는 함수이다.

        @param challengeTitle 챌린지 제목
        @param year 표시할 연도
        @param month 표시할 월
        @param successDays 해당 월 인증 성공 일(1~31)
        @param proofImageUrlByDay 날짜별 인증 사진 URL
        @param participantJoinDate 참여 시작일(하단 "N일부터" 문구)
        @param monthlySuccessCount 이번 달 성공 횟수
        @param daysInMonth 해당 월 총 일수
        @param totalSuccessDays 챌린지 누적 성공 일수
        @return 렌더링된 PNG용 BufferedImage
     */
    public BufferedImage render(
            String challengeTitle,
            int year,
            int month,
            Set<Integer> successDays,
            Map<Integer, String> proofImageUrlByDay,
            LocalDate participantJoinDate,
            int monthlySuccessCount,
            int daysInMonth,
            int totalSuccessDays
    ) {
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        try {
            applyQuality(g);
            drawBackground(g);
            drawDecorativeGradients(g);

            int contentX = PADDING_LEFT;
            int y = PADDING_TOP;

            y = drawChallengeBadge(g, contentX, y, month, year);
            y += SECTION_GAP;

            y = drawChallengeTitle(g, contentX, y, challengeTitle);
            y += SECTION_GAP;

            y = drawMonthStatsRow(g, contentX, y, year, month, monthlySuccessCount, daysInMonth);
            y += SECTION_GAP;

            y = drawCalendarSection(
                    g, contentX, y, year, month, successDays, proofImageUrlByDay
            );

            drawFooterSection(g, contentX, y, participantJoinDate, totalSuccessDays);
        }
        finally {
            g.dispose();
        }

        return canvas;
    }

    /*
        Figma 메인 카드 배경을 그리는 함수이다.
     */
    private void drawBackground(Graphics2D g) {
        Shape card = new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, CARD_RADIUS, CARD_RADIUS);
        g.setClip(card);
        g.setColor(COLOR_BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setClip(null);
    }

    /*
        Figma 상단·하단 radial gradient 장식을 그리는 함수이다.
     */
    private void drawDecorativeGradients(Graphics2D g) {
        drawRadialGlow(g, 698, -227, 461, new Color(0, 153, 81, 26));
        drawRadialGlow(g, -503, 1123, 461, new Color(0, 153, 81, 26));
    }

    /*
        radial gradient 원형 장식 하나를 그리는 함수이다.

        @param centerX 원 중심 x
        @param centerY 원 중심 y
        @param radius 원 반지름
        @param edgeColor 가장자리 색(중심은 투명)
     */
    private void drawRadialGlow(Graphics2D g, int centerX, int centerY, int radius, Color edgeColor) {
        float[] fractions = {0f, 0.7f, 1f};
        Color[] colors = {
                new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), 26),
                new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), 8),
                new Color(edgeColor.getRed(), edgeColor.getGreen(), edgeColor.getBlue(), 0)
        };
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point(centerX, centerY),
                radius,
                fractions,
                colors
        );
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0));
    }

    /*
        상단 챌린지 배지(• JAN · 2026 · CHALLENGE)를 그리는 함수이다.

        @return 배지 하단 y 좌표
     */
    private int drawChallengeBadge(Graphics2D g, int x, int y, int month, int year) {
        int badgeWidth = 430;
        int badgeHeight = 60;
        int radius = badgeHeight / 2;

        Shape badge = new RoundRectangle2D.Double(x, y, badgeWidth, badgeHeight, radius, radius);
        g.setColor(new Color(0, 153, 81, 15));
        g.fill(badge);
        g.setColor(new Color(0, 153, 81, 56));
        g.setStroke(new BasicStroke(1f));
        g.draw(badge);

        String badgeText = "• " + MONTH_ABBREV[month - 1] + " · " + year + " · CHALLENGE";
        Font font = font(FONT_SEMI_BOLD, 32f);
        g.setFont(applyLetterSpacing(font, 0.6f));
        g.setColor(COLOR_GREEN);

        FontMetrics metrics = g.getFontMetrics();
        int textX = x + 20;
        int textY = y + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent();

        g.drawString(badgeText, textX, textY);

        return y + badgeHeight;
    }

    /*
        챌린지 제목을 그리는 함수이다.

        @return 제목 영역 하단 y 좌표
     */
    private int drawChallengeTitle(Graphics2D g, int x, int y, String challengeTitle) {
        Font font = font(FONT_EXTRA_BOLD, 84f);
        g.setFont(applyLetterSpacing(font, -2.94f));
        g.setColor(COLOR_TEXT_PRIMARY);

        FontMetrics metrics = g.getFontMetrics();
        int textY = y + metrics.getAscent();

        g.drawString(challengeTitle, x, textY);

        return y + 105;
    }

    /*
        월 표시와 이번 달 성공 횟수 행을 그리는 함수이다.

        @return 행 하단 y 좌표
     */
    private int drawMonthStatsRow(
            Graphics2D g,
            int x,
            int y,
            int year,
            int month,
            int monthlySuccessCount,
            int daysInMonth
    ) {
        int rowHeight = 95;

        Font monthFont = font(FONT_SEMI_BOLD, 32f);
        g.setFont(applyLetterSpacing(monthFont, 1.04f));
        g.setColor(COLOR_TEXT_MUTED);

        String monthLabel = MONTH_FULL_EN[month - 1] + " " + year;
        FontMetrics monthMetrics = g.getFontMetrics();
        int monthY = y + rowHeight - 22 - monthMetrics.getDescent();

        g.drawString(monthLabel, x, monthY);

        Font labelFont = font(FONT_SEMI_BOLD, 40f);
        g.setFont(applyLetterSpacing(labelFont, -0.4f));
        g.setColor(COLOR_TEXT_SECONDARY);

        String countLabel = "이번달 해낸 횟수";
        FontMetrics labelMetrics = g.getFontMetrics();
        int labelWidth = labelMetrics.stringWidth(countLabel);

        Font countFont = font(FONT_EXTRA_BOLD, 72f);
        g.setFont(applyLetterSpacing(countFont, -2.88f));
        FontMetrics countMetrics = g.getFontMetrics();
        String countValue = String.valueOf(monthlySuccessCount);
        int countWidth = countMetrics.stringWidth(countValue);

        Font slashFont = font(FONT_BOLD, 28f);
        g.setFont(applyLetterSpacing(slashFont, -2.88f));
        FontMetrics slashMetrics = g.getFontMetrics();
        String slashValue = "/" + daysInMonth;
        int slashWidth = slashMetrics.stringWidth(slashValue);

        int gap = 14;
        int blockWidth = labelWidth + gap + countWidth + slashWidth;
        int blockRight = x + CONTENT_WIDTH;
        int labelX = blockRight - blockWidth;

        g.setFont(applyLetterSpacing(labelFont, -0.4f));
        g.setColor(COLOR_TEXT_SECONDARY);
        g.drawString(countLabel, labelX, monthY);

        int countX = labelX + labelWidth + gap;
        int countBaseline = y + rowHeight - 22;

        g.setFont(applyLetterSpacing(countFont, -2.88f));
        g.setColor(COLOR_GREEN);
        g.drawString(countValue, countX, countBaseline);

        g.setFont(applyLetterSpacing(slashFont, -2.88f));
        g.setColor(COLOR_TEXT_SECONDARY);
        int slashX = countX + countWidth + 6;
        int slashY = countBaseline - 10;

        g.drawString(slashValue, slashX, slashY);

        return y + rowHeight;
    }

    /*
        요일 헤더와 날짜 그리드를 그리는 함수이다.

        @return 캘린더 섹션 하단 y 좌표
     */
    private int drawCalendarSection(
            Graphics2D g,
            int x,
            int y,
            int year,
            int month,
            Set<Integer> successDays,
            Map<Integer, String> proofImageUrlByDay
    ) {
        String[] weeks = {"일", "월", "화", "수", "목", "금", "토"};
        Font weekFont = font(FONT_MEDIUM, 32f);
        g.setFont(weekFont);
        g.setColor(COLOR_WEEKDAY);

        int weekHeaderHeight = 73;
        FontMetrics weekMetrics = g.getFontMetrics();

        for (int i = 0; i < weeks.length; i++) {
            int cellX = cellX(x, i);
            int textWidth = weekMetrics.stringWidth(weeks[i]);
            int textX = cellX + (int) Math.round((CELL_SIZE - textWidth) / 2.0);
            int textY = y + weekHeaderHeight - 1;

            g.drawString(weeks[i], textX, textY);
        }

        int gridY = y + weekHeaderHeight + (int) Math.round(CELL_GAP);
        drawCalendarGrid(g, x, gridY, year, month, successDays, proofImageUrlByDay);

        int gridHeight = (int) Math.round(CELL_SIZE * 6 + CELL_GAP * 5);

        return gridY + gridHeight;
    }

    /*
        달력 grid를 생성하는 핵심 함수이다.
     */
    private void drawCalendarGrid(
            Graphics2D g,
            int startX,
            int startY,
            int year,
            int month,
            Set<Integer> successDays,
            Map<Integer, String> proofImageUrlByDay
    ) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDay = yearMonth.atDay(1);
        int firstColumn = toSundayStartColumn(firstDay.getDayOfWeek());
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        int day = 1;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                int index = row * 7 + col;

                if (index < firstColumn || day > daysInMonth) {
                    continue;
                }

                int x = cellX(startX, col);
                int y = cellY(startY, row);
                int size = (int) Math.round(CELL_SIZE);
                LocalDate cellDate = LocalDate.of(year, month, day);
                boolean success = successDays.contains(day);
                boolean futureDay = cellDate.isAfter(today);
                String proofImageUrl = proofImageUrlByDay.get(day);

                drawCell(g, x, y, size, size, day, success, futureDay, proofImageUrl);

                day++;
            }
        }
    }

    /*
        달력 grid의 각 날짜 칸을 그리는 함수이다.
     */
    private void drawCell(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            int day,
            boolean success,
            boolean futureDay,
            String proofImageUrl
    ) {
        Shape rect = new RoundRectangle2D.Double(x, y, w, h, CELL_RADIUS, CELL_RADIUS);

        if (futureDay) {
            return;
        }

        if (success && proofImageUrl != null) {
            BufferedImage proof = loadImageSafely(proofImageUrl, w, h);

            if (proof != null) {
                Shape previousClip = g.getClip();
                g.setClip(rect);
                g.drawImage(proof, x, y, w, h, null);
                g.setColor(COLOR_IMAGE_OVERLAY);
                g.fill(rect);
                g.setClip(previousClip);

                drawCenteredDayNumber(g, x, y, w, h, String.valueOf(day), Color.WHITE);

                return;
            }
        }

        if (success) {
            g.setColor(COLOR_GREEN);
            g.fill(rect);
            drawCenteredDayNumber(g, x, y, w, h, String.valueOf(day), Color.WHITE);

            return;
        }

        g.setColor(COLOR_CELL_INCOMPLETE);
        g.fill(rect);
        drawCenteredDayNumber(g, x, y, w, h, String.valueOf(day), COLOR_CELL_INCOMPLETE_TEXT);
    }

    /*
        날짜 숫자를 셀 가운데에 그리는 함수이다.
     */
    private void drawCenteredDayNumber(Graphics2D g, int x, int y, int w, int h, String text, Color color) {
        Font dayFont = font(FONT_REGULAR, 42.28f);
        g.setFont(dayFont);
        g.setColor(color);

        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - metrics.getHeight()) / 2 + metrics.getAscent();

        g.drawString(text, textX, textY);
    }

    /*
        하단 시작일·누적 성공 메시지·브랜드 영역을 그리는 함수이다.
     */
    private void drawFooterSection(
            Graphics2D g,
            int x,
            int y,
            LocalDate participantJoinDate,
            int totalSuccessDays
    ) {
        int footerTop = y + 37;
        int contentX = x;

        g.setColor(COLOR_FOOTER_BORDER);
        g.fillRect(contentX, footerTop, CONTENT_WIDTH, 1);

        int messageY = footerTop + 37;

        if (participantJoinDate != null) {
            String sinceText = JOIN_DATE_FORMAT.format(participantJoinDate) + "부터";
            Font sinceFont = font(FONT_SEMI_BOLD, 44f);
            g.setFont(applyLetterSpacing(sinceFont, -0.44f));
            g.setColor(COLOR_TEXT_SECONDARY);

            FontMetrics sinceMetrics = g.getFontMetrics();
            int sinceWidth = sinceMetrics.stringWidth(sinceText);
            int sinceX = contentX + CONTENT_WIDTH - sinceWidth;
            int sinceY = messageY + sinceMetrics.getAscent();

            g.drawString(sinceText, sinceX, sinceY);
            messageY += 53 + 12;
        }

        drawTotalSuccessMessage(g, contentX, messageY, totalSuccessDays);

        int brandTop = messageY + 97 + 37;
        drawBrandSection(g, contentX, brandTop);
    }

    /*
        "총 N일 해냈어요!" 문구를 그리는 함수이다.
     */
    private void drawTotalSuccessMessage(Graphics2D g, int x, int y, int totalSuccessDays) {
        String prefix = "총 ";
        String highlight = totalSuccessDays + "일";
        String suffix = " 해냈어요!";

        Font messageFont = font(FONT_BLACK, 92f);
        messageFont = applyLetterSpacing(messageFont, -4.14f);
        g.setFont(messageFont);

        FontMetrics metrics = g.getFontMetrics();
        int prefixWidth = metrics.stringWidth(prefix);
        int highlightWidth = metrics.stringWidth(highlight);
        int suffixWidth = metrics.stringWidth(suffix);
        int totalWidth = prefixWidth + highlightWidth + suffixWidth;
        int startX = x + CONTENT_WIDTH - totalWidth;
        int baseline = y + metrics.getAscent();

        int highlightX = startX + prefixWidth;
        int underlineY = baseline + 10;
        int underlineHeight = 12;

        g.setColor(new Color(0, 153, 81, 38));
        g.fillRoundRect(highlightX, underlineY, highlightWidth, underlineHeight, 6, 6);

        g.setColor(COLOR_TEXT_PRIMARY);
        g.drawString(prefix, startX, baseline);

        g.setColor(COLOR_GREEN);
        g.drawString(highlight, highlightX, baseline);

        g.setColor(COLOR_TEXT_PRIMARY);
        g.drawString(suffix, highlightX + highlightWidth, baseline);
    }

    /*
        하단 브랜드(로고 + 해냄) 영역을 그리는 함수이다.
     */
    private void drawBrandSection(Graphics2D g, int x, int y) {
        float[] dash = {6f, 6f};
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
        g.setColor(new Color(17, 20, 24, 31));
        g.drawLine(x, y, x + CONTENT_WIDTH, y);

        int logoSize = 74;
        String brandText = "해냄";
        Font brandFont = font(FONT_BOLD, 44f);
        g.setFont(applyLetterSpacing(brandFont, -0.44f));
        g.setColor(COLOR_TEXT_PRIMARY);

        FontMetrics brandMetrics = g.getFontMetrics();
        int brandTextWidth = brandMetrics.stringWidth(brandText);
        int gap = 14;
        int totalWidth = logoSize + gap + brandTextWidth;
        int startX = x + (CONTENT_WIDTH - totalWidth) / 2;
        int contentY = y + 36;

        BufferedImage logo = loadClasspathImage("share/haenam-logo.png");

        if (logo != null) {
            g.drawImage(logo, startX, contentY, logoSize, logoSize, null);
        }
        else {
            g.setColor(COLOR_GREEN);
            g.fillOval(startX, contentY, logoSize, logoSize);
        }

        int textX = startX + logoSize + gap;
        int textY = contentY + (logoSize + brandMetrics.getAscent() - brandMetrics.getDescent()) / 2;

        g.drawString(brandText, textX, textY);
    }

    /*
        classpath 리소스 이미지를 안전하게 읽는 함수이다.
     */
    private BufferedImage loadClasspathImage(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);

            if (!resource.exists()) {
                return null;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                return ImageIO.read(inputStream);
            }
        }
        catch (Exception ignored) {
            return null;
        }
    }

    /*
        Pretendard 폰트를 classpath에서 로드하는 함수이다.
     */
    private Font font(String classpathLocation, float size) {
        Font baseFont = fontCache.computeIfAbsent(classpathLocation, this::loadFontResource);

        return baseFont.deriveFont(size);
    }

    /*
        폰트 파일을 읽어 GraphicsEnvironment에 등록하는 함수이다.
     */
    private Font loadFontResource(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);

            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Font loadedFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loadedFont);

                    return loadedFont;
                }
            }

            String ttfPath = classpathLocation.replace(".otf", ".ttf");
            ClassPathResource ttfResource = new ClassPathResource(ttfPath);

            if (ttfResource.exists()) {
                try (InputStream inputStream = ttfResource.getInputStream()) {
                    Font loadedFont = Font.createFont(Font.TRUETYPE_FONT, inputStream);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loadedFont);

                    return loadedFont;
                }
            }
        }
        catch (Exception ignored) {
        }

        return new Font("SansSerif", Font.PLAIN, 12);
    }

    /*
        Figma letter-spacing 값을 AWT 폰트에 적용하는 함수이다.

        @param letterSpacingPx CSS letter-spacing(px)
     */
    private Font applyLetterSpacing(Font baseFont, float letterSpacingPx) {
        float fontSize = baseFont.getSize2D();
        float tracking = (letterSpacingPx / fontSize) * 1000f;
        Map<TextAttribute, Object> attributes = new HashMap<>();

        attributes.put(TextAttribute.FAMILY, baseFont.getFamily());
        attributes.put(TextAttribute.SIZE, fontSize);
        attributes.put(TextAttribute.WEIGHT, baseFont.getAttributes().get(TextAttribute.WEIGHT));
        attributes.put(TextAttribute.TRACKING, tracking);

        return new Font(attributes);
    }

    /*
        이미지 렌더링 품질 옵션을 설정하는 함수이다.
     */
    private void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    /*
        인증 사진 URL에서 이미지를 읽는 함수이다.
     */
    private BufferedImage loadImageSafely(String imageUrl, int w, int h) {
        try {
            String thumbnailUrl = buildThumbnailUrl(imageUrl, w, h);

            return ImageIO.read(new URL(thumbnailUrl));
        }
        catch (Exception e) {
            return null;
        }
    }

    /*
        클라우디너리 URL을 썸네일 크기로 변환하는 함수이다.
     */
    private String buildThumbnailUrl(String originalUrl, int w, int h) {
        if (originalUrl == null) {
            return null;
        }

        return originalUrl.replace(
                "/upload/",
                "/upload/c_fill,w_" + w + ",h_" + h + ",g_auto/"
        );
    }

    /*
        n번째 열의 x 좌표를 계산하는 함수이다.
     */
    private int cellX(int startX, int column) {
        return startX + (int) Math.round(column * (CELL_SIZE + CELL_GAP));
    }

    /*
        n번째 행의 y 좌표를 계산하는 함수이다.
     */
    private int cellY(int startY, int row) {
        return startY + (int) Math.round(row * (CELL_SIZE + CELL_GAP));
    }

    /*
        달력의 날짜를 일요일 시작 열 인덱스로 변환하는 함수이다.
     */
    private int toSundayStartColumn(DayOfWeek dayOfWeek) {
        return dayOfWeek.getValue() % 7;
    }
}
