package com.work.total_app.helpers.files;

import com.work.total_app.config.StorageProperties;
import com.work.total_app.models.file.OwnerRef;
import com.work.total_app.models.file.OwnerType;
import com.work.total_app.utils.OwnerMetadataProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves permanent filesystem directory paths for stored files.
 *
 * Strategy:
 *  - Uses configurable templates from StorageProperties per OwnerType, with a fallback to OTHER.
 *  - Interpolates placeholders using an OwnerMetadataProvider for the specific owner id.
 *  - Applies basic sanitization to path segments to avoid unsafe characters.
 */
@Component
public class PathResolver {

    private final StorageProperties storageProperties;
    private final Map<OwnerType, OwnerMetadataProvider> providers;

    /**
     * Constructor that auto-wires all dependencies.
     * Uses @Lazy on providers to avoid circular dependency issues with services.
     */
    public PathResolver(StorageProperties storageProperties,
                       @Lazy List<OwnerMetadataProvider> providerList) {
        this.storageProperties = storageProperties;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        OwnerMetadataProvider::supports,
                        provider -> provider
                ));
    }

    /**
     * Build the absolute directory path where an owner's files should be persisted.
     */
    public Path resolvePermanentDir(OwnerRef owner) {
        String template = storageProperties.getTemplates().getOrDefault(owner.type(),
                storageProperties.getTemplates().get(OwnerType.OTHER));
        Map<String, String> meta = providerFor(owner.type()).metadataFor(owner.id());

        String resolved = substitute(template, meta, owner);
        return Paths.get(resolved).normalize();
    }

    private OwnerMetadataProvider providerFor(OwnerType type) {
        var p = providers.get(type);
        if (p == null) throw new IllegalStateException("No metadata provider for " + type);
        return p;
    }

    /** Replace template placeholders with metadata and generic owner fallbacks. */
    private String substitute(String template, Map<String, String> meta, OwnerRef owner) {
        String out = template;
        // întâi placeholders din meta
        for (var e : meta.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", safe(e.getValue()));
        }
        // fallback-uri generice
        out = out.replace("{ownerType}", safe(owner.type().name()));
        out = out.replace("{ownerId}", String.valueOf(owner.id()));

        // prepend baseDir și curăță dublurile de slash
        out = storageProperties.getBaseDir() + "/" + out;
        return out.replaceAll("/{2,}", "/");
    }

    /** Make a path segment safe for filesystem usage (strip diacritics, disallow reserved chars, limit length). */
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
