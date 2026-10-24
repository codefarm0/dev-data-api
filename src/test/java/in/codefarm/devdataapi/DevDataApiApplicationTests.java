package in.codefarm.devdataapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DevDataApiApplicationTests {

	@LocalServerPort
	private int port;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
		Path tempDir = Files.createTempDirectory("devdataapi-test");
		registry.add("devdataapi.data-dir", () -> tempDir.toString());
	}

	@Test
	void usersApiShouldListAndCreateRecords() throws Exception {
		HttpClient client = HttpClient.newHttpClient();

		HttpRequest listRequest = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/users"))
				.GET()
				.build();
		HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
		assertThat(listResponse.statusCode()).isEqualTo(200);
		assertThat(listResponse.body()).contains("email");

		String payload = """
			{
			  "firstName": "Charlie",
			  "lastName": "Brown",
			  "email": "charlie.brown@example.test",
			  "status": "ACTIVE"
			}
			""";

		HttpRequest createRequest = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/users"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();
		HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
		assertThat(createResponse.statusCode()).isEqualTo(201);
		assertThat(createResponse.body()).contains("charlie.brown@example.test");
		assertThat(createResponse.body()).contains("\"id\"");
	}
}
