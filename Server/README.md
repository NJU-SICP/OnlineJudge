# Online Judge 后端

## 程序缺陷

1. 编译Docker容器和评测代码的任务可能失败，不会自动重试

## 使用方式

需要Java 11，首先用Gradle安装依赖（直接用的IDEA比较省事）。

本地开发或者服务器运行的时候，需要有Docker，并配置运行MongoDB（数据库）、Minio（对象存储）、RabbitMQ（消息队列）和Docker Registry四个服务。
（安装这些服务比较麻烦，这里可以直接运行`docker-compose.yml`）。
Docker默认使用Unix文件监听请求，可以通过修改systemd配置的方式实现通过TCP监听。

1. 修改`application.properties`配置各种服务的地址，其他按需修改。
2. `gradle bootRun`运行程序。
3. `gradle bootJar`生成jar包，和`application.properties`一起复制到服务器上，然后通过`java -jar sicp-xxx.jar`即可运行。

## Ok客户端部署方式

1. 使用`python3 -m client.cli.publish`生成可执行文件
2. 将`ok`放置在`resources/static/misc/ok-client`文件夹中，改名为版本号
3. 切记要修改`MiscController`中的Ok版本号
