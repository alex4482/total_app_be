package com.work.total_app.helpers.files;

import com.work.total_app.config.StorageProperties;
import com.work.total_app.models.file.OwnerRef;
import com.work.total_app.models.file.OwnerType;
import com.work.total_app.utils.OwnerMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PathResolver {

    @Autowired
    private StorageProperties storageProperties;

    private final Map<OwnerType, OwnerMetadataProvider> providers; // map de provider-e

    public Path resolvePermanentDir(OwnerRef owner) {
        String template = storageProperties.getTemplates().getOrDefault(owner.type(),
                storageProperties.getTemplates().get(OwnerType.OTHER));
        Map<String, String> meta = providerFor(owner.type()).metadataFor(owner.id());

        String resolved = substitute(template, meta, owner);  // înlocuiește placeholder-ele
        return Paths.get(resolved).normalize();
    }

    private OwnerMetadataProvider providerFor(OwnerType type) {
        var p = providers.get(type);
        if (p == null) throw new IllegalStateException("No metadata provider for " + type);
        return p;
    }

    private String substitute(String template, Map<String, String> meta, OwnerRef owner) {
        String out = template;
        // întâi placeholders din meta
        for (var e : meta.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", safe(e.getValue()));
        }
        // fallback-uri generice
        out = out.replace("{ownerType}", safe(owner.type().name()));
        out = out.replace("{ownerId}", String.valueOf(owner.id()));

        out = storageProperties.getBaseDir() + '/' + out;
        // curăță dublurile de slash și spațiile
        return out.replaceAll("/{2,}", "/");
    }

    private String safe(String segment) {
        if (segment == null || segment.isBlank()) return "_";
        String noDiacritics = java.text.Normalizer.normalize(segment, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // fără caractere periculoase pentru FS
        String cleaned = noDiacritics.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        // opțional, limitează lungimea
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }
}
