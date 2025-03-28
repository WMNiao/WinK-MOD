package i.WinKcode.hack.hacks.combat;

import i.WinKcode.hack.Hack;
import i.WinKcode.hack.HackCategory;
import i.WinKcode.utils.RayCastUtils;
import i.WinKcode.utils.TimerUtils;
import i.WinKcode.utils.Utils;
import i.WinKcode.utils.ValidUtils;
import i.WinKcode.utils.visual.ChatUtils;
import i.WinKcode.value.Mode;
import i.WinKcode.value.types.BooleanValue;
import i.WinKcode.value.types.DoubleValue;
import i.WinKcode.value.types.IntegerValue;
import i.WinKcode.value.types.ModeValue;
import i.WinKcode.wrappers.Wrapper;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class KillAura extends Hack{
	
	public ModeValue priority;
	public ModeValue mode;
	public BooleanValue walls;
	public BooleanValue autoDelay;
	public BooleanValue packetReach;
    public IntegerValue minCPS;
    public IntegerValue maxCPS;
    public DoubleValue packetRange;
    public DoubleValue range;
    public IntegerValue FOV;
    
    public TimerUtils timer;
    public EntityLivingBase target;
    public static float[] facingCam = null;
    public float[] facing = null;
    Vec3d randomCenter = null;
    boolean phaseOne = false;
    boolean phaseTwo = false;
    boolean phaseThree = false;
    
	public KillAura() {
		super("KillAura", HackCategory.COMBAT);
		this.GUIName = "杀戮光环";
		
		this.priority = new ModeValue("优先级", new Mode("最近的", true), new Mode("生命值", false));
		this.mode = new ModeValue("类型", new Mode("简易", true), new Mode("AAC", false));
		walls = new BooleanValue("穿墙", false);
		autoDelay = new BooleanValue("自动延迟", false);
		packetReach = new BooleanValue("瞬移砍人", false);
		packetRange = new DoubleValue("百米大刀", 10.0D, 1.0D, 100D);
		minCPS = new IntegerValue("MinCPS", 4, 1, 30);
		maxCPS = new IntegerValue("MaxCPS", 8, 1, 30);
		range = new DoubleValue("范围", 3.4D, 1.0D, 7.0D);
		FOV = new IntegerValue("视角", 360, 1, 360);
		
		this.addValue(mode, priority, walls, autoDelay, packetReach, minCPS, maxCPS, packetRange, range, FOV);
		
		this.timer = new TimerUtils();
	}
	
	@Override
	public String getDescription() {
		return "攻击你周围的实体.";
	}
	
	@Override
	public void onEnable() {
		facingCam = null;
		if(mode.getMode("AAC").isToggled()) {
			facing = null;
			randomCenter = null;
			phaseOne = false;
			phaseTwo = false;
			phaseThree = false;
		}
		super.onEnable();
	}
	
	@Override
	public void onDisable() {
		this.facingCam = null;
		this.target = null;
		AutoShield.block(false);
		super.onDisable();
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void onCameraSetup(CameraSetup event) {
		if(!(mode.getMode("AAC").isToggled() 
				&& event.getEntity() == Wrapper.INSTANCE.player() && target != null)) 
			return;
		
		float yaw = Utils.getRotationsNeeded(target)[0] - 180;
		float pitch = Utils.getRotationsNeeded(target)[1] + 10;
		facingCam = new float[] { Utils.getRotationsNeeded(target)[0], pitch };
		
		event.setYaw(yaw);
		event.setPitch(pitch);
		super.onCameraSetup(event);
	}
    
	@Override
	public void onClientTick(ClientTickEvent event) {
		if(mode.getMode("AAC").isToggled()) {
			phaseOne(); phaseTwo(); phaseFour();
		}
		else if(mode.getMode("简易").isToggled()) {
			killAuraUpdate(); killAuraAttack(target);
		}
		super.onClientTick(event);
	}
	
	@Override
	public void onPlayerTick(PlayerTickEvent event) {
		if(mode.getMode("AAC").isToggled()) phaseThree(event);
		super.onPlayerTick(event);
	}
	
	void phaseOne() {
		if(target != null) {
			randomCenter = Utils.getRandomCenter(target.getEntityBoundingBox());
			facing = Utils.getSmoothNeededRotations(randomCenter, 100.0F, 100.0F);
		}
		killAuraUpdate();
		if(target != null) 
			phaseOne = true;
	}
	
	void phaseTwo() {
		if(target == null || randomCenter == null || !phaseOne) return;
		if(facing[0] == Utils.getNeededRotations(randomCenter)[0]) {
			phaseOne = false;
			phaseTwo = true;
		}
	}
	
	void phaseThree(PlayerTickEvent event) {
		if(target == null 
				|| facing == null 
				|| event.player != Wrapper.INSTANCE.player()) 
			return;
		
		if(target.hurtTime <= target.maxHurtTime) {
          	event.player.rotationYaw = facing[0];
          	event.player.rotationPitch = facing[1];
          	Wrapper.INSTANCE.player().rotationYawHead = facing[0];
		}
		if(!phaseTwo) return;
		
      	event.player.rotationYaw = facing[0];
      	event.player.rotationPitch = facing[1];
      	Wrapper.INSTANCE.player().rotationYawHead = facing[0];
        phaseTwo = false;
        phaseThree = true;
	}
	
	void phaseFour() {
		if(target == null 
				|| randomCenter == null 
				|| !phaseThree 
				|| facing[0] != Utils.getNeededRotations(randomCenter)[0]) {
			facingCam = null;
			return;
		}
		Entity rayCastEntity = RayCastUtils.rayCast((float)(range.getValue().floatValue() + 1.0F), facing[0], facing[1]);
		killAuraAttack(rayCastEntity == null ? target : (EntityLivingBase) rayCastEntity);
	}
	
	void killAuraUpdate(){
		for (Object object : Utils.getEntityList()) {
			if(!(object instanceof EntityLivingBase)) continue;
			EntityLivingBase entity = (EntityLivingBase) object;
			if(!check(entity)) continue;
			target = entity;
		}
	}
	
	public void killAuraAttack(EntityLivingBase entity) {
		if(entity == null) { AutoShield.block(false); return; }
		if(this.autoDelay.getValue()) {
    		if (Wrapper.INSTANCE.player().getCooledAttackStrength(0) == 1) {
    			processAttack(entity);
    			target = null;
    		}
    	}
		else
		{
			if(this.minCPS.getValue() > this.maxCPS.getValue()) ChatUtils.error("minCPS > maxCPS");
			int CPS = Utils.random(this.minCPS.getValue(), this.maxCPS.getValue());
			int r1 = Utils.random(1, 50),
					r2 = Utils.random(1, 60),
					r3 = Utils.random(1, 70);
			if (timer.isDelay((1000 + ((r1 - r2) + r3)) / CPS)) {
				processAttack(entity);
				timer.setLastMS();
				facingCam = null;
				target = null;
				phaseThree = false;
	        }
		}
	}
	
	public void processAttack(EntityLivingBase entity) {
		AutoShield.block(false);
		if(!isInAttackRange(entity) || !ValidUtils.isInAttackFOV(entity, (FOV.getValue() / 2))) return;
		EntityPlayerSP player = Wrapper.INSTANCE.player();
		float sharpLevel = EnchantmentHelper.getModifierForCreature(player.getHeldItemMainhand(), entity.getCreatureAttribute());
		if(this.packetReach.getValue()) {
            double posX = entity.posX - 3.5 * Math.cos(Math.toRadians(Utils.getYaw(entity) + 90.0f));
            double posZ = entity.posZ - 3.5 * Math.sin(Math.toRadians(Utils.getYaw(entity) + 90.0f));
            Wrapper.INSTANCE.sendPacket(
            		new CPacketPlayer.PositionRotation(posX, entity.posY, posZ, 
            				Utils.getYaw(entity), Utils.getPitch(entity), player.onGround));
            Wrapper.INSTANCE.sendPacket(
            		new CPacketUseEntity(entity));
            Wrapper.INSTANCE.sendPacket(
            		new CPacketPlayer.Position(player.posX, player.posY, player.posZ, player.onGround));
		}
		else
		{
			if(autoDelay.getValue() || mode.getMode("简易").isToggled())
				Utils.attack(entity);
			 else 
				Wrapper.INSTANCE.sendPacket(new CPacketUseEntity(entity));
		}
		Utils.swingMainHand();
		if (sharpLevel > 0.0f) 
			player.onEnchantmentCritical(entity);
		AutoShield.block(true);
    }
	
	boolean isPriority(EntityLivingBase entity) {
		return priority.getMode("最近的").isToggled() && ValidUtils.isClosest(entity, target)
				|| priority.getMode("生命值").isToggled() && ValidUtils.isLowHealth(entity, target);
	}
	
    boolean isInAttackRange(EntityLivingBase entity) {
        return packetReach.getValue() ? entity.getDistance(Wrapper.INSTANCE.player()) <= (float)(packetRange.getValue().floatValue()) 
        		: entity.getDistance(Wrapper.INSTANCE.player()) <= (float)(range.getValue().floatValue());
    }
	
	public boolean check(EntityLivingBase entity) {
		if(entity instanceof EntityArmorStand) { return false; }
		if(ValidUtils.isValidEntity(entity)){ return false; }
		if(!ValidUtils.isNoScreen()) { return false; }
		if(entity == Wrapper.INSTANCE.player()) { return false; }
		if(entity.isDead) { return false; }
        if (entity.deathTime > 0) { return false; }
		if(ValidUtils.isBot(entity)) { return false; }
		if(!ValidUtils.isFriendEnemy(entity)) { return false; }
    	if(!ValidUtils.isInvisible(entity)) { return false; }
    	if(!ValidUtils.isInAttackFOV(entity, (FOV.getValue()))) { return false; }
		if(!isInAttackRange(entity)) { return false; }
		if(!ValidUtils.isTeam(entity)) { return false; }
    	if(!ValidUtils.pingCheck(entity)) { return false; }
		if(!this.walls.getValue()) { if(!Wrapper.INSTANCE.player().canEntityBeSeen(entity)) { return false; } }
		if(!isPriority(entity)) { return false; }
		return true;
    }
}
