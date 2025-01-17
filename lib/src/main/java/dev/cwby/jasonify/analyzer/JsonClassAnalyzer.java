package dev.cwby.jasonify.analyzer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonClassAnalyzer {

    public static List<ExecutableElement> getMethods(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream().filter(element -> element.getKind() == ElementKind.METHOD).map(ExecutableElement.class::cast).toList();
    }

    // TODO: handle is(methods)
    public static String getMethodNameByField(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public static boolean hasPublicGetter(VariableElement element) {
        var typeElement = (TypeElement) element.getEnclosingElement();
        String getterName = getMethodNameByField(element.getSimpleName().toString());
        return getMethods(typeElement).stream().anyMatch(method -> method.getSimpleName().toString().equals(getterName) && method.getModifiers().contains(Modifier.PUBLIC));
    }

    private boolean isFieldAccessible(VariableElement el, boolean isRecord) {
        if (isRecord) {
            return true;
        }

        return el.getModifiers().contains(Modifier.PUBLIC) || hasPublicGetter(el);
    }

    private boolean isList(VariableElement variableElement, Elements elementUtils, Types typeUtils) {
        TypeMirror listType = elementUtils.getTypeElement(List.class.getCanonicalName()).asType();
        return typeUtils.isAssignable(typeUtils.erasure(variableElement.asType()), typeUtils.erasure(listType));
    }

    private boolean isMap(VariableElement variableElement, Elements elementUtils, Types typeUtils) {
        TypeMirror mapType = elementUtils.getTypeElement(Map.class.getCanonicalName()).asType();
        return typeUtils.isAssignable(typeUtils.erasure(variableElement.asType()), typeUtils.erasure(mapType));
    }

    public String getInnerMostType(Class<?> type, VariableElement field) {
        TypeMirror currentType = field.asType();

        while (currentType instanceof DeclaredType declaredType) {
            String rawType = declaredType.asElement().toString();
            if (!rawType.equals(type.getCanonicalName()) || declaredType.getTypeArguments().isEmpty()) {
                break;
            }
            currentType = declaredType.getTypeArguments().getLast();
        }

        return currentType.toString();
    }

    public JsonClassMetadata processClass(TypeElement typeElement, Set<String> annotatedClasses, ProcessingEnvironment processingEnv) {
        Elements elementUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();

        var elements = typeElement.getEnclosedElements();
        boolean isRecord = typeElement.getKind() == ElementKind.RECORD;

        List<VariableElement> variableElements = elements.stream().filter(el -> el.getKind().isField() && isFieldAccessible((VariableElement) el, isRecord)).map(VariableElement.class::cast).toList();

        List<JsonFieldMetadata> fields = new ArrayList<>();
        for (VariableElement variableElement : variableElements) {
            boolean isAnnotated = annotatedClasses.contains(variableElement.asType().toString().replace("[]", ""));

            boolean isList = isList(variableElement, elementUtils, typeUtils);
            boolean isMap = isMap(variableElement, elementUtils, typeUtils);

            Class<?> type = null;
            if (isList) {
                type = List.class;
            } else if (isMap) {
                type = Map.class;
            }

            String innerMostType = type == null ? null : getInnerMostType(type, variableElement);

            if (!isAnnotated && annotatedClasses.contains(innerMostType)) {
                System.out.println("changing annotated object value");
                isAnnotated = true;
            }

            var fieldMetadata = new JsonFieldMetadata(variableElement, isAnnotated, isMap, isList, isRecord);

            fields.add(fieldMetadata);
        }

        return new JsonClassMetadata(typeElement.getSimpleName().toString(), typeElement.getQualifiedName().toString(), fields, true, true);
    }
}
