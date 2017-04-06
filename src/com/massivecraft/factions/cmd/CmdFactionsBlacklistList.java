package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Selector;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeSelector;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.pager.Msonifier;
import com.massivecraft.massivecore.pager.Pager;

public class CmdFactionsBlacklistList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsBlacklistList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
		this.addParameter(TypeFaction.get(), "faction", "your");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameter
		final int page = this.readArg();
		final Faction faction = this.readArg();
		final MPlayer mplayer = msender;
		final TypeSelector type = TypeSelector.get();
		
		// Pager create
		String title = "Blacklist for " + faction.describeTo(mplayer);
		final Pager<String> pager = new Pager<>(this, title, page, new Msonifier<String>()
		{
			@Override
			public Mson toMson(String item, int index)
			{
				Selector selector = type.readSafe(item, sender);
				return type.getVisualMson(selector, sender);
			}
		});
		
		// Pager message
		pager.messageAsync();
	}

}
