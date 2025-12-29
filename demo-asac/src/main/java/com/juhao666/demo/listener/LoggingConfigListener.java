package com.juhao666.demo.listener;
import com.juhao666.demo.model.ConfigItem;
import com.juhao666.demo.store.RegistryStore;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;


/**
 * 配置变更监听器 - 独立实现类
 * 演示如何实现RegistryStore.ConfigListener接口
 */
@Component
public class LoggingConfigListener implements ConfigListener{
    @Autowired
    private RegistryStore registryStore;

    // 配置变更历史记录
    private final Map<String, List<ConfigChangeRecord>> changeHistory =
            new ConcurrentHashMap<>();

    // 统计信息
    private int totalNotifications = 0;
    /**
     * 获取配置变更历史
     */
    public List<ConfigChangeRecord> getChangeHistory(String dataId, String group) {
        String key = dataId + ":" + group;
        return changeHistory.getOrDefault(key, new CopyOnWriteArrayList<>());
    }

    /**
     * 获取总通知数
     */
    public int getTotalNotifications() {
        return totalNotifications;
    }

    /**
     * 获取所有配置的变更历史
     */
    public Map<String, List<ConfigChangeRecord>> getAllChangeHistory() {
        return new ConcurrentHashMap<>(changeHistory);
    }

    /**
     * 配置变更记录
     */
    public static class ConfigChangeRecord {
        private final String dataId;
        private final String group;
        private final long version;
        private final String md5;
        private final long timestamp;

        public ConfigChangeRecord(String dataId, String group, long version, String md5, long timestamp) {
            this.dataId = dataId;
            this.group = group;
            this.version = version;
            this.md5 = md5;
            this.timestamp = timestamp;
        }

        public String getDataId() {
            return dataId;
        }

        public String getGroup() {
            return group;
        }

        public long getVersion() {
            return version;
        }

        public String getMd5() {
            return md5;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "ConfigChangeRecord{" +
                    "dataId='" + dataId + '\'' +
                    ", group='" + group + '\'' +
                    ", version=" + version +
                    ", md5='" + md5 + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
    @Override
    public void onConfigChanged(String dataId, ConfigItem newConfig) {
        //todo
    }
}
