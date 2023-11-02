package info.sigmaclient.module.impl.other;

import info.sigmaclient.Client;
import info.sigmaclient.event.Event;
import info.sigmaclient.event.RegisterEvent;
import info.sigmaclient.event.impl.EventUpdate;
import info.sigmaclient.module.Module;
import info.sigmaclient.module.data.ModuleData;
import info.sigmaclient.module.impl.combat.Killaura;
import info.sigmaclient.util.misc.ChatUtil;
import net.minecraft.entity.EntityLivingBase;

import java.text.DecimalFormat;

public class LeaveInfomator extends Module {
    public LeaveInfomator(ModuleData data) {
        super(data);
        this.setHidden(true);
    }
    public String enemy_health = "0.00";
    public String enemy_name = "(not found)";
    public String self_health = "0.00";
    private final String chatPrefix = "\247b[LI]";
    private EntityLivingBase prev_target;

    @RegisterEvent(events = {EventUpdate.class})
    public void onEnable() {
        super.onEnable();
        this.setSuffix("false");
    }
    public void onEvent(Event event) {
        if (this.getSuffix().equals("true")) {
            getHealthInfoMessage();
            this.setSuffix("false");
        }
        else if ( event instanceof EventUpdate) {
            Module killaura = Client.getModuleManager().get(Killaura.class);
            if (killaura.isEnabled()) {
                if (mc.thePlayer != null && (Killaura.target != null || prev_target != null) && mc.theWorld != null)
                    if (prev_target == Killaura.target) {
                        enemy_health = getRoundedHealth(Killaura.target.getHealth());
                        enemy_name = Killaura.target.getName();
                    }
                prev_target = Killaura.target;
                assert mc.thePlayer != null;
                self_health = getRoundedHealth(mc.thePlayer.getHealth());
            }
        }
    }
    public void getHealthInfoMessage() {
        ChatUtil.printChat(chatPrefix + ": \2477" + "You leaved from PVP.");
        if (!enemy_health.equalsIgnoreCase("0,00")){
            ChatUtil.printChat(chatPrefix + ": \2477" + "Your enemy " + "\247c" + enemy_name + " \2477" + "health level was \2474" + enemy_health);
        } else {
            ChatUtil.printChat(chatPrefix + ": \2477" + "Your enemy " + "\247c" + enemy_name + " \2474has died.");
        }
        ChatUtil.printChat(chatPrefix + ": \2477" + "Your health level was \2474" + self_health);
    }
    private String getRoundedHealth(float health) {
        return new DecimalFormat("#0.00").format(health);
    }
}
