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

package com.zrp200.rkpd2.windows;

import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Chrome;
import com.zrp200.rkpd2.actors.hero.Belongings;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.mobs.npcs.Blacksmith;
import com.zrp200.rkpd2.items.Item;
import com.zrp200.rkpd2.items.bags.Bag;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.scenes.GameScene;
import com.zrp200.rkpd2.scenes.PixelScene;
import com.zrp200.rkpd2.ui.ItemSlot;
import com.zrp200.rkpd2.ui.RedButton;
import com.zrp200.rkpd2.ui.RenderedTextBlock;
import com.zrp200.rkpd2.ui.Window;

public class WndBlacksmith extends Window {

	private static final int BTN_SIZE	= 36;
	private static final float GAP		= 2;
	private static final float BTN_GAP	= 10;
	private static final int WIDTH		= 116;
	
	private ItemButton btnPressed;
	
	private ItemButton btnItem1;
	private ItemButton btnItem2;
	private RedButton btnReforge;
	
	public WndBlacksmith( Blacksmith troll, Hero hero ) {
		
		super();
		
		IconTitle titlebar = new IconTitle();
		titlebar.icon( troll.sprite() );
		titlebar.label( Messages.titleCase( troll.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );
		
		RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, "prompt"), 6 );
		message.maxWidth( WIDTH);
		message.setPos(0, titlebar.bottom() + GAP);
		add( message );
		
		btnItem1 = new ItemButton() {
			@Override
			protected void onClick() {
				btnPressed = btnItem1;
				GameScene.selectItem( itemSelector );
			}
		};
		btnItem1.setRect( (WIDTH - BTN_GAP) / 2 - BTN_SIZE, message.top() + message.height() + BTN_GAP, BTN_SIZE, BTN_SIZE );
		add( btnItem1 );
		
		btnItem2 = new ItemButton() {
			@Override
			protected void onClick() {
				btnPressed = btnItem2;
				GameScene.selectItem( itemSelector );
			}
		};
		btnItem2.setRect( btnItem1.right() + BTN_GAP, btnItem1.top(), BTN_SIZE, BTN_SIZE );
		add( btnItem2 );
		
		btnReforge = new RedButton( Messages.get(this, "reforge") ) {
			@Override
			protected void onClick() {
				Blacksmith.upgrade( btnItem1.item, btnItem2.item );
				hide();
			}
		};
		btnReforge.enable( false );
		btnReforge.setRect( 0, btnItem1.bottom() + BTN_GAP, WIDTH, 20 );
		add( btnReforge );
		
		
		resize( WIDTH, (int)btnReforge.bottom() );
	}
	
	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(WndBlacksmith.class, "select");
		}

		@Override
		public Class<?extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item.isUpgradable();
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && btnPressed.parent != null) {
				btnPressed.item( item );
				
				if (btnItem1.item != null && btnItem2.item != null) {
					String result = Blacksmith.verify( btnItem1.item, btnItem2.item );
					if (result != null) {
						GameScene.show( new WndMessage( result ) );
						btnReforge.enable( false );
					} else {
						btnReforge.enable( true );
					}
				}
			}
		}
	};
	
	public static class ItemButton extends Component {
		
		protected NinePatch bg;
		protected ItemSlot slot;
		
		public Item item = null;
		
		@Override
		protected void createChildren() {
			super.createChildren();
			
			bg = Chrome.get( Chrome.Type.RED_BUTTON);
			add( bg );
			
			slot = new ItemSlot() {
				@Override
				protected void onPointerDown() {
					bg.brightness( 1.2f );
					Sample.INSTANCE.play( Assets.Sounds.CLICK );
				}
				@Override
				protected void onPointerUp() {
					bg.resetColor();
				}
				@Override
				protected void onClick() {
					ItemButton.this.onClick();
				}
			};
			slot.enable(true);
			add( slot );
		}
		
		protected void onClick() {}
		
		@Override
		protected void layout() {
			super.layout();
			
			bg.x = x;
			bg.y = y;
			bg.size( width, height );
			
			slot.setRect( x + 2, y + 2, width - 4, height - 4 );
		}
		
		public void item( Item item ) {
			slot.item( this.item = item );
		}

		public void clear(){
			slot.clear();
		}
	}
}
