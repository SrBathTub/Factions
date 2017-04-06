package com.massivecraft.factions.cmd;

import java.util.List;

import org.bukkit.Bukkit;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPermColl;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.pager.Msonifier;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.predicate.Predicate;

public class CmdFactionsPermList extends FactionsCommand
{
	// -------------------------------------------- //
	// REUSABLE PREDICATE
	// CONSTANTS
	// -------------------------------------------- //
	
	private static final Predicate<MPerm> PREDICATE_MPERM_VISIBLE = new Predicate<MPerm>()
	{
		@Override
		public boolean apply(MPerm mperm)
		{
			return mperm.isVisible();
		}
	};
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsPermList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameter
		int page = this.readArg();
		
		// TODO: Change to "used" Faction
		final Faction faction = msender.getFaction();
		
		// Pager create
		String title = String.format("Perms for %s", msenderFaction.describeTo(msender));
		final Pager<MPerm> pager = new Pager<>(this, title, page, new Msonifier<MPerm>()
		{
			@Override
			public Mson toMson(MPerm mperm, int index)
			{
				return faction.getPermittedLine(mperm);
			}
		});
		
		final Predicate<MPerm> predicate = msender.isOverriding() ? null : PREDICATE_MPERM_VISIBLE;
		Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), new Runnable()
		{
			@Override
			public void run()
			{
				// Get items
				List<MPerm> items = MPermColl.get().getAll(predicate);
				
				// Pager items
				pager.setItems(items);
				
				// Pager message
				pager.message();
			}
		});
	}
	
}