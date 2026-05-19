package com.example.smartagent.skill;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkillDescriptor {
    String name();
    String description() default "Skill description";
    String version() default "1.0.0";
    String category() default "general";
    boolean enabled() default true;
}