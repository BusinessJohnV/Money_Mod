package businessjohnv.moneymod.api;

public sealed interface MoneyResult {
    record Success(Transaction transaction) implements MoneyResult {}

    record InvalidAmount() implements MoneyResult {}

    record InsufficientFunds(long balance, long required) implements MoneyResult {}

    record AccountNotFound(AccountId id) implements MoneyResult {}

    record SameAccount() implements MoneyResult {}

    record BalanceLimitExceeded() implements MoneyResult {}

    default boolean isSuccess() {
        return this instanceof Success;
    }
}
