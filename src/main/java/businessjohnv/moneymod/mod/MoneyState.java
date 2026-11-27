package businessjohnv.moneymod.mod;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.AccountType;
import businessjohnv.moneymod.api.Transaction;
import businessjohnv.moneymod.api.TransactionType;
import businessjohnv.moneymod.core.Account;
import businessjohnv.moneymod.core.AccountRepository;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MoneyState extends PersistentState implements AccountRepository {
    private static final int MAX_LEDGER_SIZE = 15_000;

    private static final PersistentState.Type<MoneyState> TYPE = new PersistentState.Type<>(MoneyState::new, MoneyState::LoadNbt, null);
    private static final Path FILEPATH = Path.of("money-mod", "history.jsonl");

    private final Map<AccountId, Account> accounts = new LinkedHashMap<>();
    private final ArrayDeque<Transaction> ledger = new ArrayDeque<>();
    private long totalPrinted;
    private long totalBurned;
    private long nextTransactionId = 1;

    public static MoneyState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        return overworld.getPersistentStateManager().getOrCreate(TYPE, MoneyMod.MOD_ID);
    }

    @Override
    public Collection<Account> all() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    @Override
    public Optional<Account> find(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void put(Account account) {
        accounts.put(account.id(), account);
        markDirty();
    }

    @Override
    public long totalPrinted() {
        return totalPrinted;
    }

    @Override
    public long totalBurned() {
        return totalBurned;
    }

    @Override
    public void addMinted(long amount) {
        totalPrinted += amount;
        markDirty();
    }

    @Override
    public void addBurned(long amount) {
        totalBurned += amount;
        markDirty();
    }

    @Override
    public long nextTransactionId() {
        markDirty();
        return nextTransactionId++;
    }

    @Override
    public void appendTransaction(Transaction transaction) {
        ledger.addLast(transaction);
        while (ledger.size() > MAX_LEDGER_SIZE) {
            ledger.removeFirst();
        }

        markDirty();
        saveTransactionToHistory(transaction);
    }

    @Override
    public List<Transaction> recentTransactions(AccountId id, int limit) {
        List<Transaction> result = new ArrayList<>();
        Iterator<Transaction> iterator = ledger.descendingIterator();
        while (iterator.hasNext() && result.size() < limit) {
            Transaction transaction = iterator.next();
            if (id.equals(transaction.from()) || id.equals(transaction.to())) result.add(transaction);
        }

        return result;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList accountList = new NbtList();

        for (Account a : accounts.values()) {
            NbtCompound c = new NbtCompound();
            c.putString("id", a.id().value().toString());
            c.putString("type", a.type().name());
            c.putLong("balance", a.balance());

            accountList.add(c);
        }
        nbt.put("accounts", accountList);

        NbtList ledgerList = new NbtList();

        for (Transaction ts : ledger) {
            NbtCompound c = new NbtCompound();
            c.putLong("id", ts.id());
            c.putLong("time", ts.timestampMillis());
            c.putString("type", ts.type().name());

            if (ts.from() != null) c.putString("from", ts.from().value().toString());
            if (ts.to() != null) c.putString("to", ts.to().value().toString());

            c.putLong("amount", ts.amount());
            c.putString("note", ts.note());

            ledgerList.add(c);
        }
        nbt.put("ledger", ledgerList);

        nbt.putLong("totalPrinted", totalPrinted);
        nbt.putLong("totalBurned", totalBurned);
        nbt.putLong("nextTransactionId", nextTransactionId);

        return nbt;
    }

    public static MoneyState LoadNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        MoneyState state = new MoneyState();

        NbtList accountList = nbt.getList("accounts", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < accountList.size(); i++) {
            NbtCompound c = accountList.getCompound(i);
            AccountId id = new  AccountId(UUID.fromString(c.getString("id")));
            AccountType type = AccountType.valueOf(c.getString("type"));

            state.accounts.put(id, new Account(id, type, c.getLong("balance")));
        }

        NbtList ledgerList = nbt.getList("ledger", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < ledgerList.size(); i++) {
            NbtCompound c = ledgerList.getCompound(i);
            AccountId from = c.contains("from") ? new AccountId(UUID.fromString(c.getString("from"))) : null;
            AccountId to = c.contains("to") ? new AccountId(UUID.fromString(c.getString("to"))) : null;

            state.ledger.addLast(new Transaction(
                    c.getLong("id"), c.getLong("time"), TransactionType.valueOf(c.getString("type")), from, to, c.getLong("amount"), c.getString("note")
                    ));
        }

        state.totalPrinted = nbt.getLong("totalPrinted");
        state.totalBurned = nbt.getLong("totalBurned");
        state.nextTransactionId = nbt.getLong("nextTransactionId");

        return state;
    }

    private void saveTransactionToHistory(Transaction transaction) {
        JsonObject json = new JsonObject();

        json.addProperty("id", transaction.id());
        json.addProperty("time", transaction.timestampMillis());
        json.addProperty("type", transaction.type().name());
        json.addProperty("from", transaction.from() == null ? null : transaction.from().toString());
        json.addProperty("to", transaction.to() == null ? null : transaction.to().toString());
        json.addProperty("amount", transaction.amount());
        json.addProperty("note", transaction.note());

        try {
            Files.createDirectories(FILEPATH.getParent());
            Files.writeString(FILEPATH, json + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            MoneyMod.LOGGER.error("Error saving transaction #{} to history file {}",transaction.id(), FILEPATH, e);
        }
    }
}
