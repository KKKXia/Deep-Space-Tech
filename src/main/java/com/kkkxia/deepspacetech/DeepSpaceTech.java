package com.kkkxia.deepspacetech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.kkkxia.deepspacetech.common.materials.AddonMaterialHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** mod 入口——GTNH 附属 mod，复用 GT5-Unofficial 的电力、算力、配方、材料与结构体系。 */
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
     * 材料注册必须在这里排队：GT 回调 addon 材料的位置是 {@code Materials.init()}（GT preInit 内部），
     * 本 mod 自己的 preInit 晚于它，已经太迟。
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

    /**
     * 只跑 dev 自检：机器要复用的 {@code electrolyzerNonCellRecipes} 有相当一部分由 GT++ 在它自己的
     * load-complete 里生成，早于这个阶段去看那张表会误判成机器没配方可用。
     */
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }
}
