package dev.cwby.jasonify.generator;

import com.palantir.javapoet.*;
import dev.cwby.jasonify.SerializerManager;
import dev.cwby.jasonify.analyzer.ClassUtils;
import dev.cwby.jasonify.analyzer.JsonClassMetadata;
import dev.cwby.jasonify.analyzer.JsonFieldMetadata;
import dev.cwby.jasonify.annotation.JsonIgnore;
import dev.cwby.jasonify.reader.JsonParser;
import dev.cwby.jasonify.reader.JsonToken;
import dev.cwby.jasonify.serializer.IJsonDeserializer;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DeserializerCodeGenerator {

    // TODO: basic interface for spliting deserialization types
    private final List<JsonClassMetadata> jsonClassMetadata;
    private final Filer filer;

    public DeserializerCodeGenerator(List<JsonClassMetadata> jsonClassMetadata, Filer filer) {
        this.jsonClassMetadata = jsonClassMetadata;
        this.filer = filer;
    }

    public void write() {
        jsonClassMetadata.forEach(this::writeClass);
    }

    public void writeClass(JsonClassMetadata jcm) {
        ClassName className = ClassUtils.getClassName(jcm.qualifiedName());
        var typeSpec = TypeSpec.classBuilder(jcm.simpleName() + "$$JasonifyDeserializer").addModifiers(Modifier.PUBLIC).addSuperinterface(ParameterizedTypeName.get(ClassName.get(IJsonDeserializer.class), className)).addMethod(generateParseJsonMethod(jcm, className)).build();
        try {
            JavaFile.builder("dev.cwby.jasonify.deserializers", typeSpec).build().writeTo(filer);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public MethodSpec generateParseJsonMethod(JsonClassMetadata jcm, ClassName className) {
        return MethodSpec.methodBuilder("parseJson").addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(className).addParameter(JsonParser.class, "parser", Modifier.FINAL).addException(IOException.class).addStatement("parser.skipNulltoken()").addStatement("var instance = new $T()", className).addCode(generateDeserializationCode(jcm)).addStatement("return instance").build();
    }

    private CodeBlock generateDeserializationCode(JsonClassMetadata jcm) {
        var builder = CodeBlock.builder();

        builder.beginControlFlow("while (parser.nextToken() != $T.END_OBJECT && parser.getCurrentToken() != $T.END_DOCUMENT)", JsonToken.class, JsonToken.class);
        builder.beginControlFlow("if (parser.getCurrentValue() != null)");
        builder.beginControlFlow("switch (parser.getCurrentValue())");

        for (var field : jcm.fields()) {
            if (!field.hasAnnotation(JsonIgnore.class)) {
                builder.add("case $S:\n$>", field.getName());
                builder.add(addFieldDeserialization(field));
                builder.add("break;$<\n");
            }
        }
        builder.add("default:\n$>");
        builder.addStatement("parser.nextToken()");
        builder.addStatement("parser.skipOrSkipChildren()");
        builder.add("break;$<\n");

        builder.endControlFlow();
        builder.endControlFlow();
        builder.endControlFlow();
        return builder.build();
    }

    private CodeBlock addFieldDeserialization(JsonFieldMetadata field) {
        var builder = CodeBlock.builder();
        if (field.isByteArray()) {
            builder.addStatement("parser.nextToken()");
            builder.addStatement("instance.$L = parser.decodeByteArray(parser.getCurrentValue())", field.getName());
        } else if (field.isList() || field.isArray()) {
            builder.addStatement("parser.nextToken()");
            builder.add(generateListDeserialization(field));
        } else if (field.isMap()) {
            builder.addStatement("parser.nextToken()");
            builder.add(generateMapDeserialization(field));
        } else {
            builder.beginControlFlow("if (parser.nextToken() == JsonToken.$L)", field.getDeserializationToken());
            builder.addStatement("instance.$L = parser.$L()", field.getName(), field.getDeserializationMethod());
            builder.endControlFlow();
        }
        return builder.build();
    }

    private CodeBlock generateListDeserialization(JsonFieldMetadata field) {
        return generateForLoop(field, getInnerBlockForArrayOrList(field));
    }

    private CodeBlock getInnerBlockForArrayOrList(JsonFieldMetadata field) {
        var builder = CodeBlock.builder();
        int depth = field.getDepth();

        if (field.isAnnotatedObject()) {
            String type = field.getInnerMost();
            ClassName className = field.getClassNameForType(type);

            builder.beginControlFlow("if (parser.getCurrentToken() == JsonToken.START_OBJECT)");
            if (field.isArray()) {
                String arrayType = field.getType().replace("[", "").replace("]", "");
                ClassName arrayClassName = field.getClassNameForType(arrayType);

                builder.addStatement("list$L[list$LCount] = $T.fromJson(parser, $T.class)", depth - 1, depth - 1, SerializerManager.class, arrayClassName);
            } else {
                builder.addStatement("list$L.add($T.fromJson(parser, $T.class))", depth - 1, SerializerManager.class, className);
            }
            builder.endControlFlow();
            builder.beginControlFlow("else");
            builder.addStatement("parser.skipOrSkipChildren()");
            builder.endControlFlow();
        } else {
            builder.addStatement("list$L.add(parser.getCurrentValue())", depth - 1);
        }

        return builder.build();
    }

    private CodeBlock generateForLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
        var builder = CodeBlock.builder();
        generateNestedLoops(builder, field.getCallable(), innerBlock, field, field.getDepth(), 0);
        return builder.build();
    }

    private String getArrayDimensionsByDepth(String arraySize, int depth) {
        if (depth == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i == 0) {
                stringBuilder.append("[").append(arraySize).append("]");
            } else {
                stringBuilder.append("[]");
            }
        }

        return stringBuilder.toString();
    }


    private void generateNestedLoops(CodeBlock.Builder builder, String callable, CodeBlock innerBlock, JsonFieldMetadata field, int depth, int currentDepth) {
        String type = field.getTypeForDepth(currentDepth);
        TypeName className = field.getClassnameWithGenerics(type);
        builder.beginControlFlow("if (parser.getCurrentToken() == JsonToken.$L)", JsonToken.START_ARRAY);
        if (field.isArray()) {
            TypeName baseType = ClassName.bestGuess(field.getType().replace("[", "").replace("]", ""));
            String arrayDimensionsWithSize = getArrayDimensionsByDepth("parser.countArrayEntries()", depth - currentDepth);
            builder.addStatement("var list$L = new $T$L", currentDepth, baseType, arrayDimensionsWithSize);
            builder.addStatement("int list$LCount = 0", currentDepth);
        } else {
            builder.addStatement("$T list$L = new $T<>()", className, currentDepth, ArrayList.class);
        }
        builder.beginControlFlow("while (parser.nextToken() != JsonToken.$L)", JsonToken.END_ARRAY);

        if (currentDepth + 1 < depth) {
            generateNestedLoops(builder, callable, innerBlock, field, depth, currentDepth + 1);
        } else {
            builder.add(innerBlock);
        }

        builder.endControlFlow();
        if (currentDepth > 0) {
            if (field.isArray()) {
                builder.addStatement("list$L[list$LCount] = list$L", currentDepth - 1, currentDepth - 1, currentDepth);
                builder.addStatement("list$LCount++", currentDepth - 1);
            } else {
                builder.addStatement("list$L.add(list$L)", currentDepth - 1, currentDepth);
            }
        } else {
            builder.addStatement("instance.$L = list$L", callable, currentDepth);
        }
        builder.endControlFlow();
        builder.beginControlFlow("else");
        builder.addStatement("parser.skipOrSkipChildren()");
        builder.endControlFlow();
    }

    private CodeBlock generateMapDeserialization(JsonFieldMetadata field) {
        return generateMapLoop(field, getInnerBlockForMap(field));
    }

    private CodeBlock getInnerBlockForMap(JsonFieldMetadata field) {
        var builder = CodeBlock.builder();
        int depth = field.getDepth();

        if (field.isAnnotatedObject()) {
            String type = field.getInnerMost();
            ClassName className = field.getClassNameForType(type);

            // bosta
            builder.beginControlFlow("if (parser.getCurrentToken() == JsonToken.FIELD_NAME)");
            builder.addStatement("String key = parser.getCurrentValue()");
            builder.addStatement("parser.nextToken()");
            builder.beginControlFlow("if (parser.getCurrentToken() == JsonToken.START_OBJECT)");
            builder.addStatement("map$L.put(key, $T.fromJson(parser, $T.class))", depth - 1, SerializerManager.class, className);
            builder.endControlFlow();
            builder.beginControlFlow("else if (parser.getCurrentToken() == JsonToken.START_OBJECT || parser.getCurrentToken() == JsonToken.START_ARRAY)");
            builder.addStatement("parser.skipOrSkipChildren()");
            builder.endControlFlow();
            builder.endControlFlow();
            builder.beginControlFlow("else");
            builder.addStatement("parser.skipOrSkipChildren()");
            builder.endControlFlow();
        } else {
            // bosta
            builder.beginControlFlow("if (parser.getCurrentToken() == JsonToken.FIELD_NAME)");
            builder.addStatement("String key = parser.getCurrentValue()");
            builder.addStatement("parser.nextToken()");
            builder.beginControlFlow("if (parser.getCurrentToken() == JsonToken.$L)", field.getDeserializationToken());
            builder.addStatement("map$L.put(key, parser.$L())", depth - 1, field.getDeserializationMethod());
            builder.endControlFlow();
            builder.beginControlFlow("else if (parser.getCurrentToken() == JsonToken.START_OBJECT || parser.getCurrentToken() == JsonToken.START_ARRAY)");
            builder.addStatement("parser.skipOrSkipChildren()");
            builder.endControlFlow();
            builder.endControlFlow();
            builder.beginControlFlow("else");
            builder.addStatement("parser.skipOrSkipChildren()");
            builder.endControlFlow();
        }

        return builder.build();
    }

    private CodeBlock generateMapLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
        var builder = CodeBlock.builder();
        generateMapNestedLoops(builder, field.getCallable(), innerBlock, field, field.getDepth(), 0);
        return builder.build();
    }


    private void generateMapNestedLoops(CodeBlock.Builder builder, String callable, CodeBlock innerBlock, JsonFieldMetadata field, int depth, int currentDepth) {
        String type = field.getTypeForDepth(currentDepth);
        TypeName className = field.getClassnameWithGenerics(type);
        builder.beginControlFlow("if (parser.getCurrentToken() == JsonToken.$L)", JsonToken.START_OBJECT);
        builder.addStatement("$T map$L = new $T<>()", className, currentDepth, LinkedHashMap.class);
        builder.beginControlFlow("while (parser.nextToken() != JsonToken.$L && parser.getCurrentToken() != JsonToken.$L)", JsonToken.END_OBJECT, JsonToken.END_DOCUMENT);

        if (currentDepth + 1 < depth) {
            generateMapNestedLoops(builder, callable, innerBlock, field, depth, currentDepth + 1);
        } else {
            builder.add(innerBlock);
        }

        builder.endControlFlow();
        if (currentDepth > 0) {
            builder.addStatement("map$L.add(map$L)", currentDepth - 1, currentDepth);
        } else {
            builder.addStatement("instance.$L = map$L", callable, currentDepth);
        }
        builder.endControlFlow();
        builder.beginControlFlow("else");
        builder.addStatement("parser.skipOrSkipChildren()");
        builder.endControlFlow();
    }
}
