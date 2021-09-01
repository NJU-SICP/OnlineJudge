# SICP Online Judge

## 文件结构

- `Server`：服务器代码，使用Java编写
- `Client`：web interface，是一个React应用，使用JavaScript编写
- `Ok`：command-line interface，使用Python编写，源代码来自Berkely

## 使用方式

1. 不同部分的开发配置方式具体见各个文件夹的`README.md`
2. 可以按照如下方式部署各个文件，使用`docker-compose`启动依赖程序，然后使用`systemd`来控制服务器程序（或者偷懒直接`java -jar sicp-CURRENT.jar`）

```
/srv/sicp
├── app # 放置jar包和配置文件
│   ├── application.properties
│   ├── sicp-x.y.z-SNAPSHOT.jar
│   └── sicp-CURRENT.jar -> /srv/sicp/app/sicp-x.y.z-SNAPSHOT.jar
├── data # 此文件夹不需要手动创建
│   ├── logs
│   ├── minio
│   ├── mongo
│   ├── rabbitmq
│   └── registry
└── docker-compose.yml
```

## 版本管理

1. `Client/src/config.js`
2. `Ok/client/__init__.py`，此处的修改需要同步到
  - `Server/src/main/java/cn/edu/nju/sicp/controllers/MiscController.java`
  - `Server/src/main/resources/static/misc/ok-client`
3. `Server/build.gradle`

## 版权声明

由于使用了来自Berkeley的Okpy源代码，本项目使用Apache License发布。
