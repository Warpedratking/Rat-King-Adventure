/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2022 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zrp200.rkpd2.items.stones;

import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.buffs.Invisibility;
import com.zrp200.rkpd2.items.Item;
import com.zrp200.rkpd2.sprites.ItemSpriteSheet;

public abstract class Runestone extends Item {
	
	{
		stackable = true;
		defaultAction = AC_THROW;
	}

	//runestones press the cell they're thrown to by default, but a couple stones override this
	protected boolean pressesCell = true;

	@Override
	protected void onThrow(int cell) {
		if (Dungeon.level.pit[cell] || !getDefaultAction().equals(AC_THROW)){
			super.onThrow( cell );
		} else {
			if (pressesCell) Dungeon.level.pressCell( cell );
			activate(cell);
			Invisibility.dispel();
		}
	}
	
	protected abstract void activate(int cell);
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	@Override
	public boolean isIdentified() {
		return true;
	}
	
	@Override
	public int value() {
		return 15 * quantity;
	}

	@Override
	public int energyVal() {
		return 3 * quantity;
	}

	public static class PlaceHolder extends Runestone {
		
		{
			image = ItemSpriteSheet.STONE_HOLDER;
		}
		
		@Override
		protected void activate(int cell) {
			//does nothing
		}
		
		@Override
		public boolean isSimilar(Item item) {
			return item instanceof Runestone;
		}
		
		@Override
		public String info() {
			return "";
		}
	}
}
