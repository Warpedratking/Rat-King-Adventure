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

package com.zrp200.rkpd2.levels;

import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.zrp200.rkpd2.Bones;
import com.zrp200.rkpd2.Challenges;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.Statistics;
import com.zrp200.rkpd2.actors.Actor;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.blobs.Blob;
import com.zrp200.rkpd2.actors.blobs.SacrificialFire;
import com.zrp200.rkpd2.actors.buffs.Buff;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.actors.mobs.GoldenMimic;
import com.zrp200.rkpd2.actors.mobs.Mimic;
import com.zrp200.rkpd2.actors.mobs.Mob;
import com.zrp200.rkpd2.actors.mobs.Statue;
import com.zrp200.rkpd2.items.*;
import com.zrp200.rkpd2.items.artifacts.Artifact;
import com.zrp200.rkpd2.items.artifacts.DriedRose;
import com.zrp200.rkpd2.items.food.Food;
import com.zrp200.rkpd2.items.food.MysteryMeat;
import com.zrp200.rkpd2.items.food.SmallRation;
import com.zrp200.rkpd2.items.journal.GuidePage;
import com.zrp200.rkpd2.items.keys.GoldenKey;
import com.zrp200.rkpd2.items.keys.Key;
import com.zrp200.rkpd2.items.potions.PotionOfStrength;
import com.zrp200.rkpd2.items.scrolls.ScrollOfUpgrade;
import com.zrp200.rkpd2.items.weapon.Weapon;
import com.zrp200.rkpd2.journal.Document;
import com.zrp200.rkpd2.journal.Notes;
import com.zrp200.rkpd2.levels.builders.Builder;
import com.zrp200.rkpd2.levels.builders.FigureEightBuilder;
import com.zrp200.rkpd2.levels.builders.LoopBuilder;
import com.zrp200.rkpd2.levels.painters.Painter;
import com.zrp200.rkpd2.levels.rooms.Room;
import com.zrp200.rkpd2.levels.rooms.connection.TunnelRoom;
import com.zrp200.rkpd2.levels.rooms.secret.SecretRoom;
import com.zrp200.rkpd2.levels.rooms.special.MagicalFireRoom;
import com.zrp200.rkpd2.levels.rooms.special.PitRoom;
import com.zrp200.rkpd2.levels.rooms.special.ShopRoom;
import com.zrp200.rkpd2.levels.rooms.special.SpecialRoom;
import com.zrp200.rkpd2.levels.rooms.standard.EntranceRoom;
import com.zrp200.rkpd2.levels.rooms.standard.ExitRoom;
import com.zrp200.rkpd2.levels.rooms.standard.StandardRoom;
import com.zrp200.rkpd2.levels.traps.*;
import com.zrp200.rkpd2.utils.DungeonSeed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public abstract class RegularLevel extends Level {
	
	protected ArrayList<Room> rooms;
	
	protected Builder builder;
	
	protected Room roomEntrance;
	protected Room roomExit;
	
	@Override
	protected boolean build() {
		
		builder = builder();
		
		ArrayList<Room> initRooms = initRooms();
		Random.shuffle(initRooms);
		
		do {
			for (Room r : initRooms){
				r.neigbours.clear();
				r.connected.clear();
			}
			rooms = builder.build((ArrayList<Room>)initRooms.clone());
		} while (rooms == null);
		
		return painter().paint(this, rooms);
		
	}

	protected static final float[] SIZE_MODIFIER = {.5f,.7f}; // range of values
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> initRooms = new ArrayList<>();
		initRooms.add ( roomEntrance = new EntranceRoom());
		initRooms.add( roomExit = new ExitRoom());

		//force max standard rooms and multiple by 1.5x for large levels
		int standards = standardRooms(feeling == Feeling.LARGE);
		if (feeling == Feeling.LARGE){
			standards = (int)Math.ceil(standards * 1.5f);
		}
		// reduce by designated amount to reduce levelsize for rkpd2, much like rkpd does.
		// reduce for rkpd2, inspired by rkpd
		if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.BIGGER)
			standards *= 4;
		else
			standards = (int)Math.floor(standards * Random.Float(SIZE_MODIFIER[0],SIZE_MODIFIER[1]));
		if (Dungeon.isChallenged(Challenges.MANY_MOBS)){
			standards *= 4;
		}
		for (int i = 0; i < standards; i++) {
			int sizeCat = standards-i;
			if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.BIGGER)
				sizeCat = 0;
			StandardRoom s;
			do {
				s = StandardRoom.createRoom();
			} while (!s.setSizeCat( standards-i ));
			i += s.sizeCat.roomValue-1;
			initRooms.add(s);
		}
		
		if (Dungeon.shopOnLevel())
			initRooms.add(new ShopRoom());

		//force max special rooms and add one more for large levels
		int specials = specialRooms(feeling == Feeling.LARGE);
		if (feeling == Feeling.LARGE){
			specials++;
		}
		if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.BIGGER)
			specials += 2;
		// reduce size of special rooms by same factor for net decrease. This will do much less in comparison to regular rooms but proportions are proportions.
		//specials = Random.round( specials*SIZE_MODIFIER );
		SpecialRoom.initForFloor();
		for (int i = 0; i < specials; i++) {
			SpecialRoom s = SpecialRoom.createRoom();
			if (s instanceof PitRoom) specials++;
			initRooms.add(s);
		}
		
		int secrets = SecretRoom.secretsForFloor(Dungeon.getDepth());
		// amount of secrets is not reduced.
		//one additional secret for secret levels
		if (feeling == Feeling.SECRETS) secrets++;
		for (int i = 0; i < secrets; i++) {
			initRooms.add(SecretRoom.createRoom());
		}
		
		return initRooms;
	}
	
	protected int standardRooms(boolean forceMax){
		return 0;
	}
	
	protected int specialRooms(boolean forceMax){
		return 0;
	}
	
	protected Builder builder(){
		if (Random.Int(2) == 0){
			return new LoopBuilder()
					.setLoopShape( 2 ,
							Random.Float(0f, 0.65f),
							Random.Float(0f, 0.50f));
		} else {
			return new FigureEightBuilder()
					.setLoopShape( 2 ,
							Random.Float(0.3f, 0.8f),
							0f);
		}

	}
	
	protected abstract Painter painter();
	
	protected int nTraps() {
		return Random.NormalIntRange( 2, 3 + (Dungeon.getDepth() /5) );
	}
	
	protected Class<?>[] trapClasses(){
		return new Class<?>[]{WornDartTrap.class};
	}

	protected float[] trapChances() {
		return new float[]{1};
	}
	
	@Override
	public int mobLimit() {
		if (Dungeon.getDepth() <= 1 && !Dungeon.isChallenged(Challenges.KROMER)){
			if (!Statistics.amuletObtained) return 0;
			else                            return 10;
		}

		int mobs = 3 + Dungeon.getDepth() % 5 + Random.Int(3);
		if (feeling == Feeling.LARGE){
			mobs = (int)Math.ceil(mobs * 1.33f);
		}
		if (Dungeon.bossLevel() && Dungeon.getDepth() > 25){
			mobs *= 3;
		}
		return mobs;
	}
	
	@Override
	protected void createMobs() {
		//on floor 1, 8 pre-set mobs are created so the player can get level 2.
		int mobsToSpawn = Dungeon.getDepth() == 1 ? 8 : mobLimit();
		if (Dungeon.isChallenged(Challenges.MANY_MOBS)){
			mobsToSpawn *= 16;
		}
		if (Dungeon.isChallenged(Challenges.TOO_MANY_MOBS)){
			mobsToSpawn *= 1000;
		}
		if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.BIGGER)
			mobsToSpawn *= 2;

		ArrayList<Room> stdRooms = new ArrayList<>();
		boolean allowCorridors = Dungeon.isChallenged(Challenges.TOO_MANY_MOBS);
		for (Room room : rooms) {
			if (room instanceof StandardRoom && room != roomEntrance) {
				for (int i = 0; i < ((StandardRoom) room).sizeCat.roomValue; i++) {
					stdRooms.add(room);
				}
			} else if(allowCorridors && room instanceof TunnelRoom){
				stdRooms.add(room);
			}
		}
		Random.shuffle(stdRooms);
		Iterator<Room> stdRoomIter = stdRooms.iterator();

		if (Dungeon.isChallenged(Challenges.TOO_MANY_MOBS)){
				HashSet<Integer> cells = new HashSet<>();
				HashSet<Integer> largeCells = new HashSet<>();
				while (stdRoomIter.hasNext()) {
					Room r = stdRoomIter.next();
					for (int i = r.left; i < r.right; i++) {
						for (int j = r.top; j < r.bottom; j++) {
							int c = pointToCell(new Point(i, j));
							if (passable[c] && !solid[c] && c != exit && c != entrance) {
								cells.add(c);
								if (openSpace[c]) largeCells.add(c);
							}
						}
					}
				}
				while (mobsToSpawn > 0) {
					Mob mob = createMob();
					if (cells.size() <= 0) break;
					HashSet<Integer> set = mob.properties().contains(Char.Property.LARGE) ? largeCells : cells;

					mobsToSpawn--;
					if (set.size() > 0) {
						int cell = Random.element(set);
						mob.pos = cell;
						cells.remove(cell);
						largeCells.remove(cell);
						mobs.add(mob);
					}
				}
		} else {
			while (mobsToSpawn > 0) {
				Mob mob = createMob();
				Room roomToSpawn;

				if (!stdRoomIter.hasNext()) {
					stdRoomIter = stdRooms.iterator();
				}
				roomToSpawn = stdRoomIter.next();

				int tries = 30;
				do {
					mob.pos = pointToCell(roomToSpawn.random());
					tries--;
				} while (tries >= 0 && (findMob(mob.pos) != null || !passable[mob.pos] || solid[mob.pos] || mob.pos == exit()
						|| (!openSpace[mob.pos] && mob.properties().contains(Char.Property.LARGE))));

				if (tries >= 0) {
					mobsToSpawn--;
					mobs.add(mob);

					//chance to add a second mob to this room, except on floor 1
					if (Dungeon.getDepth() > 1 && mobsToSpawn > 0 && Random.Int(4) == 0) {
						mob = createMob();

						tries = 30;
						do {
							mob.pos = pointToCell(roomToSpawn.random());
							tries--;
						} while (tries >= 0 && (findMob(mob.pos) != null || !passable[mob.pos] || solid[mob.pos] || mob.pos == exit()
								|| (!openSpace[mob.pos] && mob.properties().contains(Char.Property.LARGE))));

						if (tries >= 0) {
							mobsToSpawn--;
							mobs.add(mob);
						}
					}
				}
			}
		}

		for (Mob m : mobs){
			if (map[m.pos] == Terrain.HIGH_GRASS || map[m.pos] == Terrain.FURROWED_GRASS) {
				map[m.pos] = Terrain.GRASS;
				losBlocking[m.pos] = false;
			}

		}

	}

	@Override
	public int randomRespawnCell( Char ch ) {
		int count = 0;
		int cell = -1;

		while (true) {

			if (++count > 30) {
				return -1;
			}

			Room room = randomRoom( StandardRoom.class );
			if (room == null || room == roomEntrance) {
				continue;
			}

			cell = pointToCell(room.random(1));
			if (!heroFOV[cell]
					&& Actor.findChar( cell ) == null
					&& passable[cell]
					&& !solid[cell]
					&& (!Char.hasProp(ch, Char.Property.LARGE) || openSpace[cell])
					&& room.canPlaceCharacter(cellToPoint(cell), this)
					&& cell != exit()) {
				return cell;
			}

		}
	}
	
	@Override
	public int randomDestination( Char ch ) {
		
		int count = 0;
		int cell = -1;
		
		while (true) {
			
			if (++count > 30) {
				return -1;
			}
			
			Room room = Random.element( rooms );
			if (room == null) {
				continue;
			}

			ArrayList<Point> points = room.charPlaceablePoints(this);
			if (!points.isEmpty()){
				cell = pointToCell(Random.element(points));
				if (passable[cell] && (!Char.hasProp(ch, Char.Property.LARGE) || openSpace[cell])) {
					return cell;
				}
			}
			
		}
	}
	
	@Override
	protected void createItems() {
		
		// drops 3/4/5 items 60%/30%/10% of the time
		int nItems = 3 + Random.chances(new float[]{6, 3, 1});

		if (feeling == Feeling.LARGE){
			nItems += 2;
		}
		if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.BIGGER)
			nItems *= 1.5f;
		
		for (int i=0; i < nItems; i++) {

			Item toDrop = Generator.random();
			if (toDrop == null) continue;

			int cell = randomDropCell();
			if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
				map[cell] = Terrain.GRASS;
				losBlocking[cell] = false;
			}

			Heap.Type type = null;
			switch (Random.Int( 20 )) {
			case 0:
				type = Heap.Type.SKELETON;
				break;
			case 1:
			case 2:
			case 3:
			case 4:
				type = Heap.Type.CHEST;
				break;
			case 5:
				if (Dungeon.getDepth() > 1 && findMob(cell) == null){
					mobs.add(Mimic.spawnAt(cell, toDrop));
					continue;
				}
				type = Heap.Type.CHEST;
				break;
			default:
				type = Heap.Type.HEAP;
				break;
			}

			if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.CHESTS)
				type = Heap.Type.CHEST;

			if ((toDrop instanceof Artifact && Random.Int(2) == 0) ||
					(toDrop.isUpgradable() && Random.Int(4 - toDrop.level()) == 0)){

				if (Dungeon.getDepth() > 1 && Random.Int(10) == 0 && findMob(cell) == null){
					mobs.add(Mimic.spawnAt(cell, toDrop, GoldenMimic.class));
				} else {
					Heap dropped = drop(toDrop, cell);
					if (heaps.get(cell) == dropped) {
						dropped.type = Heap.Type.LOCKED_CHEST;
						addItemToSpawn(new GoldenKey(Dungeon.getDepth()));
					}
				}
			} else {
				Heap dropped = drop( toDrop, cell );

				if (toDrop instanceof Weapon && ((Weapon) toDrop).tier == 6){
					type = Heap.Type.EBONY_CHEST;
				}
				dropped.type = type;
				if (type == Heap.Type.SKELETON){
					dropped.setHauntedIfCursed();
				}
			}
			
		}

		for (Item item : itemsToSpawn) {
			Heap.Type type = Heap.Type.HEAP;
			if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.CHESTS)
				type = Heap.Type.CHEST;
			int cell = randomDropCell();
			if (Dungeon.isChallenged(Challenges.REDUCED_POWER)){
				if (Dungeon.depth > 1 && findMob(cell) == null
					&& (item instanceof ScrollOfUpgrade || item instanceof PotionOfStrength ||
						item instanceof Stylus || item instanceof Torch)){
					mobs.add(Mimic.spawnAt(cell, item));
				} else {
					drop(item, cell).type = type;
					if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
						map[cell] = Terrain.GRASS;
						losBlocking[cell] = false;
					}
				}
			}
			else {
				drop(item, cell).type = type;
				if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
					map[cell] = Terrain.GRASS;
					losBlocking[cell] = false;
				}
			}
		}

		//use a separate generator for this to prevent held items, meta progress, and talents from affecting levelgen
		//we can use a random long for the seed as it will be the same long every time
		Random.pushGenerator( Random.Long() );

		Item item = Bones.get();
		if (item != null) {
			int cell = randomDropCell();
			if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
				map[cell] = Terrain.GRASS;
				losBlocking[cell] = false;
			}
			drop( item, cell ).setHauntedIfCursed().type = Heap.Type.REMAINS;
		}

		DriedRose rose = Dungeon.hero.belongings.getItem( DriedRose.class );
		if (rose != null && rose.isIdentified() && !rose.cursed){
			//aim to drop 1 petal every 2 floors
			int petalsNeeded = (int) Math.ceil((float)((Dungeon.getDepth() / 2) - rose.droppedPetals) / 3);

			for (int i=1; i <= petalsNeeded; i++) {
				//the player may miss a single petal and still max their rose.
				if (rose.droppedPetals < 11) {
					item = new DriedRose.Petal();
					int cell = randomDropCell();
					Heap.Type type = Heap.Type.HEAP;
					if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.CHESTS)
						type = Heap.Type.CHEST;
					drop( item, cell ).type = type;
					if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
						map[cell] = Terrain.GRASS;
						losBlocking[cell] = false;
					}
					rose.droppedPetals++;
				}
			}
		}

		//cached rations try to drop in a special room on floors 2/3/4/6/7/8, to a max of 4/6 (3/5 for rogue, but actual rations)
		if (Dungeon.hero.hasTalent(Talent.CACHED_RATIONS,Talent.ROYAL_PRIVILEGE)) {
			Talent.CachedRationsDropped dropped = Buff.affect(Dungeon.hero, Talent.CachedRationsDropped.class);
			int large = Dungeon.hero.pointsInTalent(Talent.CACHED_RATIONS),
					small = Dungeon.hero.pointsInTalent(Talent.ROYAL_PRIVILEGE);
			if (small > 0) small = (small + 1) * 2;
			if (large > 0) large++;
			int total = small + large;
			if (dropped.count() < total) {
				int cell;
				int tries = 100;
				boolean valid;
				do {
					cell = randomDropCell(SpecialRoom.class);
					valid = cell != -1 && !(room(cell) instanceof SecretRoom)
							&& !(room(cell) instanceof ShopRoom)
							&& map[cell] != Terrain.EMPTY_SP
							&& map[cell] != Terrain.WATER
							&& map[cell] != Terrain.PEDESTAL;
				} while (tries-- > 0 && !valid);
				if (valid) {
					if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
						map[cell] = Terrain.GRASS;
						losBlocking[cell] = false;
					}
					// rogue gets regular food.
					if (Dungeon.isChallenged(Challenges.NO_VEGAN)){
					drop( new MysteryMeat(), cell).type = Heap.Type.CHEST;
				}
				else
					drop( dropped.count() < large ? new Food() : new SmallRation(), cell).type = Heap.Type.CHEST;
				dropped.countUp(1);}
			}
		}

		//guide pages
		Collection<String> allPages = Document.ADVENTURERS_GUIDE.pageNames();
		ArrayList<String> missingPages = new ArrayList<>();
		for ( String page : allPages){
			if (!Document.ADVENTURERS_GUIDE.isPageFound(page)){
				missingPages.add(page);
			}
		}

		//a total of 6 pages drop randomly, the rest are specially dropped or are given at the start
		missingPages.remove(Document.GUIDE_SEARCHING);

		//chance to find a page is 0/25/50/75/100% for floors 1/2/3/4/5+
		float dropChance = 0.25f*(Dungeon.getDepth()-1);
		if (!missingPages.isEmpty() && Random.Float() < dropChance){
			GuidePage p = new GuidePage();
			p.page(missingPages.get(0));
			int cell = randomDropCell();
			Heap.Type type = Heap.Type.HEAP;
			if (Dungeon.specialSeed == DungeonSeed.SpecialSeed.CHESTS)
				type = Heap.Type.CHEST;
			if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
				map[cell] = Terrain.GRASS;
				losBlocking[cell] = false;
			}
			drop( p, cell ).type = type;
		}

		Random.popGenerator();

	}
	
	public ArrayList<Room> rooms() {
		return new ArrayList<>(rooms);
	}
	
	//FIXME pit rooms shouldn't be problematic enough to warrant this
	public boolean hasPitRoom(){
		for (Room r : rooms) {
			if (r instanceof PitRoom) {
				return true;
			}
		}
		return false;
	}
	
	protected Room randomRoom( Class<?extends Room> type ) {
		Random.shuffle( rooms );
		for (Room r : rooms) {
			if (type.isInstance(r)) {
				return r;
			}
		}
		return null;
	}
	
	public Room room( int pos ) {
		for (Room room : rooms) {
			if (room.inside( cellToPoint(pos) )) {
				return room;
			}
		}
		
		return null;
	}

	protected int randomDropCell(){
		return randomDropCell(StandardRoom.class);
	}
	
	protected int randomDropCell( Class<?extends Room> roomType ) {
		int tries = 100;
		while (tries-- > 0) {
			Room room = randomRoom( roomType );
			if (room != null && room != roomEntrance) {
				int pos = pointToCell(room.random());
				if (passable[pos] && !solid[pos]
						&& pos != exit()
						&& heaps.get(pos) == null
						&& (Dungeon.isChallenged(Challenges.TOO_MANY_MOBS) || findMob(pos) == null)) {
					
					Trap t = traps.get(pos);
					
					//items cannot spawn on traps which destroy items
					if (t == null ||
							! (t instanceof BurningTrap || t instanceof BlazingTrap
							|| t instanceof ChillingTrap || t instanceof FrostTrap
							|| t instanceof ExplosiveTrap || t instanceof DisintegrationTrap)) {
						
						return pos;
					}
				}
			}
		}
		return -1;
	}
	
	@Override
	public int fallCell( boolean fallIntoPit ) {
		if (Dungeon.isChallenged(Challenges.TOO_MANY_MOBS)){
			HashSet<Integer> cells = new HashSet<>();
			for (Room r : rooms) {
				if(r instanceof SpecialRoom) continue;
				for (int i = r.left; i < r.right; i++) {
					for (int j = r.top; j < r.bottom; j++) {
						int c = pointToCell(new Point(i, j));
						if (passable[c] && !solid[c]) {
							cells.add(c);
						}
					}
				}
			}
			for (Mob mob : mobs) {
				cells.remove(mob.pos);
			}
			return Random.element(cells);
		}
		if (fallIntoPit) {
			for (Room room : rooms) {
				if (room instanceof PitRoom) {
					int result;
					do {
						result = pointToCell(room.random());
					} while (traps.get(result) != null
							|| findMob(result) != null
							|| heaps.get(result) != null);
					return result;
				}
			}
		}
		
		return super.fallCell( false );
	}

	@Override
	public boolean isLevelExplored( int depth ) {
		//A level is considered fully explored if:

		//There are no levelgen heaps which are undiscovered, in an openable container, or which contain keys
		for (Heap h : heaps.valueList()){
			if (h.autoExplored) continue;

			if (!h.seen || (h.type != Heap.Type.HEAP && h.type != Heap.Type.FOR_SALE && h.type != Heap.Type.CRYSTAL_CHEST)){
				return false;
			}
			for (Item i : h.items){
				if (i instanceof Key){
					return false;
				}
			}
		}

		//There is no magical fire or sacrificial fire
		for (Blob b : blobs.values()){
			if (b.volume > 0 && (b instanceof MagicalFireRoom.EternalFire || b instanceof SacrificialFire)){
				return false;
			}
		}

		//There are no statues or mimics (unless they were made allies)
		for (Mob m : mobs.toArray(new Mob[0])){
			if (m.alignment != Char.Alignment.ALLY && (m instanceof Statue || m instanceof Mimic)){
				return false;
			}
		}

		//There are no barricades, locked doors, or hidden doors
		for (int i = 0; i < length; i++){
			if (map[i] == Terrain.BARRICADE || map[i] == Terrain.LOCKED_DOOR || map[i] == Terrain.SECRET_DOOR){
				return false;
			}
		}

		//There are no unused keys for this depth in the journal
		for (Notes.KeyRecord rec : Notes.getRecords(Notes.KeyRecord.class)){
			if (rec.depth() == depth){
				return false;
			}
		}

		//Note that it is NOT required for the player to see every tile or discover every trap.
		return true;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( "rooms", rooms );
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		
		rooms = new ArrayList<>( (Collection<Room>) ((Collection<?>) bundle.getCollection( "rooms" )) );
		for (Room r : rooms) {
			r.onLevelLoad( this );
			if (r instanceof EntranceRoom ){
				roomEntrance = r;
			} else if (r instanceof ExitRoom ){
				roomExit = r;
			}
		}
	}
	
}
