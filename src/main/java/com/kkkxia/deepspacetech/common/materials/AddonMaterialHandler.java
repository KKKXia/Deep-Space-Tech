package com.kkkxia.deepspacetech.common.materials;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IMaterialHandler;

/**
 * 把本 mod 的材料挂进 GT 的材料初始化流程。
 *
 * <p>
 * {@link IMaterialHandler} 是 GT 为 addon 预留的材料回调接口，但它的调用点在
 * {@code Materials.init()} 内部，所以本类必须在 GT 的 preInit 之前被排进
 * {@link GregTechAPI#sBeforeGTPreload}——这一步由 {@code DeepSpaceTech.construction} 完成。
 *
 * <p>
 * 为什么用 {@code sBeforeGTPreload} 而不是自己的 FML preInit：
 * 本 mod 的 preInit 晚于 GT 的整个 preInit（含 {@code Materials.init()} 与 {@code LoaderGTBlockFluid}），
 * 那时材料表与流体生成都已结束，再注册的材料不会进入 {@code Materials.values()}，
 * 也就拿不到 GT 的自动流体生成。
 */
public class AddonMaterialHandler implements IMaterialHandler {

    /**
     * 排入 GT 的 preload 前钩子。必须在 FML 构造期调用（见 {@code DeepSpaceTech#construction}）。
     *
     * <p>
     * {@code sBeforeGTPreload} 是 {@code List<Runnable>}，所以这里用 lambda 包一层：
     * 真正调用 {@link Materials#add} 的时刻是 GT 跑这个列表时（{@code GTMod} 中紧邻
     * {@code Materials.init()} 之前），而不是本 mod 的构造期——
     * 这样 {@code Materials} 的类初始化时机与 GT 原本的流程只差几十行，风险最小。
     */
    public static void install() {
        GregTechAPI.sBeforeGTPreload.add(() -> Materials.add(new AddonMaterialHandler()));
    }

    @Override
    public void onMaterialsInit() {
        AddonMaterials.init();
    }
}
