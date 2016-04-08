package com.minecolonies.client.gui;

import com.blockout.controls.Button;
import com.blockout.controls.Label;
import com.blockout.views.Window;
import com.minecolonies.MineColonies;
import com.minecolonies.colony.buildings.BuildingHome;
import com.minecolonies.lib.Constants;
import com.minecolonies.network.messages.BuildRequestMessage;
import com.minecolonies.network.messages.OpenInventoryMessage;
import com.minecolonies.util.LanguageHandler;

/**
 * Window for the home building
 */
public class WindowHomeBuilding extends Window implements Button.Handler
{

    private static final    String              BUTTON_INVENTORY                = "inventory";
    private static final    String              BUTTON_BUILD                    = "build";
    private static final    String              BUTTON_REPAIR                   = "repair";
    private static final    String              LABEL_BUILDING_NAME = "name";
    private static final    String              HOME_BUILDING_RESOURCE_SUFFIX   = ":gui/windowHutHome.xml";

    private                 BuildingHome.View   building;

    /**
     * Creates the Window object
     *
     * @param building       View of the home building
     */
    public WindowHomeBuilding(BuildingHome.View building)
    {
        super(Constants.MOD_ID + HOME_BUILDING_RESOURCE_SUFFIX);
        this.building = building;
    }

    @Override
    public void onOpened()
    {

        findPaneOfTypeByID(LABEL_BUILDING_NAME, Label.class).setLabel(
                LanguageHandler.getString("com.minecolonies.gui.workerHuts.homeHut"));

        /*
        If level == 0, set build button, and disable repair button
         */
        if (building.getBuildingLevel() == 0)
        {

            findPaneOfTypeByID(BUTTON_BUILD, Button.class).setLabel(
                    LanguageHandler.getString("com.minecolonies.gui.workerHuts.build"));

            findPaneByID(BUTTON_REPAIR).disable();
        }
        else if (building.isBuildingMaxLevel())
            /*
            Else if level == max, disable build button
             */
        {
            Button button = findPaneOfTypeByID(BUTTON_BUILD, Button.class);

            button.setLabel(LanguageHandler.getString("com.minecolonies.gui.workerHuts.upgradeUnavailable"));
            button.disable();

        }
    }

    @Override
    public void onButtonClicked(Button button)
    {
        switch (button.getID())
        {
            case BUTTON_INVENTORY:
                MineColonies.getNetwork().sendToServer(new OpenInventoryMessage(building));
                break;
            case BUTTON_BUILD:
                MineColonies.getNetwork().sendToServer(new BuildRequestMessage(building, BuildRequestMessage.BUILD));
                break;
            case BUTTON_REPAIR:
                MineColonies.getNetwork().sendToServer(new BuildRequestMessage(building, BuildRequestMessage.REPAIR));
                break;
            default:
                break;
        }
    }
}
