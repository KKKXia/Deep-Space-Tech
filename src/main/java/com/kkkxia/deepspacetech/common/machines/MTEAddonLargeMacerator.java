package com.kkkxia.deepspacetech.common.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.structure.IStructureInstance;
import gregtech.api.structure.IStructureProvider;
import gregtech.api.structure.StructureWrapper;
import gregtech.api.structure.StructureWrapperInstanceInfo;
import gregtech.api.structure.StructureWrapperTooltipBuilder;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * 示范 2：使用 GT 电力系统的多方块机器——结构走 GT 新版结构 API，耗电/超频/并行/GUI 都交给 GT 基类。
 */
public class MTEAddonLargeMacerator extends MTEExtendedPowerMultiBlockBase<MTEAddonLargeMacerator>
    implements ISurvivalConstructable, IStructureProvider<MTEAddonLargeMacerator> {

    private static final Casings CASING = Casings.SolidSteelMachineCasing;
    private static final int PARALLELS_PER_TIER = 4;

    /**
     * 结构实例：原型持有 wrapper，实例持有 wrapper + instance info；结构定义是静态数据，只在原型上编译一次。
     */
    protected final StructureWrapper<MTEAddonLargeMacerator> structure;
    protected final StructureWrapperInstanceInfo<MTEAddonLargeMacerator> structureInstanceInfo;

    public MTEAddonLargeMacerator(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);

        structure = new StructureWrapper<>(this);
        structureInstanceInfo = null;

        structure.loadStructure();
    }

    public MTEAddonLargeMacerator(MTEAddonLargeMacerator prototype) {
        super(prototype.mName);

        structure = prototype.structure;
        structureInstanceInfo = new StructureWrapperInstanceInfo<>(structure);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEAddonLargeMacerator(this);
    }

    /** 结构字符画：{@code ~} 是控制器自身位置，空格表示不校验该位置，其余字符在 {@link #compile} 里绑定。 */
    @Override
    public String[][] getDefinition() {
        // spotless:off
        return transpose(new String[][] {
            { "AAA", "AAA", "AAA" },
            { "A~A", "A A", "AAA" },
            { "AAA", "AAA", "AAA" }
        });
        // spotless:on
    }

    @Override
    public IStructureDefinition<MTEAddonLargeMacerator> compile(String[][] definition) {
        structure.addCasing('A', CASING)
            .withUnlimitedHatches(
                1,
                Arrays.asList(
                    HatchElement.Energy,
                    HatchElement.Maintenance,
                    HatchElement.InputBus,
                    HatchElement.OutputBus,
                    HatchElement.Muffler));

        return structure.buildStructure(definition);
    }

    @Override
    public IStructureInstance<MTEAddonLargeMacerator> getStructureInstance() {
        return structureInstanceInfo;
    }

    @Override
    public IStructureDefinition<MTEAddonLargeMacerator> getStructureDefinition() {
        return structure.getStructureDefinition();
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        structure.checkStructure(this, errors);

        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasMufflerHatch(errors);
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

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.maceratorRecipes;
    }

    /**
     * 并行数 = 每级并行 × 输入电压等级；{@code getMaxInputVoltage()} 由 GT 基类从能源仓汇总得到。
     */
    @Override
    public int getMaxParallelRecipes() {
        final int tier = Math.max(1, GTUtility.getTier(getMaxInputVoltage()));
        return PARALLELS_PER_TIER * tier;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        return new ITexture[] { CASING.getCasingTexture() };
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_MACERATOR;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final StructureWrapperTooltipBuilder<MTEAddonLargeMacerator> tt = new StructureWrapperTooltipBuilder<>(
            structure);
        // 不能把 beginStructureBlock()/addAllCasingInfo() 接在 addInfo(...) 之后：
        // MultiblockTooltipBuilder 的方法返回父类型，链式调用会丢掉子类重载。
        tt.addMachineType(StatCollector.translateToLocal("deepspacetech.machine.large_macerator.type"))
            .addInfo(StatCollector.translateToLocal("deepspacetech.machine.large_macerator.info"))
            .addInfo(
                StatCollector
                    .translateToLocalFormatted("deepspacetech.machine.large_macerator.parallel", PARALLELS_PER_TIER));
        tt.beginStructureBlock();
        tt.addController(StatCollector.translateToLocal("deepspacetech.machine.large_macerator.controller"));
        tt.addAllCasingInfo();
        tt.toolTipFinisher();
        return tt;
    }
}
