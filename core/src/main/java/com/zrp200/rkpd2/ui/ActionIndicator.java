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

package com.zrp200.rkpd2.ui;

import com.watabou.input.GameAction;
import com.watabou.noosa.Image;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.SPDAction;
import com.zrp200.rkpd2.actors.buffs.*;
import com.zrp200.rkpd2.items.artifacts.CloakOfShadows;
import com.zrp200.rkpd2.items.artifacts.KromerCloak;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.PixelScene;

public class ActionIndicator extends Tag {

	Image icon;

	public static Action action;
	public static ActionIndicator instance;

	public ActionIndicator() {
		super( 0xFFFF4C );

		instance = this;

		setSize( SIZE, SIZE );
		visible = false;
	}
	
	@Override
	public GameAction keyAction() {
		return SPDAction.TAG_ACTION;
	}
	
	@Override
	public void destroy() {
		super.destroy();
		instance = null;

		longClickListener.destroy();
	}
	
	@Override
	protected synchronized void layout() {
		super.layout();
		
		if (icon != null){
			if (!flipped)   icon.x = x + (SIZE - icon.width()) / 2f + 1;
			else            icon.x = x + width - (SIZE + icon.width()) / 2f - 1;
			icon.y = y + (height - icon.height()) / 2f;
			PixelScene.align(icon);
			if (!members.contains(icon))
				add(icon);
		}
	}
	
	private boolean needsLayout = false;
	
	@Override
	public synchronized void update() {
		super.update();

		if (!Dungeon.hero.ready){
			if (icon != null) icon.alpha(0.5f);
		} else {
			if (icon != null) icon.alpha(1f);
		}

		if (!visible && action != null){
			visible = true;
			updateIcon();
			flash();
		} else {
			visible = action != null;
		}
		
		if (needsLayout){
			layout();
			needsLayout = false;
		}
	}

	@Override
	protected void onClick() {
		if (action != null && Dungeon.hero.ready) {
			action.doAction();
		}
	}

	@Override
	protected String hoverText() {
		String text = (action == null ? null : action.actionName());
		if (text != null){
			return Messages.titleCase(text);
		} else {
			return null;
		}
	}

	@Override
	protected boolean onLongClick() {
		return findAction(true);
	}

	private final Button longClickListener = new Button() {
		@Override public GameAction keyAction() { return SPDAction.TAG_CYCLE; }
		@Override protected void onClick()      { findAction(true); }
	};

	public static boolean setAction(Action action){
		if(action == null || !action.usable() || ActionIndicator.action == action) return false;
		ActionIndicator.action = action;
		updateIcon();
		return true;
	}

	// list of action buffs that we should replace it with.
	private static final Class<?extends Buff>[] actionBuffClasses = new Class[]{Preparation.class, CloakOfShadows.cloakRecharge.class, KromerCloak.cloakStealth.class, SnipersMark.class, Combo.class, Momentum.class, SpiritBuff.class, RobotBuff.class, RKChampionBuff.class, BrawlerBuff.class};
	private static boolean findAction(boolean cycle) {
		if(action == null) cycle = false;
		int start = -1;
		if(cycle) while(++start < actionBuffClasses.length && !actionBuffClasses[start].isInstance(action));

		for(int i = (start+1)%actionBuffClasses.length; i != start && i < actionBuffClasses.length; i++) {
			for(Buff b : Dungeon.hero.buffs(actionBuffClasses[i])) {
				if( setAction( (Action) b ) ) return true;
			}
			if(cycle && i+1 == actionBuffClasses.length) i = -1;
		}
		return false;
	}

	public static void clearAction(Action action){
		if(ActionIndicator.action == action) {
			ActionIndicator.action = null;
			findAction(false);
		}
	}

	public static void updateIcon(){
		if (instance != null){
			synchronized (instance) {
				if (instance.icon != null) {
					instance.icon.killAndErase();
					instance.icon = null;
				}
				if (action != null) {
					instance.icon = action.actionIcon();
					instance.needsLayout = true;
				}
			}
		}
	}

	public interface Action{

		public default String actionName() {
			return Messages.get(this, "action_name");
		}

		public Image actionIcon();

		public void doAction();

		default boolean usable() { return true; }
		default boolean isSelectable() { return action != this && usable(); }
	}

}
