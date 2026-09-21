package com.kkkxia.deepspacetech.loaders;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.kkkxia.deepspacetech.DeepSpaceTech;
import com.kkkxia.deepspacetech.common.AddonItemList;
import com.kkkxia.deepspacetech.common.config.Config;
import com.kkkxia.deepspacetech.common.materials.AddonMaterials;

import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

/**
 * 配方注册（本 mod 的 postInit）。
 *
 * <p>
 * 配方的写法与 GT 自己的配方 loader 完全一致：{@code GTValues.RA.stdBuilder()}
 * 链式配置后 {@code addTo(RecipeMaps.xxx)}。<b>没有自建的配方系统</b>——
 * 因此本 mod 的配方自动获得 GT 的配方查找索引、NEI 显示、机器催化、超频计算与并行支持。
 *
 * <p>
 * 两点值得注意的约定：
 * <ul>
 * <li>输入输出优先用 {@link GTOreDictUnificator#get(OrePrefixes, Object, long)} 而不是裸 ItemStack，
 * 这样配方会随材料前缀自动适配（"钢粉"是哪种钢粉由矿辞统一决定）；</li>
 * <li>能耗用 {@link TierEU#RECIPE_LV} 这类"配方电压"常量，而不是 {@code TierEU.LV}。
 * GT 明确区分二者：{@code LV} 是 1 安培的精确电压，{@code RECIPE_LV} 预留了线损余量，
 * 配方应当使用后者。</li>
 * </ul>
 *
 * <p>
 * {@link #require} 的作用：{@code GTOreDictUnificator.get(...)} 在"材料没有该形态物品"时会返回 null，
 * 而 GT 的 {@code itemInputs(...)} 遇到 null 只会在日志里打一行 "null detected in itemInputs" 然后跳过，
 * 结果是配方静默少了一个输入。这里改为显式失败，让材料/前缀写错在启动时就暴露。
 */
public final class LoaderAddonRecipes {

    private static final int RECIPE_DURATION_SHORT = 5 * 20;
    private static final int RECIPE_DURATION_LONG = 20 * 20;

    private LoaderAddonRecipes() {}

    public static void run() {
        // 先确认 GT 为本 mod 的材料自动生成了流体，避免后面写配方时拿到 null
        AddonMaterials.verify();

        registerRecyclingRecipes();
        registerMachineRecipes();
        registerMaterialRecipes();

        if (Config.debugLogging) {
            DeepSpaceTech.LOG.info("Registered addon recipes");
        }
    }

    /** 回收机壳：给本 mod 的内容一个材料来源，同时示范读取 GT 机壳物品。 */
    private static void registerRecyclingRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(Casings.SolidSteelMachineCasing.toStack(1))
            .itemOutputs(require(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Steel, 4), "Steel dust"))
            .duration(RECIPE_DURATION_SHORT)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.maceratorRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(Casings.ComputerCasing.toStack(1))
            .itemOutputs(require(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Aluminium, 4), "Aluminium dust"))
            .duration(RECIPE_DURATION_SHORT)
            .eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.maceratorRecipes);
    }

    /** 本 mod 机器的合成：用 GT 组装机产出，输入全部是 GT 材料与电路。 */
    private static void registerMachineRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                require(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 8), "Steel plate"),
                require(ItemList.Circuit_Integrated.get(1), "Integrated circuit"))
            .itemOutputs(require(AddonItemList.Machine_Addon_Large_Macerator.get(1), "Large Macerator"))
            .duration(RECIPE_DURATION_LONG)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                require(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 8), "Aluminium plate"),
                require(ItemList.Circuit_Good.get(1), "Good circuit"),
                require(ItemList.Tool_DataStick.get(1), "Data stick"))
            .itemOutputs(require(AddonItemList.Machine_Addon_Computation_Lab.get(1), "Computation Lab"))
            .duration(RECIPE_DURATION_LONG)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    /**
     * 使用本 mod 自注册材料的流体。
     *
     * <p>
     * 这条配方是"材料注册成功"的端到端证据：材料由本 mod 通过 GT 的 addon 入口登记，
     * 流体由 GT 的 {@code LoaderGTBlockFluid} 自动生成，配方由 GT 的组装机配方表执行——
     * 整条链路上本 mod 没有自建任何材料或流体基础设施。
     */
    private static void registerMaterialRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                require(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4), "Steel plate"),
                require(ItemList.Circuit_Integrated.get(1), "Integrated circuit"))
            .fluidInputs(requireFluid(AddonMaterials.Voidsteel.getFluid(144), "Voidsteel fluid"))
            .itemOutputs(require(AddonItemList.Machine_Addon_Computation_Lab.get(1), "Computation Lab"))
            .duration(RECIPE_DURATION_LONG)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    /**
     * 断言物品解析成功。
     *
     * @throws IllegalStateException 解析结果为 null（材料缺少该形态物品、或 GT 常量未初始化）
     */
    private static ItemStack require(ItemStack stack, String what) {
        if (stack == null) {
            throw new IllegalStateException(
                "Could not resolve " + what
                    + " while registering addon recipes. "
                    + "Check the material/prefix combination or the GT ItemList constant.");
        }
        return stack;
    }

    /**
     * 断言流体解析成功。
     *
     * @throws IllegalStateException 解析结果为 null（材料没有流体、或流体未被 GT 自动生成）
     */
    private static FluidStack requireFluid(FluidStack stack, String what) {
        if (stack == null) {
            throw new IllegalStateException(
                "Could not resolve " + what
                    + " while registering addon recipes. "
                    + "The material must declare addFluid() before GT's preload runs.");
        }
        return stack;
    }
}
