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
import java.util.List;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

public class DeserializerCodeGenerator {

  private final List<JsonClassMetadata> jsonClassMetadata;
  private final Filer filer;
  private String instanceName;

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
    String obj = className.simpleName();
    instanceName = Character.toLowerCase(obj.charAt(0)) + obj.substring(1);

    return MethodSpec.methodBuilder("parseJson")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(className)
        .addParameter(JsonParser.class, "parser", Modifier.FINAL)
        .addException(IOException.class)
        .addStatement("var instance = new $T()", className)
        .addCode(generateDeserializationCode(jcm))
        .addStatement("return instance")
        .build();
  }

  private CodeBlock generateDeserializationCode(JsonClassMetadata jcm) {
    var builder = CodeBlock.builder();
    builder.beginControlFlow("while (parser.nextToken() != $T.END_DOCUMENT)", JsonToken.class);
    builder.beginControlFlow("if (parser.getCurrentValue() != null)");

    builder.beginControlFlow("switch (parser.getCurrentValue())");

    for (var field : jcm.fields()) {
      if (!field.hasAnnotation(JsonIgnore.class)) {
        builder.beginControlFlow("case $S:", field.getName());
        builder.add(addFieldDeserialization(field));
        builder.endControlFlow("break");
      }
    }

    builder.endControlFlow();
    builder.endControlFlow();
    builder.endControlFlow();
    return builder.build();
  }

  private ClassName getClassNameOrPrimitive(JsonFieldMetadata jfm) {
    ClassName className;
    String type = jfm.getType();
    try {
      String packagePath = type.substring(0, type.lastIndexOf('.'));
      String simpleName = type.substring(type.lastIndexOf('.') + 1);
      className = ClassName.get(packagePath, simpleName);
    } catch (Exception e) {
      className = ClassName.get("", type);
    }
    return className;
  }

  private CodeBlock addFieldDeserialization(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();
    if (field.isList()) {
      builder.addStatement("// list");
    } else if (field.isArray()) {
      builder.addStatement("// array");
    } else if (field.isMap()) {
      builder.addStatement("// map");
    } else {
      builder.addStatement("parser.nextToken()");
      builder.addStatement(
          "instance.$L = parser.$L()", field.getName(), field.getDeserializationMethod());
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
      builder.addStatement("$T.appendToWriter(v$L)", SerializerManager.class, depth - 1);
    } else {
      String methodType =
          field.isArray() ? field.getJGString() : field.getMethodForType(field.getInnerMost());
      builder.addStatement("$L(v$L)", methodType, depth - 1);
    }

    return builder.build();
  }

  private CodeBlock generateForLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
    var builder = CodeBlock.builder();

    builder.beginControlFlow("if ($L != null)", instanceName);
    generateNestedLoops(builder, field.getCallable(), innerBlock, field.getDepth(), 0);
    builder.endControlFlow();

    return builder.build();
  }

  private void generateNestedLoops(
      CodeBlock.Builder builder,
      String callable,
      CodeBlock innerBlock,
      int depth,
      int currentDepth) {
    String loopVar = "v" + currentDepth;
    if (currentDepth > 0) {
      builder.beginControlFlow("for(var $L : v$L)", loopVar, currentDepth - 1);
    } else {
      builder.beginControlFlow("for(var $L : $L.$L)", loopVar, instanceName, callable);
    }

    if (currentDepth + 1 < depth) {
      generateNestedLoops(builder, callable, innerBlock, depth, currentDepth + 1);
    } else {
      builder.add(innerBlock);
    }

    builder.endControlFlow();
  }
}
