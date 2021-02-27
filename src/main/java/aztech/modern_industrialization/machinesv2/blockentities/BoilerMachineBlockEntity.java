package aztech.modern_industrialization.machinesv2.blockentities;

import aztech.modern_industrialization.MIFluids;
import aztech.modern_industrialization.blocks.tank.MITanks;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machinesv2.MachineBlockEntity;
import aztech.modern_industrialization.machinesv2.components.OrientationComponent;
import aztech.modern_industrialization.machinesv2.components.sync.ProgressBar;
import aztech.modern_industrialization.machinesv2.components.sync.TemperatureBar;
import aztech.modern_industrialization.machinesv2.gui.MachineGuiParameters;
import aztech.modern_industrialization.machinesv2.models.MachineCasings;
import aztech.modern_industrialization.machinesv2.models.MachineModelClientData;
import aztech.modern_industrialization.util.ItemStackHelper;
import aztech.modern_industrialization.util.RenderHelper;
import net.fabricmc.fabric.impl.content.registry.FuelRegistryImpl;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.List;

public class BoilerMachineBlockEntity extends MachineBlockEntity implements Tickable {

    private static final int BURN_TIME_MULTIPLIER = 5;

    public static final int WATER_SLOT_X = 50;
    public static final int WATER_SLOT_Y = 32;

    public static final int INPUT_SLOT_X = 15;
    public static final int INPUT_SLOT_Y = 32;

    public static final int OUTPUT_SLOT_X = 134;
    public static final int OUTPUT_SLOT_Y = 32;

    private final MIInventory inventory;
    private final boolean bronze;

    private final int temperatureMax;
    protected int burningTick, burningTickProgress, temperature;

    protected final OrientationComponent orientation;
    protected final ProgressBar.Parameters PROGRESS_BAR;
    protected final TemperatureBar.Parameters TEMPERATURE_BAR;

    protected boolean isActive = false;


    public BoilerMachineBlockEntity(BlockEntityType<?> type, boolean bronze) {
        super(type, new MachineGuiParameters.Builder(bronze ? "bronze_boiler" : "steel_boiler", true).backgroundHeight(180).build());
        orientation = new OrientationComponent(new OrientationComponent.Params(true, false, false));

        int capacity = 81000 * (bronze ? 2* MITanks.BRONZE.bucketCapacity : 2* MITanks.STEEL.bucketCapacity);

        List<ConfigurableItemStack> itemStacks = Arrays.asList(ConfigurableItemStack.standardInputSlot());
        SlotPositions itemPositions = new SlotPositions.Builder().addSlot(INPUT_SLOT_X, INPUT_SLOT_Y).build();

        List<ConfigurableFluidStack> fluidStacks = Arrays.asList(ConfigurableFluidStack.lockedInputSlot(capacity, Fluids.WATER),
                ConfigurableFluidStack.lockedOutputSlot(capacity, MIFluids.STEAM));
        SlotPositions fluidPositions = new SlotPositions.Builder().addSlot(WATER_SLOT_X, WATER_SLOT_Y).addSlot(OUTPUT_SLOT_X, OUTPUT_SLOT_Y).build();
        inventory = new MIInventory(itemStacks, fluidStacks, itemPositions, fluidPositions);

        this.bronze = bronze;
        this.burningTickProgress = 1;
        this.burningTick = 0;
        this.temperatureMax = bronze ? 1100 : 2100;
        PROGRESS_BAR = new ProgressBar.Parameters(133, 50, "furnace", true);
        TEMPERATURE_BAR = new TemperatureBar.Parameters(42, 75, temperatureMax);
        registerClientComponent(new ProgressBar.Server(PROGRESS_BAR, () -> (float) burningTick / burningTickProgress));
        registerClientComponent(new TemperatureBar.Server(TEMPERATURE_BAR, () -> temperature));


    }

    @Override
    public MIInventory getInventory() {
        return inventory;
    }

    @Override
    protected ActionResult onUse(PlayerEntity player, Hand hand, Direction face) {
        if (orientation.onUse(player, hand, face)) {
            markDirty();
            if (!world.isClient()) {
                sync();
            }
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    protected MachineModelClientData getModelData() {
        MachineModelClientData data = new MachineModelClientData(bronze ? MachineCasings.BRICKED_BRONZE : MachineCasings.BRICKED_STEEL);
        data.isActive = isActive;
        orientation.writeModelData(data);
        return data;
    }

    @Override
    public void onPlaced(LivingEntity placer, ItemStack itemStack) {
        orientation.onPlaced(placer, itemStack);
    }


    private void readTag(CompoundTag tag){
        isActive = tag.getBoolean("isActive");
        burningTick = tag.getInt("burningTick");
        burningTickProgress = tag.getInt("burningTickProgress");
        temperature = tag.getInt("temperature");
    }

    private void writeTag(CompoundTag tag){
        tag.putBoolean("isActive", isActive);
        tag.putInt("burningTick", burningTick);
        tag.putInt("burningTickProgress", burningTickProgress);
        tag.putInt("temperature", temperature);
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        orientation.readNbt(tag);
        isActive = tag.getBoolean("isActive");
        RenderHelper.forceChunkRemesh((ClientWorld) world, pos);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        orientation.writeNbt(tag);
        tag.putBoolean("isActive", isActive);
        return tag;
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        getInventory().writeNbt(tag);
        writeTag(tag);
        return tag;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        getInventory().readNbt(tag);
        orientation.readNbt(tag);
        readTag(tag);
    }

    @Override
    public void tick() {
        if (world.isClient)
            return;

        boolean wasActive = isActive;

        this.isActive = false;
        if (burningTick == 0) {
            ConfigurableItemStack stack = inventory.itemStacks.get(0);
            Item fuel = stack.getItemKey().getItem();
            if (ItemStackHelper.consumeFuel(stack, true)) {
                Integer fuelTime = FuelRegistryImpl.INSTANCE.get(fuel);
                if (fuelTime != null && fuelTime > 0) {
                    burningTickProgress = fuelTime * BURN_TIME_MULTIPLIER / (bronze ? 1 : 2);
                    burningTick = burningTickProgress;
                    ItemStackHelper.consumeFuel(stack, false);
                }
            }
        }

        if (burningTick > 0) {
            isActive = true;
            --burningTick;
        }

        if (isActive) {
            temperature = Math.min(temperature + 1, temperatureMax);
        } else {
            temperature = Math.max(temperature - 1, 0);
        }

        if (temperature > 100) {
            int steamProduction = 81 * ((4 * (temperature - 100)) / 1000);
            if (inventory.fluidStacks.get(0).getAmount() > 0) {
                long remSpace = inventory.fluidStacks.get(1).getRemainingSpace();
                long waterAvail = inventory.fluidStacks.get(0).getAmount();
                long actualProduced = Math.min(Math.min(steamProduction, remSpace), waterAvail);
                if (actualProduced > 0) {
                    inventory.fluidStacks.get(1).increment(actualProduced);
                    inventory.fluidStacks.get(0).decrement(actualProduced);
                }
            }
        }

        for (Direction direction : Direction.values()){
            getInventory().autoExtractFluids(world, pos, direction);
        }


        if (isActive != wasActive) {
            sync();
        }
        markDirty();
    }
}