package dev.cwby.jasonify.analyzer;

import javax.lang.model.element.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonClassAnalyzer {

  public static List<ExecutableElement> getMethods(TypeElement typeElement) {
    return typeElement.getEnclosedElements().stream()
        .filter(element -> element.getKind() == ElementKind.METHOD)
        .map(ExecutableElement.class::cast)
        .toList();
  }

  public static String getMethodNameByField(String fieldName) {
    return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
  }

  public static boolean hasPublicGetter(VariableElement element) {
    var typeElement = (TypeElement) element.getEnclosingElement();
    String getterName = getMethodNameByField(element.getSimpleName().toString());
    return getMethods(typeElement).stream()
        .anyMatch(
            method ->
                method.getSimpleName().toString().equals(getterName)
                    && method.getModifiers().contains(Modifier.PUBLIC));
  }

  private boolean isFieldAccessible(VariableElement el) {
    return el.getModifiers().contains(Modifier.PUBLIC) || hasPublicGetter(el);
  }

  public JsonClassMetadata processClass(TypeElement typeElement, Set<String> annotatedClasses) {
    var elements = typeElement.getEnclosedElements();

    List<VariableElement> variableElements =
        elements.stream()
            .filter(el -> el.getKind().isField() && isFieldAccessible((VariableElement) el))
            .map(VariableElement.class::cast)
            .toList();

    List<JsonFieldMetadata> fields = new ArrayList<>();
    for (VariableElement variableElement : variableElements) {
      boolean isAnnotated = false;

      if (annotatedClasses.contains(variableElement.asType().toString())) {
        isAnnotated = true;
        System.out.println(variableElement.asType().toString());
      }

      fields.add(new JsonFieldMetadata(variableElement, isAnnotated));
    }

    return new JsonClassMetadata(
        typeElement.getSimpleName().toString(), typeElement.getQualifiedName().toString(), fields);
  }
}
