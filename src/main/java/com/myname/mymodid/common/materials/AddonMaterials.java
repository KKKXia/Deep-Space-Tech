package com.myname.mymodid.common.materials;

import com.myname.mymodid.MyMod;

import gregtech.api.enums.MaterialBuilder;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;

/**
 * 本 addon 的材料——通过 GT 官方为 addon 开放的入口注册，而不是自建材料体系。
 *
 * <p>
 * <b>为什么必须在 GT 的 preInit 之前登记：</b>GT 的材料回调点在
 * {@code Materials.init()} 内部（源码里明确写着 "This is where addon mods can add/manipulate materials"），
 * 而 {@code Materials.init()} 由 {@code GTMod.onPreInitialization} 调用。
 * 本 mod 声明了 {@code required-after:gregtech}，自己的 preInit 晚于 GT 的整个 preInit，
 * 因此唯一可用的更早窗口是 {@code GregTechAPI.sBeforeGTPreload}
 * （GT 在调用 {@code Materials.init()} 之前会先跑这个列表）。
 * 登记动作见 {@link AddonMaterialHandler}。
 *
 * <p>
 * <b>注册后本 mod 免费获得什么（这就是"不重复造轮子"）：</b>
 * <ul>
 * <li>材料进入 GT 的 {@code Materials.values()}，被 GT 的所有遍历式逻辑认作一等材料；</li>
 * <li>{@code addFluid()} 会让 GT 的 {@code LoaderGTBlockFluid} 自动生成对应流体
 * （它在 GT preload 中遍历 {@code Materials.values()} 并处理 {@code hasCorrespondingFluid()}），
 * 连同颜色、温度、本地化名一起生成；</li>
 * <li>可被 {@code GTValues.RA.stdBuilder().fluidInputs(...)} 直接使用，并获得 GT 的流体显示与 NEI 支持。</li>
 * </ul>
 *
 * <p>
 * <b>本 mod 无法免费获得什么（有意不声明）：</b>物品形态（粉/锭/板）。
 * GT 的物品 damage → 材料映射来自 {@code MaterialsIDMap} 这张 GT 自有的 1000 项硬编码表，
 * 表外的材料 {@code mMetaItemSubID} 恒为 -1，因此 GT 的 meta 物品不会为其生成任何物品。
 * 为 addon 材料提供物品需要 addon 自带 meta 物品与贴图，属于独立的一块工作，
 * 这里不做"声明了却生成不出来"的假声明。
 */
public final class AddonMaterials {

    /** 材料名。GT 的 {@code Materials} 构造器对同名材料是静默替换，因此这个名字必须全局唯一。 */
    private static final String VOIDSTEEL_NAME = "Voidsteel";

    /** 本 mod 的材料实例；由 GT 在 preload 阶段回调后可用。 */
    public static Materials Voidsteel;

    private AddonMaterials() {}

    /**
     * 由 GT 的材料回调触发（{@code Materials.init()} 内部）。
     *
     * @throws IllegalStateException 材料名已被其它 mod 占用
     */
    public static void init() {
        // GT 的 Materials 构造器用 MATERIALS_MAP.put(mName, this) 登记，同名会静默覆盖，
        // 所以这里提前拦住，避免悄悄顶掉别的 mod 的材料。
        final Materials existing = Materials.get(VOIDSTEEL_NAME);
        if (existing != null && existing != Materials._NULL) {
            throw new IllegalStateException(
                "Material name '" + VOIDSTEEL_NAME
                    + "' is already registered as "
                    + existing
                    + ". Rename the addon material to avoid silently replacing another mod's material.");
        }

        Voidsteel = new MaterialBuilder().setName(VOIDSTEEL_NAME)
            .setDefaultLocalName("Voidsteel")
            .setChemicalFormula("Vs")
            .setARGB(0x5A4A8E)
            // 只声明流体：GT 的 LoaderGTBlockFluid 会遍历 Materials.values() 为它自动生成流体
            .addFluid()
            // 让 GT 额外生成"熔融流体"变体（SMELTING_TO_FLUID 是 GT 自己的遍历条件之一）
            .addSubTag(SubTag.SMELTING_TO_FLUID)
            .constructMaterial();

        MyMod.LOG.info("Registered addon material {}", Voidsteel.mName);
    }

    /**
     * 加载完成后的自检：确认 GT 确实为本 mod 的材料生成了流体。
     *
     * <p>
     * 这不是多余的断言——它把"addon 材料到底有没有被 GT 的自动生成流程覆盖到"
     * 变成一条启动日志，而不是等到写配方时才发现拿到 null。
     */
    public static void verify() {
        if (Voidsteel == null) {
            throw new IllegalStateException("Addon material was never registered; the GT material hook did not run");
        }
        if (Voidsteel.getFluid(1) == null) {
            throw new IllegalStateException(
                "GT did not auto-generate a fluid for " + Voidsteel.mName
                    + ". Check that addFluid() was called before GT's preload ran.");
        }
        MyMod.LOG.info("Verified addon material {} -> fluid {}", Voidsteel.mName, Voidsteel.mFluid.getName());
    }
}
