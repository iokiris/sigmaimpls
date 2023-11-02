package info.sigmaclient.module.impl.combat;

import info.sigmaclient.event.Event;
import info.sigmaclient.event.RegisterEvent;
import info.sigmaclient.event.impl.EventUpdate;
import info.sigmaclient.management.notifications.Notifications;
import info.sigmaclient.module.Module;
import info.sigmaclient.module.data.ModuleData;
import info.sigmaclient.module.data.Setting;
import info.sigmaclient.util.misc.ChatUtil;
import net.minecraft.network.play.client.C03PacketPlayer;


public class DamageHeal extends Module {
    private static final String MULTIPLIER = "MULTIPLIER";
    private static final String OFFSET = "OFFSET";
    private static final String NOGROUND = "NOGROUND";
    private boolean HealState = false;
    private float start_health;
    private float prev_health = 0.0F;
    private int timer = 0;
    private final String chatPrefix = "\247b[DamageHeal]";
    public DamageHeal(ModuleData data) {
        super(data);
        this.settings.put(MULTIPLIER, new Setting<Double>(MULTIPLIER, 0.5, "Multiplier", 0.1,
                0.5, 1.5));
        this.settings.put(OFFSET, new Setting<Double>(OFFSET, 0.049, "Multiplier", 0.001,
                0.03, 0.06));
        this.settings.put(NOGROUND, new Setting<Boolean>(NOGROUND, false, "Send's last packet with not-on-ground value."));
    }

    public void onEnable() {
        super.onEnable();
        this.start_health = mc.thePlayer.getHealth();
        this.prev_health = 0.0F;
        this.timer = 0;
        if (mc.thePlayer != null) {
            double offset = ((Number)((Setting)this.settings.get(OFFSET)).getValue()).doubleValue();
            double damage = ((Number)((Setting)this.settings.get(MULTIPLIER)).getValue()).doubleValue();
            for(int i = 0; (double)i < 80.0 + 40.0 * (damage - 0.5); ++i) {
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ, false));
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, false));
            }
            if (((Boolean)((Setting)this.settings.get(NOGROUND)).getValue()).booleanValue()) {
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true));
                this.HealState = true;
            }
            else {
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.1D, mc.thePlayer.posZ, true));
            }
            this.HealState = true;
        }
    }
    @RegisterEvent(events={EventUpdate.class})
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (this.HealState) {
                float health = mc.thePlayer.getHealth();
                if (((this.prev_health > this.start_health) && (this.prev_health > health)) || this.timer >= 20) {
                    float healthDelta = this.prev_health - this.start_health;

                    Notifications.getManager().post("DamageHeal", String.valueOf(healthDelta) + " restored");
                    ChatUtil.printChat(chatPrefix + ": \2474" + String.valueOf(healthDelta) + " \2477restored. (\2474" + this.start_health + " \2477-> \2476" + this.prev_health + "\2477)");
                    //ChatUtil.printChat(chatPrefix + ": \2474" + "PREV: " + this.prev_health + "START: " + this.start_health);
                    this.HealState = false;
                    this.toggle();
                }
                else if (this.prev_health <= health) {
                    this.prev_health = health;
                }
                ++this.timer;
            }
        }
    }
}