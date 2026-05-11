package awa.timed;

import arc.files.Fi;
import arc.util.Log;
import arc.util.serialization.Json;

public class TimedTaskConfig{
    public boolean autoHostEnabled = true;
    public float autoHostDelaySec = 5f;
    public boolean customMapsOnly = true;

    public static TimedTaskConfig load(Fi file){
        TimedTaskConfig defaults = new TimedTaskConfig();
        defaults.sanitize();

        if(file == null){
            return defaults;
        }

        Json json = new Json();
        json.setIgnoreUnknownFields(true);

        try{
            if(file.parent() != null){
                file.parent().mkdirs();
            }

            if(!file.exists()){
                file.writeString(render(defaults), false, "UTF-8");
                return defaults;
            }

            TimedTaskConfig loaded = json.fromJson(TimedTaskConfig.class, file.readString("UTF-8"));
            if(loaded == null){
                file.writeString(render(defaults), false, "UTF-8");
                return defaults;
            }

            loaded.sanitize();
            file.writeString(render(loaded), false, "UTF-8");
            return loaded;
        }catch(Throwable t){
            Log.err("加载定时任务配置失败，正在重写默认配置。");
            Log.err(t);
            try{
                file.writeString(render(defaults), false, "UTF-8");
            }catch(Throwable writeError){
                Log.err(writeError);
            }
            return defaults;
        }
    }

    public void sanitize(){
        autoHostDelaySec = Math.max(autoHostDelaySec, 0f);
    }

    private static String render(TimedTaskConfig cfg){
        StringBuilder out = new StringBuilder(256);
        out.append("{\n");
        out.append("  \"autoHostEnabled\": ").append(cfg.autoHostEnabled).append(",\n");
        out.append("  \"autoHostDelaySec\": ").append(cfg.autoHostDelaySec).append(",\n");
        out.append("  \"customMapsOnly\": ").append(cfg.customMapsOnly).append("\n");
        out.append("}\n");
        return out.toString();
    }
}
