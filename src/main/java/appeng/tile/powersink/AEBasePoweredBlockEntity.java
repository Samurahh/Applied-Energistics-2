/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.tile.powersink;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import team.reborn.energy.EnergySide;
import team.reborn.energy.EnergyStorage;
import team.reborn.energy.EnergyTier;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.events.MENetworkPowerStorage.PowerEventType;
import appeng.core.AEConfig;
import appeng.tile.AEBaseInvBlockEntity;

public abstract class AEBasePoweredBlockEntity extends AEBaseInvBlockEntity
        implements IAEPowerStorage, IExternalPowerSink, EnergyStorage {

    // values that determine general function, are set by inheriting classes if
    // needed. These should generally remain static.
    private double internalMaxPower = 10000;
    private boolean internalPublicPowerStorage = false;
    private AccessRestriction internalPowerFlow = AccessRestriction.READ_WRITE;
    // the current power buffer.
    private double internalCurrentPower = 0;
    private static final Set<Direction> ALL_SIDES = ImmutableSet.copyOf(EnumSet.allOf(Direction.class));
    private Set<Direction> internalPowerSides = ALL_SIDES;

    // IC2 private IC2PowerSink ic2Sink;

    public AEBasePoweredBlockEntity(BlockEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
        // IC2 this.ic2Sink = Integrations.ic2().createPowerSink( this, this );
        // IC2 this.ic2Sink.setValidFaces( this.internalPowerSides );
    }

    protected final Set<Direction> getPowerSides() {
        return this.internalPowerSides;
    }

    protected void setPowerSides(final Set<Direction> sides) {
        this.internalPowerSides = ImmutableSet.copyOf(sides);
        // IC2 this.ic2Sink.setValidFaces( sides );
        // trigger re-calc!
    }

    @Override
    public CompoundTag toTag(final CompoundTag data) {
        super.toTag(data);
        data.putDouble("internalCurrentPower", this.getInternalCurrentPower());
        return data;
    }

    @Override
    public void fromTag(BlockState state, final CompoundTag data) {
        super.fromTag(state, data);
        this.setInternalCurrentPower(data.getDouble("internalCurrentPower"));
    }

    @Override
    public final double getExternalPowerDemand(final PowerUnits externalUnit, final double maxPowerRequired) {
        return PowerUnits.AE.convertTo(externalUnit,
                Math.max(0.0, this.getFunnelPowerDemand(externalUnit.convertTo(PowerUnits.AE, maxPowerRequired))));
    }

    protected double getFunnelPowerDemand(final double maxRequired) {
        return this.getInternalMaxPower() - this.getInternalCurrentPower();
    }

    @Override
    public final double injectExternalPower(final PowerUnits input, final double amt, Actionable mode) {
        return PowerUnits.AE.convertTo(input, this.funnelPowerIntoStorage(input.convertTo(PowerUnits.AE, amt), mode));
    }

    protected double funnelPowerIntoStorage(final double power, final Actionable mode) {
        return this.injectAEPower(power, mode);
    }

    @Override
    public final double injectAEPower(double amt, final Actionable mode) {
        if (amt < 0.000001) {
            return 0;
        }

        final double required = this.getAEMaxPower() - this.getAECurrentPower();
        final double insertable = Math.min(required, amt);

        if (mode == Actionable.MODULATE) {
            if (this.getInternalCurrentPower() < 0.01 && insertable > 0.01) {
                this.PowerEvent(PowerEventType.PROVIDE_POWER);
            }

            this.setInternalCurrentPower(this.getInternalCurrentPower() + insertable);
        }

        return amt - insertable;
    }

    protected void PowerEvent(final PowerEventType x) {
        // nothing.
    }

    @Override
    public final double getAEMaxPower() {
        return this.getInternalMaxPower();
    }

    @Override
    public final double getAECurrentPower() {
        return this.getInternalCurrentPower();
    }

    @Override
    public final boolean isAEPublicPowerStorage() {
        return this.isInternalPublicPowerStorage();
    }

    @Override
    public final AccessRestriction getPowerFlow() {
        return this.getInternalPowerFlow();
    }

    @Override
    public final double extractAEPower(final double amt, final Actionable mode, final PowerMultiplier multiplier) {
        return multiplier.divide(this.extractAEPower(multiplier.multiply(amt), mode));
    }

    protected double extractAEPower(double amt, final Actionable mode) {
        if (mode == Actionable.SIMULATE) {
            if (this.getInternalCurrentPower() > amt) {
                return amt;
            }
            return this.getInternalCurrentPower();
        }

        final boolean wasFull = this.getInternalCurrentPower() >= this.getInternalMaxPower() - 0.001;
        if (wasFull && amt > 0.001) {
            this.PowerEvent(PowerEventType.REQUEST_POWER);
        }

        if (this.getInternalCurrentPower() > amt) {
            this.setInternalCurrentPower(this.getInternalCurrentPower() - amt);
            return amt;
        }

        amt = this.getInternalCurrentPower();
        this.setInternalCurrentPower(0);
        return amt;
    }

    public double getInternalCurrentPower() {
        return this.internalCurrentPower;
    }

    public void setInternalCurrentPower(final double internalCurrentPower) {
        this.internalCurrentPower = internalCurrentPower;
    }

    public double getInternalMaxPower() {
        return this.internalMaxPower;
    }

    public void setInternalMaxPower(final double internalMaxPower) {
        this.internalMaxPower = internalMaxPower;
    }

    private boolean isInternalPublicPowerStorage() {
        return this.internalPublicPowerStorage;
    }

    public void setInternalPublicPowerStorage(final boolean internalPublicPowerStorage) {
        this.internalPublicPowerStorage = internalPublicPowerStorage;
    }

    private AccessRestriction getInternalPowerFlow() {
        return this.internalPowerFlow;
    }

    public void setInternalPowerFlow(final AccessRestriction internalPowerFlow) {
        this.internalPowerFlow = internalPowerFlow;
    }

    @Override
    public void onReady() {
        super.onReady();

        // IC2 this.ic2Sink.onLoad();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();

        // IC2 this.ic2Sink.onChunkUnloaded();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();

        // IC2 this.ic2Sink.invalidate();
    }

    @Override
    public double getMaxInput(EnergySide side) {
        double attemptedInsert = AEConfig.instance().getPowerTransactionLimitTechReborn();
        double overflow = this.injectExternalPower(PowerUnits.TR, attemptedInsert, Actionable.SIMULATE);
        double couldInsert = attemptedInsert - overflow;
        if (couldInsert < 0.001) {
            return 0;
        }

        return PowerUnits.AE.convertTo(PowerUnits.TR, couldInsert);
    }

    @Override
    public double getStored(EnergySide energySide) {
        // This block acts as if it cannot actually store any energy
        return 0.0;
    }

    @Override
    public void setStored(double v) {
        v = MathHelper.clamp(v, 0.001, getMaxStoredPower());
        this.injectExternalPower(PowerUnits.TR, v, Actionable.MODULATE);
    }

    @Override
    public double getMaxStoredPower() {
        return AEConfig.instance().getPowerTransactionLimitTechReborn();
    }

    @Override
    public EnergyTier getTier() {
        return EnergyTier.INFINITE;
    }

}
