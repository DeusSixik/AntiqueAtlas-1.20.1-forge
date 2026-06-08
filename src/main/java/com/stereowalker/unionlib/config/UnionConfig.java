package com.stereowalker.unionlib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnionConfig {
    String name();
    boolean autoReload() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Entry {
        String group() default "";
        String name() default "";
        String translatable() default "";
        ConfigSide side() default ConfigSide.Shared;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Comment {
        String[] comment() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Range {
        double min() default Double.NEGATIVE_INFINITY;
        double max() default Double.POSITIVE_INFINITY;
        boolean useSlider() default false;
    }
}
