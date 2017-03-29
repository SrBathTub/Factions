package com.massivecraft.factions.util;

import com.massivecraft.factions.RelationParticipator;
import com.massivecraft.factions.entity.Board;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.massivecraft.massivecore.mson.Mson.EMPTY;
import static com.massivecraft.massivecore.mson.Mson.SPACE;
import static com.massivecraft.massivecore.mson.Mson.mson;

public class AsciiMap
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	private static final char[] FACTION_KEY_CHARS = "\\/#?ç¬£$%=&^ABCDEFGHJKLMNOPQRSTUVWXYZÄÖÜÆØÅ1234567890abcdeghjmnopqrsuvwxyÿzäöüæøåâêîûô".toCharArray();
	private static final int KEY_SIZE = FACTION_KEY_CHARS.length;
	
	// Map Heights & Widths
	private static final int WIDTH = 49;
	private static final int WIDTH_HALF = 24;
	private static final int HEIGHT = 8;
	private static final int HEIGHT_HALF = 4;
	private static final int HEIGHT_FULL = 17;
	private static final int HEIGHT_FULL_HALF = 8;
	
	private static final String TITLE_FORMAT = "(%d,%d) %s";
	private static final Mson KEY_MIDDLE = mson("+").color(ChatColor.AQUA);
	private static final Mson KEY_WILDERNESS = mson("-").color(ChatColor.GRAY).tooltip();
	private static final Mson KEY_OVERFLOW = mson("-").style(ChatColor.MAGIC).add(mson("").style(ChatColor.RESET));
	private static final Mson OVERFLOW_MESSAGE = Mson.format("%s: Too Many Factions (>%d) on this Map.", KEY_OVERFLOW.toPlain(true), FACTION_KEY_CHARS.length);
	private static final Mson LEGEND_SEPARATOR = mson(": ");
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	private final RelationParticipator relationParticipator;
	public RelationParticipator getRelationParticipator() { return this.relationParticipator; }
	
	private final double angle;
	public double getAngle() { return this.angle; }
	
	private final PS center;
	public PS getCenter() { return this.center; }
	
	private final PS topLeft;
	public PS getTopLeft() { return this.topLeft; }
	
	private final Board board;
	public Board getBoard() { return this.board; }
	
	private final Map<Faction, Mson> factionChars = new HashMap<>();
	public Map<Faction, Mson> getFactionChars() { return this.factionChars; }
	
	private final int height;
	private int getHeight() { return this.height; }
	
	private final int heightHalf;
	private int getHeightHalf() { return this.heightHalf; }
	
	private boolean overflown = false;
	public boolean isOverflown() { return this.overflown; }
	public void setOverflown(boolean overflown) { this.overflown = overflown; }
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public AsciiMap(RelationParticipator relationParticipator, Location location, boolean fullHeight)
	{
		this.relationParticipator = relationParticipator;
		this.angle = location.getYaw();
		this.center = PS.valueOf(location).getChunk(true);
		this.height = fullHeight ? HEIGHT_FULL : HEIGHT;
		this.heightHalf = fullHeight ? HEIGHT_FULL_HALF : HEIGHT_HALF;
		this.topLeft = center.plusChunkCoords(-WIDTH_HALF, -this.heightHalf);
		this.board = BoardColl.get().get(center.getWorld());
	}
	
	// -------------------------------------------- //
	// RENDER
	// -------------------------------------------- //
	
	public List<Mson> render()
	{
		// Create
		List<Mson> ret = new ArrayList<>();
		
		// Fill
		ret.add(this.getTitle());
		ret.addAll(this.getLines());
		ret.add(this.getFactionLegend());
		
		// Return
		return ret;
	}
	
	private Mson getTitle()
	{
		// Prepare
		PS chunk = this.getCenter();
		Faction faction = this.getBoard().getFactionAt(chunk);
		int chunkX = chunk.getChunkX();
		int chunkZ = chunk.getChunkZ();
		String factionName = faction.getName(this.getRelationParticipator());
		
		// Titleize
		return Txt.titleize(String.format(TITLE_FORMAT, chunkX, chunkZ, factionName));
	}
	
	private List<Mson> getLines()
	{
		// Create
		List<Mson> ret = new MassiveList<>();
		List<String> asciiCompass = AsciiCompass.getAsciiCompass(this.getAngle());
		
		// Fill
		for (int deltaZ = 0; deltaZ < this.getHeight(); deltaZ++)
		{
			ret.add(this.getLine(deltaZ, asciiCompass));
		}
		
		// Return
		return ret;
	}
	
	private Mson getLine(int deltaZ, List<String> asciiCompass)
	{
		// Create
		boolean isCompassLine = deltaZ < asciiCompass.size();
		Mson ret = isCompassLine ? mson(asciiCompass.get(deltaZ)) : EMPTY;
		Mson factionChar;
		
		// Fill
		for (int deltaX = isCompassLine ? 3 : 0; deltaX < WIDTH; deltaX++)
		{
			boolean isMiddle = deltaX == WIDTH_HALF && deltaZ == this.getHeightHalf();
			factionChar = isMiddle ? KEY_MIDDLE : this.getCharFaction(deltaZ, deltaX);
			ret = ret.add(factionChar);
		}
		
		// Return
		return ret;
	}
	
	private Mson getCharFaction(int deltaZ, int deltaX)
	{
		// Calculate overflow
		int index = this.getFactionChars().size();
		if (!this.isOverflown() && index >= KEY_SIZE) this.setOverflown(true);
		
		PS herePs = this.getTopLeft().plusChunkCoords(deltaX, deltaZ);
		Faction hereFaction = this.getBoard().getFactionAt(herePs);
		boolean contains = this.getFactionChars().containsKey(hereFaction);
		
		// Is Wilderness or known?
		if (hereFaction.isNone()) return KEY_WILDERNESS;
		if (contains) return this.getFactionChars().get(hereFaction);
		
		// Create descriptions
		ChatColor color = hereFaction.getColorTo(this.getRelationParticipator());
		String name = hereFaction.getName(this.getRelationParticipator());
		String tooltip = color.toString() + name;
		
		// Is overflown?
		if (this.isOverflown()) return KEY_OVERFLOW.tooltip(tooltip);
		
		// Create new one
		Mson factionChar = mson(String.valueOf(FACTION_KEY_CHARS[index])).color(color);
		factionChar = factionChar.tooltip(tooltip);
		
		// Store for later use
		this.getFactionChars().put(hereFaction, factionChar);
		
		// Return
		return factionChar;
	}
	
	private Mson getFactionLegend()
	{
		// Create
		List<Mson> ret = new MassiveList<>();
		
		// Fill
		for (Entry<Faction, Mson> entry : this.getFactionChars().entrySet())
		{
			Faction here = entry.getKey();
			Mson factionChar = entry.getValue();
			ChatColor color = here.getColorTo(this.getRelationParticipator());
			
			ret.add(mson(factionChar, LEGEND_SEPARATOR, here.getName()).color(color));
		}
		
		// Add overflown message if needed
		if (this.isOverflown()) ret.add(OVERFLOW_MESSAGE);
		
		// Return
		return Mson.implode(ret, SPACE);
	}
	
}
