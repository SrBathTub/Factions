package com.massivecraft.factions.cmd;

public class CmdFactionsPerm extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsPermList cmdFactionsPermList = new CmdFactionsPermList();
	public CmdFactionsPermShow cmdFactionsPermShow = new CmdFactionsPermShow();
	public CmdFactionsPermSet cmdFactionsPermSet = new CmdFactionsPermSet();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsPerm()
	{
		// Children
		this.addChild(this.cmdFactionsPermList);
		this.addChild(this.cmdFactionsPermShow);
		this.addChild(this.cmdFactionsPermSet);
	}
	
}
