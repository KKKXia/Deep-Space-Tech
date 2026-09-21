package com.kkkxia.deepspacetech.common;

import static gregtech.api.util.GTRecipeBuilder.WILDCARD;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import gregtech.api.interfaces.IItemContainer;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/**
 * 本 mod 的物品索引表。
 *
 * <p>
 * 沿用 GT 的 {@link IItemContainer} 约定（GT 的 {@code ItemList}、tectech 的 {@code CustomItemList}
 * 都是这么做的），好处是配方代码可以用 {@code isStackEqual(...)} 做矿辞/NBT 感知的比较，
 * 而不是自己写 ItemStack 相等判断——这正是"避免重复造轮子"在物品层的体现。
 *
 * <p>
 * 实现体是 IItemContainer 的固定样板（GT 没有提供抽象基类，因为 enum 无法继承类），
 * 与 tectech {@code CustomItemList} 的实现保持一致，便于对照阅读。
 */
public enum AddonItemList implements IItemContainer {

    /** 示范 1：单方块机器（LV）。 */
    Machine_Addon_Macerator_LV,
    /** 示范 1：单方块机器（MV）。 */
    Machine_Addon_Macerator_MV,
    /** 示范 2：多方块控制器。 */
    Machine_Addon_Large_Macerator,
    /** 示范 3：算力机器。 */
    Machine_Addon_Computation_Lab;

    private ItemStack mStack;
    private boolean mHasNotBeenSet = true;

    @Override
    public IItemContainer set(Item aItem) {
        mHasNotBeenSet = false;
        if (aItem == null) {
            return this;
        }
        mStack = GTUtility.copyAmount(1, new ItemStack(aItem, 1, 0));
        return this;
    }

    @Override
    public IItemContainer set(ItemStack aStack) {
        mHasNotBeenSet = false;
        mStack = GTUtility.copyAmount(1, aStack);
        return this;
    }

    @Override
    public Item getItem() {
        checkSet();
        return GTUtility.isStackInvalid(mStack) ? null : mStack.getItem();
    }

    @Override
    public Block getBlock() {
        checkSet();
        final Item item = getItem();
        return item == null ? null : Block.getBlockFromItem(item);
    }

    @Override
    public boolean isStackEqual(Object aStack) {
        return isStackEqual(aStack, false, false);
    }

    @Override
    public boolean isStackEqual(Object aStack, boolean aWildcard, boolean aIgnoreNBT) {
        // 先做类型收窄：GTUtility.isStackInvalid(Object) 已标注 @Deprecated，只保留 ItemStack 重载可用
        if (!(aStack instanceof ItemStack stack) || GTUtility.isStackInvalid(stack)) {
            return false;
        }
        return GTUtility.areUnificationsEqual(stack, aWildcard ? getWildcard(1) : get(1), aIgnoreNBT);
    }

    @Override
    public ItemStack get(long aAmount, Object... aReplacements) {
        checkSet();
        if (GTUtility.isStackInvalid(mStack)) {
            return copyReplacement(aAmount, aReplacements);
        }
        return GTUtility.copyAmount((int) aAmount, GTOreDictUnificator.get(mStack));
    }

    @Override
    public ItemStack getWildcard(long aAmount, Object... aReplacements) {
        checkSet();
        if (GTUtility.isStackInvalid(mStack)) {
            return copyReplacement(aAmount, aReplacements);
        }
        // 用 int 重载：GT 的 long 重载已标注 @Deprecated，仅作为转型 shim 存在
        return GTUtility.copyAmountAndMetaData((int) aAmount, WILDCARD, GTOreDictUnificator.get(mStack));
    }

    @Override
    public ItemStack getUndamaged(long aAmount, Object... aReplacements) {
        checkSet();
        if (GTUtility.isStackInvalid(mStack)) {
            return copyReplacement(aAmount, aReplacements);
        }
        return GTUtility.copyAmountAndMetaData((int) aAmount, 0, GTOreDictUnificator.get(mStack));
    }

    @Override
    public ItemStack getAlmostBroken(long aAmount, Object... aReplacements) {
        checkSet();
        if (GTUtility.isStackInvalid(mStack)) {
            return copyReplacement(aAmount, aReplacements);
        }
        return GTUtility
            .copyAmountAndMetaData((int) aAmount, mStack.getMaxDamage() - 1, GTOreDictUnificator.get(mStack));
    }

    @Override
    public ItemStack getWithDamage(long aAmount, long aMetaValue, Object... aReplacements) {
        checkSet();
        if (GTUtility.isStackInvalid(mStack)) {
            return copyReplacement(aAmount, aReplacements);
        }
        return GTUtility.copyAmountAndMetaData((int) aAmount, (int) aMetaValue, GTOreDictUnificator.get(mStack));
    }

    @Override
    public ItemStack getWithName(long aAmount, String aDisplayName, Object... aReplacements) {
        final ItemStack stack = get(1, aReplacements);
        if (GTUtility.isStackInvalid(stack)) {
            return null;
        }
        stack.setStackDisplayName(aDisplayName);
        return GTUtility.copyAmount((int) aAmount, stack);
    }

    @Override
    public ItemStack getWithCharge(long aAmount, int aEnergy, Object... aReplacements) {
        final ItemStack stack = get(1, aReplacements);
        if (GTUtility.isStackInvalid(stack)) {
            return null;
        }
        GTModHandler.chargeElectricItem(stack, aEnergy, Integer.MAX_VALUE, true, false);
        return GTUtility.copyAmount((int) aAmount, stack);
    }

    @Override
    public IItemContainer registerOre(Object... aOreNames) {
        checkSet();
        for (Object oreName : aOreNames) {
            GTOreDictUnificator.registerOre(oreName, get(1));
        }
        return this;
    }

    @Override
    public IItemContainer registerWildcardAsOre(Object... aOreNames) {
        checkSet();
        for (Object oreName : aOreNames) {
            GTOreDictUnificator.registerOre(oreName, getWildcard(1));
        }
        return this;
    }

    @Override
    public boolean hasBeenSet() {
        return !mHasNotBeenSet;
    }

    private void checkSet() {
        if (mHasNotBeenSet) {
            throw new IllegalAccessError("AddonItemList." + name() + " has not been set to an Item yet!");
        }
    }

    /**
     * 从替代物品里取第一个 ItemStack 并复制指定数量。
     *
     * <p>
     * 等价于 GT 的 {@code GTUtility.copyAmount(long, Object...)}，但那个重载已标注
     * {@code @Deprecated}（GT 指向 {@code copyAmount(int, ItemStack)}）。addon 不该依赖
     * 即将被移除的重载，因此这里自己实现同样的语义——这也是本项目"只锚定稳定 API"原则的一个具体体现。
     */
    private static ItemStack copyReplacement(long aAmount, Object... aReplacements) {
        for (Object candidate : aReplacements) {
            if (candidate instanceof ItemStack stack) {
                return GTUtility.copyAmount((int) aAmount, stack);
            }
        }
        return null;
    }
}
