package com.kkkxia.deepspacetech.common.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.kkkxia.deepspacetech.DeepSpaceTech;

/**
 * 本 mod 配置。外部 addon 不能往 GT 的 {@code ConfigurationManager} 注册表里塞东西，
 * 因此使用 Forge 原生的 {@link Configuration}。
 */
public final class Config {

    private static final String CATEGORY_DEBUG = "debug";

    /** 是否输出内容注册明细，排查"机器没注册上"时打开。 */
    public static boolean debugLogging;

    private Config() {}

    public static void synchronizeConfiguration(File configFile) {
        final Configuration configuration = new Configuration(configFile);
        configuration.load();

        debugLogging = configuration.getBoolean(
            "debugLogging",
            CATEGORY_DEBUG,
            false,
            "Log detailed content registration output (machines, recipes)");

        if (configuration.hasChanged()) {
            configuration.save();
        }
        DeepSpaceTech.LOG.info("Configuration loaded, debugLogging={}", debugLogging);
    }
}
