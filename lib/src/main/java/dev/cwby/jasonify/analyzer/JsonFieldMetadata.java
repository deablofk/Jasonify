package dev.cwby.jasonify.analyzer;

import com.palantir.javapoet.ClassName;
import dev.cwby.jasonify.annotation.JsonName;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class JsonFieldMetadata {

  private final String type;
  private final String name;
  private final boolean array;
  private final boolean annotatedObject;
  private final boolean hasGetter;
  private final boolean map;
  private final boolean list;
  private final VariableElement field;
  private final boolean isRecord;

  public JsonFieldMetadata(
      VariableElement element,
      boolean isAnnotatedObject,
      boolean isMap,
      boolean isList,
      boolean isRecord) {
    this.type = element.asType().toString();
    this.name = String.valueOf(element.getSimpleName());
    this.array = element.asType().getKind() == TypeKind.ARRAY;
    this.annotatedObject = isAnnotatedObject;
    this.hasGetter = JsonClassAnalyzer.hasPublicGetter(element);
    this.map = isMap;
    this.list = isList;
    this.field = element;
    this.isRecord = isRecord;
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

  public String getJsonName() {
    return hasAnnotation(JsonName.class) ? field.getAnnotation(JsonName.class).value() : name;
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

  public int getDepth() {
    if (array) {
      return getArrayDepth();
    } else if (list) {
      return getDepthForType(List.class);
    } else if (map) {
      return getDepthForType(Map.class);
    }
    return 0;
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

  public boolean hasAnnotation(Class<?> annotationClass) {
    return field.getAnnotationMirrors().stream()
        .anyMatch(
            x ->
                x.getAnnotationType()
                    .asElement()
                    .toString()
                    .equals(annotationClass.getCanonicalName()));
  }

  public int getDepthForType(Class<?> classType) {
    int depth = 0;
    TypeMirror currentType = field.asType();

    while (currentType instanceof DeclaredType declaredType) {
      String rawType = declaredType.asElement().toString();
      if (!rawType.equals(classType.getCanonicalName())) {
        break;
      }
      depth++;
      if (declaredType.getTypeArguments().isEmpty()) {
        break;
      }
      currentType = declaredType.getTypeArguments().getLast();
    }

    return depth;
  }

  public String getInnerMost() {
    Class<?> classType = null;
    if (isMap()) {
      classType = Map.class;
    } else if (isList()) {
      classType = List.class;
    }

    return getInnerMostType(classType);
  }

  public String getInnerMostType(Class<?> type) {
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

  public String getJGString() {
    String tempType;
    if (isList()) {
      tempType = getElementListType();
    } else if (isMap()) {
      tempType = getMapValueType();
    } else {
      tempType = type;
    }

    return getMethodForType(tempType);
  }

  public String getMethodForType(String type) {
    return switch (type.replace("[]", "")) {
      case "java.lang.Character", "java.lang.String" -> "writeString";
      case "byte", "java.lang.Byte" -> "writeBase64String";
      case "boolean", "java.lang.Boolean" -> "writeBoolean";
      case "int", "java.lang.Integer", "double", "java.lang.Double", "float", "java.lang.Float" ->
          "writeNumber";
      default -> throw new IllegalStateException("Unexpected value: " + type);
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

  public String getCallable() {
    if (isRecord) {
      return getName() + "()";
    }

    return hasGetter() ? getterMethod() : getName();
  }

  public boolean isByteArray() {
    return isArray() && getType().replace("[]", "").equals("byte");
  }

  public boolean isRecord() {
    return isRecord;
  }

  public boolean hasSetter() {
    return false;
  }

  public String getDeserializationMethod() {
    return switch (type.replace("[]", "")) {
      case "java.lang.Character", "java.lang.String" -> "getCurrentValue";
      case "byte", "java.lang.Byte" -> "un";
      case "boolean", "java.lang.Boolean" -> "getCurrentValueBoolean";
      case "int", "java.lang.Integer" -> "getCurrentValueInteger";
      case "double", "java.lang.Double" -> "getCurrentValueDouble";
      case "float", "java.lang.Float" -> "getCurrentValueFloat";
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  public ClassName getGenericTypeList() {
    TypeMirror typeMirror = ((DeclaredType) field.asType()).getTypeArguments().getFirst();

    if (typeMirror instanceof DeclaredType declaredType) {
      TypeElement typeElement = (TypeElement) declaredType.asElement();
      return ClassName.get(typeElement);
    }

    throw new IllegalArgumentException("Unsupported type: " + typeMirror);
  }
}
