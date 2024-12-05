package dev.cwby.jasonify.analyzer;

import java.util.List;

public record JsonClassMetadata(
    String simpleName, String qualifiedName, List<JsonFieldMetadata> fields) {}