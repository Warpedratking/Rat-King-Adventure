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

package com.zrp200.rkpd2.actors.buffs;

import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.sprites.CharSprite;
import com.zrp200.rkpd2.ui.BuffIndicator;

public class Healing extends Buff {

	private int healingLeft;
	
	private float percentHealPerTick;
	private int flatHealPerTick;
	
	{
		//unlike other buffs, this one acts after the hero and takes priority against other effects
		//healing is much more useful if you get some of it off before taking damage
		actPriority = HERO_PRIO - 1;
		
		type = buffType.POSITIVE;
	}
	
	@Override
	public boolean act(){
		
		target.HP = Math.min(target.HT, target.HP + healingThisTick());

		if (target.HP == target.HT && target instanceof Hero){
			((Hero)target).resting = false;
		}

		healingLeft -= healingThisTick();
		
		if (healingLeft <= 0){
			detach();
		}
		
		spend( TICK );
		
		return true;
	}
	
	private int healingThisTick(){
		return (int)GameMath.gate(1,
				Math.round(healingLeft * percentHealPerTick) + flatHealPerTick,
				healingLeft);
	}
	
	public void setHeal(int amount, float percentPerTick, int flatPerTick){
		healingLeft = amount;
		percentHealPerTick = percentPerTick;
		flatHealPerTick = flatPerTick;
	}
	
	public void increaseHeal( int amount ){
		healingLeft += amount;
	}
	
	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add( CharSprite.State.HEALING );
		else    target.sprite.remove( CharSprite.State.HEALING );
	}
	
	private static final String LEFT = "left";
	private static final String PERCENT = "percent";
	private static final String FLAT = "flat";
	
	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(LEFT, healingLeft);
		bundle.put(PERCENT, percentHealPerTick);
		bundle.put(FLAT, flatHealPerTick);
	}
	
	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		healingLeft = bundle.getInt(LEFT);
		percentHealPerTick = bundle.getFloat(PERCENT);
		flatHealPerTick = bundle.getInt(FLAT);
	}
	
	@Override
	public int icon() {
		return BuffIndicator.HEALING;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(healingLeft);
	}
	
	@Override
	public String toString() {
		return Messages.get(this, "name");
	}
	
	@Override
	public String desc() {
		return Messages.get(this, "desc", healingThisTick(), healingLeft);
	}
}
