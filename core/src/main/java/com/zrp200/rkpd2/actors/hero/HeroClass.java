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

package com.zrp200.rkpd2.actors.hero;

import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Random;
import com.zrp200.rkpd2.*;
import com.zrp200.rkpd2.actors.hero.abilities.ArmorAbility;
import com.zrp200.rkpd2.actors.hero.abilities.Ratmogrify;
import com.zrp200.rkpd2.actors.hero.abilities.huntress.NaturesPower;
import com.zrp200.rkpd2.actors.hero.abilities.huntress.SpectralBlades;
import com.zrp200.rkpd2.actors.hero.abilities.huntress.SpiritHawk;
import com.zrp200.rkpd2.actors.hero.abilities.mage.ElementalBlast;
import com.zrp200.rkpd2.actors.hero.abilities.mage.WarpBeacon;
import com.zrp200.rkpd2.actors.hero.abilities.mage.WildMagic;
import com.zrp200.rkpd2.actors.hero.abilities.rat_king.LegacyWrath;
import com.zrp200.rkpd2.actors.hero.abilities.rat_king.MusRexIra;
import com.zrp200.rkpd2.actors.hero.abilities.rat_king.OmniAbility;
import com.zrp200.rkpd2.actors.hero.abilities.rat_king.Wrath;
import com.zrp200.rkpd2.actors.hero.abilities.rogue.DeathMark;
import com.zrp200.rkpd2.actors.hero.abilities.rogue.ShadowClone;
import com.zrp200.rkpd2.actors.hero.abilities.rogue.SmokeBomb;
import com.zrp200.rkpd2.actors.hero.abilities.warrior.Endure;
import com.zrp200.rkpd2.actors.hero.abilities.warrior.HeroicLeap;
import com.zrp200.rkpd2.actors.hero.abilities.warrior.Shockwave;
import com.zrp200.rkpd2.items.BrokenSeal;
import com.zrp200.rkpd2.items.Generator;
import com.zrp200.rkpd2.items.Item;
import com.zrp200.rkpd2.items.Waterskin;
import com.zrp200.rkpd2.items.armor.ClothArmor;
import com.zrp200.rkpd2.items.armor.ScoutArmor;
import com.zrp200.rkpd2.items.artifacts.Artifact;
import com.zrp200.rkpd2.items.artifacts.CloakOfShadows;
import com.zrp200.rkpd2.items.bags.MagicalHolster;
import com.zrp200.rkpd2.items.bags.PotionBandolier;
import com.zrp200.rkpd2.items.bags.ScrollHolder;
import com.zrp200.rkpd2.items.bags.VelvetPouch;
import com.zrp200.rkpd2.items.food.Food;
import com.zrp200.rkpd2.items.food.MysteryMeat;
import com.zrp200.rkpd2.items.potions.PotionOfHealing;
import com.zrp200.rkpd2.items.potions.PotionOfInvisibility;
import com.zrp200.rkpd2.items.potions.PotionOfLiquidFlame;
import com.zrp200.rkpd2.items.potions.PotionOfMindVision;
import com.zrp200.rkpd2.items.quest.Chaosstone;
import com.zrp200.rkpd2.items.scrolls.*;
import com.zrp200.rkpd2.items.wands.Wand;
import com.zrp200.rkpd2.items.wands.WandOfMagicMissile;
import com.zrp200.rkpd2.items.weapon.SpiritBow;
import com.zrp200.rkpd2.items.weapon.melee.Dagger;
import com.zrp200.rkpd2.items.weapon.melee.Gloves;
import com.zrp200.rkpd2.items.weapon.melee.MagesStaff;
import com.zrp200.rkpd2.items.weapon.melee.WornShortsword;
import com.zrp200.rkpd2.items.weapon.missiles.MissileWeapon;
import com.zrp200.rkpd2.items.weapon.missiles.ThrowingKnife;
import com.zrp200.rkpd2.items.weapon.missiles.ThrowingStone;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.utils.DungeonSeed;
import ​com.​zrp200.​rkpd2.​items.quest.Kromer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import static com.zrp200.rkpd2.Dungeon.hero;
public enum HeroClass {

	WARRIOR(HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR),
	MAGE(HeroSubClass.BATTLEMAGE, HeroSubClass.WARLOCK) {
		@Override public int getBonus(Item item) { return item instanceof Wand ? MAGE_WAND_BOOST : 0; }
	},
	ROGUE(HeroSubClass.ASSASSIN, HeroSubClass.FREERUNNER) {
		//@Override public int getBonus(Item item) { return item instanceof Weapon ? 1 : 0; }
	},
	HUNTRESS(HeroSubClass.SNIPER, HeroSubClass.WARDEN) {
		@Override public int getBonus(Item item) {
			return item instanceof MissileWeapon ? 1 : 0;
		}
	},
	RAT_KING (HeroSubClass.KING);

	private ArrayList<HeroSubClass> subClasses;

	public static final int MAGE_WAND_BOOST = 2;
	public int getBonus(Item item) { return 0; }

	public HeroSubClass secretSub(){
		switch (this){
			case HUNTRESS:
				return HeroSubClass.WARLOCK;
			case WARRIOR:
				return HeroSubClass.BRAWLER;
			case ROGUE:
				return HeroSubClass.DECEPTICON;
			case MAGE:
				return HeroSubClass.SPIRITUALIST;
			case RAT_KING:
				return HeroSubClass.RK_CHAMPION;
			default:
				return null;
		}
	}

	HeroClass(HeroSubClass... subClasses ) {
		this.subClasses = new ArrayList<>(Arrays.asList(subClasses));
	}

	public static void giveSecondClass(HeroClass heroClass){
		hero.heroClass2 = heroClass;
		Talent.initSecondClassTalents(hero.heroClass2, hero.talents, hero.metamorphedTalents);
		switch (heroClass) {
			case WARRIOR:
				new BrokenSeal().identify().collect();
				break;

			case MAGE:
				new MagesStaff(new WandOfMagicMissile()).identify().collect();
				break;

			case ROGUE:
				new CloakOfShadows().identify().collect();
				break;

			case HUNTRESS:
				new SpiritBow().identify().collect();
				break;

			case RAT_KING:
				if (hero.heroClass != WARRIOR)
					new BrokenSeal().identify().collect();
				if (hero.heroClass != MAGE)
					new MagesStaff(new WandOfMagicMissile()).identify().collect();
				if (hero.heroClass != ROGUE)
					new CloakOfShadows().identify().collect();
				if (hero.heroClass != HUNTRESS)
					new SpiritBow().identify().collect();
				break;
		}
	}

	public void initHero( Hero hero ) {

		hero.heroClass = this;
		Talent.initClassTalents(hero);

		Item i = new ClothArmor().identify();
		if (!Challenges.isItemBlocked(i)) hero.belongings.armor = (ClothArmor) i;

		if (Dungeon.isChallenged(Challenges.NO_VEGAN)){
			i = new MysteryMeat();
		}
		else i = new Food();
		if (!Challenges.isItemBlocked(i)) i.collect();

		// give all bags.
		new VelvetPouch().collect();
		new PotionBandolier().collect();
		new ScrollHolder().collect();
		new MagicalHolster().collect();
		new Kromer().quantity(99).collect();
		Dungeon.LimitedDrops.VELVET_POUCH.drop();
		Dungeon.LimitedDrops.POTION_BANDOLIER.drop();
		Dungeon.LimitedDrops.SCROLL_HOLDER.drop();
		Dungeon.LimitedDrops.MAGICAL_HOLSTER.drop();

		Waterskin waterskin = new Waterskin();
		waterskin.collect();


		new ScrollOfIdentify().identify();
		if (Badges.isUnlocked(Badges.Badge.CHAMPION_7)){
			new Chaosstone().collect();
		}
		switch (this) {
			case WARRIOR:
				initWarrior(hero);
				break;

			case MAGE:
				initMage(hero);
				break;

			case ROGUE:
				initRogue(hero);
				break;

			case HUNTRESS:
				initHuntress(hero);
				break;
			case RAT_KING:
				initRatKing(hero);
				break;
		}

		for (int s = 0; s < QuickSlot.SIZE; s++) {
			if (Dungeon.quickslot.getItem(s) == null) {
				Dungeon.quickslot.setSlot(s, waterskin);
				break;
			}
		}
	}

	public Badges.Badge masteryBadge() {
		switch (this) {
			case WARRIOR:
				return Badges.Badge.MASTERY_WARRIOR;
			case MAGE:
				return Badges.Badge.MASTERY_MAGE;
			case ROGUE:
				return Badges.Badge.MASTERY_ROGUE;
			case HUNTRESS:
				return Badges.Badge.MASTERY_HUNTRESS;
			case RAT_KING:
				return Badges.Badge.MASTERY_RAT_KING;
		}
		return null;
	}

	private static void initWarrior( Hero hero ) {
		(hero.belongings.weapon = new WornShortsword()).identify();
		ThrowingStone stones = new ThrowingStone();
		stones.quantity(3).collect();
		Dungeon.quickslot.setSlot(0, stones);

		if (hero.belongings.armor != null){
			hero.belongings.armor.affixSeal(new BrokenSeal());
		}


		new PotionOfHealing().identify();
		new ScrollOfRage().identify();
	}

	private static void initMage( Hero hero ) {
		MagesStaff staff;

		staff = new MagesStaff(new WandOfMagicMissile());

		(hero.belongings.weapon = staff).identify();
		hero.belongings.weapon.activate(hero);

		Dungeon.quickslot.setSlot(0, staff);

		new ScrollOfUpgrade().identify();
		new PotionOfLiquidFlame().identify();
	}

	private static void initRogue( Hero hero ) {
		(hero.belongings.weapon = new Dagger()).identify();

		if (Dungeon.specialSeed != DungeonSeed.SpecialSeed.ROGUE) {
			CloakOfShadows cloak = new CloakOfShadows();
			(hero.belongings.artifact = cloak).identify();
			hero.belongings.artifact.activate(hero);
			Dungeon.quickslot.setSlot(0, cloak);
		} else {
			Random.pushGenerator();
			Artifact cloak = Generator.randomArtifact();
			(hero.belongings.artifact = cloak).identify();
			hero.belongings.artifact.activate(hero);
			Random.popGenerator();
			Dungeon.quickslot.setSlot(0, cloak);
		}

		ThrowingKnife knives = new ThrowingKnife();
		knives.quantity(3).collect();

		Dungeon.quickslot.setSlot(1, knives);

		new ScrollOfMagicMapping().identify();
		new PotionOfInvisibility().identify();
	}

	private static void initHuntress( Hero hero ) {

		(hero.belongings.weapon = new Gloves()).identify();
		(hero.belongings.armor = new ScoutArmor()).identify();
		SpiritBow bow = new SpiritBow();
		bow.identify().collect();

		Dungeon.quickslot.setSlot(0, bow);
		Dungeon.quickslot.setSlot(1, hero.belongings.armor);

		new PotionOfMindVision().identify();
		new ScrollOfLullaby().identify();
	}

	private static void initRatKing( Hero hero ) {
		// warrior
		if (hero.belongings.armor != null){
			hero.belongings.armor.affixSeal(new BrokenSeal());
		}
		// mage
		MagesStaff staff = new MagesStaff(new WandOfMagicMissile());
		(hero.belongings.weapon = staff).identify();
		hero.belongings.weapon.activate(hero);
		// rogue
		CloakOfShadows cloak = new CloakOfShadows();
		(hero.belongings.artifact = cloak).identify();
		hero.belongings.artifact.activate( hero );
		// huntress
		SpiritBow bow = new SpiritBow();
		bow.identify().collect();
		// allocating slots
		Dungeon.quickslot.setSlot(0, bow);
		Dungeon.quickslot.setSlot(1, cloak);
		Dungeon.quickslot.setSlot(2, staff);
	}

	public String title() {
		return Messages.get(HeroClass.class, name());
	}

	public String desc(){
		return Messages.get(HeroClass.class, name()+"_desc");
	}

	public ArrayList<HeroSubClass> subClasses() {
		ArrayList<HeroSubClass> subClasses = this.subClasses;
		if ((Badges.isUnlocked(Badges.Badge.DEFEATED_RK) || Badges.isUnlocked(secretSub().secretBadge()))
				&& !subClasses.contains(secretSub())){
			subClasses.add(secretSub());
		}
		subClasses.remove(null);
		return subClasses;
	}

	public ArmorAbility[] armorAbilities(){
		switch (this) {
			case WARRIOR: default:
				return new ArmorAbility[]{new HeroicLeap(), new Shockwave(), new Endure()};
			case MAGE:
				return new ArmorAbility[]{new ElementalBlast(), new WildMagic(), new WarpBeacon()};
			case ROGUE:
				return new ArmorAbility[]{new SmokeBomb(), new DeathMark(), new ShadowClone()};
			case HUNTRESS:
				return new ArmorAbility[]{new SpectralBlades(), new NaturesPower(), new SpiritHawk()};
			case RAT_KING:
				return new ArmorAbility[]{new LegacyWrath(), new Ratmogrify(), new MusRexIra(), new Wrath(), new OmniAbility()};
		}
	}

	public String spritesheet() {
		switch (this) {
			case WARRIOR: default:
				return Assets.Sprites.WARRIOR;
			case MAGE:
				return Assets.Sprites.MAGE;
			case ROGUE:
				return Assets.Sprites.ROGUE;
			case HUNTRESS:
				return Assets.Sprites.HUNTRESS;
			case RAT_KING:
				return Assets.Sprites.RAT_KING_HERO;
		}
	}

	public String splashArt(){
		return "splashes/" + name().toLowerCase(Locale.ENGLISH) + ".jpg";
		/*switch (this) {
			case WARRIOR: default:
				return Assets.Splashes.WARRIOR;
			case MAGE:
				return Assets.Splashes.MAGE;
			case ROGUE:
				return Assets.Splashes.ROGUE;
			case HUNTRESS:
				return Assets.Splashes.HUNTRESS;
		}*/
	}
	
	public String[] perks() {
		String[] perks = new String[5];
		for(int i=0; i < perks.length; i++) perks[i] = Messages.get(HeroClass.class, name() + "_perk" + (i+1));
		return perks;
	}
	
	public boolean isUnlocked(){
		//always unlock on debug builds
		return DeviceCompat.isDebug() || this != RAT_KING || Badges.isUnlocked(Badges.Badge.UNLOCK_RAT_KING);
		/*
		switch (this){
			case WARRIOR: default:
				return true;
			case MAGE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_MAGE);
			case ROGUE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_ROGUE);
			case HUNTRESS:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_HUNTRESS);
		}
		 */
	}
	
	public String unlockMsg() {
		return Messages.get(HeroClass.class, name() + "_unlock");
	}

}
