package com.kkkxia.deepspacetech.common;

import java.util.ArrayList;
import java.util.List;

import com.kkkxia.deepspacetech.DeepSpaceTech;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/**
 * 本 addon 占用的机器 ID：GT 的 id 数组是全局共享命名空间且会写进存档，故必须是固定常量，不能运行时自增。
 * 只逐个校验实际用到的 id：GT 的 {@code MetaTileEntityIDs} 并不列全已用 id，静态判断空闲区间不可靠。
 */
public final class AddonIds {

    /** 电压等级数：tier 1..12 即 LV..UMV，与 GT 单方块电解机/化学反应釜的覆盖范围一致。 */
    public static final int TIER_COUNT = 12;

    public static final int MACHINE_ADDON_MACERATOR_LV = 31600;
    public static final int MACHINE_ADDON_MACERATOR_MV = 31601;
    public static final int MACHINE_ADDON_LARGE_MACERATOR = 31602;
    public static final int MACHINE_ADDON_COMPUTATION_LAB = 31603;

    /** 多流体槽电解机 ID 基址：第 tier 级 = {@code MULTI_TANK_ELECTROLYSER_BASE + tier}。 */
    public static final int MULTI_TANK_ELECTROLYSER_BASE = 31610;
    /** 多流体槽化学反应釜 ID 基址：第 tier 级 = {@code MULTI_TANK_CHEMICAL_REACTOR_BASE + tier}。 */
    public static final int MULTI_TANK_CHEMICAL_REACTOR_BASE = 31630;

    private static final int[] USED_IDS = buildUsedIds();

    private AddonIds() {}

    public static int multiTankElectrolyserId(int tier) {
        return MULTI_TANK_ELECTROLYSER_BASE + tier;
    }

    public static int multiTankChemicalReactorId(int tier) {
        return MULTI_TANK_CHEMICAL_REACTOR_BASE + tier;
    }

    private static int[] buildUsedIds() {
        final List<Integer> ids = new ArrayList<>();
        ids.add(MACHINE_ADDON_MACERATOR_LV);
        ids.add(MACHINE_ADDON_MACERATOR_MV);
        ids.add(MACHINE_ADDON_LARGE_MACERATOR);
        ids.add(MACHINE_ADDON_COMPUTATION_LAB);
        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            ids.add(multiTankElectrolyserId(tier));
            ids.add(multiTankChemicalReactorId(tier));
        }
        final int[] result = new int[ids.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    /** 注册前预检全部用到的 id，冲突时直接抛异常并指出占用者。 */
    public static void validate() {
        final List<String> conflicts = new ArrayList<>();
        for (int id : USED_IDS) {
            final IMetaTileEntity existing = GregTechAPI.METATILEENTITIES[id];
            if (existing != null) {
                conflicts.add(id + " -> " + existing.getMetaName());
            }
        }
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                "Addon meta tile entity ids are already occupied: " + conflicts
                    + ". Change the conflicting constants in AddonIds to free ids.");
        }
    }

    /** 调试用：输出一段可用的连续空闲 id（搬迁 id 时打开 {@code debugLogging}）。 */
    public static void logFreeIdSuggestions() {
        final int needed = USED_IDS.length;
        // 从高位区段找起：addon 占高位 id，避免与 GT 后续新增的低位机器挤在一起
        final int suggestion = findFreeBlock(needed, 20000);
        if (suggestion > 0) {
            DeepSpaceTech.LOG
                .info("Suggested free meta tile entity id block: {}..{}", suggestion, suggestion + needed - 1);
        } else {
            DeepSpaceTech.LOG.warn("No free contiguous meta tile entity id block of size {} was found", needed);
        }
    }

    private static int findFreeBlock(int needed, int fromId) {
        int runStart = -1;
        for (int id = fromId; id < GregTechAPI.METATILEENTITIES.length; id++) {
            if (GregTechAPI.METATILEENTITIES[id] == null) {
                if (runStart < 0) {
                    runStart = id;
                }
                if (id - runStart + 1 >= needed) {
                    return runStart;
                }
            } else {
                runStart = -1;
            }
        }
        return -1;
    }
}
