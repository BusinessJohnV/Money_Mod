package businessjohnv.moneymod;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoneyMod implements ModInitializer {
	public static final String MOD_ID = "money-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Money_Mod Initializing");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
