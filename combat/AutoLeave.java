package info.sigmaclient.module.impl.combat;

import info.sigmaclient.Client;
import info.sigmaclient.event.Event;
import info.sigmaclient.event.RegisterEvent;
import info.sigmaclient.event.impl.EventUpdate;
import info.sigmaclient.management.notifications.Notifications;
import info.sigmaclient.module.Module;
import info.sigmaclient.module.data.ModuleData;
import info.sigmaclient.module.data.Setting;

public class AutoLeave extends Module {
    private final String HEALTH = "HEALTH";
    private final Module Kick = Client.getModuleManager().get(info.sigmaclient.module.impl.combat.Kick.class);
    public AutoLeave(ModuleData data) {
        super(data);
        settings.put(HEALTH, new Setting<>(HEALTH, 7, "Health to leave.", 0.5, 1, 10));
    }

    @RegisterEvent(events={EventUpdate.class})
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            double health = ((Number) settings.get(HEALTH).getValue()).doubleValue();
            if (mc.thePlayer.getHealth() <= (health * 2) && !mc.thePlayer.capabilities.isCreativeMode && !mc.isIntegratedServerRunning()) {
                Notifications.getManager().post("AutoLeave", "Detected.");
                Kick.toggle();
                this.toggle();
            }
        }
    }
}
