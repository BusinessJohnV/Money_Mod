package businessjohnv.moneymod.api;

import businessjohnv.moneymod.core.Account;

import java.util.Collection;
import java.util.OptionalLong;

public interface MoneyAPI {
    boolean hasAccount(AccountId id);

    Collection<Account> getBalanceAll();

    OptionalLong getBalance(AccountId id);

    MoneyResult transfer(AccountId from, AccountId to, long amount, String note);
}
