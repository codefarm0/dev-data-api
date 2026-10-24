package in.codefarm.devdataapi.domain.user;

public record User(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status
) {
    public User {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
