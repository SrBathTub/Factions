package com.massivecraft.factions.cmd.type;

import java.util.Collection;

import org.bukkit.command.CommandSender;

import com.massivecraft.factions.Selector;
import com.massivecraft.factions.SelectorType;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.command.type.Type;
import com.massivecraft.massivecore.command.type.TypeAbstract;

public class TypeSelector extends TypeAbstract<Selector>
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private Type<MPlayer> typeMPlayer = TypeMPlayer.get();
	private TypeRank typeRank = TypeRank.get();
	private TypeRel typeRel = TypeRel.get();
	private TypeFaction typeFaction = TypeFaction.get();
	
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static TypeSelector i = new TypeSelector();
	public static TypeSelector get() { return i; }
	private TypeSelector()
	{
		super(Selector.class);
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public Selector read(String arg, CommandSender sender) throws MassiveException
	{
		if (arg == null) throw new MassiveException().setMsg("selector can't be null.");
		if (arg.length() < 2) throw new MassiveException().setMsg("selector must be longer than two characters.");
		
		// Get Prefix
		SelectorType prefix = this.getPrefix(arg);
		return prefix != null ? this.readPrefixed(arg, sender, prefix) : this.readPrioritized(arg, sender);
	}
	
	private Selector readPrefixed(String arg, CommandSender sender, SelectorType prefix) throws MassiveException
	{
		// Cut off prefix length
		arg = arg.substring(prefix.getPrefix().length());
		
		// Use correct type to read the selector
		switch (prefix)
		{
			case RANK:
				return typeRank.read(arg, sender);
			case RELATION:
				return typeRel.read(arg, sender);
			case PLAYER:
				return typeMPlayer.read(arg, sender);
			case FACTION:
				return typeFaction.read(arg, sender);
			default:
				throw new MassiveException().setMsg("<b>An error has occurred with the Selector prefix <h>%s", prefix);
		}
	}
	
	private Selector readPrioritized(String arg, CommandSender sender) throws MassiveException
	{
		Selector ret;
		
		// Try Relation
		ret = readSafe(arg, sender, this.typeRel);
		if (ret != null) return ret;
		
		// Try Faction
		ret = readSafe(arg, sender, this.typeFaction);
		if (ret != null) return ret;
		
		// Try Rank
		ret = readSafe(arg, sender, this.typeRank);
		if (ret != null) return ret;
		
		// Try Player
		ret = readSafe(arg, sender, this.typeMPlayer);
		if (ret != null) return ret;
		
		// Error
		throw new MassiveException().setMsg("<h>%s<b> did not match any selector.", arg);
	}
	
	@Override
	public Collection<String> getTabList(CommandSender sender, String arg)
	{
		// Create
		Collection<String> ret = new MassiveList<>();
		
		// Fill
		ret.addAll(typeFaction.getTabList(sender, arg));
		ret.addAll(typeMPlayer.getTabList(sender, arg));
		// TODO: Add in once that TypeRank is actual rank: ret.addAll(typeRank.getTabList(sender, arg));
		ret.addAll(typeRel.getTabList(sender, arg));
		
		// Return
		return ret;
	}
	
	private SelectorType getPrefix(String arg)
	{
		for (SelectorType selectorType : SelectorType.values())
		{
			if (arg.startsWith(selectorType.getPrefix())) return selectorType;
		}
		return null;
	}
	
	// -------------------------------------------- //
	// SAFE READING
	// -------------------------------------------- //
	
	public Selector readSafe(String arg, CommandSender sender) { return readSafe(arg, sender, this); }
	private static <T> T readSafe(String arg, CommandSender sender, Type<T> type)
	{
		try
		{
			return type.read(arg, sender);
		}
		catch (MassiveException e)
		{
			return null;
		}
	}
	
}
