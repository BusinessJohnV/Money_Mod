package businessjohnv.moneymod.core;

import businessjohnv.moneymod.api.Account;
import businessjohnv.moneymod.api.AccountType;

public record AccountBalance(Account id, AccountType type, long balance) {
    public AccountBalance withBalance(long newBalance) {
        return new AccountBalance(id, type, newBalance);
    }
}
