package net.fabricmc.endallmagic;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.endallmagic.api.ModCommands;
import net.fabricmc.endallmagic.client.ClientUtils;
import net.fabricmc.endallmagic.client.MagicHud;
import net.fabricmc.endallmagic.client.gui.MagicScreenFactory;
import net.fabricmc.endallmagic.common.MagicUser;
import net.fabricmc.endallmagic.common.network.ClientToServer;
import net.fabricmc.endallmagic.common.particles.ModParticles;
import net.fabricmc.endallmagic.common.sounds.ModSoundEvents;
import net.fabricmc.endallmagic.common.spells.FireBall;
import net.fabricmc.endallmagic.common.spells.HealSpell;
import net.fabricmc.endallmagic.common.spells.Spell;
import net.fabricmc.endallmagic.common.spells.SpellConfig;
import net.fabricmc.endallmagic.config.EndAllMagicConfig;
import net.fabricmc.endallmagic.items.Staff;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.MinecraftClient;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

public class EndAllMagic implements ModInitializer {
	// custom reg public static final Registry<Spell> SPELL = createRegistry("spell", Spell.class);

	public static final String MOD_ID = "endallmagic";
	// screen handler reg
	public static final ScreenHandlerType<MagicScreenFactory.Handler> MAGIC_SCREEN = Registry.register(Registry.SCREEN_HANDLER, new Identifier(MOD_ID, "magic_screen"), MagicScreenFactory.type());
	// spell reg
	public static final Registry<Spell> SPELL = FabricRegistryBuilder.createSimple(Spell.class, new Identifier(MOD_ID, "spell")).buildAndRegister();
	
	public static ConfigHolder<EndAllMagicConfig> configHolder;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Staff STAFF = new Staff(new FabricItemSettings().group(ItemGroup.MISC));
	@Override
	public void onInitialize() {
		AutoConfig.register(EndAllMagicConfig.class, JanksonConfigSerializer::new);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "staff"), STAFF);
		configHolder = AutoConfig.getConfigHolder(EndAllMagicConfig.class);


		// ServerLoginNetworking.registerGlobalReceiver(NetworkFactory.CONFIG, NetworkFactory::loginQueryResponse); send config deets on login
		ServerPlayNetworking.registerGlobalReceiver(ClientToServer.ID, ClientToServer::handle);
		ServerPlayNetworking.registerGlobalReceiver(ClientToServer.SCREEN, ClientToServer::switchScreen);

		SpellConfig.register();
		ModSoundEvents.register();
		ModParticles.register();
		ModCommands.register();
		commonEvents();

		Registry.register(Registry.ATTRIBUTE, new Identifier(MOD_ID, "mana_regen"), EntityAttributes.MANA_REGEN);

		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		if (FabricLoader.getInstance().getEnvironmentType() ==  EnvType.CLIENT){
			((ClientUtils) MinecraftClient.getInstance()).addSpell(new FireBall());
			((ClientUtils) MinecraftClient.getInstance()).addSpell(new HealSpell());
		}
			
	}

	public static class DataTrackers {
        public static final TrackedData<Integer> MANA = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
		public static final TrackedData<Integer> LEVEL = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
		public static final TrackedData<Integer> AFFINITY = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
		public static final TrackedData<Boolean> SHOW_MANA = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
		private DataTrackers(){};
	}

	public static class EntityAttributes {
		public static final EntityAttribute MANA_COST = new ClampedEntityAttribute("attribute.name.generic." + MOD_ID + ".mana_cost", 1D, 0D, 1024D).setTracked(true);
		public static final EntityAttribute MANA_REGEN = new ClampedEntityAttribute("attribute.name.generic." + MOD_ID + ".mana_regen", 1D, 0D, 1024D).setTracked(true);
		private EntityAttributes(){};
	}

	public static EndAllMagicConfig getConfig() {
		return configHolder.getConfig();
	}

	public static void commonEvents() {

		//-----Copy Player Data Callback-----//
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			MagicUser newUser = (MagicUser) newPlayer;
			MagicUser oldUser = (MagicUser) oldPlayer;
			newUser.setMana(oldUser.getCurrentMana());
			oldUser.getKnownSpells().forEach(newUser::setKnownSpell);
			newUser.setAffinity(oldUser.getAffinity());
			newUser.setLevel(oldUser.getLevel());
		});

		//-----Command Callback-----//
		CommandRegistrationCallback.EVENT.register(ModCommands::init);
	}
}
