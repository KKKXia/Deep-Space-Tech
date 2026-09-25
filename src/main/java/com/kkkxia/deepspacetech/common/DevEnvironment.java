package com.kkkxia.deepspacetech.common;

import net.minecraft.launchwrapper.Launch;

/**
 * dev 环境判定：1.7.10 的权威判据是 LaunchWrapper 黑板上的 {@code fml.deobfuscatedEnvironment}（GTValues.DEVENV 同源）。
 * 黑板为 null（未正确初始化环境）时按 GT 的做法视为 dev；每次调用都重读黑板，避免 FML 构造期的时序风险。
 */
public final class DevEnvironment {

    private static final String DEOBFUSCATED_ENVIRONMENT_KEY = "fml.deobfuscatedEnvironment";

    private DevEnvironment() {}

    public static boolean isDev() {
        return Launch.blackboard == null || (boolean) Launch.blackboard.get(DEOBFUSCATED_ENVIRONMENT_KEY);
    }
}
