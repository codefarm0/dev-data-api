package in.codefarm.devdataapi.domain.user;

public record UserCreateRequest(
        String firstName,
        String lastName,
        String email,
        String status
) {
    public UserCreateRequest {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
