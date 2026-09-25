package com.kkkxia.deepspacetech.common.machines;

import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe.SpecialEffects;
import gregtech.api.recipe.RecipeMaps;

/**
 * 多流体槽电解机：单方块，配方池复用 GT 多方块工业电解机的 {@code electrolyzerNonCellRecipes}，
 * 因此 NEI 上与本机器的催化页就是工业电解机那一页。
 */
public class MTEMultiTankElectrolyser extends MTETieredMultiFluidMachine {

    private static final String OVERLAY = "ELECTROLYZER";
    private static final SoundResource SOUND = SoundResource.GTCEU_LOOP_ELECTROLYZER;

    public MTEMultiTankElectrolyser(int aID, String aName, String aNameRegional, int aTier, String[] aDescription) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            aDescription,
            RecipeMaps.electrolyzerNonCellRecipes,
            SOUND,
            SpecialEffects.NONE,
            OVERLAY);
    }

    private MTEMultiTankElectrolyser(String aName, int aTier, String[] aDescription, int aAmperage,
        ITexture[][][] aTextures) {
        super(
            aName,
            aTier,
            aDescription,
            RecipeMaps.electrolyzerNonCellRecipes,
            aAmperage,
            aTextures,
            SOUND,
            SpecialEffects.NONE);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEMultiTankElectrolyser(mName, mTier, mDescriptionArray, mAmperage, mTextures)
            .setProgressBarTexture(progressBarTexture)
            .setRecipeCatalystPriority(recipeCatalystPriority);
    }
}
