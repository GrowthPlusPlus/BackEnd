// 정성우

package me.jsu.springbootdeveloper.scheduler.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
    어노테이션을 만들기 위한 인터페이스이다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireUserYearlyActivitySchema {
}
