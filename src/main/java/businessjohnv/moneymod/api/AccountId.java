package businessjohnv.moneymod.api;

import java.util.UUID;

public record AccountId(UUID value) {
    public AccountId {
        if (value == null) throw new IllegalArgumentException("id cannot be null");
    }

    public static AccountId getAccount(UUID playerId) {
        return new AccountId(playerId);
    }

    public static AccountId getAccountRandom() {
        return new AccountId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
