package in.codefarm.devdataapi.storage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import in.codefarm.devdataapi.config.DevDataApiProperties;
import tools.jackson.databind.ObjectMapper;

@Repository
public class JsonResourceRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonResourceRepository.class);

    private final ObjectMapper objectMapper;
    private final Path dataDirectory;

    public JsonResourceRepository(DevDataApiProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.dataDirectory = Path.of(properties.getDataDir());
    }

    public <T> List<T> readList(String resourceName, Class<T> type) {
        List<T> aggregated = new ArrayList<>();
        List<Path> files = findResourceFiles(resourceName);

        for (Path file : files) {
            if (!Files.exists(file)) {
                continue;
            }

            List<T> items = objectMapper.readValue(
                    file.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, type)
            );
            if (items != null) {
                aggregated.addAll(items);
            }
        }

        return deduplicateByLastId(resourceName, aggregated);
    }

    public <T> void writeList(String resourceName, List<T> values) {
        Path file = resolve(resourceName);
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), values);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write resource " + resourceName, e);
        }
    }

    public Path resolve(String resourceName) {
        return dataDirectory.resolve(resourceName).resolve(resourceName + ".json");
    }

    public <T> void ensureResource(String resourceName, List<T> seedData) {
        Path resourceDir = dataDirectory.resolve(resourceName);
        Path file = resolve(resourceName);
        try {
            Files.createDirectories(resourceDir);
            if (Files.notExists(file) && seedData != null) {
                writeList(resourceName, seedData);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize resource directory: " + resourceName, e);
        }
    }

    public void warnIfDuplicateIds(String resourceName) {
        Map<Long, String> seen = new LinkedHashMap<>();
        for (Path file : findResourceFiles(resourceName)) {
            if (!Files.exists(file)) {
                continue;
            }

            List<Map<String, Object>> rows = objectMapper.readValue(
                    file.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );

            for (Map<String, Object> row : rows) {
                Object rawId = row.get("id");
                if (!(rawId instanceof Number number)) {
                    continue;
                }
                long id = number.longValue();
                if (seen.containsKey(id)) {
                    log.warn("Duplicate id {} detected in resource {}. Keeping last occurrence from {}.", id, resourceName, file.getFileName());
                }
                seen.put(id, file.getFileName().toString());
            }
        }
    }

    private <T> List<T> deduplicateByLastId(String resourceName, List<T> items) {
        Map<Long, T> deduplicated = new LinkedHashMap<>();
        for (T item : items) {
            Long id = extractId(item);
            if (id == null) {
                continue;
            }
            if (deduplicated.containsKey(id)) {
                log.warn("Duplicate id {} detected in resource {}. Keeping last occurrence in file order.", id, resourceName);
                deduplicated.remove(id);
            }
            deduplicated.put(id, item);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private Long extractId(Object item) {
        if (item == null) {
            return null;
        }

        for (Class<?> current = item.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!"id".equals(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(item);
                    return value instanceof Number number ? number.longValue() : null;
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to read id field from resource type: " + item.getClass().getName(), e);
                }
            }
        }

        return null;
    }

    private List<Path> findResourceFiles(String resourceName) {
        Path resourceDir = dataDirectory.resolve(resourceName);
        if (!Files.exists(resourceDir)) {
            return List.of();
        }

        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(resourceDir)) {
            stream.filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.equals(resourceName + ".json")
                                || (fileName.startsWith(resourceName + "-") && fileName.endsWith(".json"));
                    })
                    .sorted()
                    .forEach(files::add);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list resource files for: " + resourceName, e);
        }

        return files;
    }
}
