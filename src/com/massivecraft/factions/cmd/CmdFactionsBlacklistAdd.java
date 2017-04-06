package com.massivecraft.factions.cmd;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Selector;
import com.massivecraft.factions.cmd.type.TypeSelector;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.type.TypeNullable;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

public class CmdFactionsBlacklistAdd extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsBlacklistAdd()
	{
		// Parameters
		this.addParameter(TypeSelector.get(), "selector");
		this.addParameter(TypeNullable.get(TypeString.get()), "reason");
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameters
		Selector selector = this.readArg();
		String reason = this.readArg();
		
		// TODO: Take "used" Faction
		Faction faction = msender.getFaction();
		
		// Add
		faction.addToBlacklist(selector);
		
		// Inform
		message(mson(TypeSelector.get().getVisualMson(selector), " was blacklisted.").color(ChatColor.YELLOW));
		if (reason != null) msg("<i>Reason: %s", reason);
	}

}
