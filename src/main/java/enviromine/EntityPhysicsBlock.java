package enviromine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import enviromine.handlers.EM_PhysManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockSand;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingSand;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class EntityPhysicsBlock extends EntityFallingSand
{
	
	public boolean isAnvil2 = true;
	public boolean isBreakingAnvil2;
	public int fallHurtMax2;
	public float fallHurtAmount2;
	public boolean isLandSlide = false;
	
	public EntityPhysicsBlock(World world)
	{
		super(world);
		this.setIsAnvil(true);
		this.fallHurtMax2 = 40;
		this.fallHurtAmount2 = 2.0F;
	}
	
	public EntityPhysicsBlock(World world, double x, double y, double z, int id, int meta, boolean update)
	{
		super(world, x, y, z, flowerID(id), meta);
		this.setIsAnvil(true);
		this.fallHurtMax2 = 40;
		this.fallHurtAmount2 = 2.0F;
		
		if(update)
		{
			EM_PhysManager.schedulePhysUpdate(world, (int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z), false, "Collapse");
		}
	}
	
	public static int flowerID(int id)
	{
		if(Block.blocksList[id] instanceof BlockFlower)
		{
			return 0;
		} else
		{
			return id;
		}
	}
	
	@Override
	/**
	 * Returns true if other Entities should be prevented from moving through this Entity.
	 */
	public boolean canBeCollidedWith()
	{
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void onUpdate()
	{
		if(this.blockID == 0)
		{
			this.setDead();
		} else
		{
			this.prevPosX = this.posX;
			this.prevPosY = this.posY;
			this.prevPosZ = this.posZ;
			++this.fallTime;
			this.motionY -= 0.03999999910593033D;
			this.moveEntity(this.motionX, this.motionY, this.motionZ);
			this.motionX *= 0.9800000190734863D;
			this.motionY *= 0.9800000190734863D;
			this.motionZ *= 0.9800000190734863D;
			
			if(!this.worldObj.isRemote)
			{
				int i = MathHelper.floor_double(this.posX);
				int j = MathHelper.floor_double(this.posY);
				int k = MathHelper.floor_double(this.posZ);
				
				if(this.fallTime == 1)
				{
					if(this.worldObj.getBlockId(i, j, k) != this.blockID && !isLandSlide)
					{
						this.setDead();
						return;
					}
					
					this.worldObj.setBlockToAir(i, j, k);
				}
				
				try
				{
			        AxisAlignedBB axisalignedbb = Block.blocksList[blockID].getCollisionBoundingBoxFromPool(this.worldObj, i, j - 1, k);
			        if(axisalignedbb != null)
			        {
			        	List fallingBlocks = this.worldObj.getEntitiesWithinAABB(EntityPhysicsBlock.class, axisalignedbb);
			        	
			        	fallingBlocks.remove(this);
			        	
				        if(fallingBlocks.size() >= 1 && isLandSlide)
				        {
				        	this.motionY = 0;
				        	this.setPosition(i + 0.5D, j + 0.5D, k + 0.5D);
				        }
			        }
				} catch(NullPointerException e)
				{
				}
				
				if(this.onGround)
				{
					this.motionX *= 0.699999988079071D;
					this.motionZ *= 0.699999988079071D;
					this.motionY *= -0.5D;
					
					if(this.worldObj.getBlockId(i, j, k) != Block.pistonMoving.blockID)
					{
						this.setDead();
						
						if(!this.worldObj.canPlaceEntityOnSide(Block.anvil.blockID, i, j, k, true, 1, (Entity)null, (ItemStack)null) && !EM_PhysManager.blockNotSolid(this.worldObj, i, j, k, false))
						{
							j += 1;
						}
						
						if(!this.isBreakingAnvil2 && this.worldObj.canPlaceEntityOnSide(Block.anvil.blockID, i, j, k, true, 1, (Entity)null, (ItemStack)null) && !BlockSand.canFallBelow(this.worldObj, i, j - 1, k) && this.worldObj.setBlock(i, j, k, this.blockID, this.metadata, 3))
						{
							EM_PhysManager.schedulePhysUpdate(this.worldObj, i, j, k, true, "Collapse");
							
							if(Block.blocksList[this.blockID] instanceof BlockSand)
							{
								((BlockSand)Block.blocksList[this.blockID]).onFinishFalling(this.worldObj, i, j, k, this.metadata);
							}
							
							if(this.fallingBlockTileEntityData != null && Block.blocksList[this.blockID] instanceof ITileEntityProvider)
							{
								TileEntity tileentity = this.worldObj.getBlockTileEntity(i, j, k);
								
								if(tileentity != null)
								{
									NBTTagCompound nbttagcompound = new NBTTagCompound();
									tileentity.writeToNBT(nbttagcompound);
									Iterator iterator = this.fallingBlockTileEntityData.getTags().iterator();
									
									while(iterator.hasNext())
									{
										NBTBase nbtbase = (NBTBase)iterator.next();
										
										if(!nbtbase.getName().equals("x") && !nbtbase.getName().equals("y") && !nbtbase.getName().equals("z"))
										{
											nbttagcompound.setTag(nbtbase.getName(), nbtbase.copy());
										}
									}
									
									tileentity.readFromNBT(nbttagcompound);
									tileentity.onInventoryChanged();
								}
							}
						} else if(this.shouldDropItem && !this.isBreakingAnvil2)
						{
							this.entityDropItem(new ItemStack(this.blockID, 1, Block.blocksList[this.blockID].damageDropped(this.metadata)), 0.0F);
						}
					}
				} else if(this.fallTime > 100 && !this.worldObj.isRemote && (j < 1 || j > 256) || this.fallTime > 600)
				{
					if(this.shouldDropItem)
					{
						this.entityDropItem(new ItemStack(this.blockID, 1, Block.blocksList[this.blockID].damageDropped(this.metadata)), 0.0F);
					}
					
					this.setDead();
				}
			}
		}
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	protected void fall(float par1)
	{
		if(this.isAnvil2)
		{
			int i = MathHelper.ceiling_float_int(par1 - 1.0F);
			
			if(isLandSlide)
			{
				i = 2;
			}
			
			if(i > 0)
			{
				ArrayList arraylist = new ArrayList(this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox));
				
				DamageSource damagesource;
				
				if(isLandSlide)
				{
					damagesource = EnviroDamageSource.landslide;
				} else
				{
					damagesource = this.blockID == Block.anvil.blockID ? DamageSource.anvil : DamageSource.fallingBlock;
				}
				
				Iterator iterator = arraylist.iterator();
				
				while(iterator.hasNext())
				{
					Entity entity = (Entity)iterator.next();
					entity.attackEntityFrom(damagesource, (float)Math.min(MathHelper.floor_float((float)i * this.fallHurtAmount2), this.fallHurtMax2));
				}
				
				if(this.blockID == Block.anvil.blockID && (double)this.rand.nextFloat() < 0.05000000074505806D + (double)i * 0.05D)
				{
					int j = this.metadata >> 2;
					int k = this.metadata & 3;
					++j;
					
					if(j > 2)
					{
						this.isBreakingAnvil2 = true;
					} else
					{
						this.metadata = k | j << 2;
					}
				}
			}
		}
	}
}
