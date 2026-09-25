package com.kkkxia.deepspacetech.common.machines;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.fluid.FluidStackTank;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.kkkxia.deepspacetech.DeepSpaceTech;
import com.kkkxia.deepspacetech.common.AddonIds;
import com.kkkxia.deepspacetech.common.gui.MultiFluidMachineGui;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/**
 * 多路物品 / 流体的单方块机器基类：沿用 GT 单方块机器的加工主循环，只把单罐换成 6+6 个
 * FluidStack 罐，并复用 GT 的多路流体配方池（maxIO 6/6/6/6）。
 */
public abstract class MTETieredMultiFluidMachine extends MTEBasicMachineWithRecipe {

    public static final int ITEM_INPUT_SLOTS = 6;
    public static final int ITEM_OUTPUT_SLOTS = 6;
    public static final int FLUID_INPUT_TANKS = 6;
    public static final int FLUID_OUTPUT_TANKS = 6;

    private final FluidStack[] mInputFluids = new FluidStack[FLUID_INPUT_TANKS];
    private final FluidStack[] mOutputFluids = new FluidStack[FLUID_OUTPUT_TANKS];
    private final FluidStackTank[] mInputTankViews = new FluidStackTank[FLUID_INPUT_TANKS];
    private final FluidStackTank[] mOutputTankViews = new FluidStackTank[FLUID_OUTPUT_TANKS];
    private FluidStack[] mPendingOutputFluids;

    /** aRecipe 传 null 以避免 GT 自动注册按等级生成的合成表，本 mod 的合成表自行注册。 */
    protected MTETieredMultiFluidMachine(int aID, String aName, String aNameRegional, int aTier, String[] aDescription,
        RecipeMap<?> aRecipes, SoundResource aSound, SpecialEffects aSpecialEffect, String aOverlays) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            aDescription,
            aRecipes,
            ITEM_INPUT_SLOTS,
            ITEM_OUTPUT_SLOTS,
            true,
            true,
            tankCapacityForTier(aTier),
            aSound,
            aSpecialEffect,
            aOverlays,
            null);
        initTankViews();
    }

    protected MTETieredMultiFluidMachine(String aName, int aTier, String[] aDescription, RecipeMap<?> aRecipes,
        int aAmperage, ITexture[][][] aTextures, SoundResource aSound, SpecialEffects aSpecialEffect) {
        super(
            aName,
            aTier,
            aDescription,
            aRecipes,
            ITEM_INPUT_SLOTS,
            ITEM_OUTPUT_SLOTS,
            true,
            true,
            tankCapacityForTier(aTier),
            aAmperage,
            aTextures,
            aSound,
            aSpecialEffect);
        initTankViews();
    }

    private void initTankViews() {
        for (int i = 0; i < FLUID_INPUT_TANKS; i++) {
            final int index = i;
            mInputTankViews[i] = new FluidStackTank(
                () -> mInputFluids[index],
                fluid -> mInputFluids[index] = fluid,
                this::getCapacity);
        }
        for (int i = 0; i < FLUID_OUTPUT_TANKS; i++) {
            final int index = i;
            mOutputTankViews[i] = new FluidStackTank(
                () -> mOutputFluids[index],
                fluid -> mOutputFluids[index] = fluid,
                this::getCapacity);
        }
    }

    /** 单罐容量：本机器有 6+6 个罐，LV..LuV 取 GT 同等级整机容量的一半，更高等级两者同为 256k 上限。 */
    protected static int tankCapacityForTier(int aTier) {
        return (int) Math.min(256_000L, 4_000L * (1L << Math.max(0, aTier)));
    }

    // ---- 流体层 ----

    public int fluidInputTankCount() {
        return FLUID_INPUT_TANKS;
    }

    public int fluidOutputTankCount() {
        return FLUID_OUTPUT_TANKS;
    }

    public FluidStackTank inputTankView(int index) {
        return mInputTankViews[index];
    }

    public FluidStackTank outputTankView(int index) {
        return mOutputTankViews[index];
    }

    protected FluidStack[] storedInputFluids() {
        final List<FluidStack> fluids = new ArrayList<>(FLUID_INPUT_TANKS);
        for (FluidStack fluid : mInputFluids) {
            if (fluid != null && fluid.amount > 0) {
                fluids.add(fluid);
            }
        }
        return fluids.toArray(new FluidStack[0]);
    }

    private static FluidStack firstNonEmpty(FluidStack[] tanks) {
        for (FluidStack fluid : tanks) {
            if (fluid != null && fluid.amount > 0) {
                return fluid;
            }
        }
        return null;
    }

    /** 让基类的"是否可自动输出流体"判断看到输出罐。 */
    @Override
    public FluidStack getDrainableStack() {
        return firstNonEmpty(mOutputFluids);
    }

    @Override
    public FluidStack getFillableStack() {
        return firstNonEmpty(mInputFluids);
    }

    @Override
    public int getCapacity() {
        return mTankCapacity;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return 0;
        }
        int remaining = resource.amount;

        for (int i = 0; i < FLUID_INPUT_TANKS && remaining > 0; i++) {
            final FluidStack tank = mInputFluids[i];
            if (tank != null && tank.isFluidEqual(resource)) {
                final int moved = Math.min(getCapacity() - tank.amount, remaining);
                if (moved > 0) {
                    if (doFill) {
                        tank.amount += moved;
                    }
                    remaining -= moved;
                }
            }
        }

        for (int i = 0; i < FLUID_INPUT_TANKS && remaining > 0; i++) {
            // amount <= 0 的残留也算空罐：GT 扣料只把 amount 减到 0，不置 null
            if (mInputFluids[i] == null || mInputFluids[i].amount <= 0) {
                final int moved = Math.min(getCapacity(), remaining);
                if (doFill) {
                    final FluidStack copy = resource.copy();
                    copy.amount = moved;
                    mInputFluids[i] = copy;
                }
                remaining -= moved;
            }
        }

        final int accepted = resource.amount - remaining;
        if (doFill && accepted > 0) {
            getBaseMetaTileEntity().markDirty();
        }
        return accepted;
    }

    /** 从输出罐抽取；输入罐不对外开放，避免管道把原料抽走。 */
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        for (int i = 0; i < FLUID_OUTPUT_TANKS; i++) {
            final FluidStack tank = mOutputFluids[i];
            if (tank == null || tank.amount <= 0) {
                continue;
            }
            final int used = Math.min(tank.amount, maxDrain);
            final FluidStack drained = tank.copy();
            drained.amount = used;
            if (doDrain) {
                tank.amount -= used;
                if (tank.amount <= 0) {
                    mOutputFluids[i] = null;
                }
                getBaseMetaTileEntity().markDirty();
            }
            return drained;
        }
        return null;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection side) {
        final FluidTankInfo[] info = new FluidTankInfo[FLUID_INPUT_TANKS + FLUID_OUTPUT_TANKS];
        for (int i = 0; i < FLUID_INPUT_TANKS; i++) {
            info[i] = new FluidTankInfo(mInputFluids[i], getCapacity());
        }
        for (int i = 0; i < FLUID_OUTPUT_TANKS; i++) {
            info[FLUID_INPUT_TANKS + i] = new FluidTankInfo(mOutputFluids[i], getCapacity());
        }
        return info;
    }

    // ---- GUI 属性 ----

    /**
     * 直接复用配方表 frontend 的 UI 属性：MTEBasicMachine 的默认实现会把流体槽数钳到 1，
     * 而这里需要与 NEI 配方页同源的 6/6/6/6 布局（同一份 frontend 数据）。
     */
    @Override
    protected BasicUIProperties getUIProperties() {
        return getRecipeMap().getFrontend()
            .getUIProperties()
            .toBuilder()
            .maxItemInputs(mInputSlotCount)
            .maxItemOutputs(mOutputItems.length)
            .progressBarTexture(progressBarTexture)
            .build();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new MultiFluidMachineGui<>(this, getUIProperties()).build(data, syncManager, uiSettings);
    }

    // ---- 配方判定 ----

    @Override
    public int checkRecipe(boolean skipOC) {
        final RecipeMap<?> map = getRecipeMap();
        if (map == null) {
            return DID_NOT_FIND_RECIPE;
        }
        // 上一轮残留的待输出流体若还没写出去，说明配方被中断了，直接丢弃
        mPendingOutputFluids = null;

        final GTRecipe recipe = map.findRecipeQuery()
            .items(getAllInputs())
            .fluids(storedInputFluids())
            .specialSlot(getSpecialSlot())
            .voltage(GTValues.V[mTier])
            .cachedRecipe(mLastRecipe)
            .find();
        if (recipe == null) {
            return DID_NOT_FIND_RECIPE;
        }
        if (recipe.mCanBeBuffered) {
            mLastRecipe = recipe;
        }

        // 输出放不下就等下一 tick——必须先判容量再扣输入，否则会丢料
        if (recipe.mNeedsEmptyOutput) {
            if (!isOutputEmpty() || firstNonEmpty(mOutputFluids) != null) {
                mOutputBlocked++;
                return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
            }
        } else if (!canOutput(recipe.mOutputs) || !hasRoomForFluidOutputs(recipe)) {
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }

        if (!recipe.isRecipeInputEqual(true, storedInputFluids(), getAllInputs())) {
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        dropExhaustedInputTanks();

        // mOutputItems 是 final 数组（长度在构造期由输出槽数决定），只能逐元素写入
        for (int i = 0; i < mOutputItems.length; i++) {
            mOutputItems[i] = recipe.rollOutput(getBaseMetaTileEntity(), i);
        }
        mPendingOutputFluids = rollFluidOutputs(recipe);
        if (!skipOC) {
            calculateCustomOverclock(recipe);
            // GT 用 MAX_VALUE - 1 表示"这张配方对本机器来说过于极端"
            if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1) {
                return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
            }
        }
        return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
    }

    private boolean hasRoomForFluidOutputs(GTRecipe recipe) {
        for (FluidStack output : recipe.mFluidOutputs) {
            if (output == null || output.amount <= 0) {
                continue;
            }
            if (findRoomForFluid(output) < output.amount) {
                return false;
            }
        }
        return true;
    }

    private int findRoomForFluid(FluidStack output) {
        int room = 0;
        for (int i = 0; i < FLUID_OUTPUT_TANKS; i++) {
            final FluidStack tank = mOutputFluids[i];
            if (tank == null) {
                room += getCapacity();
            } else if (tank.isFluidEqual(output)) {
                room += getCapacity() - tank.amount;
            }
        }
        return room;
    }

    /** 逐路掷流体产出概率（基类只掷 0 号一路）；一路都没中时返回 null。 */
    private FluidStack[] rollFluidOutputs(GTRecipe recipe) {
        final FluidStack[] outputs = new FluidStack[recipe.mFluidOutputs.length];
        boolean anyRolled = false;
        for (int i = 0; i < outputs.length; i++) {
            if (getBaseMetaTileEntity().getRandomNumber(10000) < recipe.getFluidOutputChance(i)) {
                outputs[i] = recipe.getFluidOutput(i);
                anyRolled |= outputs[i] != null;
            }
        }
        return anyRolled ? outputs : null;
    }

    /**
     * 基类只写单路 mOutputFluid；多路产出暂存到 mPendingOutputFluids，在这里才写进输出罐，
     * 以保持"完成才产出"的时序（否则加工途中拆机可提前拿到产物）。
     */
    @Override
    public void endProcess() {
        super.endProcess();
        flushPendingOutputFluids();
    }

    /** 把被扣光的输入罐置空：GT 的扣料只把 amount 减到 0，留着会让 0 量残留占满罐位。 */
    private void dropExhaustedInputTanks() {
        for (int i = 0; i < FLUID_INPUT_TANKS; i++) {
            if (mInputFluids[i] != null && mInputFluids[i].amount <= 0) {
                mInputFluids[i] = null;
            }
        }
    }

    private void flushPendingOutputFluids() {
        final FluidStack[] pending = mPendingOutputFluids;
        mPendingOutputFluids = null;
        if (pending == null) {
            return;
        }
        for (FluidStack output : pending) {
            if (output == null || output.amount <= 0) {
                continue;
            }
            int remaining = output.amount;
            for (int i = 0; i < FLUID_OUTPUT_TANKS && remaining > 0; i++) {
                final FluidStack tank = mOutputFluids[i];
                if (tank != null && tank.isFluidEqual(output)) {
                    final int moved = Math.min(getCapacity() - tank.amount, remaining);
                    tank.amount += moved;
                    remaining -= moved;
                }
            }
            for (int i = 0; i < FLUID_OUTPUT_TANKS && remaining > 0; i++) {
                if (mOutputFluids[i] == null) {
                    final int moved = Math.min(getCapacity(), remaining);
                    final FluidStack copy = output.copy();
                    copy.amount = moved;
                    mOutputFluids[i] = copy;
                    remaining -= moved;
                }
            }
        }
    }

    // ---- dev 自检 ----

    /**
     * 启动自检：用一条真实的多路流体配方跑完整配方判定，失败直接抛异常。
     * 必须在所有 mod 的 FMLLoadCompleteEvent 之后调用：electrolyzerNonCellRecipes
     * 的大部分配方由 GT++ 在其 load-complete 中批量生成，在那之前配方池几乎是空的。
     */
    public static void runRecipeSelfCheck() {
        checkMap("electrolyzerNonCellRecipes", RecipeMaps.electrolyzerNonCellRecipes, true);
        checkMap("multiblockChemicalReactorRecipes", RecipeMaps.multiblockChemicalReactorRecipes, false);
    }

    /**
     * newMetaEntity 出来的实例是干净的，但 GT 只在原型构造器里设置 base TE，
     * 实例的 base TE 要放置到世界才有，这里补一个空壳。
     */
    private static MTETieredMultiFluidMachine createProbe(IMetaTileEntity prototype) {
        final MTETieredMultiFluidMachine probe = (MTETieredMultiFluidMachine) prototype
            .newMetaEntity(prototype.getBaseMetaTileEntity());
        probe.setBaseMetaTileEntity(GregTechAPI.constructBaseMetaTileEntity());
        return probe;
    }

    private static void checkMap(String label, RecipeMap<?> map, boolean electrolyser) {
        final Collection<GTRecipe> recipes = map.getAllRecipes();
        int maxFluids = 0;
        int maxItems = 0;
        int multiFluidCount = 0;
        GTRecipe target = null;
        for (GTRecipe recipe : recipes) {
            maxFluids = Math.max(maxFluids, recipe.mFluidInputs.length);
            maxItems = Math.max(maxItems, recipe.mInputs.length);
            if (recipe.mFluidInputs.length >= 2) {
                multiFluidCount++;
            }
            // 只挑本 mod 真有对应等级、装得下且有输入可扣的配方：化学反应釜池里混着
            // EU/t 高到 2e9 的占位配方，按"输入最多"选会挑到本 mod 不存在的等级。
            if (recipe.mEUt <= 0 || tierForVoltage(recipe.mEUt) < 0
                || recipe.mInputs.length > ITEM_INPUT_SLOTS
                || recipe.mFluidInputs.length > FLUID_INPUT_TANKS
                || (recipe.mInputs.length < 1 && recipe.mFluidInputs.length < 1)) {
                continue;
            }
            if (target == null || isStrongerProbe(recipe, target)) {
                target = recipe;
            }
        }

        DeepSpaceTech.LOG.info(
            "{}: {} recipes, up to {} item inputs, up to {} fluid inputs, {} with 2+ fluids",
            label,
            recipes.size(),
            maxItems,
            maxFluids,
            multiFluidCount);

        if (target == null) {
            DeepSpaceTech.LOG.warn("{}: no usable recipe to self-check with", label);
            return;
        }

        final int tier = tierForVoltage(target.mEUt);
        final int id = electrolyser ? AddonIds.multiTankElectrolyserId(tier)
            : AddonIds.multiTankChemicalReactorId(tier);
        final IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        if (!(prototype instanceof MTETieredMultiFluidMachine)) {
            throw new IllegalStateException(
                "Self-check expected a multi-fluid machine at tier " + tier + " for " + label);
        }
        final MTETieredMultiFluidMachine probe = createProbe(prototype);
        checkUiProperties(label, probe);

        for (int i = 0; i < target.mInputs.length && i < probe.mInputSlotCount; i++) {
            probe.mInventory[probe.getInputSlot() + i] = GTUtility
                .copyAmount(target.mInputs[i].stackSize, target.mInputs[i]);
        }
        for (FluidStack fluid : target.mFluidInputs) {
            final int accepted = probe.fill(fluid.copy(), true);
            if (accepted < fluid.amount) {
                throw new IllegalStateException(
                    "Self-check " + label
                        + ": fill() accepted "
                        + accepted
                        + " of "
                        + fluid.amount
                        + " mB "
                        + fluid.getFluid()
                            .getName()
                        + " - the machine has "
                        + FLUID_INPUT_TANKS
                        + " input tanks, check fill()");
            }
        }

        final long itemsBefore = countItems(probe);
        final long fluidBefore = countFluids(probe);

        final int result = probe.checkRecipe(false);
        if (result != FOUND_AND_SUCCESSFULLY_USED_RECIPE) {
            throw new IllegalStateException(
                "Self-check " + label
                    + " FAILED: checkRecipe returned "
                    + result
                    + " for a recipe with "
                    + target.mInputs.length
                    + " item and "
                    + target.mFluidInputs.length
                    + " fluid inputs at tier "
                    + tier);
        }
        if (countItems(probe) >= itemsBefore && countFluids(probe) >= fluidBefore) {
            throw new IllegalStateException(
                "Self-check " + label
                    + " FAILED: recipe matched but no input was consumed"
                    + " (items "
                    + itemsBefore
                    + "->"
                    + countItems(probe)
                    + ", fluid "
                    + fluidBefore
                    + "->"
                    + countFluids(probe)
                    + ")");
        }

        // 走一遍完成钩子，确认暂存流体真的进了输出罐（这段是自写逻辑，必须实测）。
        final int stagedFluids = probe.mPendingOutputFluids == null ? 0 : probe.mPendingOutputFluids.length;
        probe.endProcess();
        final long fluidOutput = countOutputFluids(probe);
        if (stagedFluids > 0 && fluidOutput <= 0) {
            throw new IllegalStateException(
                "Self-check " + label
                    + " FAILED: endProcess() did not move staged fluid outputs into the output tanks");
        }
        if (probe.mPendingOutputFluids != null) {
            throw new IllegalStateException(
                "Self-check " + label + " FAILED: staged fluid outputs were not cleared by endProcess()");
        }

        DeepSpaceTech.LOG.info(
            "Self-check {} passed: matched a recipe with {} item + {} fluid inputs at tier {}"
                + " (items {}->{}, fluid {}->{} mB, staged {} fluid outputs -> {} mB in output tanks,"
                + " overclocked to {} EU/t over {} ticks)",
            label,
            target.mInputs.length,
            target.mFluidInputs.length,
            tier,
            itemsBefore,
            countItems(probe),
            fluidBefore,
            countFluids(probe),
            stagedFluids,
            fluidOutput,
            probe.mEUt,
            probe.mMaxProgresstime);
    }

    /** UI 槽位属性必须与 NEI 配方页同源且为多路；一旦被钳回 1，启动即报错。 */
    private static void checkUiProperties(String label, MTETieredMultiFluidMachine probe) {
        final BasicUIProperties ui = probe.getUIProperties();
        if (ui.maxItemInputs != ITEM_INPUT_SLOTS || ui.maxItemOutputs != ITEM_OUTPUT_SLOTS
            || ui.maxFluidInputs != FLUID_INPUT_TANKS
            || ui.maxFluidOutputs != FLUID_OUTPUT_TANKS) {
            throw new IllegalStateException(
                "Self-check " + label
                    + " FAILED: GUI slots are "
                    + ui.maxItemInputs
                    + "/"
                    + ui.maxItemOutputs
                    + " items and "
                    + ui.maxFluidInputs
                    + "/"
                    + ui.maxFluidOutputs
                    + " fluids, expected "
                    + ITEM_INPUT_SLOTS
                    + "/"
                    + ITEM_OUTPUT_SLOTS
                    + " and "
                    + FLUID_INPUT_TANKS
                    + "/"
                    + FLUID_OUTPUT_TANKS);
        }
    }

    private static boolean isStrongerProbe(GTRecipe candidate, GTRecipe current) {
        final int candidateChannels = candidate.mFluidInputs.length + candidate.mInputs.length;
        final int currentChannels = current.mFluidInputs.length + current.mInputs.length;
        if (candidateChannels != currentChannels) {
            return candidateChannels > currentChannels;
        }
        if (candidate.mFluidInputs.length != current.mFluidInputs.length) {
            return candidate.mFluidInputs.length > current.mFluidInputs.length;
        }
        return candidate.mEUt < current.mEUt;
    }

    /** 覆盖给定 EU/t 的最低等级（1 = LV）；超出 UMV 返回 -1。 */
    private static int tierForVoltage(long eut) {
        for (int tier = 1; tier <= AddonIds.TIER_COUNT; tier++) {
            if (GTValues.V[tier] >= eut) {
                return tier;
            }
        }
        return -1;
    }

    private static long countItems(MTETieredMultiFluidMachine machine) {
        long total = 0;
        for (int i = machine.getInputSlot(); i < machine.getInputSlot() + machine.mInputSlotCount; i++) {
            final ItemStack stack = machine.mInventory[i];
            if (stack != null) {
                total += stack.stackSize;
            }
        }
        return total;
    }

    private static long countFluids(MTETieredMultiFluidMachine machine) {
        long total = 0;
        for (FluidStack fluid : machine.mInputFluids) {
            if (fluid != null) {
                total += fluid.amount;
            }
        }
        return total;
    }

    private static long countOutputFluids(MTETieredMultiFluidMachine machine) {
        long total = 0;
        for (FluidStack fluid : machine.mOutputFluids) {
            if (fluid != null) {
                total += fluid.amount;
            }
        }
        return total;
    }

    // ---- 持久化 ----

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        for (int i = 0; i < FLUID_INPUT_TANKS; i++) {
            if (mInputFluids[i] != null) {
                aNBT.setTag("dsInputFluid" + i, mInputFluids[i].writeToNBT(new NBTTagCompound()));
            }
        }
        for (int i = 0; i < FLUID_OUTPUT_TANKS; i++) {
            if (mOutputFluids[i] != null) {
                aNBT.setTag("dsOutputFluid" + i, mOutputFluids[i].writeToNBT(new NBTTagCompound()));
            }
        }
        // 加工途中卸载区块：进度与物品产出由 GT 基类存，待写入的流体产出必须自己存，否则会丢
        if (mPendingOutputFluids != null) {
            aNBT.setInteger("dsPendingFluidCount", mPendingOutputFluids.length);
            for (int i = 0; i < mPendingOutputFluids.length; i++) {
                if (mPendingOutputFluids[i] != null) {
                    aNBT.setTag("dsPendingFluid" + i, mPendingOutputFluids[i].writeToNBT(new NBTTagCompound()));
                }
            }
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        for (int i = 0; i < FLUID_INPUT_TANKS; i++) {
            mInputFluids[i] = aNBT.hasKey("dsInputFluid" + i)
                ? FluidStack.loadFluidStackFromNBT(aNBT.getCompoundTag("dsInputFluid" + i))
                : null;
        }
        for (int i = 0; i < FLUID_OUTPUT_TANKS; i++) {
            mOutputFluids[i] = aNBT.hasKey("dsOutputFluid" + i)
                ? FluidStack.loadFluidStackFromNBT(aNBT.getCompoundTag("dsOutputFluid" + i))
                : null;
        }
        if (aNBT.hasKey("dsPendingFluidCount")) {
            mPendingOutputFluids = new FluidStack[aNBT.getInteger("dsPendingFluidCount")];
            for (int i = 0; i < mPendingOutputFluids.length; i++) {
                mPendingOutputFluids[i] = aNBT.hasKey("dsPendingFluid" + i)
                    ? FluidStack.loadFluidStackFromNBT(aNBT.getCompoundTag("dsPendingFluid" + i))
                    : null;
            }
        } else {
            mPendingOutputFluids = null;
        }
    }
}
