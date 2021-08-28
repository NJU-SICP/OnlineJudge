# SICP Online Judge

## 文件结构

- `Server`：服务器代码，使用Java编写
- `Client`：web interface，是一个React应用，使用JavaScript编写
- `Ok`：command-line interface，使用Python编写，源代码来自Berkely

## 版本管理

1. `Client/src/config.js`
2. `Ok/client/__init__.py`，此处的修改需要同步到
  - `Server\src\main\java\cn\edu\nju\sicp\controllers\MiscController.java`
  - `Server\src\main\resources\static\misc\ok-client`
3. `Server\build.gradle`

## 版权声明

由于使用了来自Berkeley的Okpy源代码，本项目使用Apache License发布。
