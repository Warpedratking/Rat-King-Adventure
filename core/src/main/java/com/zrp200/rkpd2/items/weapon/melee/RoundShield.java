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

package com.zrp200.rkpd2.items.weapon.melee;

import com.watabou.noosa.Image;
import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.buffs.Buff;
import com.zrp200.rkpd2.actors.buffs.DummyBuff;
import com.zrp200.rkpd2.items.wands.WandOfBlastWave;
import com.zrp200.rkpd2.mechanics.Ballistica;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.sprites.ItemSpriteSheet;
import com.zrp200.rkpd2.ui.BuffIndicator;

public class RoundShield extends MeleeWeapon {

	{
		image = ItemSpriteSheet.ROUND_SHIELD;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		tier = 3;
	}

	@Override
	public int max(int lvl) {
		return  Math.round(2.5f*(tier+1)) +     //10 base, down from 20
				lvl*(tier-1);                   //+2 per level, down from +4
	}


	@Override
	public int defenseFactor( Char owner ) {
		return 4+2*buffedLvl();     //4 extra defence, plus 2 per level;
	}
	
	public String statsInfo(){
		if (isIdentified()){
			return Messages.get(this, "stats_desc", 4+2*buffedLvl());
		} else {
			return Messages.get(this, "typical_stats_desc", 4);
		}
	}

	@Override
	public int warriorAttack(int damage, Char enemy) {
		if (Dungeon.hero.lastMovPos != -1 &&
				Dungeon.level.distance(Dungeon.hero.lastMovPos, enemy.pos) >
						Dungeon.level.distance(Dungeon.hero.pos, enemy.pos) && Dungeon.hero.buff(Block.class) == null){
			Dungeon.hero.lastMovPos = -1;
			//knock out target and get blocking
			Ballistica trajectory = new Ballistica(Dungeon.hero.pos, enemy.pos, Ballistica.STOP_TARGET);
			trajectory = new Ballistica(trajectory.collisionPos, trajectory.path.get(trajectory.path.size()-1), Ballistica.PROJECTILE);
			WandOfBlastWave.throwChar(enemy, trajectory, 1, true, true, getClass());
			Buff.affect(Dungeon.hero, Block.class, 8f);
		}
		return 0;
	}

	public static class Block extends DummyBuff {
		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0x94BCCC);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (8 - visualcooldown()) / 8);
		}
	}
}