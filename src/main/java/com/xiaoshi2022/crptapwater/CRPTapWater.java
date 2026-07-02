package com.xiaoshi2022.crptapwater;

import com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI;
import com.phagens.corpseorigin.register.Moditems;
import com.xiaoshi2022.crptapwater.api.CustomCorpseWaterHandler;
import com.xiaoshi2022.crptapwater.dialogue.CorpseBrotherHelper;
import com.xiaoshi2022.crptapwater.fluid.FluidRegistry;
import com.xiaoshi2022.crptapwater.network.CorpseNetwork;
import com.xiaoshi2022.crptapwater.network.SSyncWaterTroughAnimPacket;
import com.xiaoshi2022.crptapwater.network.c2s.CCorpseDialogueAnswerPacket;
import com.xiaoshi2022.crptapwater.network.c2s.CCorpseDialogueClosePacket;
import com.xiaoshi2022.crptapwater.network.c2s.CCorpseDialogueInitPacket;
import com.xiaoshi2022.crptapwater.network.s2c.SCorpseDialogueQuestionPacket;
import com.xiaoshi2022.crptapwater.network.s2c.SCorpseDialogueResponsePacket;
import com.xiaoshi2022.crptapwater.network.s2c.SOpenCorpseDialogPacket;
import com.xiaoshi2022.crptapwater.pipe.PipeBlockEntity;
import com.xiaoshi2022.crptapwater.register.BlockEntityRegistry;
import com.xiaoshi2022.crptapwater.register.BlockRegistry;
import com.xiaoshi2022.crptapwater.register.ItemRegistry;
import com.xiaoshi2022.crptapwater.village.VillageWaterTroughBlockEntity;
import com.xiaoshi2022.crptapwater.village.VillageWaterTroughPlacer;
import com.xiaoshi2022.crptapwater.village.VillagerThirstGoal;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CRPTapWater.MODID)
public class CRPTapWater {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "crptapwater";
    // Directly reference a slf4j logger
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a creative tab for Corpse Water Company
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WATER_COMPANY_TAB =
            CREATIVE_MODE_TABS.register("water_company_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.crptapwater.water_company"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ItemRegistry.LONGSHI_MINERAL_WATER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.PIPE_BLOCK_ITEM.get());
                        output.accept(ItemRegistry.WATER_TROUGH_BLOCK_ITEM.get());
                        output.accept(Moditems.BYWATER_BUCKET.get());
                        output.accept(Moditems.BYWATER_BOTTLE.get());
                        output.accept(ItemRegistry.LONGSHI_MINERAL_WATER.get());
                        output.accept(ItemRegistry.LONGSHI_MINERAL_WATER_EMPTY.get());
                    }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public CRPTapWater(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPayloads);

        // Register Fluids
        FluidRegistry.FLUID_TYPES.register(modEventBus);
        FluidRegistry.FLUIDS.register(modEventBus);

        // Register Blocks
        BlockRegistry.BLOCKS.register(modEventBus);

        // Register Items
        ItemRegistry.ITEMS.register(modEventBus);

        // 注册水槽放置器（使用 ServerTickEvent 版本）
        NeoForge.EVENT_BUS.register(new VillageWaterTroughPlacer());

        // Register Block Entities
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);

        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (CRPTapWater) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                WaterCompanyAPI.setCorpseWaterHandler(new CustomCorpseWaterHandler());
                LOGGER.info("尸兄：饮水公司 - CustomCorpseWaterHandler 已成功替换默认实现！");
            } catch (Exception e) {
                LOGGER.error("尸兄：饮水公司 - 替换WaterCompanyAPI失败，请确保corpseorigin模组已加载: {}", e.getMessage());
            }
        });

        LOGGER.info("==================== 尸兄：饮水公司 模组已加载 ====================");
        LOGGER.info("  管道系统: 已就绪");
        LOGGER.info("  村庄水槽: 已就绪");
        LOGGER.info("  村民口渴AI: 已就绪");
        LOGGER.info("  矿泉水(龙氏): 已就绪");
        LOGGER.info("  尸兄对话系统: 已就绪（右键有智商的尸兄开始对话）");
        LOGGER.info("================================================================");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0");

        registrar.playToClient(
                SSyncWaterTroughAnimPacket.TYPE,
                SSyncWaterTroughAnimPacket.STREAM_CODEC,
                SSyncWaterTroughAnimPacket::handle
        );

        registrar.playToClient(
                SOpenCorpseDialogPacket.TYPE,
                SOpenCorpseDialogPacket.STREAM_CODEC,
                SOpenCorpseDialogPacket::handle
        );

        registrar.playToClient(
                SCorpseDialogueQuestionPacket.TYPE,
                SCorpseDialogueQuestionPacket.STREAM_CODEC,
                SCorpseDialogueQuestionPacket::handle
        );

        registrar.playToClient(
                SCorpseDialogueResponsePacket.TYPE,
                SCorpseDialogueResponsePacket.STREAM_CODEC,
                SCorpseDialogueResponsePacket::handle
        );

        registrar.playToServer(
                CCorpseDialogueInitPacket.TYPE,
                CCorpseDialogueInitPacket.STREAM_CODEC,
                CCorpseDialogueInitPacket::handle
        );

        registrar.playToServer(
                CCorpseDialogueAnswerPacket.TYPE,
                CCorpseDialogueAnswerPacket.STREAM_CODEC,
                CCorpseDialogueAnswerPacket::handle
        );

        registrar.playToServer(
                CCorpseDialogueClosePacket.TYPE,
                CCorpseDialogueClosePacket.STREAM_CODEC,
                CCorpseDialogueClosePacket::handle
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.WATER_TROUGH_BLOCK_ENTITY.get(),
                (VillageWaterTroughBlockEntity trough, Direction context) -> trough.getFluidStorage()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.PIPE_BLOCK_ENTITY.get(),
                (PipeBlockEntity pipe, Direction context) -> pipe.getFluidStorage()
        );
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ItemRegistry.PIPE_BLOCK_ITEM.get());
            event.accept(ItemRegistry.WATER_TROUGH_BLOCK_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ItemRegistry.LONGSHI_MINERAL_WATER.get());
            event.accept(ItemRegistry.LONGSHI_MINERAL_WATER_EMPTY.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(Moditems.BYWATER_BUCKET.get());
            event.accept(Moditems.BYWATER_BOTTLE.get());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting - 尸兄：饮水公司 准备开始营业！");
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide() && entity instanceof Villager villager) {
            try {
                villager.goalSelector.addGoal(3, new VillagerThirstGoal(villager));
            } catch (Exception e) {
                LOGGER.warn("注入村民口渴AI失败: {}", e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public void onCorpseBrotherInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Entity target = event.getTarget();
        Player player = event.getEntity();
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        if (target instanceof Mob mob && CorpseBrotherHelper.isCorpseBrother(mob)) {
            if (player.isShiftKeyDown()) return;

            if (CorpseBrotherHelper.canDialogue(mob)) {
                CorpseNetwork.sendToPlayer(
                        new SOpenCorpseDialogPacket(mob.getId(), mob.getUUID()),
                        serverPlayer
                );
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
            }
        }
    }
}
