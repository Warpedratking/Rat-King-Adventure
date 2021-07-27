package com.zrp200.rkpd2.actors.mobs;

import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.*;
import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Challenges;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.actors.Actor;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.blobs.*;
import com.zrp200.rkpd2.actors.buffs.*;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.actors.hero.abilities.Ratmogrify;
import com.zrp200.rkpd2.actors.hero.abilities.rat_king.Wrath;
import com.zrp200.rkpd2.effects.*;
import com.zrp200.rkpd2.effects.particles.GodfireParticle;
import com.zrp200.rkpd2.effects.particles.SparkParticle;
import com.zrp200.rkpd2.items.Heap;
import com.zrp200.rkpd2.items.Item;
import com.zrp200.rkpd2.items.artifacts.DriedRose;
import com.zrp200.rkpd2.items.scrolls.ScrollOfTeleportation;
import com.zrp200.rkpd2.items.wands.*;
import com.zrp200.rkpd2.items.weapon.SpiritBow;
import com.zrp200.rkpd2.items.weapon.enchantments.Unstable;
import com.zrp200.rkpd2.items.weapon.missiles.PhantomSpear;
import com.zrp200.rkpd2.levels.Terrain;
import com.zrp200.rkpd2.levels.traps.*;
import com.zrp200.rkpd2.mechanics.Ballistica;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.sprites.CharSprite;
import com.zrp200.rkpd2.sprites.MissileSprite;
import com.zrp200.rkpd2.sprites.RatKingBossSprite;
import com.zrp200.rkpd2.tiles.DungeonTilemap;
import com.zrp200.rkpd2.ui.BossHealthBar;
import com.zrp200.rkpd2.utils.BArray;
import com.zrp200.rkpd2.utils.GLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class RatKingBoss extends Mob {

    public int phase = -1;
    public static final int EMPEROR = 0;
    public static final int GLADIATOR = 1;
    public static final int BATTLEMAGE = 2;
    public static final int ASSASSIN = 3;
    public static final int SNIPER = 4;

    enum MagicAttack{
        MAGIC_MISSILE(0xFFFFFF, MagicMissile.MAGIC_MISSILE),
        FIREBLAST(0xff7f00, MagicMissile.FIRE_CONE),
        FROST(0x66b3ff, MagicMissile.FROST),
        CORROSION(0xae7b49, MagicMissile.CORROSION),
        POISON(0x57135d, MagicMissile.WARD),
        LIGHTNING(0x87e6e6, MagicMissile.FROGGIT),
        BLAST_WAVE(0xb3a284, MagicMissile.FORCE),
        PRISMATIC(0xcccccc, MagicMissile.RAINBOW),
        RED_FIRE(0xff0000, MagicMissile.BLOOD_CONE),
        RAT_KING(0xffc61a, MagicMissile.ELMO);

        int color;
        int boltType;

        MagicAttack(int color, int boltType){
            this.color = color;
            this.boltType = boltType;
        }
    }
    public MagicAttack attack;
    public int magicCastPos = -1;
    public int phantomSpearPositions = -1;

    {
        HP = HT = 1500 + Challenges.activeChallenges()*165;
        spriteClass = RatKingBossSprite.class;

        HUNTING = new Hunting();
        state = HUNTING;
        alignment = Alignment.ENEMY;

        viewDistance = 12;
        defenseSkill = 35;

        properties.add(Property.BOSS);
        properties.add(Property.MINIBOSS);
    }
    private float summonCooldown;
    private static final int MIN_SUMMON_CD = 3;
    private static final int MAX_SUMMON_CD = 6;

    private float abilityCooldown;
    private static final int MIN_ABILITY_CD = 5;
    private static final int MAX_ABILITY_CD = 10;
    private ArrayList<Integer> targetedCells = new ArrayList<>();
    private int lastHeroPos;

    public static class EmperorRat extends Ratmogrify.SummonedRat{
        {
            alignment = Alignment.ENEMY;
        }
    }

    public static class EmperorAlbinoRat extends Ratmogrify.SummonedAlbino{
        {
            alignment = Alignment.ENEMY;
        }
    }

    private ArrayList<Class> regularSummons = new ArrayList<>();
    {
        if (Dungeon.isChallenged(Challenges.SWARM_INTELLIGENCE)){
            for (int i = 0; i < 6; i++){
                if (i >= 3){
                    regularSummons.add(ThreadRipper.class);
                }
                if (i >= 4){
                    regularSummons.add(EmperorAlbinoRat.class);
                }
                regularSummons.add(EmperorRat.class);
            }
        } else {
            for (int i = 0; i < 8; i++){
                if (i >= 6){
                    regularSummons.add(ThreadRipper.class);
                }
                regularSummons.add(EmperorRat.class);
            }
        }
        Random.shuffle(regularSummons);
    }

    @Override
    public boolean canAttack(Char enemy) {
        if (phase == BATTLEMAGE) return false;
        if (phase == SNIPER) return new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
        if (phase == EMPEROR) return false;
        return super.canAttack(enemy);
    }

    @Override
    public float attackDelay() {
        if (phase == ASSASSIN) return super.attackDelay()*0.5f;
        return super.attackDelay();
    }

    @Override
    public float speed() {
        if (phase == GLADIATOR){
            return super.speed()*1.5f;
        }
        if (phase == ASSASSIN){
            return super.speed()*2f;
        }
        return super.speed();
    }

    @Override
    public int attackProc(Char enemy, int damage) {
        if (phase == GLADIATOR){
            if (Dungeon.isChallenged(Challenges.NO_SCROLLS) && Random.Int(2) == 0)
                Buff.prolong(enemy, PowerfulDegrade.class, 3f);
            if (Dungeon.isChallenged(Challenges.NO_ARMOR) && Random.Int(2) == 0) {
                Buff.prolong(enemy, Vulnerable.class, 3f);
                Buff.prolong(enemy, Charm.class, 2f).object = id();
            }
            if (Dungeon.isChallenged(Challenges.NO_FOOD) && Random.Int(2) == 0){
                if (enemy instanceof Hero){
                    enemy.buff(Hunger.class).affectHunger(-30);
                }
                int healAmt = Math.round(damage*0.75f);
                healAmt = Math.min( healAmt, enemy.HT - enemy.HP );

                if (healAmt > 0 && isAlive()) {
                    HP += healAmt;
                    sprite.emitter().start( Speck.factory( Speck.HEALING ), 0.4f, 1 );
                    sprite.showStatus( CharSprite.POSITIVE, Integer.toString( healAmt ) );
                }
            }
        }
        if (phase == ASSASSIN && Dungeon.level.adjacent(pos, Dungeon.hero.pos)){
            ((Hunting)state).teleport();
        } else if (phase == ASSASSIN){
            if (Dungeon.isChallenged(Challenges.NO_SCROLLS) && Random.Int(2) == 0)
                Buff.prolong(enemy, PowerfulDegrade.class, 5f);
            if (Dungeon.isChallenged(Challenges.NO_ARMOR) && Random.Int(2) == 0) {
                Buff.prolong(enemy, Vulnerable.class, 2f);
                Buff.prolong(enemy, Charm.class, 3f).object = id();
            }
        }
        if (phase == SNIPER){
            Buff.prolong(enemy, Vulnerable.class, 4f);
            Unstable.getRandomEnchant(new SpiritBow()).proc(new SpiritBow(), this, Dungeon.hero, damageRoll());
            if (Dungeon.isChallenged(Challenges.NO_SCROLLS) && Random.Int(2) == 0)
                Buff.prolong(enemy, PowerfulDegrade.class, 5f);
            if (Dungeon.isChallenged(Challenges.NO_ARMOR) && Random.Int(2) == 0) {
                Buff.prolong(enemy, Vulnerable.class, 2f);
                Buff.prolong(enemy, Charm.class, 3f).object = id();
            }
        }
        return super.attackProc(enemy, damage);
    }

    @Override
    public void damage(int dmg, Object src) {
        dmg *= Math.max(0.45f, 1 - (HP * 1.f / HT));
        if (phase == ASSASSIN){
            dmg *= 1.75f;
        }
        super.damage(dmg, src);
    }

    @Override
    public int drRoll() {
        return Random.NormalIntRange(0 + phase == GLADIATOR ? 10 : 0, 22 + phase == GLADIATOR ? 10 : 0);
    }

    @Override
    public int damageRoll() {
        if (phase == GLADIATOR){
            return Random.NormalIntRange(24, 64);
        }
        if (phase == ASSASSIN){
            return Random.NormalIntRange(12, 28);
        }
        if (phase == SNIPER){
            return Random.NormalIntRange(5, 14);
        }
        return super.damageRoll();
    }

    @Override
    public void notice() {
        super.notice();
        Buff.affect(this, PhaseTracker.class, 0);
        if (!BossHealthBar.isAssigned()) {
            BossHealthBar.assignBoss(this);
            if (HP <= HT/2) BossHealthBar.bleed(true);
            if (HP == HT) {
                for (Char ch : Actor.chars()){
                    if (ch instanceof DriedRose.GhostHero){
                        ((DriedRose.GhostHero) ch).sayBoss();
                    }
                }
            } else {
                yell(Messages.get(this, "notice_have", Dungeon.hero.name()));
            }
        }
    }

    @Override
    public void die( Object cause ) {

        for (Mob mob : (Iterable<Mob>)Dungeon.level.mobs.clone()) {
            if (mob instanceof Ratmogrify.SummonedRat || mob instanceof ThreadRipper) {
                mob.die( cause );
            }
        }

        GameScene.bossSlain();
        Dungeon.level.unseal();
        super.die( cause );
    }

    public void changePhase(){
        if (++phase > 4) phase = 0;
        ((RatKingBossSprite)sprite).changeSprite(phase);
    }

    public static class PhaseTracker extends FlavourBuff{
        @Override
        public void detach() {
            super.detach();
            ((RatKingBoss)target).changePhase();
            Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 2f, 0.85f);
            Buff.affect(target, PhaseTracker.class,
                    (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 35 : 20));
            for (Heap heap : Dungeon.level.heaps.valueList()){
                for (Item i :heap.items){
                    if (i instanceof Tengu.ShockerAbility.ShockerItem){
                        heap.remove(i);
                    }
                }
            }
        }
    }

    @Override
    public void spend(float time) {
        time *= GameMath.gate(0.66f, HP * 2f / HT, 1f);
        super.spend(time);
    }

    @Override
    public int attackSkill(Char target) {
        return 45;
    }

    public boolean haventSeen = true;

    @Override
    protected boolean act() {
        if (Dungeon.level.heroFOV[pos] && haventSeen) {
            notice();
            haventSeen = false;
        }
        if (phase == EMPEROR){
            while (summonCooldown <= 0){

                Class<?extends Mob> cls = regularSummons.remove(0);
                Mob summon = Reflection.newInstance(cls);
                regularSummons.add(cls);

                int spawnPos = -1;
                for (int i : PathFinder.NEIGHBOURS8){
                    if (Actor.findChar(pos+i) == null){
                        if (spawnPos == -1 || Dungeon.level.trueDistance(Dungeon.hero.pos, spawnPos) > Dungeon.level.trueDistance(Dungeon.hero.pos, pos+i)){
                            spawnPos = pos + i;
                        }
                    }
                }

                if (spawnPos != -1) {
                    summon.pos = spawnPos;
                    GameScene.add( summon );
                    Actor.addDelayed( new Pushing( summon, pos, summon.pos ), -1 );
                    summon.beckon(Dungeon.hero.pos);

                    summonCooldown += Random.NormalFloat(MIN_SUMMON_CD, MAX_SUMMON_CD);
                } else {
                    break;
                }
            }
            if (summonCooldown > 0) summonCooldown--;
        }
        return super.act();
    }

    protected boolean doAttack(Char enemy ) {
        if (phase == SNIPER) {
                if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
                    sprite.zap(enemy.pos);
                    return false;
                } else {
                    zap();
                    return true;
                }
        }
        return super.doAttack(enemy);
    }

    private void zap() {
        spend( 0.66f );

        attack(enemy);
    }

    public void onZapComplete() {
        zap();
        next();
    }

    @Override
    public String description() {
        String description = super.description();
        switch (phase){
            case EMPEROR:
                description += "\n\n" + Messages.get(this, "emperor");
                if (Dungeon.isChallenged(Challenges.SWARM_INTELLIGENCE)){
                    description += "\n" + Messages.get(this, "faster_summons");
                }
                break;
            case GLADIATOR:
                description += "\n\n" + Messages.get(this, "gladiator");
                if (Dungeon.isChallenged(Challenges.NO_FOOD)){
                    description += "\n" + Messages.get(this, "healing");
                }
                break;
            case BATTLEMAGE:
                description += "\n\n" + Messages.get(this, "battlemage"); break;
            case ASSASSIN:
                description += "\n\n" + Messages.get(this, "assassin");
                if (Dungeon.isChallenged(Challenges.DARKNESS)){
                    description += "\n" + Messages.get(this, "dark");
                }
                break;
            case SNIPER:
                description += "\n\n" + Messages.get(this, "sniper"); break;
        }
        if (Dungeon.isChallenged(Challenges.NO_SCROLLS)){
            description += "\n\n" + Messages.get(this, "runes");
        }
        if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
            description += "\n\n" + Messages.get(this, "bad_boss");
        }
        if (Dungeon.isChallenged(Challenges.NO_ARMOR)){
            description += "\n\n" + Messages.get(this, "armor");
        }
        return description;
    }

    private static final String PHASE = "phase";
    private static final String HAVESEEN = "haveseen";
    private static final String ATTACK = "attack";
    private static final String MAGIC_POS = "magicPos";
    private static final String SPEAR_POS = "spearPos";
    private static final String REGULAR_SUMMONS = "summons";
    private static final String SUMMON_CD = "summonCD";
    private static final String TARGETED_CELLS = "targetCells";
    private static final String TARGETED_DEST = "targetDest";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(PHASE, phase);
        bundle.put(HAVESEEN, haventSeen);
        bundle.put(ATTACK, attack);
        bundle.put(MAGIC_POS, magicCastPos);
        bundle.put(SPEAR_POS, phantomSpearPositions);
        bundle.put(REGULAR_SUMMONS, regularSummons.toArray(new Class[0]));
        bundle.put(SUMMON_CD, summonCooldown);
        int[] bundleArr = new int[targetedCells.size()];
        for (int i = 0; i < targetedCells.size(); i++){
            bundleArr[i] = targetedCells.get(i);
        }
        bundle.put(TARGETED_CELLS, bundleArr);
        bundle.put(TARGETED_DEST, lastHeroPos);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        phase = bundle.getInt(PHASE);
        haventSeen = bundle.getBoolean(HAVESEEN);
        attack = bundle.getEnum(ATTACK, MagicAttack.class);
        magicCastPos = bundle.getInt(MAGIC_POS);
        phantomSpearPositions = bundle.getInt(SPEAR_POS);
        regularSummons.clear();
        Collections.addAll(regularSummons, bundle.getClassArray(REGULAR_SUMMONS));
        summonCooldown = bundle.getFloat(SUMMON_CD);
        for (int i : bundle.getIntArray(TARGETED_CELLS)){
            targetedCells.add(i);
        }
        lastHeroPos = bundle.getInt(TARGETED_DEST);
    }

    public static class SniperCurse extends FlavourBuff{
    }

    //rat king is always hunting
    private class Hunting extends Mob.Hunting{

        public boolean doCharging(){
            int oldPos = pos;
            if (target != -1 && getCloser( target )) {

                if (Dungeon.level.water[pos] && buff(ChampionEnemy.Flowing.class) != null){
                    spend(0.01f / speed());
                }
                else spend( 1 / speed() );
                return moveSprite( oldPos,  pos );

            } else {

                spend( TICK );
                return true;
            }
        }

        public boolean doMagic(){
            if (magicCastPos == -1) {
                magicCastPos = enemy.pos;
                ArrayList<MagicAttack> possibleAttacks = new ArrayList<>(Arrays.asList(
                   MagicAttack.MAGIC_MISSILE, MagicAttack.FIREBLAST, MagicAttack.FROST,
                   MagicAttack.POISON, MagicAttack.BLAST_WAVE, MagicAttack.LIGHTNING
                ));
                if (Dungeon.isChallenged(Challenges.DARKNESS))
                    possibleAttacks.add(MagicAttack.PRISMATIC);
                if (Dungeon.isChallenged(Challenges.NO_HERBALISM))
                    possibleAttacks.add(MagicAttack.RED_FIRE);
                if (Dungeon.isChallenged(Challenges.NO_HEALING))
                    possibleAttacks.add(MagicAttack.CORROSION);
                if (Dungeon.isChallenged(Challenges.NO_SCROLLS))
                    possibleAttacks.add(MagicAttack.RAT_KING);
                attack = Random.element(possibleAttacks);
                Game.scene().addToFront(new TargetedCell(magicCastPos, attack.color));
                spend(1 / GameMath.gate(0.66f, HP * 2f / HT, 1f));
                return true;
            } else if (magicCastPos != -1) {
                sprite.zap(magicCastPos, () -> {
                            MagicMissile.boltFromChar(sprite.parent, attack.boltType, sprite, magicCastPos,
                                    () -> {
                                        next();
                                        Char ch = Actor.findChar(magicCastPos);
                                        switch (attack) {
                                            case MAGIC_MISSILE:
                                                if (ch != null) {
                                                    ch.damage(Random.NormalIntRange(15, 30), new WandOfMagicMissile());
                                                }
                                                break;
                                            case FIREBLAST:
                                                for (int i : PathFinder.NEIGHBOURS4) {
                                                    if (!Dungeon.level.solid[magicCastPos + i]) {
                                                        GameScene.add(Blob.seed(magicCastPos + i, 3, Fire.class));
                                                    }
                                                }
                                                if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                    GameScene.add(Blob.seed(magicCastPos, 40, Inferno.class));
                                                }
                                                if (ch != null) {
                                                    ch.damage((int) (Random.NormalIntRange(10, 23) *
                                                            (1 + 0.125f * Dungeon.hero.pointsInTalent(Talent.PYROMANIAC))), new WandOfFireblast());
                                                }
                                                break;
                                            case FROST:
                                                for (int k : PathFinder.NEIGHBOURS4) {
                                                    if (!Dungeon.level.solid[magicCastPos + k]) {
                                                        GameScene.add(Blob.seed(magicCastPos + k, 3, Freezing.class));
                                                    }
                                                }
                                                if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                    GameScene.add(Blob.seed(magicCastPos, 40, Blizzard.class));
                                                }
                                                if (ch != null) {
                                                    ch.damage(Random.NormalIntRange(7, 19), new WandOfFrost());
                                                    Buff.affect(ch, FrostBurn.class).reignite(ch);
                                                }
                                                break;
                                            case POISON:
                                                if (ch != null) {
                                                    ch.damage(Random.NormalIntRange(5, 21), new Poison());
                                                    Buff.affect(ch, Poison.class).set(20);
                                                }
                                                if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                    GameScene.add(Blob.seed(magicCastPos, 200, ToxicGas.class));
                                                }
                                                break;
                                            case LIGHTNING:
                                                if (ch != null) {
                                                    ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 5);
                                                    ch.damage(Random.NormalIntRange(24, 45), new WandOfLightning());
                                                    ch.sprite.parent.addToFront(new Lightning(ch.pos, ch.pos, null));
                                                }
                                                if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                    for (int k : PathFinder.NEIGHBOURS4) {
                                                        if (!Dungeon.level.solid[magicCastPos + k]) {
                                                            GameScene.add(Blob.seed(magicCastPos + k, 3, Electricity.class));
                                                        }
                                                    }
                                                }
                                                break;
                                            case CORROSION:
                                                if (ch != null) {
                                                    ch.damage(Random.NormalIntRange(8, 20), new WandOfCorrosion());
                                                    Buff.affect(ch, Corrosion.class).set(3, 6);
                                                }
                                                if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                    GameScene.add(Blob.seed(magicCastPos, 100, CorrosiveGas.class));
                                                }
                                                break;
                                            case PRISMATIC:
                                                if (ch != null) {
                                                    ch.damage(Random.NormalIntRange(10, 22), new WandOfPrismaticLight());
                                                    Buff.affect(ch, Blindness.class, 10f);
                                                }
                                                if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                    GameScene.add(Blob.seed(magicCastPos, 100, SmokeScreen.class));
                                                }
                                                break;
                                            case BLAST_WAVE:
                                                WandOfBlastWave.BlastWave.blast(magicCastPos);
                                                for (int i : PathFinder.NEIGHBOURS4){
                                                    Char c = Actor.findChar(magicCastPos+i);
                                                    if (c != null) {
                                                        Ballistica trajectory = new Ballistica(pos, c.pos, Ballistica.STOP_TARGET);
                                                        trajectory = new Ballistica(trajectory.collisionPos, trajectory.path.get(trajectory.path.size() - 1), Ballistica.PROJECTILE);
                                                        WandOfBlastWave.throwChar(c, trajectory, 3, true);
                                                        if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                            Buff.affect(c, Paralysis.class, 1f);
                                                        }
                                                    }
                                                }
                                                break;
                                            case RED_FIRE:
                                                for (int i : PathFinder.NEIGHBOURS4) {
                                                    if (!Dungeon.level.solid[magicCastPos + i]) {
                                                        GameScene.add(Blob.seed(magicCastPos + i, 3, GodSlayerFire.class));
                                                    }
                                                }
                                                if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
                                                    GameScene.add(Blob.seed(magicCastPos, 400, Inferno.class));
                                                }
                                                if (ch != null) {
                                                    ch.damage((int) (Random.NormalIntRange(15, 33) *
                                                            (1 + 0.125f * Dungeon.hero.pointsInTalent(Talent.PYROMANIAC))), new GodfireParticle());
                                                }
                                                break;
                                            case RAT_KING:
                                                sprite.parent.add(new Beam.RatRay(sprite.center(), DungeonTilemap.raisedTileCenterToWorld(magicCastPos)));
                                                if (ch != null) {
                                                    ch.damage(Random.NormalIntRange(45, 80), new Wrath());
                                                    Buff.affect(ch, PowerfulDegrade.class, 20f);
                                                }
                                                break;
                                        }
                                        magicCastPos = -1;
                                    });
                        });
                Sample.INSTANCE.play(Assets.Sounds.ZAP);
                spend(1f);

                return false;
            } else {
                spend( TICK );
                return true;
            }
        }

        public boolean doRogue(){
            if (Random.Int(3) == 0 || phantomSpearPositions != -1){
                return teleport();
            } else {
                return doCharging();
            }
        }

        public boolean teleport() {
            sprite.operate(enemy.pos, () -> {});
            if (phantomSpearPositions == -1) {
                PathFinder.buildDistanceMap(enemy.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null),
                        Dungeon.isChallenged(Challenges.DARKNESS) ? 0 : 1);
                int k;
                do {
                    k = Random.Int(PathFinder.distance.length);
                } while (PathFinder.distance[k] >= Integer.MAX_VALUE);
                Ballistica path = new Ballistica(pos, k, Ballistica.STOP_CHARS | Ballistica.STOP_TARGET);
                int collisionPos = path.collisionPos;
                if (collisionPos != Dungeon.hero.pos) {
                    //create a new collision pos
                    int newSourcePos = path.sourcePos;
                    for (int l = 0; l < path.path.size(); l++) {
                        if (path.path.get(l) == collisionPos) {
                            if (l - 1 > -1) {
                                newSourcePos = path.path.get(l - 1);
                            }
                        }
                    }
                    Ballistica path2 = new Ballistica(newSourcePos, path.collisionPos, Ballistica.MAGIC_BOLT);
                    collisionPos = path2.collisionPos;
                }
                phantomSpearPositions = collisionPos;
                Ballistica path2 = new Ballistica(pos, collisionPos, Ballistica.STOP_TARGET);
                for (int l = 0; l < path2.dist + 2; l++) {
                    Game.scene().addToFront(new TargetedCell(path2.path.get(l), 0xa9ded3));
                }
                spend(TICK*2);
                next();
                return true;
            }
            else sprite.doAfterAnim( () -> {
                    final int psp = phantomSpearPositions;
                    Callback callback = new Callback() {
                        @Override
                        public void call() {
                            Char ch = Actor.findChar(psp);
                            if (ch != null && ch != RatKingBoss.this) {
                                attack(ch);
                                Buff.detach(ch, Light.class);
                                if (Dungeon.isChallenged(Challenges.DARKNESS)) {
                                    Buff.affect(ch, Blindness.class, 10f);
                                }
                            }
                            next();
                        }
                    };
                    if (psp != -1) {
                        MissileSprite m = sprite.parent.recycle(MissileSprite.class);
                        Game.scene().addToFront(m);
                        m.reset(sprite, psp, new PhantomSpear(), callback);
                    }

                CellEmitter.bottom(pos).burst(Speck.factory(Speck.WOOL, true), 5);
                Sample.INSTANCE.play(Assets.Sounds.PUFF);
                do {
                    pos = Dungeon.level.randomDestination(RatKingBoss.this);
                } while (Actor.findChar(pos) == Dungeon.hero || Actor.findChar(pos) == enemy);
                ScrollOfTeleportation.appear(RatKingBoss.this, pos);
                Actor.addDelayed(new Actor() {
                    @Override
                    protected boolean act() {
                        switch (Random.Int(2)){
                            case 0:
                                if (buff(Tengu.BombAbility.class) == null)
                                    Tengu.throwBomb(RatKingBoss.this, enemy); break;
                            case 1:
                                if (buff(Tengu.BombAbility.class) == null)
                                    Tengu.throwShocker(RatKingBoss.this, enemy); break;
                        }
                        diactivate();
                        return true;
                    }
                }, 1);



                phantomSpearPositions = -1;
            });

            return false;
        }

        /*for (int i = 0; i < 3; i++){
            int collisionPos = new Ballistica(pos, phantomSpearPositions[i], Ballistica.MAGIC_BOLT).collisionPos;
            MissileSprite m = sprite.parent.recycle(MissileSprite.class);
            int finalI = i;
            m.reset(sprite, collisionPos, new PhantomSpear(),
                    () -> {
                        Char ch = Actor.findChar(collisionPos);
                        if (ch != null) {
                            attack(ch);
                            Buff.detach(ch, Light.class);
                            if (Dungeon.isChallenged(Challenges.DARKNESS)) {
                                Buff.affect(ch, Blindness.class, 10f);
                                phantomSpearPositions[finalI] = -1;
                            }
                        }
                    });
            m.alpha(0.5f);
        }
        next();
    }*/

        public void doYogLasers(){
            boolean terrainAffected = false;
            HashSet<Char> affected = new HashSet<>();
            //delay fire on a rooted hero
            if (!enemy.rooted) {
                for (int i : targetedCells) {
                    Ballistica b = new Ballistica(i, lastHeroPos, Ballistica.WONT_STOP);
                    //shoot beams
                    sprite.parent.add(new Beam.RatRay(DungeonTilemap.raisedTileCenterToWorld(i), DungeonTilemap.raisedTileCenterToWorld(b.collisionPos)));
                    for (int p : b.path) {
                        Char ch = Actor.findChar(p);
                        if (ch != null && (ch.alignment != alignment || ch instanceof Bee)) {
                            affected.add(ch);
                        }
                        if (Dungeon.level.flamable[p]) {
                            Dungeon.level.destroy(p);
                            GameScene.updateMap(p);
                            terrainAffected = true;
                        }
                    }
                }
                if (terrainAffected) {
                    Dungeon.observe();
                }
                for (Char ch : affected) {
                    if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
                        ch.damage(Random.NormalIntRange(40, 69), new Eye.DeathGaze());
                    } else {
                        ch.damage(Random.NormalIntRange(60, 121), new Eye.DeathGaze());
                    }
                    if (Dungeon.isChallenged(Challenges.NO_SCROLLS) && ch instanceof Hero){
                        Buff.affect(ch, PowerfulDegrade.class, 12f);
                    }

                    if (Dungeon.level.heroFOV[pos]) {
                        ch.sprite.flash();
                        CellEmitter.center(pos).burst(Speck.factory(Speck.COIN), Random.IntRange(2, 3));
                    }
                    if (!ch.isAlive() && ch == Dungeon.hero) {
                        Dungeon.fail(getClass());
                        GLog.n(Messages.get(Char.class, "kill", name()));
                    }
                }
                targetedCells.clear();
            }

            if (abilityCooldown <= 0 && HP < HT*0.8f){
                lastHeroPos = enemy.pos;

                int beams = (int) (4 + (HP * 1.0f / HT)*4);
                for (int i = 0; i < beams; i++){
                    int randompos = Random.Int(Dungeon.level.width()) + Dungeon.level.width()*2;
                    targetedCells.add(randompos);
                }

                for (int i : targetedCells){
                    Ballistica b = new Ballistica(i, Dungeon.hero.pos, Ballistica.WONT_STOP);
                    for (int p : b.path){
                        Game.scene().addToFront(new TargetedCell(p, 0xFF0000));
                    }
                }

                spend(TICK*1.5f);
                Dungeon.hero.interrupt();

                abilityCooldown += Random.NormalFloat(MIN_ABILITY_CD - 2*(1 - (HP * 1f / HT)), MAX_ABILITY_CD - 5*(1 - (HP * 1f / HT)));
            } else {
                spend(TICK);
            }
            if (abilityCooldown > 0) abilityCooldown--;
        }

        public boolean doSniper(){
            if (enemySeen && !isCharmedBy( enemy ) && canAttack( enemy )) {
                target = enemy.pos;
                return doAttack(Dungeon.hero);
            } else {
                target = enemy.pos;
                if (enemy.buff(SniperCurse.class) == null) {
                    Buff.affect(enemy, SniperCurse.class, 6f);
                } else if (enemy.buff(SniperCurse.class).cooldown() < 2){
                    int bestPos = enemy.pos;
                    for (int i : PathFinder.NEIGHBOURS8){
                        if (Dungeon.level.passable[pos + i]
                                && Actor.findChar(pos+i) == null
                                && Dungeon.level.trueDistance(pos+i, enemy.pos) > Dungeon.level.trueDistance(bestPos, enemy.pos)){
                            bestPos = pos+i;
                        }
                    }

                    if (enemy.buff(MagicImmune.class) != null){
                        bestPos = enemy.pos;
                    }

                    if (bestPos != enemy.pos){
                        ScrollOfTeleportation.appear(enemy, bestPos);
                        if (enemy instanceof Hero){
                            ((Hero) enemy).interrupt();
                            Dungeon.observe();
                        }
                    }
                }
            }
            int traps = Random.IntRange(1, 4);
            for (int i = 0; i < traps; i++){
                int trapPos;
                do {
                    trapPos = Dungeon.level.randomDestination(RatKingBoss.this);
                } while (Actor.findChar(pos) == Dungeon.hero || Actor.findChar(pos) == enemy);
                Trap t = ((Trap) Reflection.newInstance(Random.element(trapClasses)));
                Dungeon.level.setTrap(t, trapPos);
                Dungeon.level.map[t.pos] = t.visible ? Terrain.TRAP : Terrain.SECRET_TRAP;
                t.reveal();
            }
            spend(TICK);
            return true;
        }

        protected Class[] trapClasses = new Class[]{
                FrostTrap.class, StormTrap.class, CorrosionTrap.class,  DisintegrationTrap.class,
                RockfallTrap.class, FlashingTrap.class, GuardianTrap.class,
                DisarmingTrap.class,
                WarpingTrap.class, CursingTrap.class, GrimTrap.class, PitfallTrap.class, DistortionTrap.class };

        public boolean doEmperor(){
            spend(TICK);
            return true;
        }

        //prevents rare infinite loop cases
        private boolean recursing = false;

        @Override
        public boolean act(boolean enemyInFOV, boolean justAlerted) {

            enemySeen = enemyInFOV;
            if (enemyInFOV && !isCharmedBy( enemy ) && canAttack( enemy )) {
                target = Dungeon.hero.pos;
                return doAttack(enemy);
            } else {
                if (enemyInFOV) {
                    target = enemy.pos;
                } else {
                    chooseEnemy();
                    if (enemy == null){
                        //if nothing else can be targeted, target hero
                        enemy = Dungeon.hero;
                    }
                    target = enemy.pos;
                }
            }
            if (phase == GLADIATOR) return doCharging();
            if (phase == BATTLEMAGE) return doMagic();
            if (phase == ASSASSIN) return doRogue();
            if (phase == SNIPER) {
                doYogLasers();
                return doSniper();
            }


            spend( TICK );
            return true;
        }

    }


}
