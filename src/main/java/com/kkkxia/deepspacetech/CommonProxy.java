package com.kkkxia.deepspacetech;

import com.kkkxia.deepspacetech.common.AddonIds;
import com.kkkxia.deepspacetech.common.DevEnvironment;
import com.kkkxia.deepspacetech.common.config.Config;
import com.kkkxia.deepspacetech.common.machines.MTETieredMultiFluidMachine;
import com.kkkxia.deepspacetech.loaders.LoaderAddonMachines;
import com.kkkxia.deepspacetech.loaders.LoaderAddonRecipes;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * 服务端/公共代理——按 GT 的加载节奏安排本 mod 的注册。
 *
 * <p>
 * 机器原型必须在 GT 的机器注册窗口 {@code sPreloadStarted && !sPostloadStarted} 内构造：
 * 本 mod 的 preInit 落在该窗口内，而 {@code sAfterGTPreload} 在 GT preInit 里就已跑完，挂它无效。
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        // GT 的全局 MTE 数组为所有 addon 共享，先预检 ID 冲突以给出可定位的报错
        AddonIds.validate();
        if (Config.debugLogging) {
            AddonIds.logFreeIdSuggestions();
        }

        LoaderAddonMachines.run();
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {
        // 放在 postInit：此时其它 mod 的方块物品与矿辞都已就绪
        LoaderAddonRecipes.run();
    }

    /**
     * 只跑 dev 自检：{@code RecipeMaps.electrolyzerNonCellRecipes} 由 GT++ 在它自己的 load-complete
     * 里批量生成，所以必须等到所有 mod 的 load-complete 之后。
     */
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (DevEnvironment.isDev()) {
            MTETieredMultiFluidMachine.runRecipeSelfCheck();
            LoaderAddonRecipes.verifyConversionRecipes();
        }
    }
}
