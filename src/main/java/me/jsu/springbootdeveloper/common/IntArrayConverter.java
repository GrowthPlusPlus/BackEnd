// 정성우

package me.jsu.springbootdeveloper.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

/*
    db에는 배열을 저장할 수 없으므로 int 배열을 db에 저장할 수 있게 변환해주는 클래스이다.
 */
@Converter
public class IntArrayConverter implements AttributeConverter<int[], String> {

    @Override
    public String convertToDatabaseColumn(int[] attribute) {
        if (attribute == null) return null;

        return Arrays.toString(attribute)
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "");
    }

    @Override
    public int[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return new int[0];

        return Arrays.stream(dbData.split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
