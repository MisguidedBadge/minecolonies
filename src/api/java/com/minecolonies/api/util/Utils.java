package com.minecolonies.api.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * General purpose utilities class. todo: split up into logically distinct parts
 */
public final class Utils
{
    private static final NavigableMap<Long, String> suffixes = new TreeMap<> ();
    
    static
    {
    suffixes.put(1_000L, "k");
    suffixes.put(1_000_000L, "M");
    suffixes.put(1_000_000_000L, "G");
    suffixes.put(1_000_000_000_000L, "T");
    suffixes.put(1_000_000_000_000_000L, "P");
    suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    /**
     * Private constructor to hide the implicit public one.
     */
    private Utils()
    {
    }

    /**
     * Find the closest block near the points.
     *
     * @param world   the world.
     * @param point   the point where to search.
     * @param radiusX x search distance.
     * @param radiusY y search distance.
     * @param radiusZ z search distance.
     * @param height  check if blocks above the found block are air or block.
     * @param blocks  Blocks to test for.
     * @return the coordinates of the found block.
     */
    @Nullable
    public static BlockPos scanForBlockNearPoint(
      @NotNull final World world,
      @NotNull final BlockPos point,
      final int radiusX,
      final int radiusY,
      final int radiusZ,
      final int height,
      final Block... blocks)
    {
        @Nullable BlockPos closestCoords = null;
        double minDistance = Double.MAX_VALUE;

        for (int j = point.getY(); j <= point.getY() + radiusY; j++)
        {
            for (int i = point.getX() - radiusX; i <= point.getX() + radiusX; i++)
            {
                for (int k = point.getZ() - radiusZ; k <= point.getZ() + radiusZ; k++)
                {
                    if (checkHeight(world, i, j, k, height, blocks))
                    {
                        @NotNull final BlockPos tempCoords = new BlockPos(i, j, k);

                        if (world.getBlockState(tempCoords.below()).getMaterial().isSolid() || world.getBlockState(tempCoords.below(2)).getMaterial().isSolid())
                        {
                            final double distance = BlockPosUtil.getDistanceSquared(tempCoords, point);
                            if (closestCoords == null || distance < minDistance)
                            {
                                closestCoords = tempCoords;
                                minDistance = distance;
                            }
                        }
                    }
                }
            }
        }
        return closestCoords;
    }

    /**
     * Checks if the blocks above that point are all of the spezified block types.
     *
     * @param world  the world we check on.
     * @param x      the x coordinate.
     * @param y      the y coordinate.
     * @param z      the z coordinate.
     * @param height the number of blocks above to check.
     * @param blocks the block types required.
     * @return true if all blocks are of that type.
     */
    private static boolean checkHeight(@NotNull final World world, final int x, final int y, final int z, final int height, @NotNull final Block... blocks)
    {
        for (int dy = 0; dy < height; dy++)
        {
            final BlockState state = world.getBlockState(new BlockPos(x, y + dy, z));
            if (!arrayContains(blocks, state.getBlock()) && state.getMaterial().blocksMotion())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether or not the array contains the object given.
     *
     * @param array Array to scan.
     * @param key   Object to look for.
     * @return True if found, otherwise false.
     */
    private static boolean arrayContains(@NotNull final Object[] array, final Object key)
    {
        for (final Object o : array)
        {
            if (Objects.equals(key, o))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches a block in a custom range.
     *
     * @param world World instance.
     * @param block searched Block.
     * @param posX  X-coordinate.
     * @param posY  Y-coordinate.
     * @param posZ  Z-coordinate.
     * @param range the range to check around the point.
     * @return true if he found the block.
     */
    public static boolean isBlockInRange(@NotNull final World world, final Block block, final int posX, final int posY, final int posZ, final int range)
    {
        for (int x = posX - range; x < posX + range; x++)
        {
            for (int z = posZ - range; z < posZ + range; z++)
            {
                for (int y = posY - range; y < posY + range; y++)
                {
                    if (Objects.equals(world.getBlockState(new BlockPos(x, y, z)).getBlock(), block))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the flag is set in the data. E.G. - Flag: 000101. - Data: 100101. - All Flags are set in data, so returns true. Some more flags are set, but not take into account
     *
     * @param data Data to check flag in.
     * @param flag Flag to check whether it is set or not.
     * @return True if flag is set, otherwise false.
     */
    public static boolean testFlag(final long data, final long flag)
    {
        return mask(data, flag) == flag;
    }

    /**
     * Returns what flags are set, and given in mask. E.G. - Flag: 000101. - Mask: 100101. - The 4th and 6th bit are set, so only those will be returned.
     *
     * @param data Data to check.
     * @param mask Mask to check.
     * @return Byte in which both data bits and mask bits are set.
     */
    public static long mask(final long data, final long mask)
    {
        return data & mask;
    }

    /**
     * Sets a flag in in the data. E.G. - Flag: 000101 - Mask: 100001 - The 4th bit will now be set, both the 1st and 6th bit are maintained.
     *
     * @param data Data to set flag in.
     * @param flag Flag to set.
     * @return Data with flags set.
     */
    public static long setFlag(final long data, final long flag)
    {
        return data | flag;
    }

    /**
     * Unsets a flag. E.G. - Flag: 000101 - Mask: 100101 - The 4th and 6th bit will be unset, the 1st bit is maintained.
     *
     * @param data Data to remove flag from.
     * @param flag Flag to remove.
     * @return Data with flag unset.
     */
    public static long unsetFlag(final long data, final long flag)
    {
        return data & ~flag;
    }

    /**
     * Toggles flags. E.G. - Flag: 000101 - Mask: 100101 - The 4th and 6th will be toggled, the 1st bit is maintained.
     *
     * @param data Data to toggle flag in.
     * @param flag Flag to toggle.
     * @return Data with flag toggled.
     */
    public static long toggleFlag(final long data, final long flag)
    {
        return data ^ flag;
    }

    /**
     * Checks if directory exists, else creates it.
     *
     * @param directory the directory to check.
     */
    public static void checkDirectory(@NotNull final File directory)
    {
        if (!directory.exists() && !directory.mkdirs())
        {
            Log.getLogger().error("Directory doesn't exist and failed to be created: " + directory.toString());
        }
    }
    
    /**
     * Formats a long value into a abbreviated string, ie: 1000 to 1k, 1200 to 1.2k, 13000 to 13k
     * @param value to format
     * @return string version of the value
     */
    public static String format(long value)
    {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }
}
