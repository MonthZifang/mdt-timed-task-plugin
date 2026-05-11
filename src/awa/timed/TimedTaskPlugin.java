package awa.timed;

import arc.Events;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import arc.util.Timer.Task;
import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.game.EventType.ServerLoadEvent;
import mindustry.io.SaveIO;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.WorldReloader;

import java.util.concurrent.ThreadLocalRandom;

public class TimedTaskPlugin extends Plugin{
    private static final String PLUGIN_NAME = "timed-task-plugin";

    private TimedTaskConfig config;
    private Task autoHostTask;

    @Override
    public void init(){
        reloadConfig();
        Events.on(ServerLoadEvent.class, event -> scheduleAutoHost());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("timedreload", "从磁盘重新加载定时任务插件配置。", args -> {
            reloadConfig();
            Log.info("已重新加载定时任务插件配置。");
        });
    }

    private void reloadConfig(){
        config = TimedTaskConfig.load(resolveConfigFile());
        scheduleAutoHost();
    }

    private Fi resolveConfigFile(){
        Fi folder = new Fi("config").child("mods").child("config").child(PLUGIN_NAME);
        folder.mkdirs();
        return folder.child("config.json");
    }

    private void scheduleAutoHost(){
        if(autoHostTask != null){
            autoHostTask.cancel();
            autoHostTask = null;
        }

        if(config == null || !config.autoHostEnabled){
            return;
        }

        float delaySeconds = Math.max(config.autoHostDelaySec, 0f);
        autoHostTask = Timer.schedule(() -> {
            if(Vars.net != null && !Vars.net.server() && Vars.netServer != null){
                Map selected = pickRandomMap();
                if(selected == null){
                    Log.err("定时任务插件未找到可用于开服的有效地图。");
                    return;
                }

                if(!loadMap(selected)){
                    return;
                }

                Vars.netServer.openServer();
                Log.info("定时任务插件已在 @ 秒后使用地图 @ 自动开服。", delaySeconds, selected.plainName());
            }
        }, delaySeconds);
    }

    private Map pickRandomMap(){
        Vars.maps.reload();

        Seq<Map> source = Vars.maps.customMaps();
        if(source == null || source.isEmpty()){
            Log.err("定时任务插件未在 config/maps 中找到外部地图。");
            return null;
        }

        Seq<Map> valid = new Seq<>();
        for(Map map : source){
            if(map != null && map.file != null && SaveIO.isSaveValid(map.file)){
                valid.add(map);
            }
        }

        if(valid.isEmpty()){
            Log.err("定时任务插件未在 config/maps 中找到有效的外部地图。");
            return null;
        }

        return valid.get(ThreadLocalRandom.current().nextInt(valid.size));
    }

    private boolean loadMap(Map map){
        if(map == null){
            return false;
        }

        WorldReloader reloader = new WorldReloader();
        try{
            reloader.begin();
            Gamemode mode = Vars.state != null && Vars.state.rules != null ? Vars.state.rules.mode() : Gamemode.survival;
            Vars.world.loadMap(map, map.applyRules(mode));
            Vars.state.rules = map.applyRules(mode);
            Vars.logic.play();
            reloader.end();
            return true;
        }catch(Throwable t){
            Log.err("定时任务插件加载地图 @ 失败。", map.plainName());
            Log.err(t);
            Log.err("地图加载错误: @", Strings.getSimpleMessage(t));
            return false;
        }
    }
}
