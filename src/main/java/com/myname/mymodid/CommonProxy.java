package com.myname.mymodid;

import com.myname.mymodid.common.AddonIds;
import com.myname.mymodid.common.config.Config;
import com.myname.mymodid.loaders.LoaderAddonMachines;
import com.myname.mymodid.loaders.LoaderAddonRecipes;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * 服务端/公共代理——只做"按 GT 的加载节奏安排本 mod 的注册"。
 *
 * <p>
 * 时序依据（重要，决定了为什么不额外使用 {@code GregTechAPI.s*GT*} 钩子列表）：
 * 本 mod 声明了 {@code required-after:gregtech}，因此 FML 的调用顺序是
 * 
 * <pre>
 * GT.preInit → ... → 本 mod.preInit → (全部 init) → GT.postInit
 * </pre>
 * 
 * 而 GT 的机器注册窗口是 {@code sPreloadStarted && !sPostloadStarted}：
 * GT 在 preInit 开头置起，在 postInit 开头落下。所以<b>本 mod 的 preInit 正处在这个窗口内</b>，
 * 可以直接构造机器原型，不需要挂 {@code sAfterGTPreload}（那个列表在 GT 的 preInit 里就已经跑完了）。
 *
 * <p>
 * {@code s*GT*} 钩子列表留给"必须卡在 GT 某个阶段内部"的场景（例如必须在 GT 矿辞统一之后），
 * 本 mod 当前没有这种需求，所以不引入额外时序耦合。
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        // 先预检 ID 冲突：GT 的全局 MTE 数组是所有 addon 共享的，冲突时给出可定位的报错
        AddonIds.validate();
        if (Config.debugLogging) {
            AddonIds.logFreeIdSuggestions();
        }

        // 机器原型：必须在 GT 的 preload 窗口内注册
        LoaderAddonMachines.run();
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {
        // 配方放在 postInit：此时其它 mod 的方块物品与矿辞都已就绪
        LoaderAddonRecipes.run();
    }
}
