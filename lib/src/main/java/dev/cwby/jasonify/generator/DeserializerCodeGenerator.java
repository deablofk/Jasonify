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
        .addCode(validateOrSkipChildren())
        .addStatement("var instance = new $T()", className)
        .addCode(generateDeserializationCode(jcm))
        .addStatement("return instance")
        .build();
  }

  private CodeBlock validateOrSkipChildren() {
    var builder = CodeBlock.builder();

    builder.beginControlFlow("if (parser.getCurrentToken() == null)");
    builder.addStatement("parser.nextToken()");
    builder.endControlFlow();

    builder.beginControlFlow(
        "if (parser.getCurrentToken() != JsonToken.$L)", JsonToken.START_OBJECT);
    builder.addStatement("parser.skipChildren()");
    builder.addStatement("return null");
    builder.endControlFlow();

    return builder.build();
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

    builder.endControlFlow();
    builder.endControlFlow();
    builder.endControlFlow();
    return builder.build();
  }

  private CodeBlock addFieldDeserialization(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();
    if (field.isList()) {
      builder.addStatement("// list");
      builder.add(generateArraySerialization(field));
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
      builder.addStatement(
          "list$L.add($T.fromJson(parser, $T.class))",
          depth - 1,
          SerializerManager.class,
          field.getGenericTypeList());
    } else {
      builder.addStatement("list$L.add(parser.getCurrentValue())", depth - 1);
    }

    return builder.build();
  }

  private CodeBlock generateForLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
    var builder = CodeBlock.builder();

    builder.beginControlFlow(
        "if (parser.getCurrentToken() == JsonToken.$L)", JsonToken.START_ARRAY);
    generateNestedLoops(builder, field.getCallable(), innerBlock, field, field.getDepth(), 0);
    builder.endControlFlow();
    builder.beginControlFlow("else");
    builder.addStatement("instance.$L = null", field.getName());
    builder.endControlFlow();
    return builder.build();
  }

  private void generateNestedLoops(
      CodeBlock.Builder builder,
      String callable,
      CodeBlock innerBlock,
      JsonFieldMetadata field,
      int depth,
      int currentDepth) {
    builder.addStatement("$L list$L = new $T<>()", field.getType(), currentDepth, ArrayList.class);
    builder.beginControlFlow("while (parser.nextToken() != JsonToken.$L)", JsonToken.END_ARRAY);

    if (currentDepth + 1 < depth) {
      generateNestedLoops(builder, callable, innerBlock, field, depth, currentDepth + 1);
    } else {
      builder.add(innerBlock);
    }

    builder.endControlFlow();
    builder.addStatement("instance.$L = list$L", callable, currentDepth);
  }
}
