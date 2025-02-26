package com.direwolf20.laserio.common.items.cards;

import com.direwolf20.laserio.client.blockentityrenders.LaserNodeBERender;
import com.direwolf20.laserio.common.containers.CardItemContainer;
import com.direwolf20.laserio.common.containers.customhandler.CardItemHandler;
import com.direwolf20.laserio.setup.ModSetup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

import static com.direwolf20.laserio.util.MiscTools.tooltipMaker;

public class BaseCard extends Item {
    protected BaseCard.CardType CARDTYPE;

    public enum CardType {
        ITEM,
        FLUID,
        ENERGY
    }

    public enum TransferMode {
        INSERT,
        EXTRACT,
        STOCK
    }

    public BaseCard() {
        super(new Item.Properties().tab(ModSetup.ITEM_GROUP)
                .stacksTo(1));

    }

    public CardType getCardType() {
        return CARDTYPE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);

        Minecraft mc = Minecraft.getInstance();

        if (world == null || mc.player == null) {
            return;
        }

        boolean sneakPressed = Screen.hasShiftDown();

        if (!sneakPressed) {
            tooltip.add(new TranslatableComponent("laserio.tooltip.item.show_settings")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            String currentMode = getNamedTransferMode(stack).toString();
            MutableComponent toWrite = tooltipMaker("laserio.tooltip.item.card.mode", ChatFormatting.GRAY.getColor());
            int modeColor = ChatFormatting.GRAY.getColor();
            if (currentMode.equals("EXTRACT"))
                modeColor = ChatFormatting.RED.getColor();
            else if (currentMode.equals("INSERT"))
                modeColor = ChatFormatting.GREEN.getColor();
            else if (currentMode.equals("STOCK"))
                modeColor = ChatFormatting.BLUE.getColor();
            toWrite.append(tooltipMaker("laserio.tooltip.item.card.mode." + currentMode, modeColor));
            tooltip.add(toWrite);

            toWrite = tooltipMaker("laserio.tooltip.item.card.channel", ChatFormatting.GRAY.getColor());
            int channel = getChannel(stack);
            toWrite.append(tooltipMaker(String.valueOf(channel), LaserNodeBERender.colors[channel].getRGB()));
            tooltip.add(toWrite);

            int sneakyMode = getSneaky(stack);
            if (sneakyMode != -1) {
                toWrite = tooltipMaker("laserio.tooltip.item.card.sneaky", ChatFormatting.GRAY.getColor());
                toWrite.append(tooltipMaker("laserio.tooltip.item.card.sneaky." + Direction.values()[sneakyMode].toString().toUpperCase(Locale.ROOT), ChatFormatting.DARK_GREEN.getColor()));
                tooltip.add(toWrite);
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (level.isClientSide()) return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);

        NetworkHooks.openGui((ServerPlayer) player, new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new CardItemContainer(windowId, playerInventory, player, itemstack), new TranslatableComponent("")), (buf -> {
            buf.writeItem(itemstack);
        }));

        //System.out.println(itemstack.getItem().getRegistryName()+""+itemstack.getTag());
        return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
    }

    /**
     * ItemStack sensitive version of getContainerItem. Returns a full ItemStack
     * instance of the result.
     * Custom Implementation by Dire: get multiples
     *
     * @param itemStack The current ItemStack
     * @return The resulting ItemStack
     */
    public NonNullList<ItemStack> getContainerItems(ItemStack itemStack) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(2, ItemStack.EMPTY);
        nonnulllist.set(0, getInventory(itemStack).getStackInSlot(0));
        nonnulllist.set(1, getInventory(itemStack).getStackInSlot(1));

        return nonnulllist;
    }

    /**
     * ItemStack sensitive version of hasContainerItem
     *
     * @param stack The current item stack
     * @return True if this item has a 'container'
     */
    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return !(getInventory(stack).getStackInSlot(0).equals(ItemStack.EMPTY) && getInventory(stack).getStackInSlot(1).equals(ItemStack.EMPTY));
    }

    public static CardItemHandler getInventory(ItemStack stack) {
        CompoundTag compound = stack.getOrCreateTag();
        CardItemHandler handler = new CardItemHandler(CardItemContainer.SLOTS, stack);
        handler.deserializeNBT(compound.getCompound("inv"));
        if (handler.getSlots() < CardItemContainer.SLOTS)
            handler.reSize(CardItemContainer.SLOTS);
        return !compound.contains("inv") ? setInventory(stack, new CardItemHandler(CardItemContainer.SLOTS, stack)) : handler;
    }

    public static CardItemHandler setInventory(ItemStack stack, CardItemHandler handler) {
        stack.getOrCreateTag().put("inv", handler.serializeNBT());
        return handler;
    }

    public static byte setTransferMode(ItemStack card, byte mode) {
        card.getOrCreateTag().putByte("mode", mode);
        return mode;
    }

    public static byte getTransferMode(ItemStack card) {
        CompoundTag compound = card.getOrCreateTag();
        return !compound.contains("mode") ? setTransferMode(card, (byte) 0) : compound.getByte("mode");
    }

    public static byte nextTransferMode(ItemStack card) {
        byte mode = getTransferMode(card);
        return setTransferMode(card, (byte) (mode == 2 ? 0 : mode + 1));
    }

    public static TransferMode getNamedTransferMode(ItemStack card) {
        return TransferMode.values()[getTransferMode(card)];
    }

    public static byte setChannel(ItemStack card, byte channel) {
        card.getOrCreateTag().putByte("channel", channel);
        return channel;
    }

    public static byte getChannel(ItemStack card) {
        CompoundTag compound = card.getOrCreateTag();
        return !compound.contains("channel") ? setChannel(card, (byte) 0) : compound.getByte("channel");
    }

    public static byte nextChannel(ItemStack card) {
        byte k = getChannel(card);
        return setChannel(card, (byte) (k == 15 ? 0 : k + 1));
    }

    public static byte previousChannel(ItemStack card) {
        byte k = getChannel(card);
        return setChannel(card, (byte) (k == 0 ? 15 : k - 1));
    }

    public static byte setItemExtractAmt(ItemStack card, byte itemextractamt) {
        card.getOrCreateTag().putByte("itemextractamt", itemextractamt);
        return itemextractamt;
    }

    public static byte getItemExtractAmt(ItemStack card) {
        CompoundTag compound = card.getOrCreateTag();
        return !compound.contains("itemextractamt") ? setItemExtractAmt(card, (byte) 1) : compound.getByte("itemextractamt");
    }

    public static int setItemExtractSpeed(ItemStack card, int itemextractspeed) {
        card.getOrCreateTag().putInt("itemextractspeed", itemextractspeed);
        return itemextractspeed;
    }

    public static int getItemExtractSpeed(ItemStack card) {
        CompoundTag compound = card.getOrCreateTag();
        return !compound.contains("itemextractspeed") ? setItemExtractSpeed(card, 20) : compound.getInt("itemextractspeed");
    }

    public static short setPriority(ItemStack card, short priority) {
        card.getOrCreateTag().putShort("priority", priority);
        return priority;
    }

    public static short getPriority(ItemStack card) {
        CompoundTag compound = card.getOrCreateTag();
        return !compound.contains("priority") ? setPriority(card, (short) 0) : compound.getShort("priority");
    }

    public static ItemStack getFilter(ItemStack card) {
        CardItemHandler cardItemHandler = getInventory(card);
        return cardItemHandler.getStackInSlot(0);
    }

    public static byte setSneaky(ItemStack card, byte sneaky) {
        card.getOrCreateTag().putByte("sneaky", sneaky);
        return sneaky;
    }

    public static byte getSneaky(ItemStack card) {
        CompoundTag compound = card.getOrCreateTag();
        return !compound.contains("sneaky") ? setSneaky(card, (byte) -1) : compound.getByte("sneaky");
    }

    public static byte nextSneaky(ItemStack card) {
        byte k = getSneaky(card);
        return setSneaky(card, (byte) (k == 5 ? -1 : k + 1));
    }

    public static byte previousSneaky(ItemStack card) {
        byte k = getSneaky(card);
        return setSneaky(card, (byte) (k == -1 ? 5 : k - 1));
    }

    public static boolean getRegulate(ItemStack stack) {
        CompoundTag compound = stack.getOrCreateTag();
        return !compound.contains("regulate") ? setRegulate(stack, false) : compound.getBoolean("regulate");
    }

    public static boolean setRegulate(ItemStack stack, boolean regulate) {
        stack.getOrCreateTag().putBoolean("regulate", regulate);
        return regulate;
    }

    public static int getRoundRobin(ItemStack stack) {
        CompoundTag compound = stack.getOrCreateTag();
        return !compound.contains("roundRobin") ? setRoundRobin(stack, 0) : compound.getInt("roundRobin");
    }

    public static int setRoundRobin(ItemStack stack, int roundRobin) {
        stack.getOrCreateTag().putInt("roundRobin", roundRobin);
        return roundRobin;
    }

    /*public static int getRoundRobinPosition(ItemStack stack) {
        CompoundTag compound = stack.getOrCreateTag();
        return !compound.contains("roundRobinPosition") ? setRoundRobinPosition(stack, -1) : compound.getInt("roundRobinPosition");
    }

    public static int setRoundRobinPosition(ItemStack stack, int roundRobinPosition) {
        stack.getOrCreateTag().putInt("roundRobinPosition", roundRobinPosition);
        return roundRobinPosition;
    }*/

    public static boolean getExact(ItemStack stack) {
        CompoundTag compound = stack.getOrCreateTag();
        return !compound.contains("exact") ? setExact(stack, false) : compound.getBoolean("exact");
    }

    public static boolean setExact(ItemStack stack, boolean exact) {
        stack.getOrCreateTag().putBoolean("exact", exact);
        return exact;
    }
}
