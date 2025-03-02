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

package com.zrp200.rkpd2.actors.mobs;

import com.watabou.noosa.Camera;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.zrp200.rkpd2.Badges;
import com.zrp200.rkpd2.Challenges;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.Statistics;
import com.zrp200.rkpd2.actors.Actor;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.buffs.*;
import com.zrp200.rkpd2.effects.Speck;
import com.zrp200.rkpd2.effects.Splash;
import com.zrp200.rkpd2.items.artifacts.DriedRose;
import com.zrp200.rkpd2.items.keys.SkeletonKey;
import com.zrp200.rkpd2.items.potions.PotionOfExperience;
import com.zrp200.rkpd2.items.quest.GooBlob;
import com.zrp200.rkpd2.levels.traps.OozeTrap;
import com.zrp200.rkpd2.mechanics.Ballistica;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.sprites.CharSprite;
import com.zrp200.rkpd2.sprites.GooSprite;
import com.zrp200.rkpd2.ui.BossHealthBar;
import com.zrp200.rkpd2.utils.GLog;

public class Goo extends Mob {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 120 : 100;
		EXP = 10;
		defenseSkill = 8;
		spriteClass = GooSprite.class;

		properties.add(Property.BOSS);
		properties.add(Property.DEMONIC);
		properties.add(Property.ACIDIC);
	}

	private int pumpedUp = 0;
	private int healInc = 1;
	private boolean canZap;
	private int zapCD = 2;

	@Override
	public int damageRoll() {
		int min = 1;
		int max = (HP*2 <= HT) ? 12 : 8;
		if (Dungeon.isChallenged(Challenges.EVIL_MODE)){
			min = 4;
			max = (HP*2 <= HT) ? 26 : 18;
			return Random.NormalIntRange( min, max );
		}
		if (pumpedUp > 0) {
			pumpedUp = 0;
			if (enemy == Dungeon.hero) {
				Statistics.qualifiedForBossChallengeBadge = false;
				Statistics.bossScores[0] -= 100;
			}
			return Random.NormalIntRange( min*3, max*3 );
		} else {
			return Random.NormalIntRange( min, max );
		}
	}

	@Override
	public int attackSkill( Char target ) {
		int attack = 10;
		if (HP*2 <= HT) attack = 15;
		if (pumpedUp > 0) attack *= 2;
		return attack;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return (int)(super.defenseSkill(enemy) * ((HP*2 <= HT)? 1.5 : 1));
	}

	@Override
	public int drRoll() {
		return Random.NormalIntRange(0, 2);
	}

	@Override
	public boolean act() {

		if (buff(WarpedEnemy.BossEffect.class) != null){
			canZap = true;
			if (zapCD > 0) zapCD--;
		}

		if (Dungeon.level.water[pos] && HP < HT) {
			HP += healInc;
			Statistics.bossScores[0] -= 10;
			Statistics.qualifiedForBossChallengeBadge = false;

			LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
			if (lock != null) lock.removeTime(healInc*2);

			if (Dungeon.level.heroFOV[pos] ){
				sprite.emitter().burst( Speck.factory( Speck.HEALING ), healInc );
			}
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) && healInc < 3) {
				healInc++;
			}
			if (HP*2 > HT) {
				BossHealthBar.bleed(false);
				((GooSprite)sprite).spray(false);
				HP = Math.min(HP, HT);
			}
		} else {
			healInc = 1;
		}
		
		if (state != SLEEPING){
			Dungeon.level.seal();
		}

		return super.act();
	}

	@Override
	public boolean canAttack( Char enemy ) {
		if (canZap && zapCD == 0){
			return new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos;
		} else if (pumpedUp > 0){
			//we check both from and to in this case as projectile logic isn't always symmetrical.
			//this helps trim out BS edge-cases
			return Dungeon.level.distance(enemy.pos, pos) <= 2
						&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos
						&& new Ballistica( enemy.pos, pos, Ballistica.PROJECTILE).collisionPos == pos;
		} else {
			return super.canAttack(enemy);
		}
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 3 ) == 0) {
			Buff.affect( enemy, Ooze.class ).set( Ooze.DURATION );
			enemy.sprite.burst( 0x000000, 5 );
		}

		if (pumpedUp > 0) {
			Camera.main.shake( 3, 0.2f );
		}

		return damage;
	}

	public static class DarkBolt{}

	protected void zap() {
		spend( 1f );

		if (hit( this, enemy, 3 )) {
			new OozeTrap().set(enemy.pos).activate();

			int dmg = damageRoll()*2;
			if (buff(Shrink.class) != null|| enemy.buff(TimedShrink.class) != null) dmg *= 0.6f;
			ChampionEnemy.AntiMagic.effect(enemy, this);
			dmg = Math.round(dmg * AscensionChallenge.statModifier(this));
			enemy.damage( dmg, new DarkBolt() );
			pumpedUp++;
			Statistics.qualifiedForBossChallengeBadge = false;
			Statistics.bossScores[0] -= 100;

			if (enemy == Dungeon.hero && !enemy.isAlive()) {
				Badges.validateDeathFromEnemyMagic();
				Dungeon.fail( getClass() );
				GLog.n( Messages.get(this, "bolt_kill") );
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
		}
	}

	public void onZapComplete() {
		zap();
		next();
	}

	@Override
	public int defenseProc(Char enemy, int damage) {
		if (Dungeon.isChallenged(Challenges.EVIL_MODE)){
			for (int i : PathFinder.NEIGHBOURS9){
				Splash.at(pos+i, 0x000000, 5);
				if (Actor.findChar(pos+i) != null)
					Buff.affect(Actor.findChar(pos+i), Ooze.class).set( Ooze.DURATION );
			}
			Dungeon.level.spawnMob(5);
		}
		return super.defenseProc(enemy, damage);
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();

		if (pumpedUp > 0){
			((GooSprite)sprite).pumpUp( pumpedUp );
		}
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (canZap && zapCD == 0){
			canZap = false;
			zapCD = 6;
			if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
				sprite.zap( enemy.pos );
				return false;
			} else {
				zap();
				return true;
			}
		} else if (pumpedUp == 1) {
			pumpedUp++;
			((GooSprite)sprite).pumpUp( pumpedUp );

			spend( attackDelay() );

			return true;
		} else if (Dungeon.isChallenged(Challenges.EVIL_MODE) || pumpedUp >= 2 || Random.Int( (HP*2 <= HT) ? 2 : 5 ) > 0) {

			boolean visible = Dungeon.level.heroFOV[pos];

			if (visible) {
				if (pumpedUp >= 2) {
					((GooSprite) sprite).pumpAttack();
				} else {
					sprite.attack(enemy.pos);
				}
			} else {
				if (pumpedUp >= 2){
					((GooSprite)sprite).triggerEmitters();
				}
				attack( enemy );
				spend( attackDelay() );
			}

			return !visible;

		} else {

			pumpedUp++;
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
				pumpedUp++;
			}

			((GooSprite)sprite).pumpUp( pumpedUp );

			if (Dungeon.level.heroFOV[pos]) {
				sprite.showStatus( CharSprite.NEGATIVE, Messages.get(this, "!!!") );
				GLog.n( Messages.get(this, "pumpup") );
			}

			spend( attackDelay() );

			return true;
		}
	}

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti, int rolls ) {
		boolean result = super.attack( enemy, dmgMulti, dmgBonus, accMulti, rolls );
		if (pumpedUp > 0) {
			pumpedUp = 0;
			if (enemy == Dungeon.hero) {
				Statistics.qualifiedForBossChallengeBadge = false;
				Statistics.bossScores[0] -= 100;
			}
		}
		return result;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (pumpedUp != 0) {
			pumpedUp = 0;
			sprite.idle();
		}
		return super.getCloser( target );
	}

	@Override
	public void damage(int dmg, Object src) {
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
		}
		boolean bleeding = (HP*2 <= HT);
		super.damage(dmg, src);
		if ((HP*2 <= HT) && !bleeding){
			BossHealthBar.bleed(true);
			sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "enraged"));
			((GooSprite)sprite).spray(true);
			yell(Messages.get(this, "gluuurp"));
		}
		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null) lock.addTime(dmg*2);
	}

	@Override
	public void die( Object cause ) {
		
		super.die( cause );
		
		Dungeon.level.unseal();
		
		GameScene.bossSlain();
		Dungeon.level.drop( new SkeletonKey(Dungeon.getDepth()), pos ).sprite.drop();
		
		//60% chance of 2 blobs, 30% chance of 3, 10% chance for 4. Average of 2.5
		int blobs = Random.chances(new float[]{0, 0, 6, 3, 1});
		for (int i = 0; i < blobs; i++){
			int ofs;
			do {
				ofs = PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.passable[pos + ofs]);
			Dungeon.level.drop( new GooBlob(), pos + ofs ).sprite.drop( pos );
		}
		
		Badges.validateBossSlain();
		if (Dungeon.isChallenged(Challenges.NO_LEVELS))
			new PotionOfExperience().apply(Dungeon.hero);

		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[0] += 1050; //Goo has a 50 point gimme
		Statistics.bossScores[0] = Math.min(1000, Statistics.bossScores[0]);

		yell( Messages.get(this, "defeated") );
	}
	
	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			Dungeon.level.seal();
			yell(Messages.get(this, "notice"));
			for (Char ch : Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
		}
	}

	private final String PUMPEDUP = "pumpedup";
	private final String HEALINC = "healinc";

	@Override
	public void storeInBundle( Bundle bundle ) {

		super.storeInBundle( bundle );

		bundle.put( PUMPEDUP , pumpedUp );
		bundle.put( HEALINC, healInc );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {

		super.restoreFromBundle( bundle );

		pumpedUp = bundle.getInt( PUMPEDUP );
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if ((HP*2 <= HT)) BossHealthBar.bleed(true);

		healInc = bundle.getInt(HEALINC);
	}
	
}
