package dev.cwby.jasonify.analyzer;

import com.palantir.javapoet.ClassName;

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
    } else if (isMap()) {
      tempType = getMapValueType();
    } else {
      tempType = type;
    }

    return switch (tempType.replace("[]", "")) {
      case "java.lang.Character", "java.lang.String" -> "writeString";
      case "byte", "java.lang.Byte" -> "writeBase64String";
      case "boolean", "java.lang.Boolean" -> "writeBoolean";
      case "int", "java.lang.Integer", "double", "java.lang.Double", "float", "java.lang.Float" ->
          "writeNumber";
      default -> throw new IllegalStateException("Unexpected value: " + tempType);
    };
  }

  public String getElementListType() {
    return getDeclaredArguments().getFirst();
  }

  public String getMapKeyType() {
    return getDeclaredArguments().getFirst();
  }

  public String getMapValueType() {
    return getDeclaredArguments().getLast();
  }
}
