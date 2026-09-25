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
 * 示范 3：接入 GTNH 算力（Computation）系统的多方块机器——继承 tectech 的 {@link TTMultiblockBase}，
 * 不自建任何算力存储或网络；电力仍由 GT 基类处理。
 */
public class MTEAddonComputationLab extends TTMultiblockBase
    implements ISurvivalConstructable, IStructureProvider<MTEAddonComputationLab> {

    private static final Casings CASING = Casings.ComputerCasing;
    private static final long COMPUTATION_PER_TICK = 1024L;
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
                    Energy.or(EnergyMulti),
                    Maintenance,
                    InputBus,
                    OutputBus,
                    // InputData 的机器类列表是 MTEHatchDataInput，仓室匹配用 isInstance，
                    // 所以其子类 MTEHatchWirelessComputationInput（无线算力仓）也能放进来。
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

    /**
     * 本类与算力系统的唯一接触点：声明本 tick 需要的算力，不足则拒绝开工；
     * 取算力、断算力停机与 NBT 持久化都由基类完成。
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

    /** 并行数由可用算力决定，让算力成为这台机器真正的产能上限。 */
    @Override
    public int getMaxParallelRecipes() {
        final long usable = Math.max(0L, eAvailableData);
        final int fromComputation = (int) (1 + usable / COMPUTATION_PER_TICK);
        return Math.max(1, Math.min(MAX_PARALLELS, fromComputation));
    }

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
