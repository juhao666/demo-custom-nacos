package com.juhao666.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ConfigItem {
    private String dataId;           // 配置ID
    private String group;            // 分组
    private String content;          // 配置内容
    private String type;             // 类型：properties, yaml, json, xml
    private long version;            // 版本号
    private String md5;              // 内容MD5
    private long updateTime;         // 更新时间
    public String getDataId() { return dataId; }
    public void setDataId(String dataId) { this.dataId = dataId; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getMd5() { return md5; }
    public void setMd5(String md5) { this.md5 = md5; }

    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
