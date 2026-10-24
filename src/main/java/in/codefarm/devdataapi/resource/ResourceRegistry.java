package in.codefarm.devdataapi.resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ResourceRegistry {

    private final Map<String, ResourceDefinition> resources = new LinkedHashMap<>();

    public ResourceRegistry() {
        resources.put("users", new ResourceDefinition(
                "users",
                List.of("firstName", "lastName", "email", "status"),
                List.of("id", "firstName", "lastName", "email", "createdAt"),
                List.of("status", "email")
        ));
        resources.put("products", new ResourceDefinition(
                "products",
                List.of("name", "category", "status"),
                List.of("id", "name", "price", "category"),
                List.of("category", "status")
        ));
        resources.put("orders", new ResourceDefinition(
                "orders",
                List.of("status", "customerId"),
                List.of("id", "customerId", "createdAt"),
                List.of("status", "customerId")
        ));
    }

    public boolean contains(String resourceName) {
        return resources.containsKey(normalize(resourceName));
    }

    public ResourceDefinition get(String resourceName) {
        return resources.get(normalize(resourceName));
    }

    public Map<String, ResourceDefinition> all() {
        return Map.copyOf(resources);
    }

    private String normalize(String resourceName) {
        if (resourceName == null) {
            return "";
        }
        return resourceName.trim().toLowerCase();
    }
}
