package businessjohnv.moneymod.core;

public record AuditReport(long totalBalances, long totalPrinted, long totalBurned) {
    public boolean consistent() {
        return totalBalances == expected();
    }

    public long expected() {
        return totalPrinted - totalBurned;
    }
}
