package dev.cwby.jasonify.analyzer;

import com.palantir.javapoet.ClassName;

public class ClassUtils {
    public static ClassName getClassName(String qualifiedName) {
        String packageType = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        String type = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        return ClassName.get(packageType, type);
    }
}
