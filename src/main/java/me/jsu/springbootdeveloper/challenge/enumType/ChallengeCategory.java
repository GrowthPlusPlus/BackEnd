// 정성우

package me.jsu.springbootdeveloper.challenge.enumType;

/*
    챌린지가 속한 카테고리를 나타내기 위한 열거형 클래스이다.
    사진 clip 검사시에 같이 사용한다.
 */
public enum ChallengeCategory {
    FITNESS,
    STUDY,
    CODING,
    READING,
    SPORTS,
    FOOD,
    COOKING,
    CLEANING,
    OUTDOOR,
    ART,
    MUSIC,
    GARDENING,
    UNSUPPORTED;

    // String -> Enum 변환기
    public static ChallengeCategory from(String value) {
        if (value == null) {
            return UNSUPPORTED;
        }

        try {
            // 대소문자 상관없이 매칭되는 Enum을 자동으로 찾아준다.
            return ChallengeCategory.valueOf(value.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            // 목록에 없는 이상한 String이 오면 UNSUPPORTED를 반환한다.
            return UNSUPPORTED;
        }
    }
}
