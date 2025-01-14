package com.direwolf20.laserio.common.containers;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.containers.customhandler.LaserNodeItemHandler;
import com.direwolf20.laserio.common.containers.customslot.LaserNodeSlot;
import com.direwolf20.laserio.setup.Registration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

public class LaserNodeContainer extends AbstractContainerMenu {
    public static final int SLOTS = 10;
    public static final int CARDSLOTS = 9;
    private Player playerEntity;
    private IItemHandler playerInventory;
    ContainerLevelAccess containerLevelAccess;

    // Tile can be null and shouldn't be used for accessing any data that needs to be up to date on both sides
    public LaserNodeBE tile;
    public byte side;

    public LaserNodeContainer(int windowId, Inventory playerInventory, Player player, FriendlyByteBuf extraData) {
        this((LaserNodeBE) playerInventory.player.level.getBlockEntity(extraData.readBlockPos()), windowId, extraData.readByte(), playerInventory, player, new LaserNodeItemHandler(SLOTS), ContainerLevelAccess.NULL);
    }

    public LaserNodeContainer(@Nullable LaserNodeBE tile, int windowId, byte side, Inventory playerInventory, Player player, LaserNodeItemHandler handler, ContainerLevelAccess containerLevelAccess) {
        super(Registration.LaserNode_Container.get(), windowId);
        this.playerEntity = player;
        this.tile = tile;
        this.side = side;
        this.playerInventory = new InvWrapper(playerInventory);
        this.containerLevelAccess = containerLevelAccess;
        if (handler != null) {
            addSlotBox(handler, 0, 62, 32, 3, 18, 3, 18);
            addSlotRange(handler, 9, 152, 78, 1, 18);
        }

        layoutPlayerInventorySlots(8, 99);
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(containerLevelAccess, playerEntity, Registration.LaserNode.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            //If its one of the 9 slots at the top try to move it into your inventory
            if (index < SLOTS) {
                if (!this.moveItemStackTo(stack, SLOTS, 36 + SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, itemstack);
            } else {
                if (!this.moveItemStackTo(stack, 0, SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
                if (!playerIn.level.isClientSide() && !(tile == null)) {
                    tile.updateThisNode();
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stack);
        }

        return itemstack;
    }


    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            if (handler instanceof LaserNodeItemHandler && index < 9)
                addSlot(new LaserNodeSlot(handler, index, x, y));
            else
                addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0; j < verAmount; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        // Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }
}
