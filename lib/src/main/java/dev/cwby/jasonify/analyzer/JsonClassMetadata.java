package dev.cwby.jasonify.analyzer;

import java.util.List;

public record JsonClassMetadata(
        String simpleName,
        String qualifiedName,
        List<JsonFieldMetadata> fields,
        boolean serialize,
        boolean deserialize) {

    public String getPackage() {
        return qualifiedName.replace("." + simpleName, "");
    }
}
