package com.massivecraft.factions;

public enum SelectorType
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	PLAYER("p"),
	FACTION("f"),
	RANK("ra"),
	RELATION("re"),
	
	// END OF LIST
	;
	
	// -------------------------------------------- //
	// FIELD
	// -------------------------------------------- //
	
	private final String prefix;
	public String getPrefix() { return this.prefix; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	SelectorType(String prefix)
	{
		this.prefix = prefix + ":";
	}
	
}
