package businessjohnv.moneymod.core;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.Transaction;

import java.util.*;

public class InMemoryAccountRepository implements AccountRepository {
    private final Map<AccountId, Account> accounts = new HashMap<>();
    private final List<Transaction> ledger = new ArrayList<>();
    private long printed, burned, nextId = 1;

    @Override
    public Collection<Account> all() {
        return accounts.values();
    }

    @Override
    public Optional<Account> find(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void put(Account account) {
        accounts.put(account.id(), account);
    }

    @Override
    public long totalPrinted() {
        return printed;
    }

    @Override
    public long totalBurned() {
        return burned;
    }

    @Override
    public void addMinted(long amount) {
        printed += amount;
    }

    @Override
    public void addBurned(long amount) {
        burned += amount;
    }

    @Override
    public long nextTransactionId() {
        return nextId++;
    }

    @Override
    public void appendTransaction(Transaction transaction) {
        ledger.add(transaction);
    }

    @Override
    public List<Transaction> recentTransactions(AccountId id, int limit) {
        List<Transaction> out = new ArrayList<>();
        for (int i = ledger.size() - 1; i >= 0 && out.size() <limit; i--) {
            Transaction t = ledger.get(i);
            if (id.equals(t.from()) || id.equals(t.to())) {
                out.add(t);
            }
        }

        return out;
    }
}
