package com.minecolonies.coremod.colony.buildings.workerbuildings;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.buildings.workerbuildings.IBuildingDeliveryman;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.tileentities.*;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.coremod.blocks.BlockMinecoloniesRack;
import com.minecolonies.coremod.client.gui.WindowHutMinPlaceholder;
import com.minecolonies.coremod.colony.buildings.AbstractBuilding;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingWorker;
import com.minecolonies.coremod.colony.buildings.modules.WarehouseModule;
import com.minecolonies.coremod.colony.buildings.views.AbstractBuildingView;
import com.minecolonies.coremod.colony.requestsystem.resolvers.DeliveryRequestResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.PickupRequestResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.WarehouseConcreteRequestResolver;
import com.minecolonies.coremod.colony.requestsystem.resolvers.WarehouseRequestResolver;
import com.minecolonies.coremod.tileentities.TileEntityWareHouse;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.handleTileEntityPlacement;

/**
 * Class of the warehouse building.
 */
public class BuildingWareHouse extends AbstractBuilding implements IWareHouse
{
    /**
     * String describing the Warehouse.
     */
    private static final String WAREHOUSE = "warehouse";

    /**
     * Tag to store the deliverymen.
     */
    private static final String TAG_DELIVERYMAN = "deliveryman";

    /**
     * The list of deliverymen registered to this building.
     */
    private final Set<Vec3> registeredDeliverymen = new HashSet<>();

    /**
     * Max level of the building.
     */
    private static final int MAX_LEVEL = 5;

    /**
     * Max storage upgrades.
     */
    public static final int MAX_STORAGE_UPGRADE = 3;

    /**
     * Instantiates a new warehouse building.
     *
     * @param c the colony.
     * @param l the location
     */
    public BuildingWareHouse(final IColony c, final BlockPos l)
    {
        super(c, l);
    }

    @Override
    public void requestRepair(final BlockPos builder)
    {
        //To ensure that the racks are all set to in the warehouse when repaired.
        for (final BlockPos pos : containerList)
        {
            if (getColony().getWorld() != null)
            {
                final BlockEntity entity = getColony().getWorld().getBlockEntity(pos);
                if (entity instanceof TileEntityRack)
                {
                    ((AbstractTileEntityRack) entity).setInWarehouse(true);
                }
            }
        }

        super.requestRepair(builder);
    }

    @Override
    public boolean registerWithWareHouse(final IBuildingDeliveryman buildingWorker)
    {
        if (registeredDeliverymen.contains(new Vec3(buildingWorker.getID().getX(), buildingWorker.getID().getY(), buildingWorker.getID().getZ())))
        {
            return true;
        }

        if (registeredDeliverymen.size() >= getMaxAssignedDmen())
        {
            if (!registeredDeliverymen.isEmpty())
            {
                checkForRegisteredDeliverymen();
            }

            if (registeredDeliverymen.size() >= getMaxAssignedDmen())
            {
                return false;
            }
        }

        registeredDeliverymen.add(new Vec3(buildingWorker.getID().getX(), buildingWorker.getID().getY(), buildingWorker.getID().getZ()));
        return true;
    }

    @Override
    public void unregisterFromWareHouse(final IBuildingDeliveryman buildingWorker)
    {
        final Vec3 vec = new Vec3(buildingWorker.getID().getX(), buildingWorker.getID().getY(), buildingWorker.getID().getZ());
        registeredDeliverymen.remove(vec);
    }

    /**
     * Get the maximimum number of dmen that can be assigned to the warehoue.
     *
     * @return the maximum amount.
     */
    private int getMaxAssignedDmen()
    {
        return getBuildingLevel() * 2;
    }

    /**
     * Check the registered deliverymen and see if one of their huts got destroyed.
     */
    private void checkForRegisteredDeliverymen()
    {
        for (final Vec3 pos : new ArrayList<>(registeredDeliverymen))
        {
            final IColony colony = getColony();
            final IBuilding building = colony.getBuildingManager().getBuilding(new BlockPos(pos));
            if (!(building instanceof BuildingDeliveryman) || !building.hasAssignedCitizen())
            {
                registeredDeliverymen.remove(pos);
            }
        }
    }

    /**
     * Check if deliveryman is allowed to access warehouse.
     *
     * @param buildingWorker the building of the deliveryman.
     * @return true if able to.
     */
    @Override
    public boolean canAccessWareHouse(final IBuildingDeliveryman buildingWorker)
    {
        return registeredDeliverymen.contains(new Vec3(buildingWorker.getID().getX(), buildingWorker.getID().getY(), buildingWorker.getID().getZ()));
    }

    /**
     * Get the deliverymen connected with this building.
     *
     * @return the unmodifiable List of positions of them.
     */
    @Override
    public Set<Vec3> getRegisteredDeliverymen()
    {
        return new HashSet<>(Collections.unmodifiableSet(registeredDeliverymen));
    }

    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        super.deserializeNBT(compound);

        registeredDeliverymen.clear();

        final ListTag deliverymanTagList = compound.getList(TAG_DELIVERYMAN, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < deliverymanTagList.size(); i++)
        {
            final BlockPos pos = NbtUtils.readBlockPos(deliverymanTagList.getCompound(i));
            if (getColony() != null && getColony().getBuildingManager().getBuilding(pos) instanceof AbstractBuildingWorker)
            {
                registeredDeliverymen.add(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
            }
        }
    }

    @Override
    public CompoundTag serializeNBT()
    {
        final CompoundTag compound = super.serializeNBT();
        @NotNull final ListTag levelTagList = new ListTag();
        for (@NotNull final Vec3 deliverymanBuilding : registeredDeliverymen)
        {
            levelTagList.add(NbtUtils.writeBlockPos(new BlockPos(deliverymanBuilding)));
        }
        compound.put(TAG_DELIVERYMAN, levelTagList);
        return compound;
    }

    @NotNull
    @Override
    public String getSchematicName()
    {
        return WAREHOUSE;
    }

    /**
     * Returns the tile entity that belongs to the colony building.
     *
     * @return {@link TileEntityColonyBuilding} object of the building.
     */
    @Override
    public AbstractTileEntityWareHouse getTileEntity()
    {
        final AbstractTileEntityColonyBuilding entity = super.getTileEntity();
        return !(entity instanceof TileEntityWareHouse) ? null : (AbstractTileEntityWareHouse) entity;
    }

    @Override
    public boolean hasContainerPosition(final BlockPos inDimensionLocation)
    {
        return containerList.contains(inDimensionLocation) || getLocation().getInDimensionLocation().equals(inDimensionLocation);
    }

    @Override
    public int getMaxBuildingLevel()
    {
        return MAX_LEVEL;
    }

    @Override
    public void registerBlockPosition(@NotNull final Block block, @NotNull final BlockPos pos, @NotNull final Level world)
    {
        if (block instanceof BlockMinecoloniesRack)
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof ChestBlockEntity)
            {
                handleBuildingOverChest(pos, (ChestBlockEntity) entity, world, null);
            }
            if (entity instanceof TileEntityRack)
            {
                ((AbstractTileEntityRack) entity).setInWarehouse(true);
            }
        }
        super.registerBlockPosition(block, pos, world);
    }

    /**
     * Handles the chest placement.
     *
     * @param pos            at pos.
     * @param chest          the entity.
     * @param world          the world.
     * @param tileEntityData the rack te data.
     */
    public static void handleBuildingOverChest(@NotNull final BlockPos pos, final ChestBlockEntity chest, final Level world, @Nullable final CompoundTag tileEntityData)
    {
        final List<ItemStack> inventory = new ArrayList<>();
        final int size = chest.getContainerSize();
        for (int slot = 0; slot < size; slot++)
        {
            final ItemStack stack = chest.getItem(slot);
            if (!ItemStackUtils.isEmpty(stack))
            {
                inventory.add(stack.copy());
            }
            chest.removeItemNoUpdate(slot);
        }

        world.setBlock(pos, ModBlocks.blockRack.defaultBlockState(), 0x03);
        if (tileEntityData != null)
        {
            handleTileEntityPlacement(tileEntityData, world, pos);
        }
        final BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof TileEntityRack)
        {
            ((AbstractTileEntityRack) entity).setInWarehouse(true);
            for (final ItemStack stack : inventory)
            {
                if (!ItemStackUtils.isEmpty(stack))
                {
                    InventoryUtils.addItemStackToItemHandler(((AbstractTileEntityRack) entity).getInventory(), stack);
                }
            }
        }
    }

    @Override
    public ImmutableCollection<IRequestResolver<?>> createResolvers()
    {
        final ImmutableCollection<IRequestResolver<?>> supers = super.createResolvers();
        final ImmutableList.Builder<IRequestResolver<?>> builder = ImmutableList.builder();

        builder.addAll(supers);
        builder.add(new WarehouseRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)),
          new WarehouseConcreteRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN))
          );

        builder.add(new DeliveryRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)));
        builder.add(new PickupRequestResolver(getRequester().getLocation(),
          getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN)));

        return builder.build();
    }

    @Override
    public BuildingEntry getBuildingRegistryEntry()
    {
        return ModBuildings.wareHouse;
    }

    /**
     * Upgrade all containers by 9 slots.
     *
     * @param world the world object.
     */
    @Override
    public void upgradeContainers(final Level world)
    {
        if (getFirstModuleOccurance(WarehouseModule.class).getStorageUpgrade() < MAX_STORAGE_UPGRADE)
        {
            for (final BlockPos pos : getContainers())
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack && !(entity instanceof TileEntityColonyBuilding))
                {
                    ((AbstractTileEntityRack) entity).upgradeItemStorage();
                }
            }
            getFirstModuleOccurance(WarehouseModule.class).incrementStorageUpgrade();
        }
        markDirty();
    }

    @Override
    public boolean canBeGathered()
    {
        return false;
    }

    /**
     * BuildWarehouse View.
     */
    public static class View extends AbstractBuildingView
    {
        /**
         * Instantiate the warehouse view.
         *
         * @param c the colonyview to put it in
         * @param l the positon
         */
        public View(final IColonyView c, final BlockPos l)
        {
            super(c, l);
        }

        @NotNull
        @Override
        public BOWindow getWindow()
        {
            return new WindowHutMinPlaceholder<>(this, WAREHOUSE);
        }
    }
}
