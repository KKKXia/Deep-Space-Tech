package com.kkkxia.deepspacetech.common.materials;

import com.kkkxia.deepspacetech.DeepSpaceTech;

import gregtech.api.enums.MaterialBuilder;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;

/**
 * 本 addon 的材料，通过 GT 官方开放的 addon 入口注册：材料回调发生在 {@code Materials.init()} 内部，
 * 因此必须经 {@code GregTechAPI.sBeforeGTPreload} 排队（见 {@link AddonMaterialHandler}）；内容仅 dev 使用。
 */
public final class AddonMaterials {

    /** 材料名；GT 的 {@code Materials} 构造器对同名材料静默替换，因此必须全局唯一。 */
    private static final String VOIDSTEEL_NAME = "Voidsteel";

    /** 由 GT 在 preload 阶段的回调触发后才会被赋值。 */
    public static Materials Voidsteel;

    private AddonMaterials() {}

    /** 由 GT 的材料回调触发（{@code Materials.init()} 内部）。 */
    public static void init() {
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
            .addFluid()
            .addSubTag(SubTag.SMELTING_TO_FLUID)
            .constructMaterial();

        DeepSpaceTech.LOG.info("Registered addon material {}", Voidsteel.mName);
    }

    /** 加载完成后的自检：确认 GT 确实为本 mod 的材料生成了流体。 */
    public static void verify() {
        if (Voidsteel == null) {
            throw new IllegalStateException("Addon material was never registered; the GT material hook did not run");
        }
        if (Voidsteel.getFluid(1) == null) {
            throw new IllegalStateException(
                "GT did not auto-generate a fluid for " + Voidsteel.mName
                    + ". Check that addFluid() was called before GT's preload ran.");
        }
        DeepSpaceTech.LOG.info("Verified addon material {} -> fluid {}", Voidsteel.mName, Voidsteel.mFluid.getName());
    }
}
