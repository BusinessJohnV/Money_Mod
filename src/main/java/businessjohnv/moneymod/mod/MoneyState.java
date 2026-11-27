package businessjohnv.moneymod.mod;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.Transaction;
import businessjohnv.moneymod.core.Account;
import businessjohnv.moneymod.core.AccountRepository;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.*;

public class BankState extends PersistentState implements AccountRepository {
    private static final int MAX_LEDGER_SIZE = 10_000;

    private static final PersistentState.Type<BankState> TYPE = new PersistentState.Type<>(BankState::new, BankState::, null);

    private final Map<AccountId, Account> accounts = new LinkedHashMap<>();
    private final ArrayDeque<Transaction> ledger = new ArrayDeque<>();
    private long totalPrinted;
    private long totalBurned;
    private long nextTransactionId = 1;

    public static BankState get(MinecraftServer server) {
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
        MoneyMod.LOGGER.info("[ledger] #{} {} {} -> {} amount={} note={}",
                transaction.id(), transaction.type(), transaction.from(), transaction.to(), transaction.amount(), transaction.note());
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
        return null;
    }
}
