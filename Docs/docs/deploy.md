---
sidebar_position: 3
---

# Deploy the Application

## Build a Server Package

Spring framework comes with several scripts to compile the server and run it everywhere.
By default, running the following command will create a jar package and can be executed with `java -jar xxx.jar`.

```shell
$ cd Server
$ gradle bootJar # output at build/lib/sicp-xxx.jar
```

We can then copy the jar package to server.

:::tip
To learn more about how to package the server and run it with `systemd`, or package it into a single executable file,
refer to the documentation of Spring: [https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html).
:::

Before running the application, we also need another `application.yml` to setup configurations in production environment.
We can create a new file or just copy the local one to the server, and place it inside the same folder with the jar file.

:::warning
There are some important notes on hosting the server application:

1. Do not expose services to public. Before running the services defined in `docker-compose.yml`, update the port forwarding so that these ports are not accessible from the public.
2. Add user authentication to services. Keep mongo, minio, rabbitmq, registry and redis secure using passwords.
3. Change number of worker threads according to server capacity. This requires changing the concurrency of AMQP listeners in file `Server/src/main/java/cn/edu/nju/sicp/configs/AmqpConfig.java`. You need to rebuild the jar package after updating this line.
4. Setup logging. You should save logs into files and keep them rotated. See Spring docs [https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging.file-output](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging.file-output) for more details.
5. Setup HTTPS. You can either use a reverse proxy like Apache, Caddy, Nginx; or simply let Java Servlet do that for you. You will need a JKS format key store to save your certificate. See Spring docs [https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl) for more details. 
:::

## Build a Web Client Package

By default, the web client will be served at a sub-path of the course website.
This means we need to tell `yarn` where the files will be located by an environment variable.

```shell
$ cd Client
$ PUBLIC_URL=https://nju-sicp.bitbucket.io/oj yarn build
```

After packaging the files, simply copy the output folder `build` to the corresponding location of the course website.

## Build a CLI Client Package

To create an `ok` executable file, we need to create a Python virtual environment first.

```shell
$ cd Ok
$ python3 -m venv env
$ ./env/scripts/Activate.ps1 # windows
$ source ./env/bin/activate  # unix
```

Then, in the virtual environment, run the following command to package an executable Python file.

```shell
$ python3 -m client.cli.publish # will create a file named 'ok'
```

You can put that file into `Server/src/main/resources/static/misc/ok-client` and update the corresponding version string in `Server/src/main/java/cn/edu/nju/sicp/controllers/MiscController.java` so that `ok` can automatically update itself.
