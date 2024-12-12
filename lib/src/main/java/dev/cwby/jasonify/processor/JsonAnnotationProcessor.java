package dev.cwby.jasonify.processor;

import com.google.auto.service.AutoService;
import dev.cwby.jasonify.analyzer.JsonClassAnalyzer;
import dev.cwby.jasonify.analyzer.JsonClassMetadata;
import dev.cwby.jasonify.annotation.Json;
import dev.cwby.jasonify.generator.CodeGenerator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes("dev.cwby.jasonify.annotation.Json")
public class JsonAnnotationProcessor extends AbstractProcessor {

  private final JsonClassAnalyzer classAnalyzer = new JsonClassAnalyzer();
  private final CodeGenerator codeGenerator = new CodeGenerator();
  private final List<JsonClassMetadata> jsonClassMetadataList = new ArrayList<>();

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment re) {
    Set<TypeElement> classesELements =
        re.getElementsAnnotatedWith(Json.class).stream()
            .filter(el -> el.getKind() == ElementKind.CLASS)
            .map(TypeElement.class::cast)
            .collect(Collectors.toSet());

    Set<String> annotatedClasses =
        classesELements.stream()
            .map(x -> x.getQualifiedName().toString())
            .collect(Collectors.toSet());

    for (TypeElement typeElement : classesELements) {
      jsonClassMetadataList.add(
          classAnalyzer.processClass(typeElement, annotatedClasses, processingEnv));
    }

    if (re.processingOver()) {
      codeGenerator.write(jsonClassMetadataList, processingEnv.getFiler());
    }

    return true;
  }
}
