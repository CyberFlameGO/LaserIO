package com.direwolf20.laserio.common.network.packets;

import com.direwolf20.laserio.common.blockentities.LaserNodeBE;
import com.direwolf20.laserio.common.blockentities.basebe.BaseLaserBE;
import com.direwolf20.laserio.common.containers.LaserNodeContainer;
import com.direwolf20.laserio.common.containers.customhandler.LaserNodeItemHandler;
import com.direwolf20.laserio.common.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

import static com.direwolf20.laserio.common.blocks.LaserNode.SCREEN_LASERNODE;


public class PacketOpenNode {
    private BlockPos sourcePos;
    private byte side;

    public PacketOpenNode(BlockPos pos, byte side) {
        this.sourcePos = pos;
        this.side = side;
    }

    public static void encode(PacketOpenNode msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.sourcePos);
        buffer.writeByte(msg.side);
    }

    public static PacketOpenNode decode(FriendlyByteBuf buffer) {
        return new PacketOpenNode(buffer.readBlockPos(), buffer.readByte());

    }

    public static class Handler {
        public static void handle(PacketOpenNode msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null)
                    return;

                AbstractContainerMenu container = sender.containerMenu;
                if (container == null)
                    return;

                BlockEntity be = sender.level.getBlockEntity(msg.sourcePos);
                if (be == null || !(be instanceof BaseLaserBE))
                    return;

                ItemStack heldStack = sender.containerMenu.getCarried();
                if (!heldStack.isEmpty()) {
                    // set it to empty, so it's doesn't get dropped
                    sender.containerMenu.setCarried(ItemStack.EMPTY);
                }
                be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.values()[msg.side]).ifPresent(h -> {
                    MenuProvider containerProvider = new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return new TranslatableComponent(SCREEN_LASERNODE);
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
                            return new LaserNodeContainer((LaserNodeBE) be, windowId, msg.side, playerInventory, playerEntity, (LaserNodeItemHandler) h, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
                        }
                    };
                    NetworkHooks.openGui(sender, containerProvider, (buf -> {
                        buf.writeBlockPos(msg.sourcePos);
                        buf.writeByte(msg.side);
                    }));
                    if (!heldStack.isEmpty()) {
                        sender.containerMenu.setCarried(heldStack);
                        PacketHandler.sendVanillaPacket(sender, new ClientboundContainerSetSlotPacket(-1, -1, -1, heldStack));
                    }
                });


            });


            ctx.get().setPacketHandled(true);
        }
    }
}
