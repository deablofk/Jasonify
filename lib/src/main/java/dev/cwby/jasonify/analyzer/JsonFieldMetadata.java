package dev.cwby.jasonify.analyzer;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

public class JsonFieldMetadata {

  private final String type;
  private final String name;
  private final boolean array;
  private final boolean annotatedObject;
  private final boolean hasGetter;

  public JsonFieldMetadata(VariableElement element, boolean isAnnotatedObject) {
    this.type = element.asType().toString();
    this.name = String.valueOf(element.getSimpleName());
    this.array = element.asType().getKind() == TypeKind.ARRAY;
    this.annotatedObject = isAnnotatedObject;
    this.hasGetter = JsonClassAnalyzer.hasPublicGetter(element);
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

  public boolean isAnnotatedObject() {
    return annotatedObject;
  }

  public boolean hasGetter() {
    return hasGetter;
  }

  public String getterMethod() {
    return JsonClassAnalyzer.getMethodNameByField(this.name);
  }

  public String getJGString() {
    return switch (type.replace("[]", "")) {
      case "java.lang.Character", "java.lang.String" -> "writeString";
      case "byte" -> "writeBase64String";
      case "boolean" -> "writeBoolean";
      case "int", "double" -> "writeNumber";
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }
}
