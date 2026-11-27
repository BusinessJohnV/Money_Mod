package businessjohnv.moneymod.mod;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.AccountType;
import businessjohnv.moneymod.api.MoneyResult;
import businessjohnv.moneymod.api.Transaction;
import businessjohnv.moneymod.core.Account;
import businessjohnv.moneymod.core.AuditReport;
import businessjohnv.moneymod.core.Money;
import businessjohnv.moneymod.core.MoneyService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;

public final class MoneyCommands {
    private static final String CURRENCY = "crowns";

    private MoneyCommands() {}

    public static void Register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("money")
                        .then(CommandManager.literal("balances")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(MoneyCommands::BalanceAll))
                        .then(CommandManager.literal("balance")
                                .executes(MoneyCommands::Balance))
                        .then(CommandManager.literal("pay")
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", StringArgumentType.word())
                                                .executes(MoneyCommands::Pay))))
                        .then(CommandManager.literal("give")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", StringArgumentType.word())
                                                .executes(MoneyCommands::Give))))
                        .then(CommandManager.literal("take")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", StringArgumentType.word())
                                                .executes(MoneyCommands::Take))))
                        .then(CommandManager.literal("audit")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(MoneyCommands::Audit))
                        .then(CommandManager.literal("history")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("limit", StringArgumentType.word())
                                                .executes(MoneyCommands::History))))
        );
    }

    private static int BalanceAll(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        Collection<Account> balances = MoneyMod.money().getBalanceAll();
        if (balances.isEmpty()) return 0;

        for (Account account : balances) {
            commandContext.getSource().sendFeedback(() -> Text.literal("Id: " + account.id() + " Type: " + account.type() + " Balance: " + account.balance()), false);
        }

        return 1;
    }

    private static int Balance(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        ServerPlayerEntity player = commandContext.getSource().getPlayerOrThrow();
        long balance = MoneyMod.money().getBalance(AccountId.getAccount(player.getUuid())).orElse(0L);
        commandContext.getSource().sendFeedback(() -> Text.literal("Balance: " + Money(balance)), false);

        return 1;
    }

    private static int Pay(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        ServerCommandSource source = commandContext.getSource();
        ServerPlayerEntity sender = source.getPlayer();
        ServerPlayerEntity target = source.getPlayer();
        OptionalLong amount = ReadAmount(commandContext);
        if (amount.isEmpty()) return 0;

        MoneyService moneyService = MoneyMod.money();
        moneyService.ensureAccount(AccountId.getAccount(target.getUuid()), AccountType.PERSONAL);
        MoneyResult result = moneyService.transfer(AccountId.getAccount(sender.getUuid()), AccountId.getAccount(target.getUuid()), amount.getAsLong(), "payment");

        if (result.isSuccess()) {
            String text = Money(amount.getAsLong());
            source.sendFeedback(() -> Text.literal("Sent " + text + " to the player " + target.getName().getString()), false);
            target.sendMessage(Text.literal("Player " + sender.getName().getString() + " sent you " + text));

            return 1;
        }

        source.sendError(Text.literal(ErrorText(result)));
        return 0;
    }

    private static int Give(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        ServerCommandSource source = commandContext.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(commandContext, "target");
        OptionalLong amount = ReadAmount(commandContext);
        if (amount.isEmpty()) return 0;

        MoneyService moneyService = MoneyMod.money();
        AccountId id = AccountId.getAccount(target.getUuid());
        moneyService.ensureAccount(id, AccountType.PERSONAL);
        MoneyResult result = moneyService.print(id, amount.getAsLong(), "Money printed by " + source.getName() + " to " + target.getName());

        if (result.isSuccess()) {
            source.sendFeedback(() -> Text.literal("Printed " + Money(amount.getAsLong()) + " from " + target.getName().getString()), true);
            return 1;
        }

        source.sendError(Text.literal(ErrorText(result)));
        return 0;
    }

    private static int Take(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        ServerCommandSource source = commandContext.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(commandContext, "target");
        OptionalLong amount = ReadAmount(commandContext);
        if (amount.isEmpty()) return 0;

        MoneyResult result = MoneyMod.money().burn(AccountId.getAccount(target.getUuid()), amount.getAsLong(), "Money burned by " + source.getName() + " from " + target.getName().getString());

        if (result.isSuccess()) {
            source.sendFeedback(() -> Text.literal("Burned " + Money(amount.getAsLong()) + " from " + target.getName().getString()), true);
            return 1;
        }

        source.sendError(Text.literal(ErrorText(result)));
        return 0;
    }

    private static int Audit(CommandContext<ServerCommandSource> commandContext){
        AuditReport r = MoneyMod.money().audit();
        String text = "Balance summary: " + Money(r.totalBalances())
                + " | printed: " + Money(r.totalPrinted())
                + " | burned: " + Money(r.totalBurned())
                + " | " + (r.consistent() ? "OK" : "Warning, checksum does not fit!");

        if (r.consistent()) {
            commandContext.getSource().sendFeedback(() -> Text.literal(text), false);
        }
        else {
            commandContext.getSource().sendError(Text.literal(text));
        }

        return r.consistent() ? 1 : 0;
    }

    private static int History(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(commandContext, "target");
        MoneyService moneyService = MoneyMod.money();
        AccountId id = AccountId.getAccount(player.getUuid());

        List<Transaction> list = moneyService.history(id, IntegerArgumentType.getInteger(commandContext, "limit"));
        for (businessjohnv.moneymod.api.Transaction transaction : list) {
            String line = transaction.toString();
            commandContext.getSource().sendFeedback(() -> Text.literal(line), false);
        }

        return 1;
    }

    private static OptionalLong ReadAmount(CommandContext<ServerCommandSource> commandContext) {
        OptionalLong amount = Money.parse(StringArgumentType.getString(commandContext, "amount"));
        if (amount.isEmpty()){
            commandContext.getSource().sendError(Text.literal("Amount not valid. Maybe try something like 50 or 100.50"));
        }

        return amount;
    }

    private static String Money(long pennies) {
        return Money.format(pennies) + " " + CURRENCY;
    }

    private static String ErrorText(MoneyResult result) {
        return switch (result) {
            case MoneyResult.Success ok -> "";
            case MoneyResult.AccountNotFound notFound -> "Account not found or does not exist";
            case MoneyResult.BalanceLimitExceeded limitExceeded -> "Balance limit exceeded after transfer";
            case MoneyResult.InsufficientFunds insufficientFunds -> "Insufficient funds on your account";
            case MoneyResult.InvalidAmount invalidAmount -> "Invalid amount of money";
            case MoneyResult.SameAccount sameAccount -> "Cannot transfer money to yourself";
        };
    }
}
