package in.codefarm.devdataapi.domain.user;

public record UserUpdateRequest(
        String firstName,
        String lastName,
        String email,
        String status
) {}
