package asia.itsec.auth.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordEncoderAdapterTest {

    private final BcryptPasswordEncoderAdapter adapter = new BcryptPasswordEncoderAdapter();

    @Test
    void encode_thenMatches_roundTripsCorrectly() {
        String encoded = adapter.encode("Passw0rd!");

        assertThat(encoded).isNotEqualTo("Passw0rd!");
        assertThat(adapter.matches("Passw0rd!", encoded)).isTrue();
    }

    @Test
    void matches_wrongPassword_returnsFalse() {
        String encoded = adapter.encode("Passw0rd!");

        assertThat(adapter.matches("WrongPassword", encoded)).isFalse();
    }
}
