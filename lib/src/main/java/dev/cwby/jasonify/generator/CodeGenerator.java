package dev.cwby.jasonify.generator;

import com.palantir.javapoet.*;
import dev.cwby.jasonify.SerializerManager;
import dev.cwby.jasonify.analyzer.JsonClassMetadata;
import dev.cwby.jasonify.analyzer.JsonFieldMetadata;
import dev.cwby.jasonify.serializer.IJsonSerializer;
import dev.cwby.jasonify.writer.JsonGenerator;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;

public class CodeGenerator {

  public void write(List<JsonClassMetadata> jcms, Filer filer) {
    for (JsonClassMetadata jcm : jcms) {
      writeClass(jcm, filer);
    }

    writeAotMapperInitializer(jcms, filer);
  }

  public void writeAotMapperInitializer(List<JsonClassMetadata> jcms, Filer filer) {
    CodeBlock.Builder staticBlock = CodeBlock.builder();

    for (JsonClassMetadata jcm : jcms) {
      ClassName className =
          ClassName.get("dev.cwby.jasonify.serializers", jcm.simpleName() + "$$JasonifyInit");
      staticBlock.addStatement("registerSerializer($S, new $T())", jcm.qualifiedName(), className);
    }

    TypeSpec typeSpec =
        TypeSpec.classBuilder("AOTMapperInitializer").addStaticBlock(staticBlock.build()).build();

    var javaFile =
        JavaFile.builder("dev.cwby.jasonify.initializer", typeSpec)
            .addStaticImport(SerializerManager.class, "*")
            .build();

    try {
      javaFile.writeTo(filer);
    } catch (IOException e) {
      System.out.println("cantwrite: " + e.getMessage());
    }
  }

  public void writeClass(JsonClassMetadata jcm, Filer filer) {
    ClassName className =
        ClassName.get(jcm.qualifiedName().replace("." + jcm.simpleName(), ""), jcm.simpleName());
    TypeSpec typeSpec =
        TypeSpec.classBuilder(jcm.simpleName() + "$$JasonifyInit")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(ClassName.get(IJsonSerializer.class), className))
            .addMethod(generateCodeBlock(jcm.fields(), className))
            .build();
    var javaFile = JavaFile.builder("dev.cwby.jasonify.serializers", typeSpec).build();
    try {
      javaFile.writeTo(filer);
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  public MethodSpec generateCodeBlock(List<JsonFieldMetadata> fields, ClassName className) {
    String obj = className.simpleName();
    obj = obj.substring(0, 1).toLowerCase() + obj.substring(1);
    String generatorAlias = "jg";

    MethodSpec.Builder builder =
        MethodSpec.methodBuilder("toJson")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addParameter(className, obj, Modifier.FINAL);

    builder.addStatement(
        "$T $L = new $T(new $T())",
        JsonGenerator.class,
        generatorAlias,
        JsonGenerator.class,
        StringBuilder.class);

    builder.addCode(jsonGeneratorCode(fields, generatorAlias, obj));

    builder.addStatement("return $L.getJson()", generatorAlias);

    return builder.build();
  }

  public CodeBlock jsonGeneratorCode(List<JsonFieldMetadata> fields, String jg, String objAlias) {
    CodeBlock.Builder builder = CodeBlock.builder();
    builder.add("try {\n$>");
    builder.addStatement("$L.writeStartObject()", jg);
    for (JsonFieldMetadata field : fields) {
      addFieldSerializationCode(builder, field, jg, objAlias);
    }
    builder.addStatement("$L.writeEndObject()", jg);
    builder.add("$<} catch (Exception e) {\n$>e.printStackTrace();$<\n}\n");
    return builder.build();
  }

  private void addFieldSerializationCode(
      CodeBlock.Builder builder,
      JsonFieldMetadata field,
      String generatorVar,
      String instanceName) {

    builder.addStatement("$L.writeField($S)", generatorVar, field.getName());
    String callable = field.hasGetter() ? field.getterMethod() : field.getName();
    boolean isByteArray = field.isArray() && field.getType().replace("[]", "").equals("byte");

    if (field.isArray() && !isByteArray) {
      builder.addStatement("$L.writeStartArray()", generatorVar);
      String type = field.getType().replace("[]", "");
      builder.add("if ($L != null) {\n$>", instanceName);
      builder.add("for($L v : $L.$L) {$>\n", type, instanceName, callable);
    }

    if (field.isAnnotatedObject()) {
      if (field.isArray() && !isByteArray) {
        builder.addStatement("$L.writeRaw($T.toJson(v))", generatorVar, SerializerManager.class);
      } else {
        builder.addStatement(
            "$L.writeRaw($T.toJson($L.$L))",
            generatorVar,
            SerializerManager.class,
            instanceName,
            field.getName());
      }
    } else {
      if (field.isArray() && !isByteArray) {
        builder.addStatement("$L.$L(v)", generatorVar, field.getJGString());
      } else {
        builder.addStatement(
            "$L.$L($L.$L)", generatorVar, field.getJGString(), instanceName, callable);
      }
    }

    if (field.isArray() && !isByteArray) {
      builder.add("$<}\n$<}\n");
      builder.addStatement("$L.writeEndArray()", generatorVar);
    }
  }
}
