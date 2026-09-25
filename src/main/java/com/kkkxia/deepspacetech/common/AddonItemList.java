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
 * 本 mod 的物品索引表，沿用 GT 的 {@link IItemContainer} 约定（GT 的 {@code ItemList}、tectech 的 {@code CustomItemList} 亦然），
 * 这样配方代码可用 {@code isStackEqual(...)} 做矿辞/NBT 感知的比较；实现体与 tectech {@code CustomItemList} 的样板保持一致。
 */
public enum AddonItemList implements IItemContainer {

    Machine_Addon_Macerator_LV,
    Machine_Addon_Macerator_MV,
    Machine_Addon_Large_Macerator,
    Machine_Addon_Computation_Lab,

    MultiTankElectrolyser_LV,
    MultiTankElectrolyser_MV,
    MultiTankElectrolyser_HV,
    MultiTankElectrolyser_EV,
    MultiTankElectrolyser_IV,
    MultiTankElectrolyser_LuV,
    MultiTankElectrolyser_ZPM,
    MultiTankElectrolyser_UV,
    MultiTankElectrolyser_UHV,
    MultiTankElectrolyser_UEV,
    MultiTankElectrolyser_UIV,
    MultiTankElectrolyser_UMV,

    MultiTankChemicalReactor_LV,
    MultiTankChemicalReactor_MV,
    MultiTankChemicalReactor_HV,
    MultiTankChemicalReactor_EV,
    MultiTankChemicalReactor_IV,
    MultiTankChemicalReactor_LuV,
    MultiTankChemicalReactor_ZPM,
    MultiTankChemicalReactor_UV,
    MultiTankChemicalReactor_UHV,
    MultiTankChemicalReactor_UEV,
    MultiTankChemicalReactor_UIV,
    MultiTankChemicalReactor_UMV;

    /** 等级 → 物品绑定 的映射，下标 0 即 tier 1（LV）。 */
    private static final AddonItemList[] ELECTROLYSERS = { MultiTankElectrolyser_LV, MultiTankElectrolyser_MV,
        MultiTankElectrolyser_HV, MultiTankElectrolyser_EV, MultiTankElectrolyser_IV, MultiTankElectrolyser_LuV,
        MultiTankElectrolyser_ZPM, MultiTankElectrolyser_UV, MultiTankElectrolyser_UHV, MultiTankElectrolyser_UEV,
        MultiTankElectrolyser_UIV, MultiTankElectrolyser_UMV };

    private static final AddonItemList[] CHEMICAL_REACTORS = { MultiTankChemicalReactor_LV, MultiTankChemicalReactor_MV,
        MultiTankChemicalReactor_HV, MultiTankChemicalReactor_EV, MultiTankChemicalReactor_IV,
        MultiTankChemicalReactor_LuV, MultiTankChemicalReactor_ZPM, MultiTankChemicalReactor_UV,
        MultiTankChemicalReactor_UHV, MultiTankChemicalReactor_UEV, MultiTankChemicalReactor_UIV,
        MultiTankChemicalReactor_UMV };

    public static AddonItemList multiTankElectrolyser(int tier) {
        return ELECTROLYSERS[tier - 1];
    }

    public static AddonItemList multiTankChemicalReactor(int tier) {
        return CHEMICAL_REACTORS[tier - 1];
    }

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
     * 自实现的等价物，取代已标注 {@code @Deprecated} 的 {@code GTUtility.copyAmount(long, Object...)}，只锚定稳定 API。
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
