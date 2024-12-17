package dev.cwby.jasonify.generator;

import com.palantir.javapoet.*;
import dev.cwby.jasonify.SerializerManager;
import dev.cwby.jasonify.analyzer.JsonClassMetadata;
import dev.cwby.jasonify.analyzer.JsonFieldMetadata;
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
    ClassName className = getClassName(jcm.qualifiedName());
    var typeSpec =
        TypeSpec.classBuilder(jcm.simpleName() + "$$JasonifySerializer")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(ClassName.get(IJsonSerializer.class), className))
            .addMethod(generateToJsonMethod(jcm.fields(), className))
            .addField(
                FieldSpec.builder(
                        JsonGenerator.class,
                        "jg",
                        Modifier.PRIVATE,
                        Modifier.STATIC,
                        Modifier.FINAL)
                    .initializer("new $T()", JsonGenerator.class)
                    .build())
            .build();
    try {
      JavaFile.builder("dev.cwby.jasonify.serializers", typeSpec).build().writeTo(filer);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  public MethodSpec generateToJsonMethod(List<JsonFieldMetadata> fields, ClassName className) {
    String obj = className.simpleName();
    this.instanceName = obj.substring(0, 1).toLowerCase() + obj.substring(1);

    var builder =
        MethodSpec.methodBuilder("toJson")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addParameter(className, this.instanceName, Modifier.FINAL)
            .addCode(generateSerializationCode(fields))
            .addStatement("return $L.getJson()", generatorVar);

    return builder.build();
  }

  public CodeBlock generateSerializationCode(List<JsonFieldMetadata> fields) {
    var builder =
        CodeBlock.builder()
            .addStatement("jg.reset()")
            .add("try {\n$>")
            .addStatement("$L.writeStartObject()", generatorVar);

    fields.forEach(field -> addFieldSerializationCode(builder, field));

    builder
        .addStatement("$L.writeEndObject()", generatorVar)
        .add("$<} catch (Exception e) {\n$>e.printStackTrace();$<\n}\n");
    return builder.build();
  }

  private void addFieldSerializationCode(CodeBlock.Builder builder, JsonFieldMetadata field) {
    builder.addStatement("$L.writeField($S)", generatorVar, field.getName());
    boolean isByteArray = field.isArray() && field.getType().replace("[]", "").equals("byte");

    if (isByteArray) {
      builder.add("$L.writeBase64String($L)", generatorVar, field.getCallable());
    } else if (field.isList()) {
      builder.add(generateArraySerialization(field));
    } else if (field.isMap()) {
      builder.add(generateMapSerialization(field));
    } else if (field.isArray()) {
      builder.add(generateArraySerialization(field));
    } else {
      builder.add(generateSingleObjectSerialization(field));
    }
  }

  private CodeBlock generateForLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
    var builder = CodeBlock.builder();
    String type = field.getType().replace("[]", "");

    builder.addStatement("$L.writeStartArray()", generatorVar);

    int depth = field.isArray() ? field.getArrayDepth() : field.getCollectionDepth();
    if (depth > 0) {
      builder.add("if ($L != null) {\n$>", instanceName);
      generateNestedLoops(builder, type, instanceName, field.getCallable(), innerBlock, depth, 0);
      builder.add("$<}\n");
    }

    builder.addStatement("$L.writeEndArray()", generatorVar);
    return builder.build();
  }

  private void generateNestedLoops(
      CodeBlock.Builder builder,
      String type,
      String instanceName,
      String callable,
      CodeBlock innerBlock,
      int maxDepth,
      int currentDepth) {
    String loopVar = "v" + currentDepth;
    if (currentDepth > 0) {
      builder.add("for(var $L : v$L) {\n$>", loopVar, currentDepth - 1);
    } else {
      builder.add("for(var $L : $L.$L) {\n$>", loopVar, instanceName, callable);
    }

    if (currentDepth + 1 < maxDepth) {
      builder.addStatement("$L.writeStartArray()", generatorVar);
      generateNestedLoops(
          builder, type, instanceName, callable, innerBlock, maxDepth, currentDepth + 1);
      builder.addStatement("$L.writeEndArray()", generatorVar);
    } else {
      builder.add(innerBlock);
    }

    builder.add("$<}\n");
  }

  public CodeBlock generateArraySerialization(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();
    int depth = field.isArray() ? field.getArrayDepth() : field.getCollectionDepth();

    if (field.isAnnotatedObject()) {
      builder.add(
          generateForLoop(
              field,
              CodeBlock.builder()
                  .addStatement(
                      "$L.writeRaw($T.toJson(v$L))",
                      generatorVar,
                      SerializerManager.class,
                      depth - 1)
                  .build()));
    } else {
      String methodType;
      if (field.isList() && depth > 1) {
        methodType = field.getMethodForType(field.getInnermostListType());
      } else {
        methodType = field.getJGString();
      }
      builder.add(
          generateForLoop(
              field,
              CodeBlock.builder()
                  .addStatement("$L.$L(v$L)", generatorVar, methodType, depth - 1)
                  .build()));
    }
    return builder.build();
  }

  public CodeBlock generateSingleObjectSerialization(JsonFieldMetadata field) {
    CodeBlock.Builder builder = CodeBlock.builder();
    if (field.isAnnotatedObject()) {
      builder.addStatement(
          "$L.writeRaw($T.toJson($L.$L))",
          generatorVar,
          SerializerManager.class,
          instanceName,
          field.getName());
    } else {
      builder.addStatement(
          "$L.$L($L.$L)", generatorVar, field.getJGString(), instanceName, field.getCallable());
    }
    return builder.build();
  }

  private ClassName getClassName(String qualifiedName) {
    String packageType = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
    String type = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
    return ClassName.get(packageType, type);
  }

  public CodeBlock generateMapSerialization(JsonFieldMetadata field) {
    var builder = CodeBlock.builder();
    int depth = field.getMapDepth();
    String type = field.getInnerMostMapType();
    System.out.println(type);

    if (JsonAnnotationProcessor.annotatedClasses.contains(type)) {
      builder.add(
          generateMapLoop(
              field,
              CodeBlock.builder()
                  .addStatement("$L.writeField(v$L.getKey())", generatorVar, depth - 1)
                  .addStatement(
                      "$L.writeRaw($T.toJson(v$L.getValue()))",
                      generatorVar,
                      SerializerManager.class,
                      depth - 1)
                  .build()));
    } else {
      System.out.println("fieldName: " + field.getName());
      String methodType = field.getMethodForType(field.getInnerMostMapType());
      builder.add(
          generateMapLoop(
              field,
              CodeBlock.builder()
                  .addStatement("$L.writeField(v$L.getKey())", generatorVar, depth - 1)
                  .addStatement("$L.$L(v$L.getValue())", generatorVar, methodType, depth - 1)
                  .build()));
    }
    return builder.build();
  }

  private CodeBlock generateMapLoop(JsonFieldMetadata field, CodeBlock innerBlock) {
    var builder = CodeBlock.builder();
    String type = field.getType().replace("[]", "");
    int depth = field.getMapDepth();
    builder.addStatement("$L.writeStartObject()", generatorVar);
    if (depth > 0) {
      builder.add("if ($L != null) {\n$>", instanceName);
      generateNestedMap(builder, type, instanceName, field.getCallable(), innerBlock, depth, 0);
      builder.add("$<}\n");
    }

    builder.addStatement("$L.writeEndObject()", generatorVar);
    return builder.build();
  }

  private void generateNestedMap(
      CodeBlock.Builder builder,
      String type,
      String instanceName,
      String callable,
      CodeBlock innerBlock,
      int maxDepth,
      int currentDepth) {
    String loopVar = "v" + currentDepth;
    if (currentDepth > 0) {
      builder.add("for(var $L : v$L.getValue().entrySet()) {\n$>", loopVar, currentDepth - 1);
    } else {
      builder.add("for(var $L : $L.$L.entrySet()) {\n$>", loopVar, instanceName, callable);
    }

    if (currentDepth + 1 < maxDepth) {
      builder.addStatement("$L.writeField(v$L.getKey())", generatorVar, currentDepth);
      builder.addStatement("$L.writeStartObject()", generatorVar);
      generateNestedMap(
          builder, type, instanceName, callable, innerBlock, maxDepth, currentDepth + 1);
      builder.addStatement("$L.writeEndObject()", generatorVar);
    } else {
      builder.add(innerBlock);
    }

    builder.add("$<}\n");
  }
}
