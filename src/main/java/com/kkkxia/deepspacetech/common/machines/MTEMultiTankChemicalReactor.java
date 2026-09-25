package com.kkkxia.deepspacetech.common.machines;

import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineWithRecipe.SpecialEffects;
import gregtech.api.recipe.RecipeMaps;

/**
 * 多流体槽化学反应釜：单方块，配方池复用 GT 大型化学反应釜的 {@code multiblockChemicalReactorRecipes}，
 * 而不是单方块反应釜只支持 1 路流体的 {@code chemicalReactorRecipes}。
 */
public class MTEMultiTankChemicalReactor extends MTETieredMultiFluidMachine {

    private static final String OVERLAY = "CHEMICAL_REACTOR";
    private static final SoundResource SOUND = SoundResource.GTCEU_LOOP_CHEMICAL;

    public MTEMultiTankChemicalReactor(int aID, String aName, String aNameRegional, int aTier, String[] aDescription) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            aDescription,
            RecipeMaps.multiblockChemicalReactorRecipes,
            SOUND,
            SpecialEffects.NONE,
            OVERLAY);
    }

    private MTEMultiTankChemicalReactor(String aName, int aTier, String[] aDescription, int aAmperage,
        ITexture[][][] aTextures) {
        super(
            aName,
            aTier,
            aDescription,
            RecipeMaps.multiblockChemicalReactorRecipes,
            aAmperage,
            aTextures,
            SOUND,
            SpecialEffects.NONE);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEMultiTankChemicalReactor(mName, mTier, mDescriptionArray, mAmperage, mTextures)
            .setProgressBarTexture(progressBarTexture)
            .setRecipeCatalystPriority(recipeCatalystPriority);
    }
}
