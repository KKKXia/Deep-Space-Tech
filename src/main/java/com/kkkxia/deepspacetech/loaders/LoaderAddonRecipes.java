package com.kkkxia.deepspacetech.loaders;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fluids.FluidStack;

import com.kkkxia.deepspacetech.DeepSpaceTech;
import com.kkkxia.deepspacetech.common.AddonIds;
import com.kkkxia.deepspacetech.common.AddonItemList;
import com.kkkxia.deepspacetech.common.DevEnvironment;
import com.kkkxia.deepspacetech.common.config.Config;
import com.kkkxia.deepspacetech.common.materials.AddonMaterials;

import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/**
 * 配方注册（本 mod 的 postInit）：直接用 GT 的 {@code GTValues.RA.stdBuilder()} 与 GT 配方表，不自建配方系统。
 */
public final class LoaderAddonRecipes {

    private static final int RECIPE_DURATION_SHORT = 5 * 20;
    private static final int RECIPE_DURATION_LONG = 20 * 20;

    private LoaderAddonRecipes() {}

    public static void run() {
        registerTieredMachineRecipes();
        registerExampleRecipes();
    }

    /** GT 各等级的单方块电解机，下标 0 = tier 1（LV）。 */
    private static final ItemList[] GT_ELECTROLYSERS = { ItemList.Machine_LV_Electrolyzer,
        ItemList.Machine_MV_Electrolyzer, ItemList.Machine_HV_Electrolyzer, ItemList.Machine_EV_Electrolyzer,
        ItemList.Machine_IV_Electrolyzer, ItemList.ElectrolyzerLuV, ItemList.ElectrolyzerZPM, ItemList.ElectrolyzerUV,
        ItemList.ElectrolyzerUHV, ItemList.ElectrolyzerUEV, ItemList.ElectrolyzerUIV, ItemList.ElectrolyzerUMV };

    /** GT 各等级的单方块化学反应釜，下标 0 = tier 1（LV）。 */
    private static final ItemList[] GT_CHEMICAL_REACTORS = { ItemList.Machine_LV_ChemicalReactor,
        ItemList.Machine_MV_ChemicalReactor, ItemList.Machine_HV_ChemicalReactor, ItemList.Machine_EV_ChemicalReactor,
        ItemList.Machine_IV_ChemicalReactor, ItemList.ChemicalReactorLuV, ItemList.ChemicalReactorZPM,
        ItemList.ChemicalReactorUV, ItemList.ChemicalReactorUHV, ItemList.ChemicalReactorUEV,
        ItemList.ChemicalReactorUIV, ItemList.ChemicalReactorUMV };

    /**
     * 用 {@code GTModHandler.addShapelessCraftingRecipe} 注册"GT 同级机器 → 本 mod 机器"的无序合成；
     * {@code BUFFERED} 位让这些配方走 GT 的缓冲队列，在 GT 的延迟合成阶段才真正写入合成表。
     */
    private static void registerTieredMachineRecipes() {
        int registered = 0;
        int failed = 0;
        for (int tier = 1; tier <= AddonIds.TIER_COUNT; tier++) {
            if (addConversion(AddonItemList.multiTankElectrolyser(tier), GT_ELECTROLYSERS[tier - 1], tier)) {
                registered++;
            } else {
                failed++;
            }
            if (addConversion(AddonItemList.multiTankChemicalReactor(tier), GT_CHEMICAL_REACTORS[tier - 1], tier)) {
                registered++;
            } else {
                failed++;
            }
        }
        DeepSpaceTech.LOG.info("Registered {} machine conversion recipes ({} failed)", registered, failed);
        if (failed > 0) {
            DeepSpaceTech.LOG.warn(
                "{} conversion recipes were rejected by GTModHandler; the source machine items may be unset",
                failed);
        }
    }

    private static boolean addConversion(AddonItemList result, ItemList source, int tier) {
        return GTModHandler.addShapelessCraftingRecipe(
            require(result.get(1), result.name()),
            GTModHandler.RecipeBits.DO_NOT_CHECK_FOR_COLLISIONS | GTModHandler.RecipeBits.BUFFERED,
            new Object[] { require(source.get(1), source.name() + " (tier " + tier + ")") });
    }

    /**
     * dev 自检：直接问 {@code CraftingManager} 能否把 GT 机器合成本 mod 机器。必须放在所有 mod 的
     * load-complete 之后，因为 {@code BUFFERED} 配方要到 GT 的 load-complete 才真正写入合成表。
     */
    public static void verifyConversionRecipes() {
        int missing = 0;
        for (int tier = 1; tier <= AddonIds.TIER_COUNT; tier++) {
            if (!isCraftable(AddonItemList.multiTankElectrolyser(tier), GT_ELECTROLYSERS[tier - 1])) {
                missing++;
            }
            if (!isCraftable(AddonItemList.multiTankChemicalReactor(tier), GT_CHEMICAL_REACTORS[tier - 1])) {
                missing++;
            }
        }
        if (missing > 0) {
            throw new IllegalStateException(
                missing + " of "
                    + (AddonIds.TIER_COUNT * 2)
                    + " machine conversion recipes are missing from the crafting manager");
        }
        DeepSpaceTech.LOG.info(
            "Verified {} machine conversion recipes in the crafting manager",
            AddonIds.TIER_COUNT * 2);
    }

    private static boolean isCraftable(AddonItemList result, ItemList source) {
        final ItemStack expected = result.get(1);
        final ItemStack ingredient = source.get(1);
        if (expected == null || ingredient == null) {
            DeepSpaceTech.LOG.error(
                "Conversion self-check: {} or {} has no item registered",
                result.name(),
                source.name());
            return false;
        }

        final InventoryCrafting grid = new InventoryCrafting(EMPTY_CONTAINER, 3, 3);
        grid.setInventorySlotContents(0, ingredient);
        final ItemStack crafted = CraftingManager.getInstance()
            .findMatchingRecipe(grid, null);
        if (crafted == null || !GTUtility.areStacksEqual(crafted, expected, true)) {
            DeepSpaceTech.LOG.error(
                "Conversion self-check: {} does not craft into {} (got {})",
                source.name(),
                result.name(),
                crafted == null ? "nothing" : crafted.getDisplayName());
            return false;
        }
        return true;
    }

    /**
     * {@link InventoryCrafting} 的 {@code setInventorySlotContents} 会回调 {@code onCraftMatrixChanged}，
     * 传 null 会 NPE，所以需要一个什么都不做的 {@code Container}。
     */
    private static final Container EMPTY_CONTAINER = new Container() {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return true;
        }
    };

    /** 示例配方与示例机器/材料同门控——非 dev 下示例条目未绑定、Voidsteel 不存在，照常执行会抛异常。 */
    private static void registerExampleRecipes() {
        if (!DevEnvironment.isDev()) {
            DeepSpaceTech.LOG.info("Not a dev environment, skipping example recipes");
            return;
        }

        // 先确认 GT 为本 mod 的材料自动生成了流体，避免后面写配方时拿到 null
        AddonMaterials.verify();

        registerRecyclingRecipes();
        registerMachineRecipes();
        registerMaterialRecipes();

        if (Config.debugLogging) {
            DeepSpaceTech.LOG.info("Registered example recipes");
        }
    }

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
     * {@code GTOreDictUnificator.get(...)} 在材料缺少该形态物品时返回 null，而 GT 的
     * {@code itemInputs(...)} 只会打一行日志后跳过，导致配方静默少一个输入；这里改为显式失败。
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

    /** 同 {@link #require}，流体版。 */
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
