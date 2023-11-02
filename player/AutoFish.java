package info.sigmaclient.module.impl.player;

import info.sigmaclient.Client;
import info.sigmaclient.event.Event;
import info.sigmaclient.event.RegisterEvent;
import info.sigmaclient.event.impl.EventUpdate;
import info.sigmaclient.module.Module;
import info.sigmaclient.module.data.ModuleData;
import info.sigmaclient.module.data.Options;
import info.sigmaclient.module.data.Setting;
import info.sigmaclient.util.Timer;
import info.sigmaclient.util.misc.ChatUtil;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C01PacketChatMessage;


import java.util.*;
import java.util.stream.Collectors;

public class AutoFish extends Module {
    HashMap<String, List<String>> usedPhrases = new HashMap<>();
    private boolean sleeping = false;
    private static final String chatPrefix = "[AutoFish]: ";
    private final Module Kick = Client.getModuleManager().get(info.sigmaclient.module.impl.combat.Kick.class);
    private Timer timer = new Timer();
    private Timer invtimer = new Timer();
    private final String[] enLeavePhrases = new String[] {
            "...", // english phrases
    };

    private final String[] ruLeavePhrases = new String[] {
            "..." // your lang's phrases
    };

    private String messages_mode = "Disabled";
    private int stage;
    private final String[] phraseList = new String[]{"/fix", "/efix", "/feed"};
    ArrayList<ItemStack> ignoreFishRoads = new ArrayList<ItemStack>();

    private final String[] noStackArr = new String[] {
            "item.bow", "item.saddle"
    };

    private final String[] junk = new String[] {
            "tile.waterlily", "item.string", "item.stick",
            "item.bootsCloth", "item.bowl", "tile.tripWireSource",
            "item.leather"
    };

    public AutoFish(ModuleData data) {
        super(data);
        this.settings.put("SWITCHDURABILITY", new Setting<Integer>("SWITCH-DURABILITY", 5, "Durability to switch rod or stop fishing.", 1.0, 1.0, 64.0));
        this.settings.put("AUTOLEAVE", new Setting<Boolean>("Auto-Leave", false, "SELECT LEAVE METHOD IN KICK MODULE. Toggles kick at low armor strength or health level."));
        this.settings.put("LEAVEMSG", new Setting<Options>("Leave message", new Options("Priority", "English", new String[]{"English", "Russian", "Disabled"}), "Sends message to chat when auto leave is triggered."));
        this.settings.put("HEALTH", new Setting<Integer>("Health", 7, "Health level to leave.", 1, 7, 20));
        this.settings.put("ARMOR", new Setting<Integer>("Min. Armor", 26, "Durability of armor to leave.", 4, 14, 527));
        this.settings.put("NOJUNK", new Setting<Boolean>("Drop junk", false, "Automatically drops junk (ex. Stick) items from your inventory."));
        this.settings.put("ONLYSTACK", new Setting<Boolean>("Drop no-stack", false, "Automatically drop items which can't be stacked."));
        this.settings.put("AUTOCMD", new Setting<Boolean>("Commands", false, "Automatically use commands like /feed, /fix, etc."));
    }

    @Override
    public void onDisable() {
        this.stage = 0;
    }

    @Override
    public void onEnable() {
        this.sleeping = false;
        this.sortRods();
        usedPhrases = new HashMap<>();
        // this.rodArray = this.
    }

    @RegisterEvent(events = {EventUpdate.class})
    public void onEvent(Event event) {
        if (((Boolean)((Setting)this.settings.get("AUTOLEAVE")).getValue()).booleanValue()) {
            this.messages_mode = ((Options)((Setting)this.settings.get("LEAVEMSG")).getValue()).getSelected();
            this.shouldLeave();
        }
        if (event instanceof EventUpdate) {
            if (invtimer.delay(30000f)) {
                this.invController();
                invtimer.reset();
            }
            if (((EventUpdate) event).isPre()) {
                if (((Boolean)((Setting)this.settings.get("AUTOCMD")).getValue()).booleanValue()) {
                    this.shouldFix();
                }
            }

            ItemStack currentItem = mc.thePlayer.inventory.getCurrentItem();
            if (!isEmptySlot(currentItem)) {
                if (currentItem.getItem() instanceof ItemFishingRod) {
                    if (!isIgnored(currentItem)) {
                        if (mc.thePlayer.fishEntity != null && mc.thePlayer.fishEntity.motionX == 0.0D && mc.thePlayer.fishEntity.motionZ == 0.0D && mc.thePlayer.fishEntity.motionY != 0.0D) {
                            this.rightClick();
                            this.updateIgnoreList(currentItem);

                        }
                    }
                }
            } else {
                this.sortRods();
            }

            if (this.stage > 0) {
                --this.stage;
                return;
            }
            if (mc.thePlayer.fishEntity != null) {
                return;
            }

            if (!isIgnored(currentItem) && !this.sleeping) {
                System.out.println(getLure(currentItem));
                this.rightClick();
            }
        }
    }


    private void shouldLeave() {
        int minArmor = ((Number)((Setting)this.settings.get("ARMOR")).getValue()).intValue();
        int minHealth = ((Number)((Setting)this.settings.get("HEALTH")).getValue()).intValue();
        for (ItemStack armor: mc.thePlayer.inventory.armorInventory) {
            if (armor == null) continue;
            int durability = armor.getMaxDamage() - armor.getItemDamage();
            if ((mc.thePlayer.hurtTime > 0 && durability <= minArmor) || mc.thePlayer.getHealth() <= minHealth) {
                if (durability <= minArmor) {
                    if (this.isAlive()) {
                        this.leavePhrase();
                    }
                }
                this.toggle();
                Kick.toggle();
                return;
            }
        }
    }

    private boolean isAlive() {
        if (mc.currentScreen != null) {
            if (mc.currentScreen instanceof GuiGameOver || !mc.thePlayer.isEntityAlive()) {
                return false;
            }
        } else if (!mc.thePlayer.isEntityAlive()) {
            return false;
        };
        return true;
    }
    private void leavePhrase() {
        String mode = this.messages_mode;
        String msg;
        switch (mode) {
            case "Disabled": return;
            case "Russian":
                msg = chatPrefix + getRandomStringFromArray(this.ruLeavePhrases).replaceAll("(ip|me|ls|(?i))", "");;
                ChatUtil.sendChat(msg);
                break;
            case "English":
                msg = chatPrefix + getRandomStringFromArray(this.enLeavePhrases).replaceAll("(ip|me|ls|(?i))", "");;
                ChatUtil.sendChat(msg);
                break;
        }
    }
    private boolean validPhrase(String p) {
        return !Arrays.asList(this.ruLeavePhrases).contains(p) && !Arrays.asList(this.enLeavePhrases).contains(p);
    }

    public static String getRandomStringFromArray(String[] arr) {
        return arr[new Random().nextInt(arr.length)];
    }

    private void shouldFix() {
        if (this.timer.delay((float)this.randomDelay())) {
            for (String p: phraseList) {
                mc.thePlayer.sendQueue.addToSendQueue(new C01PacketChatMessage(p));
            }
            this.timer.reset();
        }
    }

    public void swap(int slot1, int hotbarSlot){
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot1, hotbarSlot, 2, mc.thePlayer);
    }

    public void drop(int slot){
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 1, 4, mc.thePlayer);
    }

    public static int getIndexFromStack(ItemStack item) {
        for (int i = 0; i < 45; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() == item) {
                return i;
            }
        }
        return 999;
    }

    public int getLure(ItemStack r) {
        return EnchantmentHelper.getEnchantmentLevel(Enchantment.lure.effectId, r);
    }

    private void invController() {
        boolean nojunk = ((Boolean)((Setting)this.settings.get("NOJUNK")).getValue()).booleanValue();
        boolean onlystack = ((Boolean)((Setting)this.settings.get("ONLYSTACK")).getValue()).booleanValue();
        this.sleeping = true;
        for (int i = 9; i < 45; i++) {
            ItemStack slot = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (slot != null) {
                Item item = slot.getItem();
                if (item instanceof ItemPotion) {
                    if (slot.getMetadata() == 0 && onlystack) {
                        drop(i);
                    }
                }
                else if ((Arrays.asList(this.junk).contains(slot.getUnlocalizedName())) && nojunk) {
                    drop(i);
                }
                else if ((Arrays.asList(this.noStackArr).contains(slot.getUnlocalizedName())) && onlystack) {
                    drop(i);
                }
            }
        }
        this.sleeping = false;
    }
    private void sortRods() {
        boolean haslure = false;
        this.sleeping = true;
        List<ItemStack> rods = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack c = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (!isEmptySlot(c)) {
                if (c.getItem() instanceof ItemFishingRod) {
                    if (!isIgnored(c)) {
                        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.lure.effectId, c) > 0) {
                            haslure = true;
                        }
                        rods.add(c);
                    }
                    //mc.thePlayer.inventory.sta()
                }
            }
        }
        if (haslure) {
            rods.sort(
                    (Comparator.comparingInt(this::getLure))
            );
        }
        int rc = rods.size();
        if (rc > 0) {
            ItemStack bestRod = rods.get(rc - 1);
            if (mc.thePlayer.inventoryContainer.getSlot(36).getStack() != bestRod) {
                int r = getIndexFromStack(bestRod);
                if (r != 999) swap(r, 0);
            }
        }
        this.sleeping = false;
    }

    private boolean isIgnored(ItemStack rod) {
        return this.ignoreFishRoads.contains(rod);
    }

    private void updateIgnoreList(ItemStack rod) {
        int current_durability = rod.getMaxDamage() - rod.getItemDamage();
        int switchDur = ((Number)((Setting)this.settings.get("SWITCHDURABILITY")).getValue()).intValue();
        if (current_durability <= switchDur && !this.isIgnored(rod)) {
            this.ignoreFishRoads.add(rod);
            this.sortRods();
        }
        else if (this.isIgnored(rod)) {
            if (current_durability > switchDur) {
                this.ignoreFishRoads.remove(rod);
            }
        }
        if (getLure(rod) < 3) {
            this.sortRods();
        }
    }

    public static boolean isEmptySlot(ItemStack slot) {
        return slot == null;
    }

    private void rightClick() {
        ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
        if (!isEmptySlot(stack) && stack.getItem() instanceof ItemFishingRod) {
            mc.rightClickMouse();
            this.stage = 15;
        }
    }

    private int randomDelay() {
        Random randy = new Random();
        return randy.nextInt(30000) + 30000;
    }
}