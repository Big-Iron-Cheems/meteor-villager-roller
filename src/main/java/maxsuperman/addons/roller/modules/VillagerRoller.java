package maxsuperman.addons.roller.modules;

import maxsuperman.addons.roller.gui.widgets.EnchantmentListWidget;
import maxsuperman.addons.roller.model.RollingEnchantment;
import maxsuperman.addons.roller.util.EnchantmentUtils;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class VillagerRoller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("Sound");
    private final SettingGroup sgChatFeedback = settings.createGroup("Chat feedback", false);

    private final Setting<Boolean> disableIfFound = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-found")
        .description("Disable enchantment from list if found")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disconnectIfFound = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect-when-found")
        .description("Disconnect from server when enchantment from list if found")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> saveListToConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("save-list-to-config")
        .description("Toggles saving and loading of rolling list to config and copypaste buffer")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enablePlaySound = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-sound")
        .description("Plays sound when it finds desired trade")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<SoundEvent>> sound = sgSound.add(new SoundEventListSetting.Builder()
        .name("sound-to-play")
        .description("Sound that will be played when desired trade is found if enabled")
        .defaultValue(List.of(SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK))
        .build()
    );

    private final Setting<Double> soundPitch = sgSound.add(new DoubleSetting.Builder()
        .name("sound-pitch")
        .description("Playing sound pitch")
        .defaultValue(1.0)
        .min(0)
        .sliderRange(0, 8)
        .build()
    );

    private final Setting<Double> soundVolume = sgSound.add(new DoubleSetting.Builder()
        .name("sound-volume")
        .description("Playing sound volume")
        .defaultValue(1.0)
        .min(0)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Boolean> pauseOnScreen = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-screens")
        .description("Pauses rolling if any screen is open")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> headRotateOnPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate-place")
        .description("Look to the block while placing it?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> failedToPlaceDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-fail-delay")
        .description("Delay after failed block place (milliseconds)")
        .defaultValue(1500)
        .min(0)
        .sliderRange(0, 10000)
        .build()
    );

    private final Setting<Boolean> failedToPlaceDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("place-fail-disable")
        .description("Disables roller if block placement fails")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxProfessionWaitTime = sgGeneral.add(new IntSetting.Builder()
        .name("max-profession-wait-time")
        .description("Time to wait if villager does not take profession (milliseconds). Zero = unlimited.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 10000)
        .build()
    );

    private final Setting<Boolean> onlyTradeable = sgGeneral.add(new BoolSetting.Builder()
        .name("only-tradeable")
        .description("Hide enchantments that are not marked as tradeable")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sortEnchantments = sgGeneral.add(new BoolSetting.Builder()
        .name("sort-enchantments")
        .description("Show enchantments sorted by their name")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instantRebreak = sgGeneral.add(new BoolSetting.Builder()
        .name("CivBreak")
        .description("Uses CivBreak to mine the lectern instantly. Best to just stay over the lectern slot.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> interactRetry = sgGeneral.add(new IntSetting.Builder()
        .name("interact-retry")
        .description("If server did not acknowledge villager interact packet, send another one after this many ticks. Zero = no retries.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 200)
        .build()
    );

    private final Setting<Boolean> cfSetup = sgChatFeedback.add(new BoolSetting.Builder()
        .name("setup")
        .description("Hints on what to do in the beginning (otherwise denoted in modules list state)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfPausedOnScreen = sgChatFeedback.add(new BoolSetting.Builder()
        .name("paused-on-screen")
        .description("Rolling paused, interact with villager to continue")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfLowerLevel = sgChatFeedback.add(new BoolSetting.Builder()
        .name("found-lower-level")
        .description("Found enchant %s but it is not max level: %d (max) > %d (found)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfTooExpensive = sgChatFeedback.add(new BoolSetting.Builder()
        .name("found-too-expensive")
        .description("Found enchant %s but it costs too much: %s (max price) < %d (cost)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfIgnored = sgChatFeedback.add(new BoolSetting.Builder()
        .name("found-not-on-the-list")
        .description("Found enchant %s but it is not in the list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfProfessionTimeout = sgChatFeedback.add(new BoolSetting.Builder()
        .name("profession-timeout")
        .description("Villager did not take profession within the specified time")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfPlaceFailed = sgChatFeedback.add(new BoolSetting.Builder()
        .name("place-failed")
        .description("Failed placing, can't place or can't get lectern to hotbar (they still trigger place-failed settings)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfDiscrepancy = sgChatFeedback.add(new BoolSetting.Builder()
        .name("discrepancy")
        .description("Somehow roller got into state it was not expecting (likely AC mess)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfSentRetryInteract = sgChatFeedback.add(new BoolSetting.Builder()
        .name("sent-retry-interact")
        .description("Lets you know server dropping initial interact packets and additional was sent.")
        .defaultValue(true)
        .build()
    );

    private enum State {
        DISABLED,
        WAITING_FOR_TARGET_BLOCK,
        WAITING_FOR_TARGET_VILLAGER,
        ROLLING_BREAKING_BLOCK,
        ROLLING_WAITING_FOR_VILLAGER_PROFESSION_CLEAR,
        ROLLING_PLACING_BLOCK,
        ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW,
        ROLLING_WAITING_FOR_VILLAGER_TRADES
    }

    private State currentState = State.DISABLED;
    private VillagerEntity rollingVillager;
    private BlockPos rollingBlockPos;
    private Block rollingBlock;
    private final List<RollingEnchantment> searchingEnchants = new ArrayList<>();
    private long failedToPlacePrevMsg = System.currentTimeMillis();
    private long currentProfessionWaitTime;
    private long waitingForTradesTicks = 0;

    public VillagerRoller() {
        super(Categories.Misc, "villager-roller", "Rolls trades.");
    }

    @Override
    public void onActivate() {
        if (toggleOnBindRelease) {
            toggleOnBindRelease = false;
            if (cfSetup.get()) {
                warning("You had 'Toggle on bind release' set to true, I just saved you some troubleshooting by turning it off");
            }
        }
        currentState = State.WAITING_FOR_TARGET_BLOCK;
        if (cfSetup.get()) {
            info("Attack block you want to roll");
        }
    }

    @Override
    public void onDeactivate() {
        currentState = State.DISABLED;
    }

    @Override
    public String getInfoString() {
        return currentState.toString();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        if (saveListToConfig.get()) {
            NbtList l = new NbtList();
            for (RollingEnchantment e : searchingEnchants) {
                l.add(e.toTag());
            }
            tag.put("rolling", l);
        }
        return tag;
    }

    @Override
    public Module fromTag(@NonNull NbtCompound tag) {
        super.fromTag(tag);
        if (saveListToConfig.get()) {
            NbtList l = tag.getListOrEmpty("rolling");
            searchingEnchants.clear();
            for (NbtElement e : l) {
                if (e.getType() != NbtElement.COMPOUND_TYPE) {
                    info("Invalid list element");
                    continue;
                }
                searchingEnchants.add(new RollingEnchantment().fromTag((NbtCompound) e));
            }
        }
        return this;
    }

    @Override
    public WWidget getWidget(@NonNull GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        EnchantmentListWidget.fillWidget(theme, list, searchingEnchants, sortEnchantments.get(), onlyTradeable.get(), this::info, this::error);
        return list;
    }


    public void triggerInteract() {
        if (pauseOnScreen.get() && mc.currentScreen != null) {
            if (cfPausedOnScreen.get()) {
                info("Rolling paused, interact with villager to continue");
            }
        } else {
            Vec3d playerPos = mc.player.getEyePos();
            Vec3d villagerPos = rollingVillager.getEyePos();
            EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, playerPos, villagerPos, rollingVillager.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(villagerPos));
            if (entityHitResult == null) {
                // Raycast didn't find villager entity?
                mc.interactionManager.interactEntity(mc.player, rollingVillager, Hand.MAIN_HAND);
                waitingForTradesTicks = 0;
            } else {
                ActionResult actionResult = mc.interactionManager.interactEntityAtLocation(mc.player, rollingVillager, entityHitResult, Hand.MAIN_HAND);
                if (!actionResult.isAccepted()) {
                    mc.interactionManager.interactEntity(mc.player, rollingVillager, Hand.MAIN_HAND);
                    waitingForTradesTicks = 0;
                }
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (currentState != State.ROLLING_WAITING_FOR_VILLAGER_TRADES) return;
        if (!(event.packet instanceof SetTradeOffersS2CPacket p)) return;
        mc.executeSync(() -> triggerTradeCheck(p.getOffers()));
    }

    private void triggerTradeCheck(TradeOfferList l) {
        for (TradeOffer offer : l) {
            ItemStack sellItem = offer.getSellItem();
            if (!sellItem.isOf(Items.ENCHANTED_BOOK) || sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS) == null)
                continue;

            for (var enchantEntry : EnchantmentUtils.getEnchantsFromStack(sellItem).object2IntEntrySet()) {
                RegistryEntry<Enchantment> enchantRegistry = enchantEntry.getKey();
                int enchantLevel = enchantEntry.getIntValue();
                var reg = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                String enchantIdString = reg.getId(enchantRegistry.value()).toString();
                String enchantName = Names.get(enchantRegistry);

                boolean found = false;
                for (RollingEnchantment e : searchingEnchants) {
                    if (!e.enabled || !e.enchantment.toString().equals(enchantIdString)) continue;
                    found = true;
                    if (e.minLevel <= 0) {
                        int ml = enchantRegistry.value().getMaxLevel();
                        if (enchantLevel < ml) {
                            if (cfLowerLevel.get()) {
                                info(String.format("Found enchant %s but it is not max level: %d (max) > %d (found)",
                                    enchantName, ml, enchantLevel));
                            }
                            continue;
                        }
                    } else if (e.minLevel > enchantLevel) {
                        if (cfLowerLevel.get()) {
                            info(String.format("Found enchant %s but it has too low level: %d (requested level) > %d (rolled level)",
                                enchantName, e.minLevel, enchantLevel));
                        }
                        continue;
                    }
                    if (e.maxCost > 0 && offer.getOriginalFirstBuyItem().getCount() > e.maxCost) {
                        if (cfTooExpensive.get()) {
                            info(String.format("Found enchant %s but it costs too much: %s (max price) < %d (cost)",
                                enchantName, e.maxCost, offer.getOriginalFirstBuyItem().getCount()));
                        }
                        continue;
                    }
                    if (disableIfFound.get()) e.enabled = false;
                    toggle();
                    if (enablePlaySound.get() && !sound.get().isEmpty()) {
                        mc.getSoundManager().play(PositionedSoundInstance.master(sound.get().getFirst(),
                            soundPitch.get().floatValue(), soundVolume.get().floatValue()));
                    }
                    if (disconnectIfFound.get()) {
                        String levelText = (enchantLevel > 1 || enchantRegistry.value().getMaxLevel() > 1) ? " " + enchantLevel : "";
                        String message = String.format(
                            "%s[%s%s%s] Found enchant %s%s%s%s for %s%d%s emeralds and automatically disconnected.",
                            Formatting.GRAY,
                            Formatting.GREEN,
                            title,
                            Formatting.GRAY,
                            Formatting.WHITE,
                            enchantName,
                            levelText,
                            Formatting.GRAY,
                            Formatting.WHITE,
                            offer.getOriginalFirstBuyItem().getCount(),
                            Formatting.GRAY
                        );
                        mc.getNetworkHandler().getConnection().disconnect(Text.of(message));
                    }
                    break;
                }
                if (!found && cfIgnored.get()) {
                    info(String.format("Found enchant %s but it is not in the list.", enchantName));
                }
            }
        }

        mc.player.closeHandledScreen();
        currentState = State.ROLLING_BREAKING_BLOCK;
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        if (currentState != State.WAITING_FOR_TARGET_VILLAGER) return;
        if (!(event.entity instanceof VillagerEntity villager)) return;

        rollingVillager = villager;
        currentState = State.ROLLING_BREAKING_BLOCK;
        if (cfSetup.get()) {
            info("We got your villager");
        }
        event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (currentState != State.WAITING_FOR_TARGET_BLOCK) return;

        rollingBlockPos = event.blockPos;
        rollingBlock = mc.world.getBlockState(rollingBlockPos).getBlock();
        currentState = State.WAITING_FOR_TARGET_VILLAGER;
        if (instantRebreak.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, rollingBlockPos, Direction.UP));
        }
        if (cfSetup.get()) {
            info("Rolling block selected, now interact with villager you want to roll");
        }
    }

    private void placeFailed(String msg) {
        if (failedToPlacePrevMsg + failedToPlaceDelay.get() <= System.currentTimeMillis()) {
            if (cfPlaceFailed.get()) {
                info(msg);
            }
            failedToPlacePrevMsg = System.currentTimeMillis();
        }
        if (failedToPlaceDisable.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        switch (currentState) {
            case ROLLING_BREAKING_BLOCK -> {
                if (instantRebreak.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, rollingBlockPos, Direction.DOWN));
                }
                if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                    currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_CLEAR;
                } else if (!instantRebreak.get() && !BlockUtils.breakBlock(rollingBlockPos, true)) {
                    error("Can not break specified block");
                    toggle();
                }
            }
            case ROLLING_WAITING_FOR_VILLAGER_PROFESSION_CLEAR -> {
                if (mc.world.getBlockState(rollingBlockPos).isOf(Blocks.LECTERN)) {
                    if (cfDiscrepancy.get()) {
                        info("Rolling block mining reverted?");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }
                rollingVillager.getVillagerData().profession().getKey().ifPresent(profession -> {
                    if (profession == VillagerProfession.NONE) {
                        currentState = State.ROLLING_PLACING_BLOCK;
                    }
                });
            }
            case ROLLING_PLACING_BLOCK -> {
                FindItemResult item = InvUtils.findInHotbar(rollingBlock.asItem());
                if (!item.found()) {
                    placeFailed("Lectern not found in hotbar");
                    return;
                }
                if (!BlockUtils.canPlace(rollingBlockPos, true)) {
                    placeFailed("Can't place lectern");
                    return;
                }
                if (!BlockUtils.place(rollingBlockPos, item, headRotateOnPlace.get(), 5)) {
                    placeFailed("Failed to place lectern");
                    return;
                }
                currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW;
                if (maxProfessionWaitTime.get() > 0) {
                    currentProfessionWaitTime = System.currentTimeMillis();
                }
            }
            case ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW -> {
                if (maxProfessionWaitTime.get() > 0 && (currentProfessionWaitTime + maxProfessionWaitTime.get() <= System.currentTimeMillis())) {
                    if (cfProfessionTimeout.get()) {
                        info("Villager did not take profession within the specified time");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }
                if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                    if (cfDiscrepancy.get()) {
                        info("Lectern placement reverted by server (AC?)");
                    }
                    currentState = State.ROLLING_PLACING_BLOCK;
                    return;
                }
                if (!mc.world.getBlockState(rollingBlockPos).isOf(Blocks.LECTERN)) {
                    if (cfDiscrepancy.get()) {
                        info("Placed wrong block?!");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }
                rollingVillager.getVillagerData().profession().getKey().ifPresent(profession -> {
                    if (profession != VillagerProfession.NONE) {
                        currentState = State.ROLLING_WAITING_FOR_VILLAGER_TRADES;
                        triggerInteract();
                    }
                });
            }
            case ROLLING_WAITING_FOR_VILLAGER_TRADES -> {
                var retryTicks = interactRetry.get();
                if (retryTicks > 0) {
                    if (waitingForTradesTicks >= retryTicks) {
                        if (cfSentRetryInteract.get()) {
                            info("Sending another interact packet");
                        }
                        triggerInteract();
                    } else {
                        waitingForTradesTicks++;
                    }
                }
            }
            default -> {
                // Wait for another state
            }
        }
    }
}
