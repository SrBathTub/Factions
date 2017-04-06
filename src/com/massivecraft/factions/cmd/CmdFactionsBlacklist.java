package com.massivecraft.factions.cmd;

public class CmdFactionsBlacklist extends FactionsCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public CmdFactionsBlacklistList cmdFactionsBlacklistList = new CmdFactionsBlacklistList();
	public CmdFactionsBlacklistAdd cmdFactionsBlacklistAdd = new CmdFactionsBlacklistAdd();
	public CmdFactionsBlacklistRemove cmdFactionsBlacklistRemove = new CmdFactionsBlacklistRemove();
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsBlacklist()
	{
		// Children
		this.addChild(this.cmdFactionsBlacklistList);
		this.addChild(this.cmdFactionsBlacklistAdd);
		this.addChild(this.cmdFactionsBlacklistRemove);
	}
	
}
