package com.juhao666.demo.listener;

import com.juhao666.demo.model.ConfigItem;

// 监听器接口
public interface ConfigListener {
    void onConfigChanged(String dataId, ConfigItem newConfig);
}
