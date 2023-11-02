package info.sigmaclient.module.impl.combat;


import info.sigmaclient.event.Event;
import info.sigmaclient.management.notifications.Notifications;
import info.sigmaclient.module.Module;
import info.sigmaclient.module.data.ModuleData;
import info.sigmaclient.module.data.Options;
import info.sigmaclient.module.data.Setting;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import java.util.Random;


public class Kick extends Module {

    public Kick(ModuleData data) {
        super(data);
        this.settings.put("MODE", new Setting("Mode", new Options(
                        "Mode",
                        "SelfHurt",
                        new String[] {"InvalidPacket", "IllegalChat", "PacketSpam", "SelfHurt"}),
                        "Mode"
                )
        );
    }

    public void onEnable() {
        super.onEnable();
        if (mc.isIntegratedServerRunning()) {
            Notifications.getManager().post("Kick", "§4§lYou can't use kick in single-player.", 1500L, Notifications.Type.WARNING);
        }
        String currentmode = ((Options)((Setting)this.settings.get("MODE")).getValue()).getSelected();
        switch(currentmode) {
            case "InvalidPacket":
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, !mc.thePlayer.onGround
            ));
            case "SelfHurt": mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(mc.thePlayer, C02PacketUseEntity.Action.ATTACK));

            case "IllegalChat": mc.thePlayer.sendChatMessage(new Random().nextInt() + "§§§" + new Random().nextInt());
            case "PacketSpam":
                Random random = new Random();
                for (int d = 0; d < 9999; d++) {
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                            (double) d,
                            (double) d,
                            (double) d,
                            random.nextBoolean()));
                }
        }
        this.toggle();
    }

    @Override
    public void onEvent(Event event) {
    }
}