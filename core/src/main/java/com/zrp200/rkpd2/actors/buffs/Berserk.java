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

import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.HeroSubClass;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.effects.SpellSprite;
import com.zrp200.rkpd2.items.BrokenSeal.WarriorShield;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.ui.BuffIndicator;
import com.zrp200.rkpd2.utils.GLog;

import java.text.DecimalFormat;

public class Berserk extends Buff {

	{
		type = buffType.POSITIVE;
	}

	private enum State{
		NORMAL, BERSERK, RECOVERING
	}
	private State state = State.NORMAL;

	private static final float LEVEL_RECOVER_START = 2f;
	private float levelRecovery;

	public int powerLossBuffer = 0;
	private float power = 0;

	private static final String STATE = "state";
	private static final String LEVEL_RECOVERY = "levelrecovery";
	private static final String POWER = "power";
	private static final String POWER_BUFFER = "power_buffer";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STATE, state);
		bundle.put(POWER, power);
		bundle.put(POWER_BUFFER, powerLossBuffer);
		if (state == State.RECOVERING) bundle.put(LEVEL_RECOVERY, levelRecovery);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);

		state = bundle.getEnum(STATE, State.class);
		power = bundle.getFloat(POWER);
		powerLossBuffer = bundle.getInt(POWER_BUFFER);
		if (state == State.RECOVERING) levelRecovery = bundle.getFloat(LEVEL_RECOVERY);
	}

	// this basically covers all of berserker's "buffed" talents.
	private boolean berserker() {
		return target instanceof Hero && ((Hero)target).isSubclassed(HeroSubClass.BERSERKER);
	}

	public static float STAMINA_REDUCTION = 1/3f;
	private static float levelRecoverStart() {
		return LEVEL_RECOVER_START - STAMINA_REDUCTION
				* Dungeon.hero.shiftedPoints(Talent.BERSERKING_STAMINA,Talent.RK_BERSERKER);
	}

	protected float maxBerserkDuration() {
		return 20;
	}
	@Override
	public boolean act() {
		if (berserking()){
			ShieldBuff buff = target.buff(WarriorShield.class);
			if (target.HP <= 0) {
					int dmg = 1 + (int) Math.ceil(target.shielding()
							* (0.05f - 0.0075*Dungeon.hero.pointsInTalent(Talent.BERSERKING_STAMINA)));
					if (buff != null && buff.shielding() > 0) {
						buff.absorbDamage(dmg);
					} else {
						//if there is no shield buff, or it is empty, then try to remove from other shielding buffs
						for (ShieldBuff s : target.buffs(ShieldBuff.class)) {
							dmg = s.absorbDamage(dmg);
							if (dmg == 0) break;
						}
					}
				if (target.shielding() <= 0) {
					target.die(this);
					if (!target.isAlive()) Dungeon.fail(this.getClass());
				}
			} else {
				state = State.RECOVERING;
				levelRecovery = levelRecoverStart();
				if (buff != null) buff.absorbDamage(buff.shielding());
				power = 0f;
			}
		} else {
			// essentially while recovering your max rage is actually capped for basically all purposes.
			if (powerLossBuffer > 0){
				powerLossBuffer--;
			} else {
				power -= GameMath.gate(recovered()/10f, power, recovered()) * (recovered() * 0.067f) * Math.pow((target.HP/(float)target.HT), 2);
			if (power <= 0 && state != State.RECOVERING) detach();
			else power = Math.max(0,power);
			}
		}
		spend(TICK);
		return true;
	}

	public float rageAmount(){
		if (berserker()){
			return Math.min(2f,1+power/0.9f);
		}
		return Math.min(1f, power);
	}

	public int damageFactor(int dmg){
		return Math.round(dmg * damageMult());
	}
	public float damageMult() {
		return Math.min(1.5f, 1f + (power / 2f));
	}

	public boolean berserking(){
		if (target.HP == 0 && state == State.NORMAL && power >= 1f){

			WarriorShield shield = target.buff(WarriorShield.class);
			if (shield != null){
				state = State.BERSERK;
				int shieldAmount = shield.maxShield() * 8;
				shieldAmount = Math.round(shieldAmount * (1f +
						+ (Dungeon.hero.hasTalent(Talent.BERSERKING_STAMINA) ? 0.1f : 0f)
						+ Dungeon.hero.byTalent(Talent.BERSERKING_STAMINA, 0.20f, Talent.RK_BERSERKER, 0.25f)));
				shield.supercharge(shieldAmount);

				SpellSprite.show(target, SpellSprite.BERSERK);
				Sample.INSTANCE.play( Assets.Sounds.CHALLENGE );
				GameScene.flash(0xFF0000);
			}

		}

		return state == State.BERSERK && target.shielding() > 0;
	}

	private float rageFactor(int damage) {
		Hero hero = Dungeon.hero;
		float weight = 0.1f*hero.pointsInTalent(Talent.ENRAGED_CATALYST,Talent.ONE_MAN_ARMY,Talent.ENDLESS_RAGE);
		return damage/(weight*target.HP+(1-weight)*target.HT)/3f;
	}

	public void damage(int damage){
		if (state == State.RECOVERING && !berserker()) return;
		float maxPower = 1f + Dungeon.hero.byTalent(
				Talent.ENDLESS_RAGE, 0.0f,
				Talent.RK_BERSERKER, 0.1f);
		power = Math.min(maxPower*recovered(), power + rageFactor(damage)*recovered() );
		BuffIndicator.refreshHero(); //show new power immediately
		powerLossBuffer = 3; //2 turns until rage starts dropping
	}

	public final float recovered() {
		return state == State.RECOVERING ? 1-levelRecovery/levelRecoverStart() : 1f;
	}
	public void recover(float percent){
		if (levelRecovery > 0){
			levelRecovery -= percent;
			if (levelRecovery <= 0) {
				state = State.NORMAL;
				if(berserker()) {
					GLog.p("You have fully recovered!"); // because by this point it should look almost exactly like the regular anyway.
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				}
				levelRecovery = 0;
			}
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.BERSERK;
	}

	@Override
	public void tintIcon(Image icon) {
		float r,g,b;
		switch (state){
			case NORMAL: default:
				r = 1;
				g = power < 1f ? .5f : 0f;
				b = 0;
				break;
			case RECOVERING: // it's supposed to look more like the above as you get closer.
				r = berserker() ? .75f*recovered() : 0;
				g = .5f*r;
				b = 1-r;
				break;
		}
		icon.hardlight(r,g,b);
	}
	
	@Override
	public float iconFadePercent() {
		switch (state){
			case RECOVERING: if(!berserker()) return recovered();
			case NORMAL: default:
				return recovered() == 0 ? 0 : 1 - power/recovered();
			case BERSERK:
				return 0f;
		}
	}

	public String iconTextDisplay(){
		switch (state){
			case NORMAL: case BERSERK: default:
				return (int)(power*100) + "%";
			case RECOVERING:
				return new DecimalFormat("#.#").format(levelRecovery);
		}
	}

	@Override
	public String toString() {
		switch (state){
			case NORMAL: default:
				return Messages.get(this, "angered");
			case BERSERK:
				return Messages.get(this, "berserk");
			case RECOVERING:
				return Messages.get(this, "recovering");
		}
	}

	private String getCurrentRageDesc() {
		return Messages.get(this,"current_rage",Math.floor(power*100),Math.floor(100*(damageMult()-1)));
	}
	@Override
	public String desc() {
		String cls = Messages.titleCase(Dungeon.hero.subClass.title());
		StringBuilder desc = new StringBuilder();
		switch (state){
			case NORMAL: default:
				desc.append(Messages.get(this, "angered_desc",cls))
						.append("\n\n")
						.append(getCurrentRageDesc());
				break;
			case BERSERK:
				desc.append( Messages.get(this, "berserk_desc",cls,damageMult()));
				break;
			case RECOVERING:
				desc.append(Messages.get(this, "recovering_desc", cls, Messages.get(
						this,"recovering_penalty_" + (berserker() ? "berserk" : "default"), cls),
						levelRecovery));
				if( berserker() ) {
					desc.append("\n\n% recovered: ").append( (int)Math.floor(recovered() * 100) );
					if(power > 0) desc.append("\n").append(getCurrentRageDesc());
				}
				break;
		}
		return desc.toString();
	}
}
