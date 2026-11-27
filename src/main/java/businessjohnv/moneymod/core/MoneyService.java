package businessjohnv.moneymod.core;

import businessjohnv.moneymod.api.*;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class MoneyService implements MoneyAPI {
    public static final long MAX_BALANCE = 1_000_000_000_000_000L;

    private final AccountRepository accRepo;
    private final Clock clock;

    public MoneyService(AccountRepository accRepo, Clock clock) {
        this.accRepo = accRepo;
        this.clock = clock;
    }

    public Account ensureAccount(AccountId id, AccountType accountType) {
        return accRepo.find(id).orElseGet(() -> {
            Account created = new Account(id, accountType, 0);
            accRepo.put(created);
            return created;
        });
    }

    @Override
    public boolean hasAccount(AccountId id) {
        return accRepo.find(id).isPresent();
    }

    @Override
    public Collection<Account> getBalanceAll() {
        return accRepo.all();
    }

    @Override
    public OptionalLong getBalance(AccountId id) {
        return accRepo.find(id).map(a -> OptionalLong.of(a.balance())).orElse(OptionalLong.empty());
    }

    @Override
    public MoneyResult transfer(AccountId from, AccountId to, long amount, String note) {
        if (!ValidAmount(amount)) return new MoneyResult.InvalidAmount();
        if (from.equals(to)) return new MoneyResult.SameAccount();

        Optional<Account> src = accRepo.find(from);
        if (src.isEmpty()) return new MoneyResult.AccountNotFound(from);

        Optional<Account> dst = accRepo.find(to);
        if (dst.isEmpty()) return new MoneyResult.AccountNotFound(to);

        Account a = src.get();
        Account b = dst.get();
        if (a.balance() < amount) return new MoneyResult.InsufficientFunds(a.balance(), amount);
        long newBalanceA = a.balance() - amount;
        long newBalanceB = b.balance() + amount;
        if (newBalanceB > MAX_BALANCE) return new MoneyResult.BalanceLimitExceeded();

        accRepo.put(a.withBalance(newBalanceA));
        accRepo.put(b.withBalance(newBalanceB));

        return new MoneyResult.Success(log(TransactionType.TRANSFER, from, to, amount, note));
    }

    public AuditReport audit() {
        long sum = 0;
        for (Account a : accRepo.all()) sum += a.balance();
        return new AuditReport(sum, accRepo.totalPrinted(), accRepo.totalBurned());
    }

    public List<Transaction> history(AccountId id, int limit) {
        return accRepo.recentTransactions(id, limit);
    }

    public static boolean ValidAmount(long amount) {
        return amount > 0 && amount <= MAX_BALANCE;
    }

    private Transaction log(TransactionType type, AccountId from, AccountId to, long amount, String note) {
        Transaction ts = new Transaction(accRepo.nextTransactionId(), clock.millis(), type, from, to, amount, note == null ? "" : note);
        accRepo.appendTransaction(ts);
        return ts;
    }

    /** Admin funkce na "tisknutí" peněz */
    public MoneyResult print(AccountId to, long amount, String note) {
        Optional<Account> dst = accRepo.find(to);
        if (!ValidAmount(amount)) return new MoneyResult.InvalidAmount();
        if (dst.isEmpty()) return new MoneyResult.AccountNotFound(to);

        Account a = dst.get();
        long newBalance = a.balance() + amount;
        if (newBalance > MAX_BALANCE) return new MoneyResult.BalanceLimitExceeded();

        accRepo.put(a.withBalance(newBalance));
        accRepo.addMinted(amount);

        return new MoneyResult.Success(log(TransactionType.PRINT, null, to, amount, note));
    }

    /** Admin funkce na "pálení" peněz */
    public  MoneyResult burn(AccountId from, long amount, String note) {
        Optional<Account> src = accRepo.find(from);
        if (!ValidAmount(amount)) return new MoneyResult.InvalidAmount();
        if (src.isEmpty()) return new MoneyResult.AccountNotFound(from);

        Account a = src.get();
        long newBalance = a.balance() - amount;
        if (a.balance() < amount) return new MoneyResult.InsufficientFunds(a.balance(), amount);

        accRepo.put(a.withBalance(newBalance));
        accRepo.addBurned(amount);

        return new MoneyResult.Success(log(TransactionType.BURN, from, null, amount, note));
    }
}
