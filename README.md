# dev-data-api

## GitHub release publishing

This project is configured to build a runnable Spring Boot jar and attach it to a GitHub Release.

### Manual release flow

1. Create and push a version tag:

	```bash
	git tag v0.1.0
	git push origin v0.1.0
	```

2. GitHub Actions will build the jar and upload it as a release asset automatically.

3. The generated artifact name is:

	```text
	dev-data-api-0.1.0.jar
	```

### Local jar build

```bash
./gradlew clean bootJar -PreleaseVersion=0.1.0
```

The jar will be created at:

```text
build/libs/dev-data-api-0.1.0.jar
```

