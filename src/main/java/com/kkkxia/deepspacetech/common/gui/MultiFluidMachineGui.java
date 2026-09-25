package com.kkkxia.deepspacetech.common.gui;

import java.util.Iterator;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.kkkxia.deepspacetech.common.machines.MTETieredMultiFluidMachine;

import gregtech.api.recipe.BasicUIProperties;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;

/**
 * 多流体单方块机器的 GUI：复用 GT 的 {@code MTEBasicMachineBaseGui}，只补上它做不到的部分——
 * GT 基类的流体槽方法各只生成 <b>1</b> 个槽（GT 单方块机器只有单罐），而本机器有 6 进 6 出。
 */
public class MultiFluidMachineGui<T extends MTETieredMultiFluidMachine> extends MTEBasicMachineBaseGui<T> {

    /** 流体槽列数，与 LargeNEIFrontend 的 xDirMaxCount 一致。 */
    private static final int FLUID_COLUMNS = 3;
    private static final int ROW_PADDING = 4;

    public MultiFluidMachineGui(T machine, BasicUIProperties properties) {
        super(machine, properties);
    }

    /**
     * 内容区：左"物品槽+流体槽"、进度条、右"物品槽+流体槽"。流体网格与上方物品槽处于同一个
     * {@code Flow.column()}，对齐是结构性的；列宽/间距沿用 GT 物品行的常量，物品槽仍停在标准位置。
     */
    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return getEmptyContent().child(
            Flow.row()
                .coverChildren()
                .horizontalCenter()
                // 三列顶部对齐：进度条才能与"物品槽"对齐，而不是与"物品+流体"整块对齐
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .childPadding(inOutColumnPadding())
                .child(itemOverFluid(createItemInputSlots(panel, syncManager), createFluidGrid(true)))
                .child(
                    new ParentWidget<>().size(properties.progressBarWidthMUI2, 3 * SLOT_SIZE)
                        .child(createProgressBar(panel, syncManager).verticalCenter()))
                .child(itemOverFluid(createItemOutputSlots(panel, syncManager), createFluidGrid(false))));
    }

    private Flow itemOverFluid(IWidget itemSlots, IWidget fluidSlots) {
        return Flow.column()
            .coverChildren()
            .childPadding(ROW_PADDING)
            .child(itemSlots)
            .child(fluidSlots);
    }

    /** 左右两块之间的间距，取值与 GT 物品行里"物品槽 → 进度条"的间距相同。 */
    private int inOutColumnPadding() {
        return (2 * SLOT_SIZE - properties.progressBarWidthMUI2) / 2;
    }

    /**
     * 3 列 × N 行的流体网格，容纳全部罐位。
     * {@code gridOfWidthHeight} 要求每个单元都返回组件，所以超出罐数的单元给等尺寸空占位而非 null。
     */
    private Grid createFluidGrid(boolean input) {
        final int tankCount = input ? machine.fluidInputTankCount() : machine.fluidOutputTankCount();
        final int rows = Math.max(1, (tankCount + FLUID_COLUMNS - 1) / FLUID_COLUMNS);

        return new Grid().coverChildren()
            .gridOfWidthHeight(FLUID_COLUMNS, rows, ($x, $y, index) -> {
                if (index >= tankCount) {
                    return new ParentWidget<>().size(SLOT_SIZE);
                }
                return new FluidSlot().backgroundOverlay(overlayFor(input, index))
                    .syncHandler(syncHandlerFor(input, index));
            });
    }

    /**
     * 摘掉父类塞在左下/右下角落流里的 0 号罐流体槽，否则会与上面的网格形成两个视图。
     * 只能"先 super 再摘"：父类构造角落流时同时塞了自动输出按钮（private）与特殊槽，无法局部重写。
     */
    private static Flow withoutFluidSlot(Flow cornerFlow) {
        final Iterator<IWidget> children = cornerFlow.getChildren()
            .iterator();
        while (children.hasNext()) {
            final IWidget child = children.next();
            if (child instanceof FluidSlot) {
                children.remove();
                child.dispose();
            }
        }
        return cornerFlow;
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return withoutFluidSlot(super.createBottomLeftCornerFlow(panel, syncManager));
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return withoutFluidSlot(super.createBottomRightCornerFlow(panel, syncManager));
    }

    /** 输出罐的槽不允许手动灌入，只能被机器产出填充。 */
    private FluidSlotSyncHandler syncHandlerFor(boolean input, int tank) {
        // 与 GT 自带流体槽一致：玩家改罐内容后立刻通知机器重查配方，否则要等到 aTick % 600 才重查
        final FluidSlotSyncHandler handler = new FluidSlotSyncHandler(
            input ? machine.inputTankView(tank) : machine.outputTankView(tank)) {

            @Override
            protected void onValueChanged() {
                super.onValueChanged();
                if (getSyncManager().isClient()) {
                    return;
                }
                baseMetaTileEntity.markInventoryBeenModified();
            }
        };
        if (!input) {
            handler.canFillSlot(false);
        }
        return handler;
    }

    private IDrawable overlayFor(boolean input, int tank) {
        return slotOverlayFunction.apply(tank, true, !input, false);
    }

    /**
     * 面板高度：默认的 166 只够"物品区 + 单流体槽"，这里要多塞一行 3 列 2 行的流体网格，故取 210。
     */
    @Override
    protected int getBasePanelHeight() {
        return 210;
    }
}

