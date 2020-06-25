
package fastminer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockGlowstone;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockWood;
import cn.nukkit.block.BlockWood2;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import fastminer.JsonUtil.BlockType;

public class Main extends PluginBase implements Listener
{
  public JsonUtil config;
  public String configFileLocation = "./plugins/FastMiner/config.json";
  public Vector<String> childCommandList = new Vector<>();
  // public Vector<FakeBlockBreakEvent> ignoreList2 = new Vector<>();
  public Vector<UUID> ignorePlayers = new Vector<>();
  public final Object lock = new Object();

  @Override
  public void onLoad()
  {
    try {
      this.childCommandList.add("help");
      this.childCommandList.add("toggle");
      this.childCommandList.add("enable");
      this.childCommandList.add("disable");
      this.childCommandList.add("reload");
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }
  
  @Override
  public void onEnable()
  {
    try {
      new File(new File(this.configFileLocation).getParent()).mkdirs();
      new File(this.configFileLocation).createNewFile();
      this.config = Main.parseJson(new String(Main.readFile(new File(this.configFileLocation)), "GBK"));
      if (this.config == null) {
        this.config = new JsonUtil();
        this.saveMyConfig();
      }
      Server.getInstance().getPluginManager().registerEvents(this, this);
      Server.getInstance().getCommandMap().getCommand("fm").addCommandParameters("child_cmd", new CommandParameter[] {
          new CommandParameter("child_cmd", this.childCommandList.stream().toArray(String[]::new))
      });
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDisable()
  {
    try {
      this.saveMyConfig();
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  public boolean isEnable(final String UUID)
  {
    boolean result = false;
    for (int i = 0; i < this.config.enable.size(); i++) {
      if (this.config.enable.get(i).uuid.equals(UUID)) {
        result = this.config.enable.get(i).enabled;
        break;
      }
    }
    return result;
  }

  public int getUUIDLocation(final String UUID)
  {
    int result = -1;
    for (int i = 0; i < this.config.enable.size(); i++) {
      if (this.config.enable.get(i).uuid.equals(UUID)) {
        result = i;
        break;
      }
    }
    return result;
  }

  public void setEnable(final String UUID, final boolean enable, final boolean save) throws Throwable
  {
    final int location = this.getUUIDLocation(UUID);
    final JsonUtil.EnableState temp = new JsonUtil.EnableState();
    temp.uuid = UUID;
    temp.enabled = enable;
    if (location == -1) {
      this.config.enable.add(temp);
    } else {
      this.config.enable.remove(location);
      this.config.enable.add(location, temp);
    }
    if (save && this.config.saveState) {
      this.saveMyConfig();
    }
  }

  @Override
  public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args)
  {
    try {
      if ("fm".equalsIgnoreCase(command.getName())) {
        if (args.length > 0) {
          if ("reload".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.reload")) {
              this.reloadMyConfig();
              sender.sendMessage("FastMiner Reload Complete!");
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("help".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.help")) {
              sender.sendMessage("childCommandList:");
              for (int i = 0; i < this.childCommandList.size(); i++) {
                sender.sendMessage(this.childCommandList.get(i));
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("toggle".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.toggle")) {
              if (sender instanceof Player) {
                final boolean temp = this.isEnable(((Player) sender).getUniqueId().toString());
                this.setEnable(((Player) sender).getUniqueId().toString(), !temp, true);
                if (temp) {
                  sender.sendMessage("已关闭连锁挖矿!");
                } else {
                  sender.sendMessage("已开启连锁挖矿!");
                }
              } else {
                sender.sendMessage("You are not a Player!");
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("enable".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.enable")) {
              if (sender instanceof Player) {
                this.setEnable(((Player) sender).getUniqueId().toString(), true, true);
                sender.sendMessage("已开启连锁挖矿!");
              } else {
                sender.sendMessage("You are not a Player!");
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else if ("disable".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("fastminer.disable")) {
              if (sender instanceof Player) {
                this.setEnable(((Player) sender).getUniqueId().toString(), false, true);
                sender.sendMessage("已关闭连锁挖矿!");
              } else {
                sender.sendMessage("You are not a Player!");
              }
            } else {
              sender.sendMessage("No Enough Permission");
            }
          } else {
            sender.sendMessage("Unknown ChildCommand!");
            if (sender.hasPermission("fastminer.help")) {
              sender.sendMessage("childCommandList:");
              for (int i = 0; i < this.childCommandList.size(); i++) {
                sender.sendMessage(this.childCommandList.get(i));
              }
            }
          }
          return true;
        }
      }
      return false;
    } catch (final Throwable e) {
      e.printStackTrace();
      return true;
    }
  }

  public static boolean isInnerCall()
  {
    StackTraceElement[] t=new Throwable().getStackTrace();
    for(StackTraceElement i : t)
    {
      if (i.getClassName().equals(Main.class.getName())&&"execute".equals(i.getMethodName())) {
        return true;
      }
    }
    return false;
  }
  
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent e)
  {
    if (this.config.notifyOnPlayerJoin) {
      e.getPlayer().sendMessage(
          "连锁挖矿已" + (this.isEnable(e.getPlayer().getUniqueId().toString()) ? "开启" : "关闭") + "! 请输入/fm toggle来切换开启状态!");
    }
  }

  public void lavaDetectInternal(final BlockBreakEvent e,final boolean force)
  {
    if (!e.isCancelled()) {
      if (!Main.isInnerCall() && !force) {
        return;
      }
      if (e.getPlayer().hasPermission("fastminer.use")) {
        if (this.isEnable(e.getPlayer().getUniqueId().toString()) && !Objects.equals(e.getPlayer().getGamemode(), 1)
            && this.isRightTools(e.getBlock(), e.getPlayer().getInventory().getItemInHand().getBlock())) {
          boolean ignore = false;
          synchronized (this.lock) {
            ignore = this.ignorePlayers.contains(e.getPlayer().getUniqueId());
          }
          if (ignore) {
            return;
          }
          if (this.hasLava(e.getBlock().getLocation(), e.getPlayer())) {
            if (this.config.lavaNotify) {
              final Player player = e.getPlayer();
              final Location block = e.getBlock().getLocation();
              player.sendMessage("方块 x=" + block.getFloorX() + " y=" + block.getFloorY() + " z=" + block.getFloorZ()
                  + " 周围有岩浆，已取消破坏事件!");
            }
            e.setCancelled(true);
            return;
          }
        }
      }
    }
  }
  
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onBlockDestroyHighest(final BlockBreakEvent e)
  {
    lavaDetectInternal(e, false);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
  public void onBlockDestroy(final BlockBreakEvent e)
  {
    /*
     * try { if (this.ignoreList2.contains(e)) { return; } } finally {
     * this.ignoreList2.clear(); }
     */
    if (!e.isCancelled()) {
      if (Main.isInnerCall()) {
        return;
      }
      if (e.getPlayer().hasPermission("fastminer.use")) {
        if (this.isEnable(e.getPlayer().getUniqueId().toString()) && !Objects.equals(e.getPlayer().getGamemode(), 1)
            && this.isRightTools(e.getBlock(), e.getPlayer().getInventory().getItemInHand().getBlock())) {
          boolean ignore = false;
          synchronized (this.lock) {
            ignore = this.ignorePlayers.contains(e.getPlayer().getUniqueId());
          }
          if (ignore) {
            return;
          }
          // e.setCancelled(true);
          lavaDetectInternal(e, true);
          this.execute(1, e.getBlock().getLocation(), e.getBlock(), e.getPlayer().getInventory().getItemInHand(),
              e.getPlayer(), true);

        }
      }
    }
  }

  public boolean isCustomBlock(final Block block)
  {
    for (final BlockType i : this.config.extraBlockType) {
      if (Block.equals(block, Block.get(i.id, i.damage >= 0 ? i.damage : 0), i.damage >= 0)) {
        return true;
      }
    }
    return false;
  }

  public boolean isRightTools(final Block block, final Block tools)
  {
    boolean result = false;
    /*
     * if(block==Material.DIAMOND_ORE || block==Material.EMERALD_ORE ||
     * block==Material.REDSTONE_ORE || block==Material.GLOWING_REDSTONE_ORE ||
     * block==Material.GOLD_ORE) if(tools==Material.IRON_PICKAXE ||
     * tools==Material.DIAMOND_PICKAXE) result=true; if(block==Material.COAL_ORE ||
     * block==Material.QUARTZ_ORE) if(tools==Material.WOOD_PICKAXE ||
     * tools==Material.STONE_PICKAXE || tools==Material.IRON_PICKAXE ||
     * tools==Material.DIAMOND_PICKAXE || tools==Material.GOLD_PICKAXE) result=true;
     * if(block==Material.GLOWING_REDSTONE_ORE || block==Material.LOG ||
     * block==Material.LOG_2) result=true; if(block==Material.IRON_ORE ||
     * block==Material.LAPIS_ORE) if(tools==Material.STONE_PICKAXE ||
     * tools==Material.IRON_PICKAXE|| tools==Material.DIAMOND_PICKAXE) result=true;
     */
    if ((block.getClass().getSimpleName().startsWith("BlockOre")) || (block instanceof BlockWood)
        || (block instanceof BlockWood2) || (block instanceof BlockGlowstone) || this.isCustomBlock(block)) {
      if (block.canHarvestWithHand() || block.canBeBrokenWith(tools.toItem())) {
        result = true;
      }
    }
    return result;
  }

  public static void subtractDurability(final Item tools, final Player player)
  {
    if (tools.isUnbreakable()) {
      return;
    }
    long level = 0;
    for (final Enchantment i : tools.getEnchantments()) {
      if (i.id == Enchantment.ID_DURABILITY) {
        level = i.getLevel();
        break;
      }
    }
    final int temp = new Random(System.nanoTime()).nextInt(100) + 1;
    if (temp <= (100 / (level + 1))) {
      tools.setDamage((tools.getDamage() + 1));
    }
  }

  public static boolean isOriginal(final Item tools)
  {
    for (final Enchantment i : tools.getEnchantments()) {
      if (i.id == Enchantment.ID_SILK_TOUCH) {
        return true;
      }
    }
    return false;
  }

  /*
   * public static int getLuckyCount(Material type,int level) { Random temp=new
   * Random(System.nanoTime()); int i = temp.nextInt(level + 2) - 1;
   *
   * if (i < 0) { i = 0; }
   *
   * return this.getCount(type,temp) * (i + 1); }
   */
  /*
   * public static int getCount(Material type,Random temp) { return
   * (type==Material.LAPIS_ORE)?4+temp.nextInt(5):1; }
   */
  public static int getLuckyLevel(final Item tools)
  {
    int result = 0;
    for (final Enchantment i : tools.getEnchantments()) {
      if (i.id == Enchantment.ID_FORTUNE_DIGGING) {
        result = i.getLevel();
        break;
      }
    }
    return result;
  }

  public static int getOriginalLevel(final Item tools)
  {
    int result = 0;
    for (final Enchantment i : tools.getEnchantments()) {
      if (i.id == Enchantment.ID_SILK_TOUCH) {
        result = i.getLevel();
        break;
      }
    }
    return result;
  }

  /*
   * public static ItemStack getLuckyBlockItem(Material block,byte data,int
   * count,int level) { ItemStack result=null; if(block==Material.COAL_ORE)
   * result=new ItemStack(Material.COAL,count); if(block==Material.EMERALD_ORE)
   * result=new ItemStack(Material.EMERALD,count); if(block==Material.DIAMOND_ORE)
   * result=new ItemStack(Material.DIAMOND,count);
   * if(block==Material.GLOWING_REDSTONE_ORE || block==Material.REDSTONE_ORE)
   * result=new ItemStack(Material.REDSTONE,count); if(block==Material.GLOWSTONE)
   * result=new ItemStack(Material.GLOWSTONE_DUST,count);
   * if(block==Material.GOLD_ORE || block==Material.IRON_ORE) result=new
   * ItemStack(block,count); if(block==Material.LAPIS_ORE) result=new
   * ItemStack(Material.INK_SACK,getLuckyCount(block,level),(short)4);
   * if(block==Material.QUARTZ_ORE) result=new ItemStack(Material.QUARTZ,count);
   * if(block==Material.LOG || block==Material.LOG_2) result=new
   * ItemStack(block,1,data); return result; }
   */
  public boolean hasLava(final Location block, final Player player)
  {
    if (player.hasPermission("fastminer.lavadetect") && this.config.lavaDetect) {
      if ((block.level.getBlockIdAt(block.getFloorX() + 1, block.getFloorY(), block.getFloorZ()) == BlockID.LAVA)
          || (block.level.getBlockIdAt(block.getFloorX() + 1, block.getFloorY(),
              block.getFloorZ()) == BlockID.STILL_LAVA)) {
        return true;
      }
      if ((block.level.getBlockIdAt(block.getFloorX(), block.getFloorY() + 1, block.getFloorZ()) == BlockID.LAVA)
          || (block.level.getBlockIdAt(block.getFloorX(), block.getFloorY() + 1,
              block.getFloorZ()) == BlockID.STILL_LAVA)) {
        return true;
      }
      if ((block.level.getBlockIdAt(block.getFloorX(), block.getFloorY(), block.getFloorZ() + 1) == BlockID.LAVA)
          || (block.level.getBlockIdAt(block.getFloorX(), block.getFloorY(),
              block.getFloorZ() + 1) == BlockID.STILL_LAVA)) {
        return true;
      }
      if ((block.level.getBlockIdAt(block.getFloorX() - 1, block.getFloorY(), block.getFloorZ()) == BlockID.LAVA)
          || (block.level.getBlockIdAt(block.getFloorX() - 1, block.getFloorY(),
              block.getFloorZ()) == BlockID.STILL_LAVA)) {
        return true;
      }
      if ((block.level.getBlockIdAt(block.getFloorX(), block.getFloorY() - 1, block.getFloorZ()) == BlockID.LAVA)
          || (block.level.getBlockIdAt(block.getFloorX(), block.getFloorY() - 1,
              block.getFloorZ()) == BlockID.STILL_LAVA)) {
        return true;
      }
      if ((block.level.getBlockIdAt(block.getFloorX(), block.getFloorY(), block.getFloorZ() - 1) == BlockID.LAVA)
          || (block.level.getBlockIdAt(block.getFloorX(), block.getFloorY(),
              block.getFloorZ() - 1) == BlockID.STILL_LAVA)) {
        return true;
      }
    }
    return false;
  }

  public void execute(final long depth, final Location block, final Block type, final Item tools, final Player player,
      final boolean nc)
  {
    if (depth >= this.config.maxDepth) {
      return;
    }
    Block originalType = type;
    if (originalType.getId() == BlockID.GLOWING_REDSTONE_ORE) {
      originalType = Block.get(BlockID.REDSTONE_ORE);
      originalType.setDamage(type.getDamage());
    }
    Block nowType = block.level.getBlock(block.getFloorX(), block.getFloorY(), block.getFloorZ());
    if (nowType.getId() == BlockID.GLOWING_REDSTONE_ORE) {
      nowType = Block.get(BlockID.REDSTONE_ORE);
      nowType.setDamage(block.level.getBlock(block.getFloorX(), block.getFloorY(), block.getFloorZ()).getDamage());
    }
    if (Block.equals(nowType, originalType, true)) {
      if (!player.isOnline()) {
        return;
      }
      if (!nc) {
        player.lastBreak=0;
        Item item=block.level.useBreakOn(block.asBlockVector3().asVector3(), tools, player, true);
        if(item!=null)
          player.getInventory().setItemInHand(item);
      }
      final Block before = tools.getBlock();
      if ((tools.getId() != BlockID.AIR) || (before.getId() == BlockID.AIR)) {
        this.execute(depth + 1, new Location(block.getX() + 1, block.getY(), block.getZ(), block.getLevel()), type,
            tools, player, false);
      }
      if ((tools.getId() != BlockID.AIR) || (before.getId() == BlockID.AIR)) {
        this.execute(depth + 1, new Location(block.getX() - 1, block.getY(), block.getZ(), block.getLevel()), type,
            tools, player, false);
      }
      if ((tools.getId() != BlockID.AIR) || (before.getId() == BlockID.AIR)) {
        this.execute(depth + 1, new Location(block.getX(), block.getY() + 1, block.getZ(), block.getLevel()), type,
            tools, player, false);
      }
      if ((tools.getId() != BlockID.AIR) || (before.getId() == BlockID.AIR)) {
        this.execute(depth + 1, new Location(block.getX(), block.getY() - 1, block.getZ(), block.getLevel()), type,
            tools, player, false);
      }
      if ((tools.getId() != BlockID.AIR) || (before.getId() == BlockID.AIR)) {
        this.execute(depth + 1, new Location(block.getX(), block.getY(), block.getZ() + 1, block.getLevel()), type,
            tools, player, false);
      }
      if ((tools.getId() != BlockID.AIR) || (before.getId() == BlockID.AIR)) {
        this.execute(depth + 1, new Location(block.getX(), block.getY(), block.getZ() - 1, block.getLevel()), type,
            tools, player, false);
      }
    }
  }

  public static byte[] readFile(final File file) throws Throwable
  {
    try (final FileInputStream input = new FileInputStream(file)) {
      final byte[] ret = new byte[input.available()];
      input.read(ret, 0, input.available());
      return ret;
    }
  }

  public static boolean writeFile(final File file, final byte[] content) throws Throwable
  {
    try (final FileOutputStream output = new FileOutputStream(file)) {
      output.write(content, 0, content.length);
      output.flush();
      return true;
    }
  }

  public static JsonUtil parseJson(final String json) throws Throwable
  {
    final Gson parse = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
    return parse.fromJson(json, JsonUtil.class);
  }

  public static String toJsonString(final JsonUtil json) throws Throwable
  {
    final Gson parse = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
    return parse.toJson(json);
  }

  public boolean saveMyConfig() throws Throwable
  {
    return Main.writeFile(new File(this.configFileLocation), Main.toJsonString(this.config).getBytes("GBK"));
  }

  public void reloadMyConfig() throws Throwable
  {
    this.config = Main.parseJson(new String(Main.readFile(new File(this.configFileLocation)), "GBK"));
  }
}
