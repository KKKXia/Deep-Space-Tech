package com.kkkxia.deepspacetech.common.materials;

import com.kkkxia.deepspacetech.DeepSpaceTech;
import com.kkkxia.deepspacetech.common.DevEnvironment;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IMaterialHandler;

/**
 * 把本 mod 的材料挂进 GT 的材料初始化流程：回调点在 {@code Materials.init()} 内部，所以必须在
 * GT preInit 之前排进 {@link GregTechAPI#sBeforeGTPreload}（由 {@code DeepSpaceTech.construction} 完成）。
 */
public class AddonMaterialHandler implements IMaterialHandler {

    /** 排入 GT 的 preload 前钩子；必须在 FML 构造期调用（见 {@code DeepSpaceTech#construction}）。 */
    public static void install() {
        GregTechAPI.sBeforeGTPreload.add(() -> Materials.add(new AddonMaterialHandler()));
    }

    @Override
    public void onMaterialsInit() {
        // Voidsteel 是示例内容，只在 dev 环境注册，非 dev 下 GT 也就不会为它生成流体
        if (!DevEnvironment.isDev()) {
            DeepSpaceTech.LOG.info("Not a dev environment, skipping example material");
            return;
        }
        AddonMaterials.init();
    }
}
