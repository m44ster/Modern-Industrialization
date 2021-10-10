package aztech.modern_industrialization.machines.blockentities.multiblocks;

import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.components.LubricantHelper;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.ShapeMatcher;
import aztech.modern_industrialization.machines.multiblocks.ShapeTemplate;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import aztech.modern_industrialization.util.Simulation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class FusionReactorBlockEntity extends AbstractCraftingMultiblockBlockEntity {

    public FusionReactorBlockEntity(BEP bep, String name, ShapeTemplate shapeTemplate) {
        super(bep, name, new OrientationComponent(new OrientationComponent.Params(false, false, false)), new ShapeTemplate[]{shapeTemplate});
    }

    @Override
    protected CrafterComponent.Behavior getBehavior() {
        return new Behavior();
    }

    private final List<EnergyComponent> energyInputs = new ArrayList<>();
    @Override
    protected void onSuccessfulMatch(ShapeMatcher shapeMatcher) {
        energyInputs.clear();
        for (HatchBlockEntity hatch : shapeMatcher.getMatchedHatches()) {
            hatch.appendEnergyInputs(energyInputs);
        }
    }

    protected ActionResult onUse(PlayerEntity player, Hand hand, Direction face) {
        ActionResult result = super.onUse(player, hand, face);
        if (!result.isAccepted()) {
            result = LubricantHelper.onUse(this.crafter, player, hand);
        }
        return result;
    }


    private class Behavior implements CrafterComponent.Behavior {
        @Override
        public long consumeEu(long max, Simulation simulation) {
            long total = 0;

            for (EnergyComponent energyComponent : energyInputs) {
                total += energyComponent.consumeEu(max - total, simulation);
            }

            return total;
        }

        @Override
        public MachineRecipeType recipeType() {
            return MIMachineRecipeTypes.FUSION_REACTOR;
        }

        @Override
        public long getBaseRecipeEu() {
            return 128000;
        }

        @Override
        public long getMaxRecipeEu() {
            return Integer.MAX_VALUE;
        }

        @Override
        public World getCrafterWorld() {
            return world;
        }
    }

}