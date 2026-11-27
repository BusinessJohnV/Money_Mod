package businessjohnv.moneymod.api;

public record Transaction(
        long id,
        long timestampMillis,
        TransactionType type,
        AccountId from,
        AccountId to,
        long amount,
        String note
) {
}
