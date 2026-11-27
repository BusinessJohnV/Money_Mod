package businessjohnv.moneymod.api;

import java.util.UUID;

public record Account(UUID id) {
    public Account {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
    }

    public static Account createAccount(UUID playerId) {
        return new Account(playerId);
    }

    public static Account createAccountRandom(String playerName) {
        return new Account(UUID.randomUUID());
    }

    public String getAccountId() {
        return id.toString();
    }
}
