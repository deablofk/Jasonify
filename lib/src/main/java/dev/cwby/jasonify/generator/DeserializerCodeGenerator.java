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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

public class DeserializerCodeGenerator {

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
    var typeSpec =
        TypeSpec.classBuilder(jcm.simpleName() + "$$JasonifyDeserializer")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(ClassName.get(IJsonDeserializer.class), className))
            .addMethod(generateParseJsonMethod(jcm, className))
            .build();
    try {
      JavaFile.builder("dev.cwby.jasonify.deserializers", typeSpec).build().writeTo(filer);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  public MethodSpec generateParseJsonMethod(JsonClassMetadata jcm, ClassName className) {
    return MethodSpec.methodBuilder("parseJson")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(className)
        .addParameter(JsonParser.class, "parser", Modifier.FINAL)
        .addException(IOException.class)
        .addStatement("parser.skipNulltoken()")
        .addStatement("var instance = new $T()", className)
        .addCode(generateDeserializationCode(jcm))
        .addStatement("return instance")
        .build();
  }

  private CodeBlock generateDeserializationCode(JsonClassMetadata jcm) {
    var builder = CodeBlock.builder();

    builder.beginControlFlow("while (parser.nextToken() != $T.END_OBJECT)", JsonToken.class);
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
    builder.addStatement("parser.skipOrSkipChildren(true)");
    builder.add("break;$<\n");

    builder.endControlFlow();
    builder.endControlFlow();
    builder.endControlFlow();
    return builder.build();
  }

  private CodeBlock addFieldDeserialization(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();
    if (field.isList()) {
      builder.addStatement("parser.nextToken()");
      builder.add(generateArraySerialization(field));
    } else if (field.isArray()) {
      builder.addStatement("// array");
      builder.addStatement("parser.nextToken()");
      builder.add(generateArraySerialization(field));
      //      builder.addStatement("parser.skipOrSkipChildren(true)");
    } else if (field.isMap()) {
      builder.addStatement("// map");
      builder.addStatement("parser.nextToken()");
      builder.addStatement("parser.skipOrSkipChildren(true)");
    } else {
      builder.beginControlFlow(
          "if (parser.nextToken() == JsonToken.$L)", field.getDeserializationToken());
      builder.addStatement(
          "instance.$L = parser.$L()", field.getName(), field.getDeserializationMethod());
      builder.endControlFlow();
    }
    return builder.build();
  }

  public CodeBlock generateArraySerialization(JsonFieldMetadata field) {
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

        builder.addStatement(
            "list$L.add($T.fromJson(parser, $T.class))",
            depth - 1,
            SerializerManager.class,
            arrayClassName);
      } else {
        builder.addStatement(
            "list$L.add($T.fromJson(parser, $T.class))",
            depth - 1,
            SerializerManager.class,
            className);
      }
      builder.endControlFlow();
      builder.beginControlFlow("else");
      builder.addStatement("parser.skipOrSkipChildren(true)");
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

  private void generateNestedLoops(
      CodeBlock.Builder builder,
      String callable,
      CodeBlock innerBlock,
      JsonFieldMetadata field,
      int depth,
      int currentDepth) {
    String type = field.getTypeForDepth(currentDepth);
    TypeName className = field.getClassnameWithGenerics(type);
    builder.beginControlFlow(
        "if (parser.getCurrentToken() == JsonToken.$L)", JsonToken.START_ARRAY);
    if (field.isArray()) {
      TypeName listTypeByArray = field.convertArrayToList(type);
      builder.addStatement("$L list$L = new $T<>()", type, currentDepth, ArrayList.class);
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
      builder.addStatement("list$L.add(list$L)", currentDepth - 1, currentDepth);
    } else {
      if (field.isArray()) {
        String arrayType = field.getType().replaceFirst("\\[]", "[0]");
        ClassName arrayClassName = field.getClassNameForType(arrayType);
        builder.addStatement(
            "instance.$L = list$L.toArray(new $L)", callable, currentDepth, arrayClassName);
      } else {
        builder.addStatement("instance.$L = list$L", callable, currentDepth);
      }
    }
    builder.endControlFlow();
    builder.beginControlFlow("else");
    builder.addStatement("parser.skipOrSkipChildren(true)");
    builder.endControlFlow();
  }
}
