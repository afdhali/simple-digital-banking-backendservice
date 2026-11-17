package tutorjava.digitalbanking.account;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** 10-digit, non-sequential (not guessable / enumerable) account numbers. */
@Component
public class AccountNumberGenerator {

    private final SecureRandom random = new SecureRandom();

    public String next() {
        long value = 1_000_000_000L + random.nextLong(9_000_000_000L);
        return Long.toString(value);
    }
}
