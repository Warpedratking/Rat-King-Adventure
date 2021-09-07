/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2021 Evan Debenham
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

import com.watabou.noosa.audio.Sample;
import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.actors.buffs.Invisibility;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.items.Item;
import com.zrp200.rkpd2.items.bags.Bag;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.windows.WndBag;

import java.util.ArrayList;

public abstract class InventoryStone extends Runestone {

	{
		defaultAction = AC_USE;
	}
	
	public static final String AC_USE	= "USE";
	
	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_USE );
		return actions;
	}
	
	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_USE)){
			curItem = detach( hero.belongings.backpack );
			activate(curUser.pos);
		}
	}
	
	@Override
	protected void activate(int cell) {
		GameScene.selectItem( itemSelector );
	}
	
	protected void useAnimation() {
		curUser.spend( 1f );
		curUser.busy();
		curUser.sprite.operate(curUser.pos);
		
		Sample.INSTANCE.play( Assets.Sounds.READ );
		Invisibility.dispel();
	}

	private String inventoryTitle(){
		return Messages.get(this, "inv_title");
	}

	protected Class<?extends Bag> preferredBag = null;

	protected boolean usableOnItem( Item item ){
		return true;
	}

	protected abstract void onItemSelected( Item item );
	
	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return inventoryTitle();
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return preferredBag;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return usableOnItem(item);
		}

		@Override
		public void onSelect( Item item ) {
			
			//FIXME this safety check shouldn't be necessary
			//it would be better to eliminate the curItem static variable.
			if (!(curItem instanceof InventoryStone)){
				return;
			}
			
			if (item != null) {
				
				((InventoryStone)curItem).onItemSelected( item );
				
			} else{
				curItem.collect( curUser.belongings.backpack );
			}
		}
	};
	
}
