package enviromine.handlers;

import java.util.EnumSet;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class EM_ClientScheduledTickHandler implements ITickHandler
{
	long elapsedTime;
	long prevTick = System.nanoTime();
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData)
	{
	}
	
	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData)
	{
	}
	
	@Override
	public EnumSet<TickType> ticks()
	{
		return EnumSet.of(TickType.CLIENT);
	}
	
	@Override
	public String getLabel()
	{
		return null;
	}
}
