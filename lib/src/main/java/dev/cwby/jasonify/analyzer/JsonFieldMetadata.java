package dev.cwby.jasonify.analyzer;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

public class JsonFieldMetadata {

  private final String type;
  private final String name;
  private final boolean array;
  private final boolean customObject;
  private final boolean hasGetter;

  public JsonFieldMetadata(VariableElement element) {
    this.type = element.asType().toString();
    this.name = String.valueOf(element.getSimpleName());
    this.array = element.asType().getKind() == TypeKind.ARRAY;
    this.customObject = element.asType().getKind().isPrimitive();
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

  public boolean isCustomObject() {
    return customObject;
  }

  public boolean hasGetter() {
    return hasGetter;
  }

  public String getterMethod() {
    return JsonClassAnalyzer.getMethodNameByField(this.name);
  }

  public String getJGString() {
    return switch (type.replace("[]", "")) {
      case "boolean" -> "writeBoolean";
      case "int" -> "writeNumber";
      case "java.lang.String" -> "writeString";
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }
}
