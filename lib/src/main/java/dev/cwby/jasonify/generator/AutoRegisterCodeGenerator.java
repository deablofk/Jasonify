package dev.cwby.jasonify.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import dev.cwby.jasonify.SerializerManager;
import dev.cwby.jasonify.analyzer.JsonClassMetadata;
import java.io.IOException;
import java.util.List;
import javax.annotation.processing.Filer;

public class AutoRegisterCodeGenerator {

  private final List<JsonClassMetadata> jsonClassMetadata;
  private final Filer filer;

  public AutoRegisterCodeGenerator(List<JsonClassMetadata> jsonClassMetadata, Filer filer) {
    this.jsonClassMetadata = jsonClassMetadata;
    this.filer = filer;
  }

  public void write() {
    var staticBlock = CodeBlock.builder();

    for (JsonClassMetadata jcm : jsonClassMetadata) {
      var className =
          ClassName.get(
              "dev.cwby.jasonify.serializers", jcm.simpleName() + "$$JasonifySerializer");
      staticBlock.addStatement(
          "registerSerializer($S, new $T())", jcm.qualifiedName(), className);
    }

    var typeSpec =
        TypeSpec.classBuilder("AOTMapperInitializer").addStaticBlock(staticBlock.build()).build();

    try {

      JavaFile.builder("dev.cwby.jasonify.initializer", typeSpec)
          .addStaticImport(SerializerManager.class, "*")
          .build()
          .writeTo(filer);
    } catch (IOException e) {
      System.out.println("cantwrite: " + e.getMessage());
    }
  }
}
