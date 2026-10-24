package in.codefarm.devdataapi.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi devDataApiOpenApi() {
        return GroupedOpenApi.builder()
                .group("dev-data-api")
                .pathsToMatch("/api/**")
                .build();
    }
}
