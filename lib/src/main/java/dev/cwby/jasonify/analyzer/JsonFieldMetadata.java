package dev.cwby.jasonify.analyzer;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import dev.cwby.jasonify.annotation.JsonName;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            case "int", "java.lang.Integer", "double", "java.lang.Double", "float", "java.lang.Float" -> "writeNumber";
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
        String tmpType = type;
        if (isMap()) {
            tmpType = getMapValueType();
        }
        return switch (tmpType.replace("[]", "")) {
            case "java.lang.Character", "java.lang.String" -> "getCurrentValue";
            case "byte", "java.lang.Byte" -> "un";
            case "boolean", "java.lang.Boolean" -> "getCurrentValueBoolean";
            case "int", "java.lang.Integer" -> "getCurrentValueInteger";
            case "double", "java.lang.Double" -> "getCurrentValueDouble";
            case "float", "java.lang.Float" -> "getCurrentValueFloat";
            default -> throw new IllegalStateException("Unexpected value: " + tmpType);
        };
    }

    public String getDeserializationToken() {
        String tmpType = type;
        if (isMap()) {
            tmpType = getMapValueType();
        }
        return switch (tmpType.replace("[]", "")) {
            case "java.lang.Character", "java.lang.String" -> "VALUE_STRING";
            case "byte", "java.lang.Byte" -> "VALUE_STRING";
            case "boolean", "java.lang.Boolean" -> "VALUE_BOOLEAN";
            case "int", "java.lang.Integer" -> "VALUE_NUMBER";
            case "double", "java.lang.Double" -> "VALUE_NUMBER";
            case "float", "java.lang.Float" -> "VALUE_NUMBER";
            default -> throw new IllegalStateException("Unexpected value: " + tmpType);
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

    public String getTypeForDepth(int depth) {
        TypeMirror currentType = field.asType();
        //    for (int i = 0;
        //        i < depth
        //            && currentType instanceof DeclaredType declaredType
        //            && !declaredType.getTypeArguments().isEmpty();
        //        i++) {
        //      currentType = declaredType.getTypeArguments().getFirst();
        //    }

        for (int i = 0; i < depth; i++) {
            if (currentType instanceof DeclaredType declaredType
                    && !declaredType.getTypeArguments().isEmpty()) {
                currentType = declaredType.getTypeArguments().get(0);
            } else if (currentType instanceof ArrayType arrayType) {
                currentType = arrayType.getComponentType();
            } else {
                break;
            }
        }

        return currentType.toString();
    }

    public TypeName getClassnameWithGenerics(String type) {
        int genericStart = type.indexOf("<");
        if (genericStart == -1) {
            return getClassNameForType(type);
        }

        String base = type.substring(0, genericStart);
        ClassName baseClassName = getClassNameForType(base);

        int genericEnd = type.lastIndexOf(">");
        String generics = type.substring(genericStart + 1, genericEnd);
        String[] genericTypes = splitGenerics(generics);

        TypeName[] typeArguments = new TypeName[genericTypes.length];
        for (int i = 0; i < genericTypes.length; i++) {
            typeArguments[i] = getClassnameWithGenerics(genericTypes[i].trim());
        }

        return ParameterizedTypeName.get(baseClassName, typeArguments);
    }

    public String[] splitGenerics(String generics) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        var builder = new StringBuilder();

        for (char c : generics.toCharArray()) {
            if (c == ',' && depth == 0) {
                result.add(builder.toString().trim());
                builder.setLength(0);
            } else {
                if (c == '<') {
                    depth++;
                }
                if (c == '>') {
                    depth--;
                }
                builder.append(c);
            }
        }
        result.add(builder.toString().trim());
        return result.toArray(new String[0]);
    }

    public ClassName getClassNameForType(String type) {
        int index = type.lastIndexOf(".");
        if (index == -1) {
            throw new IllegalArgumentException("must include package for type");
        }

        String path = type.substring(0, index);
        String className = type.substring(index + 1);
        return ClassName.get(path, className);
    }

    public TypeName convertArrayToList(String arrayType) {
        if (!arrayType.endsWith("[]")) {
            throw new IllegalArgumentException("The type must be an array type ending with '[]'");
        }

        // Remove all array brackets and count dimensions
        int dimensions = 0;
        while (arrayType.endsWith("[]")) {
            dimensions++;
            arrayType = arrayType.substring(0, arrayType.length() - 2);
        }

        ClassName baseType = ClassName.bestGuess(arrayType);

        TypeName result = baseType;
        for (int i = 0; i < dimensions; i++) {
            result = ParameterizedTypeName.get(ClassName.get("java.util", "List"), result);
        }

        return result;
    }

    public TypeName getBaseType() {
        String type = getType().replaceAll("\\[]", "");
        return getClassNameForType(type);
    }
}
