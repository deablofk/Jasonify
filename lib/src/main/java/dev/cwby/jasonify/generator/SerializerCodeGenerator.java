package dev.cwby.jasonify.generator;

import com.palantir.javapoet.*;
import dev.cwby.jasonify.SerializerManager;
import dev.cwby.jasonify.analyzer.ClassUtils;
import dev.cwby.jasonify.analyzer.JsonClassMetadata;
import dev.cwby.jasonify.analyzer.JsonFieldMetadata;
import dev.cwby.jasonify.annotation.JsonIgnore;
import dev.cwby.jasonify.processor.JsonAnnotationProcessor;
import dev.cwby.jasonify.serializer.IJsonSerializer;
import dev.cwby.jasonify.writer.JsonGenerator;
import java.io.IOException;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

public class SerializerCodeGenerator {

  private final String generatorVar;
  private final List<JsonClassMetadata> jsonClassMetadata;
  private final Filer filer;
  private String instanceName;

  public SerializerCodeGenerator(
      String generatorVar, List<JsonClassMetadata> jsonClassMetadata, Filer filer) {
    this.generatorVar = generatorVar;
    this.jsonClassMetadata = jsonClassMetadata;
    this.filer = filer;
  }

  public void write() {
    jsonClassMetadata.forEach(this::writeClass);
  }

  public void writeClass(JsonClassMetadata jcm) {
    ClassName className = ClassUtils.getClassName(jcm.qualifiedName());
    var typeSpec =
        TypeSpec.classBuilder(jcm.simpleName() + "$$JasonifySerializer")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(ClassName.get(IJsonSerializer.class), className))
            .addMethod(generateToJsonMethod(jcm.fields(), className))
            .build();
    try {
      JavaFile.builder("dev.cwby.jasonify.serializers", typeSpec).build().writeTo(filer);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  public MethodSpec generateToJsonMethod(List<JsonFieldMetadata> fields, ClassName className) {
    String obj = className.simpleName();
    instanceName = Character.toLowerCase(obj.charAt(0)) + obj.substring(1);

    return MethodSpec.methodBuilder("appendToWriter")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(className, instanceName, Modifier.FINAL)
        .addParameter(JsonGenerator.class, generatorVar, Modifier.FINAL)
        .addCode(generateSerializationCode(fields))
        .build();
  }

  private CodeBlock generateSerializationCode(List<JsonFieldMetadata> fields) {
    var builder = CodeBlock.builder();

    builder.add(startObject());
    for (var field : fields) {
      if (!field.hasAnnotation(JsonIgnore.class)) {
        builder.add(addFieldSerializationCode(field));
      }
    }
    builder.add(endObject());

    return builder.build();
  }

  private CodeBlock addFieldSerializationCode(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();
    builder.addStatement("$L.writeField($S)", generatorVar, field.getJsonName());

    if (field.isByteArray()) {
      builder.add("$L.writeBase64String($L)", generatorVar, field.getCallable());
    } else if (field.isList() || field.isArray()) {
      builder.add(generateArraySerialization(field));
    } else if (field.isMap()) {
      builder.add(generateMapSerialization(field));
    } else {
      builder.add(generateSingleObjectSerialization(field));
    }

    return builder.build();
  }

  public CodeBlock generateArraySerialization(JsonFieldMetadata field) {
    return generateForLoop(field, getInnerBlockForArrayOrList(field));
  }

  public CodeBlock generateMapSerialization(JsonFieldMetadata field) {
    return generateMapLoop(field, getInnerBlockForMap(field));
  }

  private CodeBlock getInnerBlockForArrayOrList(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();
    int depth = field.getDepth();

    if (field.isAnnotatedObject()) {
      builder.addStatement(
          "$T.appendToWriter(v$L, $L)", SerializerManager.class, depth - 1, generatorVar);
    } else {
      String methodType =
          field.isArray() ? field.getJGString() : field.getMethodForType(field.getInnerMost());
      builder.addStatement("$L.$L(v$L)", generatorVar, methodType, depth - 1);
    }

    return builder.build();
  }

  private CodeBlock getInnerBlockForMap(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();

    if (JsonAnnotationProcessor.annotatedClasses.contains(field.getInnerMost())) {
      builder.addStatement("$L.writeField(v$L.getKey())", generatorVar, field.getDepth() - 1);
      builder.addStatement(
          "$T.appendToWriter(v$L.getValue(), $L)",
          SerializerManager.class,
          field.getDepth() - 1,
          generatorVar);
    } else {
      String methodType = field.getMethodForType(field.getInnerMost());
      builder.addStatement("$L.writeField(v$L.getKey())", generatorVar, field.getDepth() - 1);
      builder.addStatement("$L.$L(v$L.getValue())", generatorVar, methodType, field.getDepth() - 1);
    }

    return builder.build();
  }

  private CodeBlock generateForLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
    var builder = CodeBlock.builder();
    builder.add(startArray());

    builder.beginControlFlow("if ($L != null)", instanceName);
    generateNestedLoops(builder, field.getCallable(), innerBlock, field.getDepth(), 0);
    builder.endControlFlow();

    builder.add(endArray());
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
      builder.add(startArray());
      generateNestedLoops(builder, callable, innerBlock, depth, currentDepth + 1);
      builder.add(endArray());
    } else {
      builder.add(innerBlock);
    }

    builder.endControlFlow();
  }

  public CodeBlock generateSingleObjectSerialization(JsonFieldMetadata field) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (field.isAnnotatedObject()) {
      builder.addStatement(
          "$T.appendToWriter($L.$L, $L)",
          SerializerManager.class,
          instanceName,
          field.getName(),
          generatorVar);

    } else {
      builder.addStatement(
          "$L.$L($L.$L)", generatorVar, field.getJGString(), instanceName, field.getCallable());
    }
    return builder.build();
  }

  private CodeBlock generateMapLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
    var builder = CodeBlock.builder();
    builder.add(startObject());

    builder.beginControlFlow("if ($L != null)", instanceName);
    generateNestedMap(builder, field.getCallable(), innerBlock, field.getDepth(), 0);
    builder.endControlFlow();

    builder.add(endObject());
    return builder.build();
  }

  private void generateNestedMap(
      CodeBlock.Builder builder,
      String callable,
      CodeBlock innerBlock,
      int depth,
      int currentDepth) {
    String loopVar = "v" + currentDepth;
    if (currentDepth > 0) {
      builder.beginControlFlow(
          "for(var $L : v$L.getValue().entrySet())", loopVar, currentDepth - 1);
    } else {
      builder.beginControlFlow("for(var $L : $L.$L.entrySet())", loopVar, instanceName, callable);
    }

    if (currentDepth + 1 < depth) {
      builder.addStatement("$L.writeField(v$L.getKey())", generatorVar, currentDepth);
      builder.add(startObject());
      generateNestedMap(builder, callable, innerBlock, depth, currentDepth + 1);
      builder.add(endObject());
    } else {
      builder.add(innerBlock);
    }

    builder.endControlFlow();
  }

  public CodeBlock startObject() {
    return CodeBlock.builder().addStatement("$L.writeStartObject()", generatorVar).build();
  }

  public CodeBlock endObject() {
    return CodeBlock.builder().addStatement("$L.writeEndObject()", generatorVar).build();
  }

  public CodeBlock startArray() {
    return CodeBlock.builder().addStatement("$L.writeStartArray()", generatorVar).build();
  }

  public CodeBlock endArray() {
    return CodeBlock.builder().addStatement("$L.writeEndArray()", generatorVar).build();
  }
}
