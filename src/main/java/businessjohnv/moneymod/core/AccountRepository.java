package businessjohnv.moneymod.core;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.Transaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
     Collection<Account> all();

     Optional<Account> find(AccountId id);

     void put(Account account);

     long totalPrinted();

     long totalBurned();

     void addMinted(long amount);

     void addBurned(long amount);

     long nextTransactionId();

     void appendTransaction(Transaction transaction);

     List<Transaction> recentTransactions(AccountId id, int limit);
}
