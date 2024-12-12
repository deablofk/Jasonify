package dev.cwby.jasonify.analyzer;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class JsonFieldMetadata {

  private final String type;
  private final String name;
  private final boolean array;
  private final boolean annotatedObject;
  private final boolean hasGetter;
  private final boolean map;
  private final boolean list;
  private final VariableElement field;

  public JsonFieldMetadata(
      VariableElement element, boolean isAnnotatedObject, boolean isMap, boolean isList) {
    this.type = element.asType().toString();
    this.name = String.valueOf(element.getSimpleName());
    this.array = element.asType().getKind() == TypeKind.ARRAY;
    this.annotatedObject = isAnnotatedObject;
    this.hasGetter = JsonClassAnalyzer.hasPublicGetter(element);
    this.map = isMap;
    this.list = isList;
    this.field = element;
  }

  public List<String> getDeclaredArguments() {
    return ((DeclaredType) field.asType())
        .getTypeArguments().stream().map(TypeMirror::toString).toList();
  }

  public String getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public boolean isArray() {
    return array;
  }

  public boolean isMap() {
    return map;
  }

  public boolean isList() {
    return list;
  }

  public boolean isAnnotatedObject() {
    return annotatedObject;
  }

  public boolean hasGetter() {
    return hasGetter;
  }

  public String getterMethod() {
    return JsonClassAnalyzer.getMethodNameByField(this.name) + "()";
  }

  public int getArrayDepth() {
    if (!array) {
      return 0;
    }

    String target = "[]";
    int count = 0;
    int index = 0;
    while ((index = type.indexOf(target, index)) != -1) {
      count++;
      index += target.length();
    }

    return count;
  }

  public String getJGString() {
    String tempType;
    if (isList()) {
      tempType = getElementListType();
    } else {
      tempType = type;
    }

    return switch (tempType.replace("[]", "")) {
      case "java.lang.Character", "java.lang.String" -> "writeString";
      case "byte" -> "writeBase64String";
      case "boolean" -> "writeBoolean";
      case "int", "double" -> "writeNumber";
      default -> throw new IllegalStateException("Unexpected value: " + tempType);
    };
  }

  public String getElementListType() {
    return getDeclaredArguments().getFirst();
  }
}
