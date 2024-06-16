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

package appeng.parts.reporting;

import net.minecraft.resources.ResourceLocation;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;

public class PanelPart extends AbstractPanelPart {

    @PartModels
    public static final ResourceLocation MODEL_OFF = AppEng.makeId("part/monitor_bright_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = AppEng.makeId("part/monitor_bright_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);

    public PanelPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    protected int getBrightnessColor() {
        return this.getColor().whiteVariant;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.isPowered() ? MODELS_ON : MODELS_OFF;
    }

}
