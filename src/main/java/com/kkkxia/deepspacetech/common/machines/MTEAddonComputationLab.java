package com.kkkxia.deepspacetech.common.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static tectech.thing.metaTileEntity.multi.base.TTMultiblockBase.HatchElement.EnergyMulti;
import static tectech.thing.metaTileEntity.multi.base.TTMultiblockBase.HatchElement.InputData;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.IStructureInstance;
import gregtech.api.structure.IStructureProvider;
import gregtech.api.structure.StructureWrapper;
import gregtech.api.structure.StructureWrapperInstanceInfo;
import gregtech.api.structure.StructureWrapperTooltipBuilder;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import tectech.thing.metaTileEntity.multi.base.TTMultiblockBase;

/**
 * 示范 3：接入 GTNH「算力（Computation）」系统的多方块机器。
 *
 * <p>
 * 算力系统在 GT5U 里由 {@code tectech} 提供（与 GT5U 同一个 jar，但是独立 mod 容器），
 * 核心是一个按队伍聚合的全局无线算力池 {@code WirelessComputationPacket}。
 * 本类通过继承 {@link TTMultiblockBase} 直接接入，<b>不自己实现任何算力存储或网络</b>。
 *
 * <p>
 * 接入只需三件事：
 * <ol>
 * <li><b>声明需求</b>：在 {@link #checkProcessing_EM()} 里给 {@code eRequiredData} 赋值；</li>
 * <li><b>声明仓室</b>：结构里允许放 {@code InputData} 仓室——这样玩家可以放
 * {@code MTEHatchWirelessComputationInput}（从队伍算力池取算力）或普通数据仓（有线传输）；</li>
 * <li><b>用算力</b>：直接读 {@code eAvailableData}，用它算并行等。</li>
 * </ol>
 *
 * <p>
 * 基类替我们完成的部分：
 * <ul>
 * <li>每 tick 调 {@code hatchesStatusUpdate_EM()} 刷新 {@code eAvailableData}
 * （默认实现会累加所有数据输入仓里数据包的算力值）；</li>
 * <li>{@code onRunningTickCheck_EM()} 自动比对 {@code eRequiredData > eAvailableData}，
 * 算力中断超过 {@code MAX_COMPUTATION_TIMEOUT}（100 tick 宽限）后自动以
 * {@code computation_loss} 停机——不需要手写断算力逻辑；</li>
 * <li>{@code eRequiredData} / {@code eAvailableData} 的 NBT 持久化。</li>
 * </ul>
 *
 * <p>
 * 电力仍然走 GT 的体系：{@link TTMultiblockBase} 继承自 {@code MTEExtendedPowerMultiBlockBase}，
 * 因此能源仓（普通与多安培）耗电、停机都由 GT 基类处理。
 */
public class MTEAddonComputationLab extends TTMultiblockBase
    implements ISurvivalConstructable, IStructureProvider<MTEAddonComputationLab> {

    /** 结构机壳：用 GT 的计算机机壳，避免 addon 自带贴图。 */
    private static final Casings CASING = Casings.ComputerCasing;
    /** 每 tick 需要的算力。 */
    private static final long COMPUTATION_PER_TICK = 1024L;
    /** 并行上限，防止算力充裕时并行爆炸。 */
    private static final int MAX_PARALLELS = 16;

    protected final StructureWrapper<MTEAddonComputationLab> structure;
    protected final StructureWrapperInstanceInfo<MTEAddonComputationLab> structureInstanceInfo;

    public MTEAddonComputationLab(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);

        structure = new StructureWrapper<>(this);
        structureInstanceInfo = null;

        structure.loadStructure();
    }

    public MTEAddonComputationLab(MTEAddonComputationLab prototype) {
        super(prototype.mName);

        structure = prototype.structure;
        structureInstanceInfo = new StructureWrapperInstanceInfo<>(structure);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEAddonComputationLab(this);
    }

    // ---- 结构 ----

    @Override
    public String[][] getDefinition() {
        // spotless:off
        return transpose(new String[][] {
            { "CCC", "CCC", "CCC" },
            { "C~C", "C C", "CCC" },
            { "CCC", "CCC", "CCC" }
        });
        // spotless:on
    }

    @Override
    public IStructureDefinition<MTEAddonComputationLab> compile(String[][] definition) {
        structure.addCasing('C', CASING)
            .withUnlimitedHatches(
                1,
                Arrays.asList(
                    // GT 的能源仓与维护仓
                    Energy.or(EnergyMulti),
                    Maintenance,
                    // 物品进出：本机器用算力加速 GT 的组装机配方
                    InputBus,
                    OutputBus,
                    // tectech 的数据仓：算力入口。
                    // InputData 的机器类列表是 MTEHatchDataInput，仓室匹配用 isInstance，
                    // 因此它的子类 MTEHatchWirelessComputationInput（无线算力仓）也能放进来。
                    InputData));

        return structure.buildStructure(definition);
    }

    @Override
    public IStructureInstance<MTEAddonComputationLab> getStructureInstance() {
        return structureInstanceInfo;
    }

    @Override
    public IStructureDefinition<MTEAddonComputationLab> getStructure_EM() {
        return structure.getStructureDefinition();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        structure.checkStructure(this, errors);

        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        // 必须有算力入口，否则这台机器永远开不了工
        checkHasDataInput(errors);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        structure.construct(this, stackSize, hintsOnly);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) {
            return -1;
        }
        return structure.survivalConstruct(this, stackSize, elementBudget, env);
    }

    // ---- 算力接入 ----

    /**
     * 配方判定前先卡算力。
     *
     * <p>
     * 这是本类与 GTNH 算力系统的唯一接触点：声明本 tick 需要多少算力，
     * 以及算力不够时拒绝开工。剩余工作（取算力、断算力停机、持久化）全由基类完成。
     */
    @Override
    @NotNull
    protected CheckRecipeResult checkProcessing_EM() {
        eRequiredData = COMPUTATION_PER_TICK;

        if (eAvailableData < eRequiredData) {
            return SimpleCheckRecipeResult.ofFailure("no_computation");
        }

        return super.checkProcessing_EM();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.assemblerRecipes;
    }

    /**
     * 并行数由可用算力决定——算力越多，一次组装越多份。
     *
     * <p>
     * 这样算力就是这台机器真正的产能上限，而不是一个可有可无的门槛。
     */
    @Override
    public int getMaxParallelRecipes() {
        final long usable = Math.max(0L, eAvailableData);
        final int fromComputation = (int) (1 + usable / COMPUTATION_PER_TICK);
        return Math.max(1, Math.min(MAX_PARALLELS, fromComputation));
    }

    // ---- 表现层 ----

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        return new ITexture[] { CASING.getCasingTexture() };
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.TECTECH_MACHINES_FX_HIGH_FREQ;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final StructureWrapperTooltipBuilder<MTEAddonComputationLab> tt = new StructureWrapperTooltipBuilder<>(
            structure);
        // 注意：不能把 beginStructureBlock()/addAllCasingInfo() 接在 addInfo(...) 之后——
        // MultiblockTooltipBuilder 的方法返回的是父类型，链式调用会丢掉子类重载。
        tt.addMachineType(StatCollector.translateToLocal("deepspacetech.machine.computation_lab.type"))
            .addInfo(StatCollector.translateToLocal("deepspacetech.machine.computation_lab.info"))
            .addInfo(
                StatCollector.translateToLocalFormatted(
                    "deepspacetech.machine.computation_lab.requirement",
                    COMPUTATION_PER_TICK));
        tt.beginStructureBlock();
        tt.addController(StatCollector.translateToLocal("deepspacetech.machine.computation_lab.controller"));
        tt.addAllCasingInfo();
        tt.toolTipFinisher();
        return tt;
    }
}
