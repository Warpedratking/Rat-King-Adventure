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

import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.HeroSubClass;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.items.artifacts.CloakOfShadows;
import com.zrp200.rkpd2.items.artifacts.KromerCloak;
import com.zrp200.rkpd2.items.artifacts.TimekeepersHourglass;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.sprites.CharSprite;
import com.zrp200.rkpd2.ui.BuffIndicator;
import com.zrp200.rkpd2.utils.SafeCast;

public class Invisibility extends FlavourBuff {

	public static final float DURATION	= 20f;

	{
		type = buffType.POSITIVE;
		announced = true;
	}
	
	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			target.invisible++;
			Hero hero = SafeCast.cast(target, Hero.class);
			if(hero != null) {
				if(hero.subClass == HeroSubClass.ASSASSIN
						|| hero.subClass == HeroSubClass.KING) {
					Buff.affect(target, Preparation.class);
				}
				if(hero.hasTalent(Talent.MENDING_SHADOWS, Talent.NOBLE_CAUSE)) {
					 Buff.affect(target, Talent.ProtectiveShadowsTracker.class);
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void detach() {
		if (target.invisible > 0)
			target.invisible--;
		super.detach();
	}
	
	@Override
	public int icon() {
		return BuffIndicator.INVISIBLE;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add( CharSprite.State.INVISIBLE );
		else if (target.invisible == 0) target.sprite.remove( CharSprite.State.INVISIBLE );
	}

	@Override
	public String toString() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}

	public static void dispel() {
		if (Dungeon.hero.pointsInTalent(Talent.BOUNTY_HUNTER) == 3){
			if (Dungeon.hero.buff(DispelDelayer.class) == null && Dungeon.hero.invisible > 0){
				Buff.affect(Dungeon.hero, DispelDelayer.class, 1f);
				Preparation preparation = Dungeon.hero.buff(Preparation.class);
				if (preparation != null){
					preparation.detach();
					if (preparation.attackLevel() > 1) {
						Preparation newPreparation = Buff.affect(Dungeon.hero, Preparation.class);
						while (newPreparation.attackLevel() != preparation.attackLevel() - 1) {
							newPreparation.turnsInvis++;
						}
					}
				}
			} else {
				actualDispel();
			}
		}
		else {
			actualDispel();
		}
	}

	public static void actualDispel() {
		for ( Buff invis : Dungeon.hero.buffs( Invisibility.class )){
			invis.detach();
		}
		CloakOfShadows.cloakStealth cloakBuff = Dungeon.hero.buff( CloakOfShadows.cloakStealth.class );
		if (cloakBuff != null) {
			cloakBuff.dispel();
		}
		KromerCloak.cloakStealth kromerBuff = Dungeon.hero.buff(KromerCloak.cloakStealth.class );
		if (kromerBuff != null) {
			kromerBuff.dispel();
		}

		//these aren't forms of invisibilty, but do dispel at the same time as it.
		TimekeepersHourglass.TimeFreezing timeFreeze = Dungeon.hero.buff( TimekeepersHourglass.TimeFreezing.class );
		if (timeFreeze != null) {
			timeFreeze.detach();
		}

		Preparation prep = Dungeon.hero.buff( Preparation.class );
		if (prep != null){
			prep.detach();
		}
	}

	public static class DispelDelayer extends FlavourBuff {

		{
			actPriority = BUFF_PRIO + 1;
		}

		@Override
		public boolean act() {
			actualDispel();
			detach();
			return true;
		}

		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.SPIRIT);
			else target.sprite.remove(CharSprite.State.SPIRIT);
		}
	}
}
