package com.hyperativa.desafio.service;

import com.hyperativa.desafio.config.AppProperties;
import com.hyperativa.desafio.exception.InvalidCardNumberException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardHashServiceTest {

    private CardHashService service;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Security(new AppProperties.Jwt("x", 1, "i")),
                new AppProperties.Card("test-pepper"));
        service = new CardHashService(props);
    }

    @Test
    void normalizeStripsNonDigits() {
        assertThat(service.normalize("4456 8979 9999 9999")).isEqualTo("4456897999999999");
        assertThat(service.normalize("4456-8979-9999-9999")).isEqualTo("4456897999999999");
    }

    @Test
    void hashIsDeterministicAndIgnoresFormatting() {
        String h1 = service.hash("4456897999999999");
        String h2 = service.hash("4456 8979 9999 9999");
        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    void hashDiffersForDifferentCards() {
        assertThat(service.hash("4456897999999999"))
                .isNotEqualTo(service.hash("4456897999999998"));
    }

    @Test
    void rejectsTooShortNumbers() {
        assertThatThrownBy(() -> service.hash("123"))
                .isInstanceOf(InvalidCardNumberException.class);
    }

    @Test
    void rejectsTooLongNumbers() {
        assertThatThrownBy(() -> service.hash("12345678901234567890"))
                .isInstanceOf(InvalidCardNumberException.class);
    }

    @Test
    void rejectsNullInput() {
        assertThatThrownBy(() -> service.hash(null))
                .isInstanceOf(InvalidCardNumberException.class);
    }

    @Test
    void differentPepperProducesDifferentHash() {
        AppProperties otherProps = new AppProperties(
                new AppProperties.Security(new AppProperties.Jwt("x", 1, "i")),
                new AppProperties.Card("other-pepper"));
        CardHashService other = new CardHashService(otherProps);
        assertThat(service.hash("4456897999999999"))
                .isNotEqualTo(other.hash("4456897999999999"));
    }
}
