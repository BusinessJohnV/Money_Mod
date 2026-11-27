package businessjohnv.moneymod.core;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.AccountType;
import businessjohnv.moneymod.api.MoneyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyServiceTest {
    private MoneyService money;
    private final AccountId acc1 = AccountId.getAccountRandom();
    private final AccountId acc2 = AccountId.getAccountRandom();

    @BeforeEach
    void setUp() {
        money = new MoneyService(new InMemoryAccountRepository(), Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        money.ensureAccount(acc1, AccountType.PERSONAL);
        money.ensureAccount(acc2, AccountType.PERSONAL);
    }

    @Test
    void printIncreaseBalanceAndAuditStaysConsistent() {
        assertInstanceOf(MoneyResult.Success.class, money.print(acc1, 1000, "test"));
        assertEquals(OptionalLong.of(1000), money.getBalance(acc1));
        assertTrue(money.audit().consistent());
    }

    @Test
    void transferMovesMoneyWithoutChangingTotal() {
        money.print(acc1, 1000, "");
        assertInstanceOf(MoneyResult.Success.class, money.transfer(acc1, acc2, 400, "payment"));
        assertEquals(OptionalLong.of(600), money.getBalance(acc1));
        assertEquals(OptionalLong.of(400), money.getBalance(acc2));
        assertEquals(1000, money.audit().totalBalances());
        assertTrue(money.audit().consistent());
    }

    @Test
    void failedTransferChangesNothing() {
        money.print(acc1, 100, "");
        assertInstanceOf(MoneyResult.InsufficientFunds.class, money.transfer(acc1, acc2, 101, ""));
        assertEquals(OptionalLong.of(100), money.getBalance(acc1));
        assertEquals(OptionalLong.of(0), money.getBalance(acc2));
        assertTrue(money.audit().consistent());
    }

    @Test
    void rejectsInvalidAmounts() {
        assertInstanceOf(MoneyResult.InvalidAmount.class, money.transfer(acc1, acc2, 0, ""));
        assertInstanceOf(MoneyResult.InvalidAmount.class, money.transfer(acc1, acc2, -5, ""));
        assertInstanceOf(MoneyResult.InvalidAmount.class, money.print(acc1, MoneyService.MAX_BALANCE + 1, ""));
    }

    @Test
    void rejectsSelfTransferAndUnknownAccounts() {
        money.print(acc1, 100, "");
        assertInstanceOf(MoneyResult.SameAccount.class, money.transfer(acc1, acc1, 100, ""));
        assertInstanceOf(MoneyResult.AccountNotFound.class, money.transfer(acc1, AccountId.getAccountRandom(), 10, ""));
        assertInstanceOf(MoneyResult.AccountNotFound.class, money.print(AccountId.getAccountRandom(), 10, ""));
    }

    @Test
    void balanceLimitEnforced() {
        money.print(acc1, MoneyService.MAX_BALANCE, "");
        assertInstanceOf(MoneyResult.BalanceLimitExceeded.class, money.print(acc1, 1, ""));
        money.print(acc2, 1, "");
        assertInstanceOf(MoneyResult.BalanceLimitExceeded.class, money.transfer(acc2, acc1, 1, ""));
        assertEquals(OptionalLong.of(1), money.getBalance(acc2));
    }

    @Test
    void burnReducesSupply() {
        money.print(acc1, 500, "");
        assertInstanceOf(MoneyResult.Success.class, money.burn(acc1, 200, ""));
        assertEquals(300, money.audit().expected());
        assertTrue(money.audit().consistent());
        assertInstanceOf(MoneyResult.InsufficientFunds.class, money.burn(acc1, 301, ""));
    }

    @Test
    void historyIsNewestFirst() {
        money.print(acc1, 1000, "");
        money.transfer(acc1, acc2, 400, "first");
        money.transfer(acc1, acc2, 200, "second");
        var h = money.history(acc2, 10);
        assertEquals(2, h.size());
        assertEquals("second", h.getFirst().note());
    }

    @Test
    void moneyParseAndFormat() {
        assertEquals(OptionalLong.of(1250), Money.parse("12.50"));
        assertEquals(OptionalLong.of(1250), Money.parse("12.5"));
        assertEquals(OptionalLong.of(1200), Money.parse("12"));
        assertEquals(OptionalLong.of(5), Money.parse("0.05"));
        assertTrue(Money.parse("12.505").isEmpty());
        assertTrue(Money.parse("1e999999999").isEmpty());
        assertTrue(Money.parse("-5").isEmpty());
        assertTrue(Money.parse("abc").isEmpty());
        assertEquals("12.50", Money.format(1250));
        assertEquals("0.05", Money.format(5));
        assertEquals("0.00", Money.format(0));
    }
}
