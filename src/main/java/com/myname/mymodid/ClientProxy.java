package com.myname.mymodid;

/**
 * 客户端代理。
 *
 * <p>
 * 本 mod 当前没有客户端专属内容：GUI 复用 GT 的 ModularUI 实现，渲染复用 GT 的机壳贴图，
 * 音效复用 GT 的 SoundResource。因此这里只作为 {@code @SidedProxy} 的落点存在。
 *
 * <p>
 * 将来若要加自定义 overlay 贴图或独立 GUI，应注册在这里而不是 CommonProxy，
 * 避免把只存在于客户端的类加载到服务端。
 */
public class ClientProxy extends CommonProxy {
}
