package com.kkkxia.deepspacetech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.kkkxia.deepspacetech.common.materials.AddonMaterialHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * mod 入口——GTNH 附属 mod。
 *
 * <p>
 * 这是一个 addon，不是独立 mod：所有通用系统（电力、算力、配方、材料、多方块结构、GUI、NEI）
 * 都直接复用 GT5-Unofficial 的实现，本 mod 只提供自己的内容与集成点。
 *
 * <p>
 * 依赖串里的每一项都是真实依赖，不是"顺手写上"：
 * <ul>
 * <li>{@code gregtech} —— 机器基类、配方表、材料、机壳、结构 API 全部来自它；</li>
 * <li>{@code tectech} —— 算力（Computation）系统的实现方，与 GT5U 同 jar 但独立 mod 容器；</li>
 * <li>{@code structurelib} / {@code gtnhlib} —— GT 自己就强制前置，本 mod 直接用到它们的结构定义与工具。</li>
 * </ul>
 *
 * <p>
 * 不声明 {@code IC2}：本 mod 不直接引用 IC2 类型，GT 自己已声明 {@code required-after:IC2}，
 * 能量接入走 GT 的 {@code MTEHatchEnergy} 即可。
 */
@Mod(
    modid = DeepSpaceTech.MODID,
    name = DeepSpaceTech.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech;" + "required-after:tectech;"
        + "required-after:structurelib;"
        + "required-after:gtnhlib;")
public class DeepSpaceTech {

    public static final String MODID = "deepspacetech";
    public static final String MOD_NAME = "Deep-Space-Tech";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "com.kkkxia.deepspacetech.ClientProxy",
        serverSide = "com.kkkxia.deepspacetech.CommonProxy")
    public static CommonProxy proxy;

    /**
     * FML 构造期钩子——本 mod 唯一一个必须早于 GT preInit 的动作。
     *
     * <p>
     * GT 回调 addon 材料的位置是 {@code Materials.init()}，而它由 {@code GTMod.onPreInitialization}
     * 调用；本 mod 的 preInit 晚于 GT 的整个 preInit，所以在自己的 preInit 里注册材料已经太迟。
     * {@code FMLConstructionEvent} 在所有 mod 的 preInit 之前派发，是唯一合适的窗口。
     *
     * <p>
     * 这里只是往 GT 的 preload 前钩子列表里排一个 Runnable（见 {@code AddonMaterialHandler}），
     * 真正的材料构造发生在 GT 跑该列表时——即 GT 自己调用 {@code Materials.init()} 的前几行，
     * 时序上正好落在"addon 可以增删材料"的窗口内。
     */
    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        AddonMaterialHandler.install();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
