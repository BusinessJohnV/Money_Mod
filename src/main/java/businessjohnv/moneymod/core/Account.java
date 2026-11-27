package businessjohnv.moneymod.core;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.AccountType;

public record Account(AccountId id, AccountType type, long balance) {
    public Account withBalance(long newBalance) {
        return new Account(id, type, newBalance);
    }
}
