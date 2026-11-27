package businessjohnv.moneymod;

import businessjohnv.moneymod.api.AccountId;
import businessjohnv.moneymod.api.AccountType;
import businessjohnv.moneymod.core.MoneyService;
import businessjohnv.moneymod.mod.MoneyCommands;
import businessjohnv.moneymod.mod.MoneyState;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;

public class MoneyMod implements ModInitializer {
	public static final String MOD_ID = "money-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static volatile MoneyService moneyService;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			moneyService = new MoneyService(MoneyState.get(server), Clock.systemUTC());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			moneyService = null;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			money().ensureAccount(AccountId.getAccount(handler.player.getUuid()), AccountType.PERSONAL);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			MoneyCommands.Register(dispatcher);
		});

		LOGGER.info("Money_Mod Initialized");
	}

	public static MoneyService money() {
		MoneyService current = moneyService;
		if (current == null) {
			throw new IllegalStateException("Money is not available (server closed)");
		}

		return current;
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
