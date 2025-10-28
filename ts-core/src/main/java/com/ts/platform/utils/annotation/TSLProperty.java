package com.ts.platform.utils.annotation;

import java.lang.annotation.*;

@Documented
@Target({
        ElementType.FIELD,
        ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
public @interface TSLProperty {
    String value() default "";

    String prefix() default "";
}
