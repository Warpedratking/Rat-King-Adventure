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

package com.zrp200.rkpd2.plants;

import com.watabou.utils.Bundle;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.buffs.Buff;
import com.zrp200.rkpd2.actors.buffs.Healing;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.effects.CellEmitter;
import com.zrp200.rkpd2.effects.Speck;
import com.zrp200.rkpd2.effects.particles.ShaftParticle;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.sprites.ItemSpriteSheet;
import com.zrp200.rkpd2.ui.BuffIndicator;

public class Sungrass extends Plant {
	
	{
		image = 3;
		seedClass = Seed.class;
	}

	@Override
	public void affectHero(Char ch, boolean isWarden) {
		if (isWarden){
			Buff.affect(ch, Healing.class).setHeal(ch.HT, 0, 1);
		} else {
			Buff.affect(ch, Health.class).boost(ch.HT);
		}
	}

	@Override
	public void activateMisc(Char ch) {
		if (Dungeon.level.heroFOV[pos]) {
			CellEmitter.get( pos ).start( ShaftParticle.FACTORY, 0.2f, 3 );
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SUNGRASS;

			plantClass = Sungrass.class;

			bones = true;
		}
	}
	
	public static class Health extends Buff {
		
		private static final float STEP = 1f;
		
		private int pos;
		private float partialHeal;
		private int level;

		{
			type = buffType.POSITIVE;
			announced = true;
		}
		
		@Override
		public boolean act() {
			if (target.pos != pos) {
				detach();
			}
			
			//for the hero, full heal takes ~50/93/111/120 turns at levels 1/10/20/30
			partialHeal += (40 + target.HT)/150f;
			
			if (partialHeal > 1){
				target.HP += (int)partialHeal;
				level -= (int)partialHeal;
				partialHeal -= (int)partialHeal;
				target.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
				
				if (target.HP >= target.HT) {
					target.HP = target.HT;
					if (target instanceof Hero){
						((Hero)target).resting = false;
					}
				}
			}
			
			if (level <= 0) {
				detach();
				if (target instanceof Hero){
					((Hero)target).resting = false;
				}
			}
			spend( STEP );
			return true;
		}

		public void boost( int amount ){
			if (target != null) {
				level += amount;
				pos = target.pos;
			}
		}
		
		@Override
		public int icon() {
			return BuffIndicator.HERB_HEALING;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (target.HT - level) / (float)target.HT);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(level);
		}
		
		@Override
		public String toString() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", level);
		}

		private static final String POS	= "pos";
		private static final String PARTIAL = "partial_heal";
		private static final String LEVEL = "level";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( POS, pos );
			bundle.put( PARTIAL, partialHeal);
			bundle.put( LEVEL, level);
		}
		
		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			pos = bundle.getInt( POS );
			partialHeal = bundle.getFloat( PARTIAL );
			level = bundle.getInt( LEVEL );

		}
	}
}
