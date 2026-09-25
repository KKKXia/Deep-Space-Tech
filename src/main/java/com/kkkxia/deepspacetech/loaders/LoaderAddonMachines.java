package com.kkkxia.deepspacetech.loaders;

import java.util.Locale;

import com.kkkxia.deepspacetech.DeepSpaceTech;
import com.kkkxia.deepspacetech.common.AddonIds;
import com.kkkxia.deepspacetech.common.AddonItemList;
import com.kkkxia.deepspacetech.common.DevEnvironment;
import com.kkkxia.deepspacetech.common.config.Config;
import com.kkkxia.deepspacetech.common.machines.MTEAddonComputationLab;
import com.kkkxia.deepspacetech.common.machines.MTEAddonLargeMacerator;
import com.kkkxia.deepspacetech.common.machines.MTEMultiTankChemicalReactor;
import com.kkkxia.deepspacetech.common.machines.MTEMultiTankElectrolyser;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.SoundResource;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.tileentities.machines.basic.MTEBasicMachineWithRecipeBuilder;

/**
 * 机器注册（本 mod 的 preInit，位于 GT 的 preload 窗口内）；沿用 GT 的"构造即注册"：
 * 机器 id 在构造器里写入 {@code GregTechAPI.METATILEENTITIES[id]}。
 */
public final class LoaderAddonMachines {

    private LoaderAddonMachines() {}

    public static void run() {
        registerTieredMachines();
        registerExampleMachines();
    }

    // description 在 preInit 就被读取，此时语言文件尚未重载，用 StatCollector 只会拿到 key，故用字面量。
    private static final String[] ELECTROLYSER_DESCRIPTION = {
        "Multi-tank single-block electrolyzer.",
        "Runs the Industrial Electrolyzer's recipe pool." };

    private static final String[] CHEMICAL_REACTOR_DESCRIPTION = {
        "Multi-tank single-block chemical reactor.",
        "Runs the Large Chemical Reactor's recipe pool." };

    private static void registerTieredMachines() {
        for (int tier = 1; tier <= AddonIds.TIER_COUNT; tier++) {
            final String tierName = GTValues.VN[tier];

            AddonItemList.multiTankElectrolyser(tier)
                .set(
                    new MTEMultiTankElectrolyser(
                        AddonIds.multiTankElectrolyserId(tier),
                        "deepspacetech.machine.multi_tank_electrolyser." + tierName.toLowerCase(Locale.ENGLISH),
                        "Multi-Tank Electrolyser (" + tierName + ")",
                        tier,
                        ELECTROLYSER_DESCRIPTION).getStackForm(1L));

            AddonItemList.multiTankChemicalReactor(tier)
                .set(
                    new MTEMultiTankChemicalReactor(
                        AddonIds.multiTankChemicalReactorId(tier),
                        "deepspacetech.machine.multi_tank_chemical_reactor." + tierName.toLowerCase(Locale.ENGLISH),
                        "Multi-Tank Chemical Reactor (" + tierName + ")",
                        tier,
                        CHEMICAL_REACTOR_DESCRIPTION).getStackForm(1L));
        }

        DeepSpaceTech.LOG
            .info("Registered {} tiered machines (LV..{})", AddonIds.TIER_COUNT * 2, GTValues.VN[AddonIds.TIER_COUNT]);
    }

    /** 示例机器只在 dev 环境注册；非 dev 下示例条目保持未绑定，引用它们的代码必须同样受 dev 门控。 */
    private static void registerExampleMachines() {
        if (!DevEnvironment.isDev()) {
            DeepSpaceTech.LOG.info("Not a dev environment, skipping example machines");
            return;
        }

        registerSingleBlockMachines();
        registerMultiBlockMachines();

        if (Config.debugLogging) {
            DeepSpaceTech.LOG.info("Registered {} example meta tile entities", 4);
        }
    }

    private static void registerSingleBlockMachines() {
        registerAddonMacerator(AddonIds.MACHINE_ADDON_MACERATOR_LV, 1, "LV", AddonItemList.Machine_Addon_Macerator_LV);
        registerAddonMacerator(AddonIds.MACHINE_ADDON_MACERATOR_MV, 2, "MV", AddonItemList.Machine_Addon_Macerator_MV);
    }

    private static void registerAddonMacerator(int id, int tier, String tierName, AddonItemList binding) {
        binding.set(
            MTEBasicMachineWithRecipeBuilder.builder(id)
                .setName(
                    "deepspacetech.machine.addon_macerator." + tierName.toLowerCase(Locale.ENGLISH),
                    "Addon Macerator (" + tierName + ")")
                .setTier(tier)
                .setDescription(
                    new String[] { "An example addon single-block machine.",
                        "Reuses GT's power, overclock, GUI and NEI handling." })
                .setRecipes(RecipeMaps.maceratorRecipes)
                .setSlotsCount(2, 1)
                .setSound(SoundResource.GTCEU_LOOP_MACERATOR)
                .setOverlays("MACERATOR")
                .build()
                .getStackForm(1L));
    }

    private static void registerMultiBlockMachines() {
        AddonItemList.Machine_Addon_Large_Macerator.set(
            new MTEAddonLargeMacerator(
                AddonIds.MACHINE_ADDON_LARGE_MACERATOR,
                "deepspacetech.multimachine.large_macerator",
                "Addon Large Macerator").getStackForm(1L));

        AddonItemList.Machine_Addon_Computation_Lab.set(
            new MTEAddonComputationLab(
                AddonIds.MACHINE_ADDON_COMPUTATION_LAB,
                "deepspacetech.multimachine.computation_lab",
                "Addon Computation Lab").getStackForm(1L));
    }
}
