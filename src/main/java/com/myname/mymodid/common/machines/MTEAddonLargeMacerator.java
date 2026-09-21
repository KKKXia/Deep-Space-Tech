package com.myname.mymodid.common.machines;

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
 * 示范 2：使用 GT 电力系统的多方块机器。
 *
 * <p>
 * 这个类演示的是"addon 做多方块时应当依赖什么、回避什么"。它<b>不</b>自己实现：
 * <ul>
 * <li>结构校验算法 —— 交给 {@link StructureWrapper}（GT 新版结构 API）；</li>
 * <li>仓室识别与收集 —— 交给 GT 的 {@link HatchElement} 与 {@code MTEMultiBlockBase.addXToMachineList}；</li>
 * <li>耗电与停机 —— 基类 {@link MTEExtendedPowerMultiBlockBase} 每 tick 自动
 * {@code drainEnergyInput(getActualEnergyUsage())}，电量不足自动 {@code stopMachine(POWER_LOSS)}；</li>
 * <li>超频与并行 —— 交给 GT 的 {@code ProcessingLogic} / {@code OverclockCalculator}；</li>
 * <li>GUI / NEI 配方页 —— 由 GT 的 ModularUI 与 NEI 集成自动提供。</li>
 * </ul>
 *
 * <p>
 * <b>为什么用 StructureWrapper 而不是旧的 {@code StructureDefinition.builder()}</b>：
 * 旧写法（{@code checkMachine(TE, ItemStack)}、{@code checkPiece(...)} 无 errors 参数、
 * {@code survivalBuildPiece} 旧签名）在 GT 5.09.54.166 里已全部标注
 * {@code @Deprecated + @ApiStatus.ScheduledForRemoval}，正处于结构重写周期中。
 * 新版把结构声明拆成"数据（{@link #getDefinition()}）+ 编译（{@link #compile(String[][])}）"，
 * 由 {@link IStructureProvider} 这组接口承载——接口是稳定的，实现可以换。
 */
public class MTEAddonLargeMacerator extends MTEExtendedPowerMultiBlockBase<MTEAddonLargeMacerator>
    implements ISurvivalConstructable, IStructureProvider<MTEAddonLargeMacerator> {

    /** 结构机壳。用 GT 现成的机壳，addon 不需要自带贴图。 */
    private static final Casings CASING = Casings.SolidSteelMachineCasing;
    /** 每个电压等级的并行数。 */
    private static final int PARALLELS_PER_TIER = 4;

    /**
     * 结构实例：原型持有 wrapper，实例持有 wrapper + instance info。
     *
     * <p>
     * 这是 GT 新版结构 API 的固定分工：结构定义是"静态数据"，只在原型上编译一次；
     * 实例侧只保存运行期状态（例如各级机壳实际数量）。
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

    // ---- 结构声明：只有"数据"和"编译"两件事 ----

    /**
     * 结构字符画。
     *
     * <p>
     * {@code ~} 是控制器自身位置（会被自动识别为偏移，不需要手写 offX/offY/offZ），
     * 空格是"不校验该位置"，其余字符在 {@link #compile} 里绑定。
     */
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

    /** 把字符绑定到机壳与允许的仓室。 */
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

    // ---- 结构校验：只做"委托 + 补充必要的仓室检查" ----

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

    // ---- 配方与并行：配方表复用 GT 的，并行按 GT 的电压等级算 ----

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.maceratorRecipes;
    }

    /**
     * 并行数 = 每级并行 × 输入电压等级。
     *
     * <p>
     * {@code getMaxInputVoltage()} 由 GT 基类从结构里的能源仓汇总得到——
     * addon 不需要自己遍历仓室、也不需要自己判断电压。
     */
    @Override
    public int getMaxParallelRecipes() {
        final int tier = Math.max(1, GTUtility.getTier(getMaxInputVoltage()));
        return PARALLELS_PER_TIER * tier;
    }

    // ---- 表现层：贴图与音效都复用 GT 资源 ----

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        // 用 GT 机壳贴图作为控制器外观。需要动态 overlay 时可在本 mod 的 assets 下加贴图，
        // 或用 GT 的 Textures.BlockIcons.createTextureWithCasing(...) 叠加。
        return new ITexture[] { CASING.getCasingTexture() };
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_MACERATOR;
    }

    // ---- tooltip：机壳与仓室信息由 StructureWrapperTooltipBuilder 自动生成 ----

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final StructureWrapperTooltipBuilder<MTEAddonLargeMacerator> tt = new StructureWrapperTooltipBuilder<>(
            structure);
        // 注意：不能把 beginStructureBlock()/addAllCasingInfo() 接在 addInfo(...) 之后——
        // MultiblockTooltipBuilder 的方法返回的是父类型，链式调用会丢掉子类重载。
        tt.addMachineType(StatCollector.translateToLocal("mymodid.machine.large_macerator.type"))
            .addInfo(StatCollector.translateToLocal("mymodid.machine.large_macerator.info"))
            .addInfo(
                StatCollector
                    .translateToLocalFormatted("mymodid.machine.large_macerator.parallel", PARALLELS_PER_TIER));
        // 结构尺寸自动取自 StructureWrapper（不需要手写 3,3,3）
        tt.beginStructureBlock();
        tt.addController(StatCollector.translateToLocal("mymodid.machine.large_macerator.controller"));
        // 机壳数量与仓室列表自动生成
        tt.addAllCasingInfo();
        tt.toolTipFinisher();
        return tt;
    }
}
