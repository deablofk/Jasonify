package dev.cwby.jasonify.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import dev.cwby.jasonify.SerializerManager;
import dev.cwby.jasonify.analyzer.JsonClassMetadata;

import javax.annotation.processing.Filer;
import java.io.IOException;
import java.util.List;

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
            if (jcm.serialize()) {
                var className =
                        ClassName.get(
                                "dev.cwby.jasonify.serializers", jcm.simpleName() + "$$JasonifySerializer");
                staticBlock.addStatement(
                        "$T.registerSerializer($S, new $T())",
                        SerializerManager.class,
                        jcm.qualifiedName(),
                        className);
            }
            if (jcm.deserialize()) {
                var className =
                        ClassName.get(
                                "dev.cwby.jasonify.deserializers", jcm.simpleName() + "$$JasonifyDeserializer");
                staticBlock.addStatement(
                        "$T.registerDeserializer($S, new $T())",
                        SerializerManager.class,
                        jcm.qualifiedName(),
                        className);
            }
        }

        var typeSpec =
                TypeSpec.classBuilder("AOTMapperInitializer").addStaticBlock(staticBlock.build()).build();

        try {
            JavaFile.builder("dev.cwby.jasonify.initializer", typeSpec).build().writeTo(filer);
        } catch (IOException e) {
            System.out.println("cantwrite: " + e.getMessage());
        }
    }
}
