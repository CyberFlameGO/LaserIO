package com.direwolf20.laserio.client.screens;

import com.direwolf20.laserio.client.renderer.LaserIOItemRenderer;
import com.direwolf20.laserio.client.screens.widgets.ChannelButton;
import com.direwolf20.laserio.client.screens.widgets.NumberButton;
import com.direwolf20.laserio.client.screens.widgets.ToggleButton;
import com.direwolf20.laserio.common.LaserIO;
import com.direwolf20.laserio.common.containers.CardItemContainer;
import com.direwolf20.laserio.common.containers.customslot.CardItemSlot;
import com.direwolf20.laserio.common.containers.customslot.FilterBasicSlot;
import com.direwolf20.laserio.common.items.cards.BaseCard;
import com.direwolf20.laserio.common.items.cards.CardItem;
import com.direwolf20.laserio.common.items.filters.BaseFilter;
import com.direwolf20.laserio.common.items.filters.FilterBasic;
import com.direwolf20.laserio.common.items.filters.FilterCount;
import com.direwolf20.laserio.common.items.filters.FilterTag;
import com.direwolf20.laserio.common.network.PacketHandler;
import com.direwolf20.laserio.common.network.packets.PacketGhostSlot;
import com.direwolf20.laserio.common.network.packets.PacketOpenFilter;
import com.direwolf20.laserio.common.network.packets.PacketUpdateCard;
import com.direwolf20.laserio.common.network.packets.PacketUpdateFilter;
import com.direwolf20.laserio.util.MiscTools;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.HashMap;
import java.util.Map;

public class CardItemScreen extends AbstractContainerScreen<CardItemContainer> {
    private final ResourceLocation GUI = new ResourceLocation(LaserIO.MODID, "textures/gui/itemcard.png");

    protected final CardItemContainer container;
    private byte currentMode;
    private byte currentChannel;
    private byte currentItemExtractAmt;
    private short currentPriority;
    private byte currentSneaky;
    private int currentTicks;
    private boolean currentExact;
    private int currentRoundRobin;
    private boolean currentRegulate;
    private int isAllowList = -1;
    private int isCompareNBT = -1;
    private boolean showFilter;
    private boolean showAllow;
    private boolean showNBT;
    private final ItemStack card;
    private ItemStack filter;
    private Map<String, Button> buttons = new HashMap<>();

    private final String[] sneakyNames = {
            "screen.laserio.default",
            "screen.laserio.down",
            "screen.laserio.up",
            "screen.laserio.north",
            "screen.laserio.south",
            "screen.laserio.west",
            "screen.laserio.east",
    };

    public CardItemScreen(CardItemContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.container = container;
        this.card = container.cardItem;
        filter = container.slots.get(0).getItem();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        toggleFilterSlots();
        if (showFilter)
            updateItemCounts();
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        Button modeButton = buttons.get("mode");
        if (MiscTools.inBounds(modeButton.x, modeButton.y, modeButton.getWidth(), modeButton.getHeight(), mouseX, mouseY)) {
            TranslatableComponent translatableComponents[] = new TranslatableComponent[3];
            translatableComponents[0] = new TranslatableComponent("screen.laserio.insert");
            translatableComponents[1] = new TranslatableComponent("screen.laserio.extract");
            translatableComponents[2] = new TranslatableComponent("screen.laserio.stock");
            this.renderTooltip(matrixStack, translatableComponents[currentMode], mouseX, mouseY);
        }
        Button channelButton = buttons.get("channel");
        if (MiscTools.inBounds(channelButton.x, channelButton.y, channelButton.getWidth(), channelButton.getHeight(), mouseX, mouseY)) {
            this.renderTooltip(matrixStack, new TextComponent(String.valueOf(currentChannel)), mouseX, mouseY);
        }
        Button sneakyButton = buttons.get("sneaky");
        if (MiscTools.inBounds(sneakyButton.x, sneakyButton.y, sneakyButton.getWidth(), sneakyButton.getHeight(), mouseX, mouseY)) {
            this.renderTooltip(matrixStack, new TranslatableComponent(String.valueOf(sneakyNames[currentSneaky + 1])), mouseX, mouseY);
        }
        Button amountButton = buttons.get("amount");
        if (MiscTools.inBounds(amountButton.x, amountButton.y, amountButton.getWidth(), amountButton.getHeight(), mouseX, mouseY)) {
            if (showExtractAmt()) {
                this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.extractamt"), mouseX, mouseY);
            }
            if (showPriority()) {
                this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.priority"), mouseX, mouseY);
            }
        }
        Button regulate = buttons.get("regulate");
        if (MiscTools.inBounds(regulate.x, regulate.y, regulate.getWidth(), regulate.getHeight(), mouseX, mouseY)) {
            if (showRegulate()) {
                this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.regulate"), mouseX, mouseY);
            }
        }
        Button roundrobin = buttons.get("roundrobin");
        if (MiscTools.inBounds(roundrobin.x, roundrobin.y, roundrobin.getWidth(), roundrobin.getHeight(), mouseX, mouseY)) {
            if (showRoundRobin()) {
                TranslatableComponent translatableComponents[] = new TranslatableComponent[3];
                translatableComponents[0] = new TranslatableComponent("screen.laserio.false");
                translatableComponents[1] = new TranslatableComponent("screen.laserio.true");
                translatableComponents[2] = new TranslatableComponent("screen.laserio.enforced");
                this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.roundrobin").append(translatableComponents[currentRoundRobin]), mouseX, mouseY);
            }
        }
        Button exact = buttons.get("exact");
        if (MiscTools.inBounds(exact.x, exact.y, exact.getWidth(), exact.getHeight(), mouseX, mouseY)) {
            if (showExtractAmt()) { //Exact is the same conditions as ExtractAmt
                this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.exact"), mouseX, mouseY);
            }
        }
        Button speedButton = buttons.get("speed");
        if (MiscTools.inBounds(speedButton.x, speedButton.y, speedButton.getWidth(), speedButton.getHeight(), mouseX, mouseY)) {
            if (showExtractAmt()) {
                this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.tickSpeed"), mouseX, mouseY);
            }
        }
        if (showAllow) {
            Button allowList = buttons.get("allowList");
            if (MiscTools.inBounds(allowList.x, allowList.y, allowList.getWidth(), allowList.getHeight(), mouseX, mouseY)) {
                if (isAllowList == 1)
                    this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.allowlist"), mouseX, mouseY);
                else
                    this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.denylist"), mouseX, mouseY);
            }
        }
        if (showNBT) {
            Button nbtButton = buttons.get("nbt");
            if (MiscTools.inBounds(nbtButton.x, nbtButton.y, nbtButton.getWidth(), nbtButton.getHeight(), mouseX, mouseY)) {
                if (isCompareNBT == 1)
                    this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.nbttrue"), mouseX, mouseY);
                else
                    this.renderTooltip(matrixStack, new TranslatableComponent("screen.laserio.nbtfalse"), mouseX, mouseY);
            }
        }
    }

    public void updateItemCounts() {
        IItemHandler handler = container.filterHandler;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            stack.setCount(container.getStackSize(i + container.SLOTS));
        }
    }

    @Override
    public void init() {
        super.init();
        Minecraft minecraft = Minecraft.getInstance();
        BlockEntityWithoutLevelRenderer blockentitywithoutlevelrenderer = new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        this.itemRenderer = new LaserIOItemRenderer(minecraft.getTextureManager(), minecraft.getModelManager(), minecraft.getItemColors(), blockentitywithoutlevelrenderer);
        currentMode = BaseCard.getTransferMode(card);
        currentChannel = BaseCard.getChannel(card);
        currentItemExtractAmt = BaseCard.getItemExtractAmt(card);
        currentPriority = BaseCard.getPriority(card);
        currentSneaky = BaseCard.getSneaky(card);
        currentTicks = BaseCard.getItemExtractSpeed(card);
        currentExact = BaseCard.getExact(card);
        currentRoundRobin = BaseCard.getRoundRobin(card);
        currentRegulate = BaseCard.getRegulate(card);

        showFilter = !(filter == null) && !filter.isEmpty() && !(filter.getItem() instanceof FilterTag);
        if (showFilter) {
            isAllowList = BaseFilter.getAllowList(filter) ? 1 : 0;
            isCompareNBT = BaseFilter.getCompareNBT(filter) ? 1 : 0;
            if (filter.getItem() instanceof FilterBasic) {
                showAllow = true;
                showNBT = true;
            } else if (filter.getItem() instanceof FilterCount) {
                showAllow = false;
                showNBT = true;
            }
        } else {
            isAllowList = -1;
            isCompareNBT = -1;
        }

        ResourceLocation[] allowListTextures = new ResourceLocation[2];
        allowListTextures[0] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/allowlistfalse.png");
        allowListTextures[1] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/allowlisttrue.png");

        buttons.put("allowList", new ToggleButton(getGuiLeft() + 135, getGuiTop() + 61, 16, 16, allowListTextures, isAllowList == 1 ? 1 : 0, (button) -> {
            isAllowList = isAllowList == 1 ? 0 : 1;
            ((ToggleButton) button).setTexturePosition(isAllowList == 1 ? 1 : 0);
        }));

        ResourceLocation[] nbtTextures = new ResourceLocation[2];
        nbtTextures[0] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/matchnbtfalse.png");
        nbtTextures[1] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/matchnbttrue.png");

        buttons.put("nbt", new ToggleButton(getGuiLeft() + 153, getGuiTop() + 61, 16, 16, nbtTextures, isCompareNBT == 1 ? 1 : 0, (button) -> {
            isCompareNBT = isCompareNBT == 1 ? 0 : 1;
            ((ToggleButton) button).setTexturePosition(isCompareNBT == 1 ? 1 : 0);
        }));

        buttons.put("amount", new NumberButton(getGuiLeft() + 147, getGuiTop() + 25, 24, 12, currentMode == 0 ? currentPriority : currentItemExtractAmt, (button) -> {
            changeAmount(-1);
        }));

        buttons.put("speed", new NumberButton(getGuiLeft() + 147, getGuiTop() + 39, 24, 12, currentTicks, (button) -> {
            changeTick(-1);
        }));

        ResourceLocation[] exactTextures = new ResourceLocation[2];
        exactTextures[0] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/exactfalse.png");
        exactTextures[1] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/exacttrue.png");
        buttons.put("exact", new ToggleButton(getGuiLeft() + 25, getGuiTop() + 25, 16, 16, exactTextures, currentExact ? 1 : 0, (button) -> {
            currentExact = !currentExact;
            ((ToggleButton) button).setTexturePosition(currentExact ? 1 : 0);
        }));

        ResourceLocation[] roundRobinTextures = new ResourceLocation[3];
        roundRobinTextures[0] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/roundrobinfalse.png");
        roundRobinTextures[1] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/roundrobintrue.png");
        roundRobinTextures[2] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/roundrobinenforced.png");
        buttons.put("roundrobin", new ToggleButton(getGuiLeft() + 5, getGuiTop() + 25, 16, 16, roundRobinTextures, currentRoundRobin, (button) -> {
            currentRoundRobin = currentRoundRobin == 2 ? 0 : currentRoundRobin + 1;
            ((ToggleButton) button).setTexturePosition(currentRoundRobin);
        }));

        ResourceLocation[] regulateTextures = new ResourceLocation[2];
        regulateTextures[0] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/regulatefalse.png");
        regulateTextures[1] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/regulatetrue.png");
        buttons.put("regulate", new ToggleButton(getGuiLeft() + 5, getGuiTop() + 25, 16, 16, regulateTextures, currentRegulate ? 1 : 0, (button) -> {
            currentRegulate = !currentRegulate;
            ((ToggleButton) button).setTexturePosition(currentRegulate ? 1 : 0);
        }));


        ResourceLocation[] modeTextures = new ResourceLocation[3];
        modeTextures[0] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/modeinserter.png");
        modeTextures[1] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/modeextractor.png");
        modeTextures[2] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/modestocker.png");

        buttons.put("mode", new ToggleButton(getGuiLeft() + 5, getGuiTop() + 5, 16, 16, modeTextures, currentMode, (button) -> {
            currentMode = BaseCard.nextTransferMode(card);
            ((ToggleButton) button).setTexturePosition(currentMode);
            ((NumberButton) buttons.get("amount")).setValue(currentMode == 0 ? currentPriority : currentItemExtractAmt);
            modeChange();
        }));

        buttons.put("channel", new ChannelButton(getGuiLeft() + 5, getGuiTop() + 65, 16, 16, currentChannel, (button) -> {
            currentChannel = BaseCard.nextChannel(card);
            ((ChannelButton) button).setChannel(currentChannel);
        }));

        ResourceLocation[] sneakyTextures = new ResourceLocation[7];
        sneakyTextures[0] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/sneaky.png");
        sneakyTextures[1] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/sneaky-down.png");
        sneakyTextures[2] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/sneaky-up.png");
        sneakyTextures[3] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/sneaky-north.png");
        sneakyTextures[4] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/sneaky-south.png");
        sneakyTextures[5] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/sneaky-west.png");
        sneakyTextures[6] = new ResourceLocation(LaserIO.MODID, "textures/gui/buttons/sneaky-east.png");
        buttons.put("sneaky", new ToggleButton(getGuiLeft() + 25, getGuiTop() + 5, 16, 16, sneakyTextures, currentSneaky + 1, (button) -> {
            currentSneaky = BaseCard.nextSneaky(card);
            ((ToggleButton) button).setTexturePosition(currentSneaky + 1);
        }));

        for (Map.Entry<String, Button> button : buttons.entrySet()) {
            addRenderableWidget(button.getValue());
        }
        if (!showNBT) removeWidget(buttons.get("nbt"));
        if (!showAllow) removeWidget(buttons.get("allowList"));
        modeChange();
        /*if (currentMode == 0) removeWidget(buttons.get("speed"));
        if (currentMode == 0) removeWidget();
        if (currentMode == 0 || currentMode == 1) removeWidget(buttons.get("roundrobin"));
        if (currentMode == 0 || currentMode == 2) removeWidget(buttons.get("regulate"));*/
    }

    public void modeChange() {
        Button speedButton = buttons.get("speed");
        Button exactButton = buttons.get("exact");
        Button rrButton = buttons.get("roundrobin");
        Button regulateButton = buttons.get("regulate");
        if (currentMode == 0) { //insert
            removeWidget(speedButton);
            removeWidget(exactButton);
            removeWidget(rrButton);
            removeWidget(regulateButton);
        } else if (currentMode == 1) { //extract
            if (!renderables.contains(speedButton))
                addRenderableWidget(speedButton);
            if (!renderables.contains(exactButton))
                addRenderableWidget(exactButton);
            if (!renderables.contains(rrButton))
                addRenderableWidget(rrButton);
            removeWidget(regulateButton);
        } else { //stock
            if (!renderables.contains(speedButton))
                addRenderableWidget(speedButton);
            if (!renderables.contains(exactButton))
                addRenderableWidget(exactButton);
            if (!renderables.contains(regulateButton))
                addRenderableWidget(regulateButton);
            removeWidget(rrButton);
        }
    }

    public void changeAmount(int change) {
        if (Screen.hasShiftDown()) change *= 10;
        if (Screen.hasControlDown()) change *= 64;
        if (change < 0) {
            if (currentMode == 0) {
                currentPriority = (short) (Math.max(currentPriority + change, -4096));
            } else {
                currentItemExtractAmt = (byte) (Math.max(currentItemExtractAmt + change, 1));
            }
        } else {
            if (currentMode == 0) {
                currentPriority = (short) (Math.min(currentPriority + change, 4096));
            } else {
                currentItemExtractAmt = (byte) (Math.min(currentItemExtractAmt + change, Math.max(container.getSlot(1).getItem().getCount() * 16, 8)));
            }
        }
    }

    public void changeTick(int change) {
        if (Screen.hasShiftDown()) change *= 10;
        if (Screen.hasControlDown()) change *= 64;
        if (change < 0) {
            currentTicks = (Math.max(currentTicks + change, Math.max(20 - container.getSlot(1).getItem().getCount() * 5, 1)));
        } else {
            currentTicks = (Math.min(currentTicks + change, 1200));//Math.max(container.getSlot(1).getItem().getCount() * 16, 1)));
        }
    }

    public void toggleFilterSlots() {
        filter = container.slots.get(0).getItem();
        showFilter = !filter.isEmpty() && !(filter.getItem() instanceof FilterTag);
        if (showFilter) { //If the filter isn't empty, and the allowList is set to -1, it means we don't have a real value for allow list yet so get it
            if (filter.getItem() instanceof FilterBasic) {
                showNBT = true;
                if (currentMode == 2) {
                    showAllow = true;
                    //removeWidget(buttons.get("allowList"));
                } else {
                    showAllow = true;
                    if (!renderables.contains(buttons.get("allowList"))) addRenderableWidget(buttons.get("allowList"));
                }
            } else if (filter.getItem() instanceof FilterCount) {
                showAllow = false;
                showNBT = true;
                removeWidget(buttons.get("allowList"));
            }
            if (isAllowList == -1) {
                isAllowList = BaseFilter.getAllowList(filter) ? 1 : 0;
                ((ToggleButton) buttons.get("allowList")).setTexturePosition(isAllowList == 1 ? 1 : 0);
                isCompareNBT = BaseFilter.getCompareNBT(filter) ? 1 : 0;
                ((ToggleButton) buttons.get("nbt")).setTexturePosition(isCompareNBT == 1 ? 1 : 0);
                if (showAllow) addRenderableWidget(buttons.get("allowList"));
                if (showNBT) addRenderableWidget(buttons.get("nbt"));
            }
        } else {
            isAllowList = -1;
            isCompareNBT = -1;
            removeWidget(buttons.get("allowList"));
            removeWidget(buttons.get("nbt"));
            showAllow = false;
            showNBT = false;
        }
        for (int i = container.SLOTS; i < container.SLOTS + container.FILTERSLOTS; i++) {
            if (i >= container.slots.size()) continue;
            Slot slot = container.getSlot(i);
            if (!(slot instanceof FilterBasicSlot)) continue;
            ((FilterBasicSlot) slot).setEnabled(showFilter);
        }
    }

    private boolean showExtractAmt() {
        return card.getItem() instanceof CardItem && BaseCard.getNamedTransferMode(card) != BaseCard.TransferMode.INSERT;
    }

    private boolean showPriority() {
        return card.getItem() instanceof CardItem && BaseCard.getNamedTransferMode(card) == BaseCard.TransferMode.INSERT;
    }

    private boolean showRegulate() {
        return card.getItem() instanceof CardItem && BaseCard.getNamedTransferMode(card) == BaseCard.TransferMode.STOCK;
    }

    private boolean showRoundRobin() {
        return card.getItem() instanceof CardItem && BaseCard.getNamedTransferMode(card) == BaseCard.TransferMode.EXTRACT;
    }

    @Override
    protected void renderLabels(PoseStack stack, int mouseX, int mouseY) {
        /*stack.pushPose();
        stack.scale(0.5f, 0.5f, 0.5f);
        if (showExtractAmt()) {
            font.draw(stack, new TranslatableComponent("screen.laserio.extractamt").getString() + ":", 5*2, 45*2, Color.DARK_GRAY.getRGB());
        }
        if (showPriority()) {
            font.draw(stack, new TranslatableComponent("screen.laserio.priority").getString() + ":", 5*2, 50*2, Color.DARK_GRAY.getRGB());
        }
        stack.popPose();*/
        //super.renderLabels(matrixStack, x, y);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
        filter = container.slots.get(0).getItem();
        if (showFilter) {
            int slotsWidth = 90;
            int slotsHeight = 54;
            relX = relX + 43;
            relY = relY + 24;
            blit(matrixStack, relX, relY, 0, 167, slotsWidth, slotsHeight);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (showFilter)
            PacketHandler.sendToServer(new PacketUpdateFilter(isAllowList == 1, isCompareNBT == 1));
        PacketHandler.sendToServer(new PacketUpdateCard(currentMode, currentChannel, currentItemExtractAmt, currentPriority, currentSneaky, (short) currentTicks, currentExact, currentRegulate, (byte) currentRoundRobin));
        super.onClose();
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        InputConstants.Key mouseKey = InputConstants.getKey(p_keyPressed_1_, p_keyPressed_2_);
        if (p_keyPressed_1_ == 256 || minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
            onClose();

            return true;
        }

        return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }


    public boolean mouseReleased(double p_mouseReleased_1_, double p_mouseReleased_3_, int p_mouseReleased_5_) {
        return super.mouseReleased(p_mouseReleased_1_, p_mouseReleased_3_, p_mouseReleased_5_);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static TranslatableComponent getTrans(String key, Object... args) {
        return new TranslatableComponent(LaserIO.MODID + "." + key, args);
    }

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        ChannelButton channelButton = ((ChannelButton) buttons.get("channel"));
        if (MiscTools.inBounds(channelButton.x, channelButton.y, channelButton.getWidth(), channelButton.getHeight(), x, y)) {
            if (btn == 0)
                currentChannel = BaseCard.nextChannel(card);
            else if (btn == 1)
                currentChannel = BaseCard.previousChannel(card);
            channelButton.setChannel(currentChannel);
            channelButton.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        NumberButton amountButton = ((NumberButton) buttons.get("amount"));
        if (MiscTools.inBounds(amountButton.x, amountButton.y, amountButton.getWidth(), amountButton.getHeight(), x, y)) {
            if (btn == 0)
                changeAmount(1);
            else if (btn == 1)
                changeAmount(-1);
            amountButton.setValue(currentMode == 0 ? currentPriority : currentItemExtractAmt);
            amountButton.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        NumberButton speedButton = ((NumberButton) buttons.get("speed"));
        if (MiscTools.inBounds(speedButton.x, speedButton.y, speedButton.getWidth(), speedButton.getHeight(), x, y)) {
            if (btn == 0)
                changeTick(1);
            else if (btn == 1)
                changeTick(-1);
            speedButton.setValue(currentTicks);
            speedButton.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if (hoveredSlot == null)
            return super.mouseClicked(x, y, btn);

        if (hoveredSlot instanceof FilterBasicSlot) {
            if (filter.getItem() instanceof FilterBasic) {
                // By splitting the stack we can get air easily :) perfect removal basically
                ItemStack stack = this.menu.getCarried();// getMinecraft().player.inventoryMenu.getCarried();
                stack = stack.copy().split(hoveredSlot.getMaxStackSize()); // Limit to slot limit
                if (ItemHandlerHelper.canItemStacksStack(stack, container.cardItem)) return true;
                hoveredSlot.set(stack); // Temporarily update the client for continuity purposes
                PacketHandler.sendToServer(new PacketGhostSlot(hoveredSlot.index, stack, stack.getCount()));
            } else if (filter.getItem() instanceof FilterCount) {
                ItemStack stack = this.menu.getCarried();// getMinecraft().player.inventoryMenu.getCarried();
                if (!stack.isEmpty()) {
                    stack = stack.copy();
                    if (ItemHandlerHelper.canItemStacksStack(stack, container.cardItem)) return true;
                    hoveredSlot.set(stack); // Temporarily update the client for continuity purposes
                    PacketHandler.sendToServer(new PacketGhostSlot(hoveredSlot.index, stack, stack.getCount()));
                } else {
                    ItemStack slotStack = hoveredSlot.getItem();
                    if (slotStack.isEmpty()) return true;
                    if (btn == 2) { //Todo IMC Inventory Sorter so this works
                        slotStack.setCount(0);
                        PacketHandler.sendToServer(new PacketGhostSlot(hoveredSlot.index, slotStack, slotStack.getCount()));
                        return true;
                    }
                    int amt = (btn == 0) ? 1 : -1;
                    if (Screen.hasShiftDown()) amt *= 10;
                    if (Screen.hasControlDown()) amt *= 64;
                    if (amt + slotStack.getCount() > 4096) amt = 4096 - slotStack.getCount();
                    //slotStack.grow(amt);

                    PacketHandler.sendToServer(new PacketGhostSlot(hoveredSlot.index, slotStack, slotStack.getCount() + amt));
                }
            }
            return true;
        }
        if (hoveredSlot instanceof CardItemSlot) { //Right click
            if (btn == 0) {
                if (filter.getItem() instanceof BaseFilter) //Save the filter before removing it from the slot
                    PacketHandler.sendToServer(new PacketUpdateFilter(isAllowList == 1, isCompareNBT == 1));
            } else if (btn == 1) {
                int slot = hoveredSlot.getSlotIndex();
                PacketHandler.sendToServer(new PacketUpdateCard(currentMode, currentChannel, currentItemExtractAmt, currentPriority, currentSneaky, (short) currentTicks, currentExact, currentRegulate, (byte) currentRoundRobin));
                PacketHandler.sendToServer(new PacketOpenFilter(slot));
                return true;
            }
        }
        return super.mouseClicked(x, y, btn);
    }
}
