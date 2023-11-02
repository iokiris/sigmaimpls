package info.sigmaclient.module.impl.combat;

import info.sigmaclient.Client;
import info.sigmaclient.event.Event;
import info.sigmaclient.event.RegisterEvent;
import info.sigmaclient.event.impl.EventUpdate;
import info.sigmaclient.module.Module;
import info.sigmaclient.module.data.ModuleData;
import info.sigmaclient.module.data.Options;
import info.sigmaclient.module.data.Setting;

public class AutoHeal extends Module {
    private final String HEALTH = "HEALTH";
    private final String MODULENAME = "MODULENAME";
    public AutoHeal(ModuleData data) {
        super(data);
        this.settings.put(HEALTH, new Setting<>(HEALTH, 7, "Health to toggle any heal module.", 0.5, 1, 20));
        this.settings.put(MODULENAME, new Setting<Options>(MODULENAME, new Options("Heal Module", "DamageHeal", new String[]{"DamageHeal", "Regen"}), "The module that should be enabled"));

    }
    private final Module DamageHeal = Client.getModuleManager().get(info.sigmaclient.module.impl.combat.DamageHeal.class);
    private final Module Regen = Client.getModuleManager().get(info.sigmaclient.module.impl.player.Regen.class);

    @RegisterEvent(events={EventUpdate.class})
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            double health = ((Number) this.settings.get(HEALTH).getValue()).doubleValue();
            String healModule = ((Options)((Setting)this.settings.get(MODULENAME)).getValue()).getSelected();
            if (mc.thePlayer.getHealth() <= health) {
                this.toggleCurrentHealModule(healModule);
            }
        }
    }

    private void toggleCurrentHealModule(String name) {
        if (name.equalsIgnoreCase("Regen")) {
            if (!Regen.isEnabled()) {
                Regen.toggle();
            }
        } else if (name.equalsIgnoreCase("DamageHeal")) {
            if (!DamageHeal.isEnabled()) {
                DamageHeal.toggle();
            }
        }
    }
}
