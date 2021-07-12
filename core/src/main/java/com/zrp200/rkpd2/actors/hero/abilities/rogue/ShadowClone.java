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

package com.zrp200.rkpd2.actors.hero.abilities.rogue;

import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.Actor;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.buffs.Invisibility;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.actors.hero.abilities.ArmorAbility;
import com.zrp200.rkpd2.actors.mobs.npcs.DirectableAlly;
import com.zrp200.rkpd2.effects.CellEmitter;
import com.zrp200.rkpd2.effects.Speck;
import com.zrp200.rkpd2.effects.particles.SmokeParticle;
import com.zrp200.rkpd2.items.armor.ClassArmor;
import com.zrp200.rkpd2.items.weapon.missiles.Kunai;
import com.zrp200.rkpd2.levels.CityLevel;
import com.zrp200.rkpd2.mechanics.Ballistica;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.sprites.HeroSprite;
import com.zrp200.rkpd2.sprites.MissileSprite;
import com.zrp200.rkpd2.sprites.MobSprite;
import com.zrp200.rkpd2.utils.BArray;
import com.zrp200.rkpd2.utils.GLog;

import java.util.ArrayList;

public class ShadowClone extends ArmorAbility {

	@Override
	public String targetingPrompt() {
		if (getShadowAlly() == null) {
			return super.targetingPrompt();
		} else {
			return Messages.get(this, "prompt");
		}
	}

	@Override
	public boolean useTargeting(){
		return false;
	}

	{
		baseChargeUse = 50f;
	}

	@Override
	public float chargeUse(Hero hero) {
		if (getShadowAlly() == null) {
			return super.chargeUse(hero);
		} else {
			return 0;
		}
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		ShadowAlly ally = getShadowAlly();

		if (ally != null){
			if (target == null){
				return;
			} else {
				ally.directTocell(target);
			}
		} else {
			ArrayList<Integer> spawnPoints = new ArrayList<>();
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
				int p = hero.pos + PathFinder.NEIGHBOURS8[i];
				if (Actor.findChar(p) == null && Dungeon.level.passable[p]) {
					spawnPoints.add(p);
				}
			}

			if (!spawnPoints.isEmpty()){
				armor.charge -= chargeUse(hero);
				armor.updateQuickslot();

				ally = new ShadowAlly(hero.lvl);
				ally.pos = Random.element(spawnPoints);
				GameScene.add(ally);

				ShadowAlly.appear(ally, ally.pos);

				Invisibility.dispel();
				hero.spendAndNext(Actor.TICK);

			} else {
				GLog.w(Messages.get(this, "no_space"));
			}
		}

	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SHADOW_BLADE, Talent.CLONED_ARMOR, Talent.PERFECT_COPY, Talent.DAR_MAGIC, Talent.HEROIC_ENERGY};
	}

	private static ShadowAlly getShadowAlly(){
		for (Char ch : Actor.chars()){
			if (ch instanceof ShadowAlly){
				return (ShadowAlly) ch;
			}
		}
		return null;
	}

	public static class ShadowAlly extends DirectableAlly {

		{
			spriteClass = ShadowSprite.class;

			HP = HT = 100;
		}

		public ShadowAlly(){
			super();
		}

		public ShadowAlly( int heroLevel ){
			super();
			int hpBonus = 20 + 5*heroLevel;
			hpBonus = Math.round(0.1f * Dungeon.hero.shiftedPoints(Talent.PERFECT_COPY) * hpBonus);
			if (hpBonus > 0){
				HT += hpBonus;
				HP += hpBonus;
			}
			defenseSkill = heroLevel + 5; //equal to base hero defense skill
		}

		@Override
		public boolean canAttack(Char enemy) {
			if (Dungeon.hero.pointsInTalent(Talent.DAR_MAGIC) > 0){
				Ballistica attack = new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE);
				return attack.collisionPos == enemy.pos;
			}
			return super.canAttack(enemy);
		}

		protected boolean doAttack( Char enemy ) {
			if (Dungeon.level.adjacent(pos, enemy.pos)){
				return super.doAttack( enemy );
			} else {

				if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
					if (Dungeon.hero.pointsInTalent(Talent.DAR_MAGIC) < 3) {
						((MissileSprite) sprite.parent.recycle(MissileSprite.class)).
								reset(sprite,
										enemy.sprite,
										new Kunai(),
										new Callback() {
											@Override
											public void call() {
												attack(enemy,
														0.25f * Dungeon.hero.pointsInTalent(Talent.DAR_MAGIC),
														0, 1);
												spend(attackDelay());
												next();
											}
										});
					} else {
						//teleport
						int bestPos = -1;
						for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
							int p = enemy.pos + PathFinder.NEIGHBOURS8[i];
							if (Actor.findChar( p ) == null && Dungeon.level.passable[p]) {
								bestPos = p;
							}
						}
						if (bestPos != -1){
							appear(this, bestPos);
							sprite.attack(enemy.pos, new Callback() {
								@Override
								public void call() {
									attack(enemy);
									spend(attackDelay());

											if (Dungeon.hero.pointsInTalent(Talent.DAR_MAGIC) == 4){
												int count = 32;
												int newPos = -1;
												for (int i : PathFinder.NEIGHBOURS8){
													if (Actor.findChar( Dungeon.hero.pos + i ) == null && Dungeon.level.passable[Dungeon.hero.pos + i]){
														newPos = Dungeon.hero.pos + i;
													}
												}

												if (newPos != -1) {

													if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);
													appear(ShadowAlly.this, newPos);
													if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);
												}


											}
									next();
								}
							});
							return false;
//							spend(attackDelay());
						}
					}
					return false;
				} else {
					return super.doAttack(enemy);
				}
			}
		}

		@Override
		protected boolean act() {
			int oldPos = pos;
			boolean result = super.act();
			//partially simulates how the hero switches to idle animation
			if ((pos == target || oldPos == pos) && sprite.looping()){
				sprite.idle();
			}
			return result;
		}

		@Override
		public void followHero() {
			GLog.i(Messages.get(this, "direct_follow"));
			super.followHero();
		}

		@Override
		public void targetChar(Char ch) {
			GLog.i(Messages.get(this, "direct_attack"));
			super.targetChar(ch);
		}

		@Override
		public int attackSkill(Char target) {
			return defenseSkill+5; //equal to base hero attack skill
		}

		@Override
		public int damageRoll() {
			int damage = Random.NormalIntRange(10, 20);
			int heroDamage = Dungeon.hero.damageRoll();
			heroDamage /= Dungeon.hero.attackDelay(); //normalize hero damage based on atk speed
			heroDamage = Math.round(0.0625f * Dungeon.hero.shiftedPoints(Talent.SHADOW_BLADE) * heroDamage);
			if (heroDamage > 0){
				damage += heroDamage;
			}
			if (Dungeon.hero.pointsInTalent(Talent.DAR_MAGIC) == 3){
				damage *= 0.75f;
			}
			return damage;
		}

		@Override
		public int attackProc( Char enemy, int damage ) {
			damage = super.attackProc( enemy, damage );
			// shifted to actually work.
			if (Random.Int(/*4*/5) < Dungeon.hero.shiftedPoints(Talent.SHADOW_BLADE)
					&& Dungeon.hero.belongings.weapon != null){
				return Dungeon.hero.belongings.weapon.proc( this, enemy, damage );
			} else {
				return damage;
			}
		}

		@Override
		public int drRoll() {
			int dr = super.drRoll();
			int heroRoll = Dungeon.hero.drRoll();
			heroRoll = Math.round(0.125f * Dungeon.hero.shiftedPoints(Talent.CLONED_ARMOR) * heroRoll);
			if (heroRoll > 0){
				dr += heroRoll;
			}
			return dr;
		}

		@Override
		public int defenseProc(Char enemy, int damage) {
			damage = super.defenseProc(enemy, damage);
			// shifted to work
			if (Random.Int(/*4*/5) < Dungeon.hero.shiftedPoints(Talent.CLONED_ARMOR)
					&& Dungeon.hero.belongings.armor != null){
				return Dungeon.hero.belongings.armor.proc( enemy, this, damage );
			} else {
				return damage;
			}
		}

		@Override
		public boolean canInteract(Char c) {
			if (super.canInteract(c)){
				return true;
			} else if (Dungeon.level.distance(pos, c.pos) <= Dungeon.hero.shiftedPoints(Talent.PERFECT_COPY)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean interact(Char c) {
			// automatically given..
			if (!Dungeon.hero.canHaveTalent(Talent.PERFECT_COPY)){
				return super.interact(c);
			}

			//some checks from super.interact
			if (!Dungeon.level.passable[pos] && !c.flying){
				return true;
			}

			if (properties().contains(Property.LARGE) && !Dungeon.level.openSpace[c.pos]
					|| c.properties().contains(Property.LARGE) && !Dungeon.level.openSpace[pos]){
				return true;
			}

			int curPos = pos;

			//warp instantly with the clone
			PathFinder.buildDistanceMap(c.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
			if (PathFinder.distance[pos] == Integer.MAX_VALUE){
				return true;
			}
			appear(this, Dungeon.hero.pos);
			appear(Dungeon.hero, curPos);
			Dungeon.observe();
			GameScene.updateFog();
			return true;
		}

		private static void appear( Char ch, int pos ) {

			ch.sprite.interruptMotion();

			if (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[ch.pos]){
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
			}

			ch.move( pos );
			if (ch.pos == pos) ch.sprite.place( pos );

			if (Dungeon.level.heroFOV[pos] || ch == Dungeon.hero ) {
				ch.sprite.emitter().burst(SmokeParticle.FACTORY, 10);
			}
		}

		private static final String DEF_SKILL = "def_skill";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DEF_SKILL, defenseSkill);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			defenseSkill = bundle.getInt(DEF_SKILL);
		}
	}

	public static class ShadowSprite extends MobSprite {

		private Emitter smoke;

		public ShadowSprite() {
			super();

			texture( Dungeon.hero.heroClass.spritesheet() );

			TextureFilm film = new TextureFilm( ((HeroSprite)Dungeon.hero.sprite).tiers(), 6, 12, 15 );

			idle = new Animation( 1, true );
			idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );

			run = new Animation( 20, true );
			run.frames( film, 2, 3, 4, 5, 6, 7 );

			die = new Animation( 20, false );
			die.frames( film, 0 );

			attack = new Animation( 15, false );
			attack.frames( film, 13, 14, 15, 0 );

			idle();
			resetColor();
		}

		@Override
		public void onComplete(Tweener tweener) {
			super.onComplete(tweener);
		}

		@Override
		public void resetColor() {
			super.resetColor();
			alpha(0.8f);
			brightness(0.0f);
		}

		@Override
		public void link( Char ch ) {
			super.link( ch );
			renderShadow = false;

			if (smoke == null) {
				smoke = emitter();
				smoke.pour( CityLevel.Smoke.factory, 0.2f );
			}
		}

		@Override
		public void update() {

			super.update();

			if (smoke != null) {
				smoke.visible = visible;
			}
		}

		@Override
		public void kill() {
			super.kill();

			if (smoke != null) {
				smoke.on = false;
			}
		}
	}
}
