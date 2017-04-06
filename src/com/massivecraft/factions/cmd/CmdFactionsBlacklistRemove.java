package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Selector;
import com.massivecraft.factions.cmd.type.TypeSelector;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.MassiveException;

public class CmdFactionsBlacklistRemove extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsBlacklistRemove()
	{
		// Parameters
		this.addParameter(TypeSelector.get(), "selector");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameters
		Selector selector = this.readArg();
		
		// TODO: Take "used" Faction
		Faction faction = msender.getFaction();
		
		// Remove
		faction.removeFromBlacklist(selector);
		
		// Inform
		message(mson(TypeSelector.get().getVisualMson(selector), " was un-blacklisted.").color(ChatColor.YELLOW));
	}

}
