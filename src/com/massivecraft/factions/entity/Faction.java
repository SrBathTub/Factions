package com.massivecraft.factions.entity;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsIndex;
import com.massivecraft.factions.FactionsParticipator;
import com.massivecraft.factions.Selector;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.RelationParticipator;
import com.massivecraft.factions.SelectorType;
import com.massivecraft.factions.cmd.CmdFactions;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeSelector;
import com.massivecraft.factions.predicate.PredicateCommandSenderFaction;
import com.massivecraft.factions.predicate.PredicateMPlayerRole;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.util.RelationUtil;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.collections.MassiveMap;
import com.massivecraft.massivecore.collections.MassiveMapDef;
import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.collections.MassiveSetDef;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.money.Money;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.predicate.Predicate;
import com.massivecraft.massivecore.predicate.PredicateAnd;
import com.massivecraft.massivecore.predicate.PredicateVisibleTo;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.store.Entity;
import com.massivecraft.massivecore.store.SenderColl;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;

public class Faction extends Entity<Faction> implements FactionsParticipator
{
	// -------------------------------------------- //
	// CONSTANTS
	// -------------------------------------------- //
	
	public static final transient String NODESCRIPTION = Txt.parse("<em><silver>no description set");
	public static final transient String NOMOTD = Txt.parse("<em><silver>no message of the day set");
	
	// -------------------------------------------- //
	// META
	// -------------------------------------------- //
	
	public static Faction get(Object oid)
	{
		return FactionColl.get().get(oid);
	}
	
	// -------------------------------------------- //
	// OVERRIDE: ENTITY
	// -------------------------------------------- //
	
	@Override
	public Faction load(Faction that)
	{
		this.setName(that.name);
		this.setDescription(that.description);
		this.setMotd(that.motd);
		this.setCreatedAtMillis(that.createdAtMillis);
		this.setHome(that.home);
		this.setPowerBoost(that.powerBoost);
		this.setInvitedPlayerIds(that.invitedPlayerIds);
		this.setRelationWishes(that.relationWishes);
		this.setFlagIds(that.flags);
		this.setPermIds(that.perms);
		
		return this;
	}
	
	@Override
	public void preDetach(String id)
	{
		if (!this.isLive()) return;
		
		// NOTE: Existence check is required for compatibility with some plugins.
		// If they have money ...
		if (Money.exists(this))
		{
			// ... remove it.
			Money.set(this, null, 0);	
		}
	}
	
	// -------------------------------------------- //
	// FIELDS: RAW
	// -------------------------------------------- //
	// In this section of the source code we place the field declarations only.
	// Each field has it's own section further down since just the getter and setter logic takes up quite some place.
	
	// The actual faction id looks something like "54947df8-0e9e-4471-a2f9-9af509fb5889" and that is not too easy to remember for humans.
	// Thus we make use of a name. Since the id is used in all foreign key situations changing the name is fine.
	// Null should never happen. The name must not be null.
	private String name = null;
	
	// Factions can optionally set a description for themselves.
	// This description can for example be seen in territorial alerts.
	// Null means the faction has no description.
	private String description = null;
	
	// Factions can optionally set a message of the day.
	// This message will be shown when logging on to the server.
	// Null means the faction has no motd
	private String motd = null;
	
	// We store the creation date for the faction.
	// It can be displayed on info pages etc.
	private long createdAtMillis = System.currentTimeMillis();
	
	// Factions can optionally set a home location.
	// If they do their members can teleport there using /f home
	// Null means the faction has no home.
	private PS home = null;
	
	// Factions usually do not have a powerboost. It defaults to 0.
	// The powerBoost is a custom increase/decrease to default and maximum power.
	// Null means the faction has powerBoost (0).
	private Double powerBoost = null;
	
	// Can anyone join the Faction?
	// If the faction is open they can.
	// If the faction is closed an invite is required.
	// Null means default.
	// private Boolean open = null;
	
	// This is the ids of the invited players.
	// They are actually "senderIds" since you can invite "@console" to your faction.
	// Null means no one is invited
	private MassiveSetDef<String> invitedPlayerIds = new MassiveSetDef<>();
	
	// The keys in this map are factionIds.
	// Null means no special relation whishes.
	private MassiveMapDef<String, Rel> relationWishes = new MassiveMapDef<>();
	
	// The flag overrides are modifications to the default values.
	// Null means default.
	private MassiveMapDef<String, Boolean> flags = new MassiveMapDef<>();

	// The perm overrides are modifications to the default values.
	// Null means default.
	private MassiveMapDef<String, Set<String>> perms = new MassiveMapDef<>();
	
	// The perm blacklist of which selectors are not allowed in any way.
	private MassiveSetDef<String> permBlacklist = new MassiveSetDef<>();
	
	// -------------------------------------------- //
	// FIELD: id
	// -------------------------------------------- //
	
	// FINER
	
	public boolean isNone()
	{
		return this.getId().equals(Factions.ID_NONE);
	}
	
	public boolean isNormal()
	{
		return ! this.isNone();
	}
	
	// -------------------------------------------- //
	// FIELD: name
	// -------------------------------------------- //
	
	// RAW
	
	@Override
	public String getName()
	{
		String ret = this.name;
		
		if (MConf.get().factionNameForceUpperCase)
		{
			ret = ret.toUpperCase();
		}
		
		return ret;
	}
	
	public void setName(String name)
	{
		// Clean input
		String target = name;
		
		// Detect Nochange
		if (MUtil.equals(this.name, target)) return;

		// Apply
		this.name = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public String getComparisonName()
	{
		return MiscUtil.getComparisonString(this.getName());
	}
	
	public String getName(String prefix)
	{
		return prefix + this.getName();
	}
	
	public String getName(RelationParticipator observer)
	{
		if (observer == null) return getName();
		return this.getName(this.getColorTo(observer).toString());
	}
	
	// -------------------------------------------- //
	// FIELD: description
	// -------------------------------------------- //
	
	// RAW
	
	public boolean hasDescription()
	{
		return this.description != null;
	}
	
	public String getDescription()
	{
		if (this.hasDescription()) return this.description;
		return NODESCRIPTION;
	}
	
	public void setDescription(String description)
	{
		// Clean input
		String target = description;
		if (target != null)
		{
			target = target.trim();
			if (target.isEmpty())
			{
				target = null;
			}
		}
		
		// Detect Nochange
		if (MUtil.equals(this.description, target)) return;

		// Apply
		this.description = target;
		
		// Mark as changed
		this.changed();
	}
	
	// -------------------------------------------- //
	// FIELD: motd
	// -------------------------------------------- //
	
	// RAW
	
	public boolean hasMotd()
	{
		return this.motd != null;
	}
	
	public String getMotd()
	{
		if (this.hasMotd()) return Txt.parse(this.motd);
		return NOMOTD;
	}
	
	public void setMotd(String description)
	{
		// Clean input
		String target = description;
		if (target != null)
		{
			target = target.trim();
			if (target.isEmpty())
			{
				target = null;
			}
		}
		
		// Detect Nochange
		if (MUtil.equals(this.motd, target)) return;

		// Apply
		this.motd = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public List<Object> getMotdMessages()
	{
		// Create
		List<Object> ret = new MassiveList<>();
		
		// Fill
		Object title = this.getName() + " - Message of the Day";
		title = Txt.titleize(title);
		ret.add(title);
		
		String motd = Txt.parse("<i>" + this.getMotd());
		ret.add(motd);
		
		ret.add("");
		
		// Return
		return ret;
	}
	
	// -------------------------------------------- //
	// FIELD: createdAtMillis
	// -------------------------------------------- //
	
	public long getCreatedAtMillis()
	{
		return this.createdAtMillis;
	}
	
	public void setCreatedAtMillis(long createdAtMillis)
	{
		// Clean input
		long target = createdAtMillis;
		
		// Detect Nochange
		if (MUtil.equals(this.createdAtMillis, createdAtMillis)) return;

		// Apply
		this.createdAtMillis = target;
		
		// Mark as changed
		this.changed();
	}
	
	// -------------------------------------------- //
	// FIELD: home
	// -------------------------------------------- //
	
	public PS getHome()
	{
		this.verifyHomeIsValid();
		return this.home;
	}
	
	public void verifyHomeIsValid()
	{
		if (this.isValidHome(this.home)) return;
		this.home = null;
		this.changed();
		msg("<b>Your faction home has been un-set since it is no longer in your territory.");
	}
	
	public boolean isValidHome(PS ps)
	{
		if (ps == null) return true;
		if (!MConf.get().homesMustBeInClaimedTerritory) return true;
		if (BoardColl.get().getFactionAt(ps) == this) return true;
		return false;
	}
	
	public boolean hasHome()
	{
		return this.getHome() != null;
	}
	
	public void setHome(PS home)
	{
		// Clean input
		PS target = home;
		
		// Detect Nochange
		if (MUtil.equals(this.home, target)) return;
		
		// Apply
		this.home = target;
		
		// Mark as changed
		this.changed();
	}
	
	// -------------------------------------------- //
	// FIELD: powerBoost
	// -------------------------------------------- //
	
	// RAW
	@Override
	public double getPowerBoost()
	{
		Double ret = this.powerBoost;
		if (ret == null) ret = 0D;
		return ret;
	}
	
	@Override
	public void setPowerBoost(Double powerBoost)
	{
		// Clean input
		Double target = powerBoost;
		
		if (target == null || target == 0) target = null;
		
		// Detect Nochange
		if (MUtil.equals(this.powerBoost, target)) return;
		
		// Apply
		this.powerBoost = target;
		
		// Mark as changed
		this.changed();
	}
	
	// -------------------------------------------- //
	// FIELD: open
	// -------------------------------------------- //
	
	// Nowadays this is a flag!
	
	@Deprecated
	public boolean isDefaultOpen()
	{
		return MFlag.getFlagOpen().isStandard();
	}
	
	@Deprecated
	public boolean isOpen()
	{
		return this.getFlag(MFlag.getFlagOpen());
	}
	
	@Deprecated
	public void setOpen(Boolean open)
	{
		MFlag flag = MFlag.getFlagOpen();
		if (open == null) open = flag.isStandard();
		this.setFlag(flag, open);
	}
	
	// -------------------------------------------- //
	// FIELD: invitedPlayerIds
	// -------------------------------------------- //
	
	// RAW
	
	public Set<String> getInvitedPlayerIds()
	{
		return this.invitedPlayerIds;
	}
	
	public void setInvitedPlayerIds(Collection<String> invitedPlayerIds)
	{
		// Clean input
		MassiveSetDef<String> target = new MassiveSetDef<>(invitedPlayerIds);
		
		// Detect Nochange
		if (MUtil.equals(this.invitedPlayerIds, target)) return;
		
		// Apply
		this.invitedPlayerIds = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public boolean isInvited(String playerId)
	{
		return this.getInvitedPlayerIds().contains(playerId);
	}
	
	public boolean isInvited(MPlayer mplayer)
	{
		return this.isInvited(mplayer.getId());
	}
	
	public boolean setInvited(String playerId, boolean invited)
	{
		List<String> invitedPlayerIds = new MassiveList<>(this.getInvitedPlayerIds());
		boolean ret;
		if (invited)
		{
			ret = invitedPlayerIds.add(playerId);
		}
		else
		{
			ret = invitedPlayerIds.remove(playerId);
		}
		this.setInvitedPlayerIds(invitedPlayerIds);
		return ret;
	}
	
	public void setInvited(MPlayer mplayer, boolean invited)
	{
		this.setInvited(mplayer.getId(), invited);
	}
	
	public List<MPlayer> getInvitedMPlayers()
	{
		List<MPlayer> mplayers = new MassiveList<>();
		
		for (String id : this.getInvitedPlayerIds())
		{	
			MPlayer mplayer = MPlayer.get(id);
			mplayers.add(mplayer);
		}
		
		return mplayers;
	}
	
	// -------------------------------------------- //
	// FIELD: relationWish
	// -------------------------------------------- //
	
	// RAW
	
	public Map<String, Rel> getRelationWishes()
	{
		return this.relationWishes;
	}
	
	public void setRelationWishes(Map<String, Rel> relationWishes)
	{
		// Clean input
		MassiveMapDef<String, Rel> target = new MassiveMapDef<>(relationWishes);
		
		// Detect Nochange
		if (MUtil.equals(this.relationWishes, target)) return;
		
		// Apply
		this.relationWishes = target;
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public Rel getRelationWish(String factionId)
	{
		Rel ret = this.getRelationWishes().get(factionId);
		if (ret == null) ret = Rel.NEUTRAL;
		return ret;
	}
	
	public Rel getRelationWish(Faction faction)
	{
		return this.getRelationWish(faction.getId());
	}
	
	public void setRelationWish(String factionId, Rel rel)
	{
		Map<String, Rel> relationWishes = this.getRelationWishes();
		if (rel == null || rel == Rel.NEUTRAL)
		{
			relationWishes.remove(factionId);
		}
		else
		{
			relationWishes.put(factionId, rel);
		}
		this.setRelationWishes(relationWishes);
	}
	
	public void setRelationWish(Faction faction, Rel rel)
	{
		this.setRelationWish(faction.getId(), rel);
	}
	
	// -------------------------------------------- //
	// FIELD: flagOverrides
	// -------------------------------------------- //
	
	// RAW
	
	public Map<MFlag, Boolean> getFlags()
	{
		// We start with default values ...
		Map<MFlag, Boolean> ret = new MassiveMap<>();
		for (MFlag mflag : MFlag.getAll())
		{
			ret.put(mflag, mflag.isStandard());
		}
		
		// ... and if anything is explicitly set we use that info ...
		Iterator<Entry<String, Boolean>> iter = this.flags.entrySet().iterator();
		while (iter.hasNext())
		{
			// ... for each entry ...
			Entry<String, Boolean> entry = iter.next();
			
			// ... extract id and remove null values ...
			String id = entry.getKey();					
			if (id == null)
			{
				iter.remove();
				this.changed();
				continue;
			}
			
			// ... resolve object and skip unknowns ...
			MFlag mflag = MFlag.get(id);
			if (mflag == null) continue;
			
			ret.put(mflag, entry.getValue());
		}
		
		return ret;
	}
	
	public void setFlags(Map<MFlag, Boolean> flags)
	{
		Map<String, Boolean> flagIds = new MassiveMap<>();
		for (Entry<MFlag, Boolean> entry : flags.entrySet())
		{
			flagIds.put(entry.getKey().getId(), entry.getValue());
		}
		setFlagIds(flagIds);
	}
	
	public void setFlagIds(Map<String, Boolean> flagIds)
	{
		// Clean input
		MassiveMapDef<String, Boolean> target = new MassiveMapDef<>();
		for (Entry<String, Boolean> entry : flagIds.entrySet())
		{
			String key = entry.getKey();
			if (key == null) continue;
			key = key.toLowerCase(); // Lowercased Keys Version 2.6.0 --> 2.7.0
			
			Boolean value = entry.getValue();
			if (value == null) continue;
			
			target.put(key, value);
		}

		// Detect Nochange
		if (MUtil.equals(this.flags, target)) return;
		
		// Apply
		this.flags = new MassiveMapDef<>(target);
		
		// Mark as changed
		this.changed();
	}
	
	// FINER
	
	public boolean getFlag(String flagId)
	{
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.get(flagId);
		if (ret != null) return ret;
		
		MFlag flag = MFlag.get(flagId);
		if (flag == null) throw new NullPointerException("flag");
		
		return flag.isStandard();
	}
	
	public boolean getFlag(MFlag flag)
	{
		if (flag == null) throw new NullPointerException("flag");
		
		String flagId = flag.getId();
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.get(flagId);
		if (ret != null) return ret;
		
		return flag.isStandard();
	}
	
	public Boolean setFlag(String flagId, boolean value)
	{
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.put(flagId, value);
		if (ret == null || ret != value) this.changed();
		return ret;
	}
	
	public Boolean setFlag(MFlag flag, boolean value)
	{
		if (flag == null) throw new NullPointerException("flag");
		
		String flagId = flag.getId();
		if (flagId == null) throw new NullPointerException("flagId");
		
		Boolean ret = this.flags.put(flagId, value);
		if (ret == null || ret != value) this.changed();
		return ret;
	}
	
	// -------------------------------------------- //
	// FIELD: permOverrides
	// -------------------------------------------- //
	
	// RAW
	public MassiveMapDef<String, Set<String>> getPermIds()
	{
		return this.perms;
	}
	
	public void setPermIds(MassiveMapDef<String, Set<String>> perms)
	{
		// Clean input
		MassiveMapDef<String, Set<String>> target = new MassiveMapDef<String, Set<String>>();
		for (Entry<String, Set<String>> entry : perms.entrySet())
		{
			String key = entry.getKey();
			if (key == null) continue;
			key = key.toLowerCase(); // Lowercased Keys Version 2.6.0 --> 2.7.0
			
			Set<String> value = entry.getValue();
			if (value == null) continue;
			
			target.put(key, value);
		}
		
		// Detect Nochange
		if (MUtil.equals(this.perms, target)) return;
		
		// Apply
		this.perms = target;
		
		// Mark as changed
		this.changed();
	}
	
	// Finer
	public Map<MPerm, Set<String>> getPerms()
	{
		// We start with default values ...
		Map<MPerm, Set<String>> ret = new MassiveMap<>();
		for (MPerm mperm : MPerm.getAll())
		{
			ret.put(mperm, new MassiveSet<>(mperm.getStandard()));
		}
		
		// ... and if anything is explicitly set we use that info ...
		for (Iterator<Entry<String, Set<String>>> it = this.perms.entrySet().iterator(); it.hasNext(); )
		{
			// ... for each entry ...
			Entry<String, Set<String>> entry = it.next();
			
			// ... extract id and remove null values ...
			String id = entry.getKey();
			if (id == null)
			{
				it.remove();
				continue;
			}
			
			// ... resolve object and skip unknowns ...
			MPerm mperm = MPerm.get(id);
			if (mperm == null) continue;
			
			ret.put(mperm, new MassiveSet<>(entry.getValue()));
		}
		
		return ret;
	}
	
	public void setPerms(Map<MPerm, Set<String>> perms)
	{
		// Create
		MassiveMapDef<String, Set<String>> permIds = new MassiveMapDef<>();
		
		// Fill
		for (Entry<MPerm, Set<String>> entry : perms.entrySet())
		{
			permIds.put(entry.getKey().getId(), entry.getValue());
		}
		
		// Set
		this.setPermIds(permIds);
	}
	
	// FINEST
	private boolean isPermitted(MPerm perm, Selector selector)
	{
		if (perm == null) throw new NullPointerException("perm");
		if (selector == null) throw new NullPointerException("permissible");
		
		String permId = perm.getId();
		String selectorId = selector.getId();
		
		Set<String> rels = this.perms.get(permId);
		return rels != null && rels.contains(selectorId);
		// TODO: What about standard? return perm.getStandard().contains(selectorId);
	}
	
	private boolean isPermitted(MPerm perm, Faction faction)
	{
		return this.isPermitted(perm, (Selector) faction) || this.isPermitted(perm, faction.getRelationTo(this));
	}
	
	private boolean isPermitted(MPerm perm, MPlayer mplayer)
	{
		if (this.isPermitted(perm, mplayer.getFaction())) return true;
		if (this.isPermitted(perm, mplayer.getRelationTo(this))) return true;
		return this.isPermitted(perm, (Selector) mplayer);
	}
	
	public boolean isPermitted(MPerm perm, Object watcherObject)
	{
		// Special
		if (watcherObject instanceof MPlayer) return this.isPermitted(perm, (MPlayer)watcherObject);
		if (watcherObject instanceof Faction) return this.isPermitted(perm, (Faction)watcherObject);
		
		// Default
		return watcherObject instanceof Selector && this.isPermitted(perm, (Selector) watcherObject);
	}
	
	private Set<String> getPermitted(String permId)
	{
		Set<String> rels = this.perms.get(permId);
		if (rels != null) return rels;
		
		MPerm perm = MPerm.get(permId);
		if (perm == null) throw new NullPointerException("perm");
		
		return perm.getStandard();
	}
	
	public Set<String> getPermitted(MPerm perm)
	{
		if (perm == null) throw new NullPointerException("perm");
		return this.getPermitted(perm.getId());
	}
	
	public void setPermittedRelations(MPerm perm, Set<String> permissibleIds)
	{
		this.getPermIds().put(perm.getId(), permissibleIds);
		this.changed();
	}
	
	public void setPermittedRelations(MPerm perm, String... permissibleIds)
	{
		this.setPermittedRelations(perm, new MassiveSet<>(permissibleIds));
	}
	
	public void setRelationPermitted(MPerm perm, String permissibleId, boolean permitted)
	{
		// Get
		Map<String, Set<String>> perms = this.getPermIds();
		String permId = perm.getId();
		Set<String> permissibleIds = perms.get(permId);
		
		// Change
		if (permitted)
		{
			permissibleIds.add(permissibleId);
		}
		else
		{
			permissibleIds.remove(permissibleId);
		}
		
		// Set
		this.setPermittedRelations(perm, permissibleIds);
	}
	
	public Mson getPermittedLine(MPerm perm)
	{
		// Create
		Mson ret = Mson.EMPTY;
		Map<SelectorType, List<Selector>> resolve = this.resolvePermitted(perm);
		
		// Fill > Ranks
		/*
		List<Selector> ranks = resolve.get(SelectorType.RANK);
		if (ranks != null)
		{
			for (Selector selector : ranks)
			{
				Rel rank = (Rel) selector;
				// TODO: Change to index here later
				ret = ret.add(rank.getName().charAt(0));
			}
		}
		*/
		
		// Fill > Relations
		List<Selector> relations = resolve.get(SelectorType.RELATION);
		if (relations != null)
		{
			for (Selector selector : relations)
			{
				Rel rank = (Rel) selector;
				ret = ret.add(String.valueOf(rank.getName().charAt(0)));
			}
		}
		
		// Fill > Factions
		List<Selector> factions = resolve.get(SelectorType.FACTION);
		if (factions != null) ret = ret.add("F");
		
		// Fill > Factions
		List<Selector> players = resolve.get(SelectorType.PLAYER);
		if (players != null) ret = ret.add("P");
		
		// Fill > Show
		ret = ret.command(CmdFactions.get().cmdFactionsPerm.cmdFactionsPermShow, perm.getId());
		List<String> showLines = Mson.toPlain(this.getPermittedShow(perm, resolve), true);
		showLines.add(ret.getTooltip());
		ret = ret.tooltip(showLines);
		
		// Return
		return ret;
	}
	
	private Map<SelectorType, List<Selector>> resolvePermitted(MPerm perm)
	{
		// Create
		Map<SelectorType, List<Selector>> ret = new MassiveMap<>();
		
		// Fill
		TypeSelector type = TypeSelector.get();
		for (String id : this.getPermitted(perm))
		{
			Selector selector = type.readSafe(id, null);
			if (selector == null) throw new IllegalStateException("Selector id " + id + "wasn't resolvable.");
			
			SelectorType selectorType = selector.getType();
			List<Selector> selectors = ret.get(selectorType);
			if (selectors == null)
			{
				selectors = new MassiveList<>(selector);
				ret.put(selectorType, selectors);
			}
			else
			{
				selectors.add(selector);
			}
		}
		
		// Return
		return ret;
	}
	
	private List<Mson> getPermittedShow(MPerm perm, Map<SelectorType, List<Selector>> resolve)
	{
		Mson header = Txt.titleize(perm.getDesc(true, true));
		
		Mson ranks = Mson.mson("Ranks: ");
		/*
		List<Selector> selectorRanks = resolve.get(SelectorType.RANK);
		if (selectorRanks != null)
		{
			for (Selector selector : selectorRanks)
			{
				Rel rank = (Rank) selector;
				ranks = ranks.add(rank.getInformation());
			}
		}
		*/
		
		Mson relations = Mson.mson("Relations: ");
		List<Selector> selectorRelations = resolve.get(SelectorType.RELATION);
		if (selectorRelations != null)
		{
			for (Selector selector : selectorRelations)
			{
				Rel relation = (Rel) selector;
				relations = relations.add(relation.getName()).add(Mson.SPACE);
			}
		}
		
		Mson factions = Mson.mson("Factions: ");
		List<Selector> selectorFactions = resolve.get(SelectorType.FACTION);
		if (selectorFactions != null)
		{
			List<Mson> factionsList = new MassiveList<>();
			for (Selector selector : selectorFactions)
			{
				factionsList.add(TypeFaction.get().getVisualMson((Faction) selector));
			}
			
			factions.add(Mson.implode(factionsList, Mson.mson(", ")));
		}
		
		return new MassiveList<>(header, ranks, relations, factions);
	}
	
	public List<Mson> getPermittedShow(MPerm perm)
	{
		// Resolve Permitted
		Map<SelectorType, List<Selector>> resolve = this.resolvePermitted(perm);
		
		// Return show
		return this.getPermittedShow(perm, resolve);
	}
	
	// -------------------------------------------- //
	// OVERRIDE: permBlacklist
	// -------------------------------------------- //
	
	// Raw
	public Set<String> getPermBlacklist()
	{
		return permBlacklist;
	}
	
	public void setPermBlacklist(Set<String> permBlacklist)
	{
		// Detect Nochange
		if (MUtil.equals(this.permBlacklist, permBlacklist)) return;
		
		// Apply permBlacklist
		this.permBlacklist = new MassiveSetDef<>(permBlacklist);
		
		// Mark as changed
		this.changed();
	}
	
	// Finer
	public boolean isPermBlacklisted(Object watcherObject)
	{
		Selector permissible = null;
		
		if (watcherObject instanceof Faction)
		{
			Faction faction = (Faction) watcherObject;
			if (this.isPermBlacklisted(faction.getRelationTo(this))) return true;
			permissible = faction;
		}
		else if (watcherObject instanceof MPlayer)
		{
			MPlayer mplayer = (MPlayer) watcherObject;
			if (this.isPermBlacklisted(mplayer.getRelationTo(this)) || this.isPermBlacklisted(mplayer.getFaction())) return true;
			permissible = mplayer;
		}
		else if (watcherObject instanceof Rel)
		{
			permissible = (Rel) watcherObject;
		}
		/*
		else if (watcherObject instanceof Rank)
		{
			permissible = (Rank) watcherObject;
		}*/
		
		return permissible != null && this.isPermBlacklisted(permissible);
	}
	
	private boolean isPermBlacklisted(Selector selector)
	{
		return this.permBlacklist.contains(selector.getId());
	}
	
	public boolean addToBlacklist(Selector selector)
	{
		// Get Id
		String id = selector.getId();
		
		// Add
		boolean ret = this.getPermBlacklist().add(id);
		
		// Changed
		if (ret) this.changed();
		
		// Return
		return ret;
	}
	
	public boolean removeFromBlacklist(Selector selector)
	{
		// Get Id
		String id = selector.getId();
		
		// Add
		boolean ret = this.getPermBlacklist().remove(id);
		
		// Changed
		if (ret) this.changed();
		
		// Return
		return ret;
	}
	
	// -------------------------------------------- //
	// OVERRIDE: Selector
	// -------------------------------------------- //
	
	@Override
	public SelectorType getType()
	{
		return SelectorType.FACTION;
	}
	
	// -------------------------------------------- //
	// OVERRIDE: RelationParticipator
	// -------------------------------------------- //
	
	@Override
	public String describeTo(RelationParticipator observer, boolean ucfirst)
	{
		return RelationUtil.describeThatToMe(this, observer, ucfirst);
	}
	
	@Override
	public String describeTo(RelationParticipator observer)
	{
		return RelationUtil.describeThatToMe(this, observer);
	}
	
	@Override
	public Rel getRelationTo(RelationParticipator observer)
	{
		return RelationUtil.getRelationOfThatToMe(this, observer);
	}
	
	@Override
	public Rel getRelationTo(RelationParticipator observer, boolean ignorePeaceful)
	{
		return RelationUtil.getRelationOfThatToMe(this, observer, ignorePeaceful);
	}
	
	@Override
	public ChatColor getColorTo(RelationParticipator observer)
	{
		return RelationUtil.getColorOfThatToMe(this, observer);
	}
	
	// -------------------------------------------- //
	// POWER
	// -------------------------------------------- //
	// TODO: Implement a has enough feature.
	
	public double getPower()
	{
		if (this.getFlag(MFlag.getFlagInfpower())) return 999999;
		
		double ret = 0;
		for (MPlayer mplayer : this.getMPlayers())
		{
			ret += mplayer.getPower();
		}
		
		ret = this.limitWithPowerMax(ret);
		ret += this.getPowerBoost();
		
		return ret;
	}
	
	public double getPowerMax()
	{
		if (this.getFlag(MFlag.getFlagInfpower())) return 999999;
	
		double ret = 0;
		for (MPlayer mplayer : this.getMPlayers())
		{
			ret += mplayer.getPowerMax();
		}
		
		ret = this.limitWithPowerMax(ret);
		ret += this.getPowerBoost();
		
		return ret;
	}
	
	private double limitWithPowerMax(double power)
	{
		// NOTE: 0.0 powerMax means there is no max power
		double powerMax = MConf.get().factionPowerMax;
		
		return powerMax <= 0 || power < powerMax ? power : powerMax;
	}
	
	public int getPowerRounded()
	{
		return (int) Math.round(this.getPower());
	}
	
	public int getPowerMaxRounded()
	{
		return (int) Math.round(this.getPowerMax());
	}
	
	public int getLandCount()
	{
		return BoardColl.get().getCount(this);
	}
	public int getLandCountInWorld(String worldName)
	{
		return Board.get(worldName).getCount(this);
	}
	
	public boolean hasLandInflation()
	{
		return this.getLandCount() > this.getPowerRounded();
	}
	
	// -------------------------------------------- //
	// WORLDS
	// -------------------------------------------- //
	
	public Set<String> getClaimedWorlds()
	{
		return BoardColl.get().getClaimedWorlds(this);
	}
	
	// -------------------------------------------- //
	// FOREIGN KEY: MPLAYER
	// -------------------------------------------- //
	
	public List<MPlayer> getMPlayers()
	{
		return new MassiveList<>(FactionsIndex.get().getMPlayers(this));
	}
	
	public List<MPlayer> getMPlayersWhere(Predicate<? super MPlayer> predicate)
	{
		List<MPlayer> ret = this.getMPlayers();
		for (Iterator<MPlayer> it = ret.iterator(); it.hasNext();)
		{
			if ( ! predicate.apply(it.next())) it.remove();
		}
		return ret;
	}
	
	public List<MPlayer> getMPlayersWhereOnline(boolean online)
	{
		return this.getMPlayersWhere(online ? SenderColl.PREDICATE_ONLINE : SenderColl.PREDICATE_OFFLINE);
	}

	public List<MPlayer> getMPlayersWhereOnlineTo(Object senderObject)
	{
		return this.getMPlayersWhere(PredicateAnd.get(SenderColl.PREDICATE_ONLINE, PredicateVisibleTo.get(senderObject)));
	}
	
	public List<MPlayer> getMPlayersWhereRole(Rel role)
	{
		return this.getMPlayersWhere(PredicateMPlayerRole.get(role));
	}
	
	public MPlayer getLeader()
	{
		List<MPlayer> ret = this.getMPlayersWhereRole(Rel.LEADER);
		if (ret.size() == 0) return null;
		return ret.get(0);
	}
	
	public List<CommandSender> getOnlineCommandSenders()
	{
		// Create Ret
		List<CommandSender> ret = new MassiveList<>();
		
		// Fill Ret
		for (CommandSender sender : IdUtil.getLocalSenders())
		{
			if (MUtil.isntSender(sender)) continue;
			
			MPlayer mplayer = MPlayer.get(sender);
			if (mplayer.getFaction() != this) continue;
			
			ret.add(sender);
		}
		
		// Return Ret
		return ret;
	}
	
	public List<Player> getOnlinePlayers()
	{
		// Create Ret
		List<Player> ret = new MassiveList<>();
		
		// Fill Ret
		for (Player player : MUtil.getOnlinePlayers())
		{
			if (MUtil.isntPlayer(player)) continue;
			
			MPlayer mplayer = MPlayer.get(player);
			if (mplayer.getFaction() != this) continue;
			
			ret.add(player);
		}
		
		// Return Ret
		return ret;
	}

	// used when current leader is about to be removed from the faction; promotes new leader, or disbands faction if no other members left
	public void promoteNewLeader()
	{
		if ( ! this.isNormal()) return;
		if (this.getFlag(MFlag.getFlagPermanent()) && MConf.get().permanentFactionsDisableLeaderPromotion) return;

		MPlayer oldLeader = this.getLeader();

		// get list of officers, or list of normal members if there are no officers
		List<MPlayer> replacements = this.getMPlayersWhereRole(Rel.OFFICER);
		if (replacements == null || replacements.isEmpty())
		{
			replacements = this.getMPlayersWhereRole(Rel.MEMBER);
		}

		if (replacements == null || replacements.isEmpty())
		{
			// faction leader is the only member; one-man faction
			if (this.getFlag(MFlag.getFlagPermanent()))
			{
				if (oldLeader != null)
				{
					// TODO: Where is the logic in this? Why MEMBER? Why not LEADER again? And why not OFFICER or RECRUIT?
					oldLeader.setRole(Rel.MEMBER);
				}
				return;
			}

			// no members left and faction isn't permanent, so disband it
			if (MConf.get().logFactionDisband)
			{
				Factions.get().log("The faction "+this.getName()+" ("+this.getId()+") has been disbanded since it has no members left.");
			}

			for (MPlayer mplayer : MPlayerColl.get().getAllOnline())
			{
				mplayer.msg("<i>The faction %s<i> was disbanded.", this.getName(mplayer));
			}

			this.detach();
		}
		else
		{
			// promote new faction leader
			if (oldLeader != null)
			{
				oldLeader.setRole(Rel.MEMBER);
			}
				
			replacements.get(0).setRole(Rel.LEADER);
			this.msg("<i>Faction leader <h>%s<i> has been removed. %s<i> has been promoted as the new faction leader.", oldLeader == null ? "" : oldLeader.getName(), replacements.get(0).getName());
			Factions.get().log("Faction "+this.getName()+" ("+this.getId()+") leader was removed. Replacement leader: "+replacements.get(0).getName());
		}
	}
	
	// -------------------------------------------- //
	// FACTION ONLINE STATE
	// -------------------------------------------- //

	public boolean isAllMPlayersOffline()
	{
		return this.getMPlayersWhereOnline(true).size() == 0;
	}
	
	public boolean isAnyMPlayersOnline()
	{
		return !this.isAllMPlayersOffline();
	}
	
	public boolean isFactionConsideredOffline()
	{
		return this.isAllMPlayersOffline();
	}
	
	public boolean isFactionConsideredOnline()
	{
		return !this.isFactionConsideredOffline();
	}
	
	public boolean isExplosionsAllowed()
	{
		boolean explosions = this.getFlag(MFlag.getFlagExplosions());
		boolean offlineexplosions = this.getFlag(MFlag.getFlagOfflineexplosions());

		if (explosions && offlineexplosions) return true;
		if ( ! explosions && ! offlineexplosions) return false;

		boolean online = this.isFactionConsideredOnline();
		
		return (online && explosions) || (!online && offlineexplosions);
	}
	
	// -------------------------------------------- //
	// MESSAGES
	// -------------------------------------------- //
	// These methods are simply proxied in from the Mixin.
	
	// CONVENIENCE SEND MESSAGE
	
	public boolean sendMessage(Object message)
	{
		return MixinMessage.get().messagePredicate(new PredicateCommandSenderFaction(this), message);
	}
	
	public boolean sendMessage(Object... messages)
	{
		return MixinMessage.get().messagePredicate(new PredicateCommandSenderFaction(this), messages);
	}
	
	public boolean sendMessage(Collection<Object> messages)
	{
		return MixinMessage.get().messagePredicate(new PredicateCommandSenderFaction(this), messages);
	}
	
	// CONVENIENCE MSG
	
	public boolean msg(String msg)
	{
		return MixinMessage.get().msgPredicate(new PredicateCommandSenderFaction(this), msg);
	}
	
	public boolean msg(String msg, Object... args)
	{
		return MixinMessage.get().msgPredicate(new PredicateCommandSenderFaction(this), msg, args);
	}
	
	public boolean msg(Collection<String> msgs)
	{
		return MixinMessage.get().msgPredicate(new PredicateCommandSenderFaction(this), msgs);
	}
	
}
