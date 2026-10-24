package in.codefarm.devdataapi.resource;

import java.util.List;

public record ResourceDefinition(
        String name,
        List<String> searchableFields,
        List<String> sortableFields,
        List<String> allowedFilters
) {
    public ResourceDefinition {
        searchableFields = searchableFields == null ? List.of() : List.copyOf(searchableFields);
        sortableFields = sortableFields == null ? List.of() : List.copyOf(sortableFields);
        allowedFilters = allowedFilters == null ? List.of() : List.copyOf(allowedFilters);
    }
}
