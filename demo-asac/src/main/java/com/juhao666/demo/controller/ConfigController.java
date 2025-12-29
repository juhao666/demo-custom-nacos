package com.juhao666.demo.controller;

import com.juhao666.demo.RegistryCenterApplication;
import com.juhao666.demo.model.Result;
import com.juhao666.demo.model.ConfigItem;
import com.juhao666.demo.model.Response;
import com.juhao666.demo.store.RegistryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1")
public class ConfigController {

    @Autowired
    RegistryStore registryStore;
    private String generateConfigKey(String dataId, String group) {
        return dataId + ":" + group;
    }

    // 配置监听器
    private static final Map<String, List<DeferredResult<Result>>> CONFIG_LISTENERS = new ConcurrentHashMap<>();
    /**
     * 获取配置接口
     */
    @GetMapping("/config")
    public Result getConfig(@RequestParam String dataId,
                            @RequestParam(required = false, defaultValue = "DEFAULT_GROUP") String group) {
        try {
            //String key = generateConfigKey(dataId, group);
            ConfigItem config = registryStore.getConfig(dataId, group); //CONFIG_STORE.get(key);

            if (config == null) {
                return Response.error("配置不存在");
            }

            return Response.success("获取配置成功", config);
        } catch (Exception e) {
            return Response.error("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 发布配置接口
     */
    @PostMapping("/config")
    public Result publishConfig(@RequestBody ConfigItem config) {
        try {
            ConfigItem publishedConfig = registryStore.publishConfig(config);
            return Response.success("配置发布成功", publishedConfig);
        } catch (Exception e) {
            return Response.error("配置发布失败: " + e.getMessage());
        }
    }

    /**
     * 配置监听接口（长轮询）
     */
    //todo
    @GetMapping("/config/listener")
    public DeferredResult<Result> listenConfig(@RequestParam String dataId,
                                               @RequestParam(required = false, defaultValue = "DEFAULT_GROUP") String group,
                                               @RequestParam(required = false) String md5) {
        System.out.printf("长轮询调用开始 dataId=%s, group=%s%n", dataId, group);
        String key = generateConfigKey(dataId, group);
        ConfigItem currentConfig = registryStore.getConfig(dataId, group); //CONFIG_STORE.get(key);
        DeferredResult<Result> deferredResult = new DeferredResult<>(30000L);
        deferredResult.onTimeout(() -> {
            deferredResult.setResult(Response.success("监听超时"));
        });

        // 检查配置是否变更
        if (currentConfig != null && md5 != null && !currentConfig.getMd5().equals(md5)) {
            deferredResult.setResult(Response.success("配置已变更", currentConfig));
        } else {
            // 添加到监听列表
            CONFIG_LISTENERS.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
            CONFIG_LISTENERS.get(key).add(deferredResult);

            // 设置完成回调
            deferredResult.onCompletion(() -> {
                List<DeferredResult<Result>> listeners = CONFIG_LISTENERS.get(key);
                if (listeners != null) {
                    listeners.remove(deferredResult);
                }
            });
        }
        System.out.printf("长轮询调用结束 result=%s%n", deferredResult.getResult());
        return deferredResult;
    }



}
