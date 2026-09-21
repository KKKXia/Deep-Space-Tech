package com.kkkxia.deepspacetech.loaders;

import java.util.Locale;

import com.kkkxia.deepspacetech.DeepSpaceTech;
import com.kkkxia.deepspacetech.common.AddonIds;
import com.kkkxia.deepspacetech.common.AddonItemList;
import com.kkkxia.deepspacetech.common.config.Config;
import com.kkkxia.deepspacetech.common.machines.MTEAddonComputationLab;
import com.kkkxia.deepspacetech.common.machines.MTEAddonLargeMacerator;

import gregtech.api.enums.SoundResource;
import gregtech.api.recipe.RecipeMaps;
import gregtech.common.tileentities.machines.basic.MTEBasicMachineWithRecipeBuilder;

/**
 * 机器注册（本 mod 的 preInit，位于 GT 的 preload 窗口内）。
 *
 * <p>
 * 对应 GT 的 {@code LoaderMetaTileEntities}，但内容量小得多——因为本 mod 不实现机器框架，
 * 只登记自己的机器。注册方式完全沿用 GT 的"构造即注册"：
 * 机器的 id 在构造器里写入 {@code GregTechAPI.METATILEENTITIES[id]}，
 * 随后 {@code getStackForm(1L)} 得到一个 damage = id 的 {@code sBlockMachines} 物品。
 *
 * <p>
 * 三种注册形态各示范一次：
 * <ol>
 * <li><b>单方块机器</b>：用 GT 的 builder 一行配置，电力、超频、GUI、NEI 全部由 GT 提供，本 mod 零机器代码；</li>
 * <li><b>多方块（纯电力）</b>：继承 {@code MTEExtendedPowerMultiBlockBase}，结构走 GT 新版结构 API；</li>
 * <li><b>多方块（电力 + 算力）</b>：继承 tectech 的 {@code TTMultiblockBase}，额外消耗算力。</li>
 * </ol>
 */
public final class LoaderAddonMachines {

    private LoaderAddonMachines() {}

    public static void run() {
        registerSingleBlockMachines();
        registerMultiBlockMachines();

        if (Config.debugLogging) {
            DeepSpaceTech.LOG.info("Registered {} addon meta tile entities", 4);
        }
    }

    // ---- 示范 1：单方块机器 ----

    /**
     * 单方块机器：一次 builder 配置就是一台完整的 GT 机器。
     *
     * <p>
     * 注意这里没有任何"机器类"。GT 的 {@code MTEBasicMachineWithRecipe} 已经覆盖了
     * 单方块加工机器的全部行为：能量缓冲、超频、输入输出槽、自动输出、GUI、NEI 配方页、
     * 贴图 overlay、音效、扳手/软锤交互。addon 要做的只是告诉它"用哪张配方表、几个槽、什么等级"。
     *
     * <p>
     * overlay 名复用 GT 已有的 {@code MACERATOR}：GT 的贴图路径是
     * {@code gregtech:textures/blocks/basicmachines/<overlay 小写>/OVERLAY_*}，
     * 因此复用已有名字就能直接拿到贴图，addon 不需要自带美术资源。
     */
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
                // 这里用字面量而不是 StatCollector：description 在 preInit 就被读取，
                // 而语言文件要到资源重载之后才可用，此时翻译会退化成 lang key。
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

    // ---- 示范 2 与 3：多方块 ----

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
