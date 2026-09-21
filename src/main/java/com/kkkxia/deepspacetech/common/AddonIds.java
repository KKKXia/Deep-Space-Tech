package com.kkkxia.deepspacetech.common;

import java.util.ArrayList;
import java.util.List;

import com.kkkxia.deepspacetech.DeepSpaceTech;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/**
 * 本 addon 占用的机器 ID。
 *
 * <p>
 * <b>为什么必须自己管理 ID：</b>GT 的机器 ID 是全局共享的单一命名空间
 * （{@code GregTechAPI.METATILEENTITIES} 数组下标，同时也是机器物品的 damage 值），
 * 且 id 会写进存档与物品 NBT，所以不能用"运行时自增"分配——一旦注册顺序变化，
 * 老存档里的机器就会变成另一台机器。因此这里的每个 id 都是固定常量。
 *
 * <p>
 * <b>为什么只校验用到的 id、而不是要求一整段连续空闲：</b>
 * GT 的 {@code MetaTileEntityIDs} 文件头自己写着 "This does not list all used IDs"——
 * 实测确实如此：id 20000 被 {@code gt.icecreammachine} 占用，但它并不在那个枚举里。
 * 也就是说"静态挑一段看着空的区间"是不可靠的，唯一可靠的判据是加载期的运行期检查。
 * 既然只能运行期检查，就没有理由要求整段连续空闲——按实际使用的 id 逐个校验即可，
 * 既更容易找到可用 id，也不会因为"区间里某个无关 id 被占"而误报。
 *
 * <p>
 * ID 冲突在加载期直接抛异常并指出占用者，而不是等 GT 构造器抛出一句难定位的
 * "id already occupied"。
 */
public final class AddonIds {

    /**
     * 本 addon 使用的机器 id。
     *
     * <p>
     * 取值策略：GT 与各 addon 的 id 分布稀疏（32766 个里只用了约 2000 个），
     * 这里选在高位区段以降低与 GT 后续新增机器撞号的概率。
     * 若将来某个 id 被占用，{@link #validate()} 会在启动时报出占用者，
     * 此时只需把对应常量改成另一个空闲值，并在 {@link #logFreeIdSuggestions()} 的帮助下确认。
     */
    public static final int MACHINE_ADDON_MACERATOR_LV = 31600;
    public static final int MACHINE_ADDON_MACERATOR_MV = 31601;
    public static final int MACHINE_ADDON_LARGE_MACERATOR = 31602;
    public static final int MACHINE_ADDON_COMPUTATION_LAB = 31603;

    /** 本 addon 实际使用的全部 id，供校验与调试输出复用。 */
    private static final int[] USED_IDS = { MACHINE_ADDON_MACERATOR_LV, MACHINE_ADDON_MACERATOR_MV,
        MACHINE_ADDON_LARGE_MACERATOR, MACHINE_ADDON_COMPUTATION_LAB };

    private AddonIds() {}

    /**
     * 注册前预检全部用到的 id。
     *
     * @throws IllegalStateException 有 id 已被 GT 或其它 addon 占用
     */
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

    /**
     * 调试用：输出一段可用的连续空闲 id。
     *
     * <p>
     * 当需要搬迁 id 时打开 {@code debugLogging}，启动日志会直接给出一个可用区段，
     * 不必再靠猜或者翻 GT 的巨型枚举。
     */
    public static void logFreeIdSuggestions() {
        final int needed = USED_IDS.length;
        // 从高位区段往上找：addon 更适合占用高位 id，避免与 GT 后续新增的低位机器挤在一起
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
