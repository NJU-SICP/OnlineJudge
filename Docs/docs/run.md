---
sidebar_position: 2
---

# Run the Application

To start the application, you need to run the services first.

## Start the Services

At the root of repository, there is a file named `docker-compose.yml`.
This file describes all required services and their configuration.
You can simply start the services with the following command.

```shell
$ docker-compose up -d
```

As an alternative, you can install the required services on your computer and run them manually.
That is, you need to install and run the following services:

- MongoDB
- Minio
- RabbitMQ
- Docker Registry
- Redis

## Start the Server

After all services have started, edit the configuration file of server at `Server/src/main/resources/application.yml`.

You need to update the following lines:

- `sicp.admin`: information of an admin account
- `sicp.jwt`: information of JWT authentication
  - replace `sicp.jwt.secret` with a long random string

You can keep the rest unchanged and start the application using `gradle`:

```shell
$ cd Server
$ gradle bootRun
```
:::warning
If something went wrong, read through the output of `gradle` and figure out which service or which configuration causes the problem.
:::

## Start the Client

Finally, start the web client using `yarn`:

```shell
$ cd Client
$ yarn start
```

If everything works fine, you can open `http://localhost:3000` in your web browser and see the login screen.

:::tip
To use the `ok` client, see the next page about deploying the application.
:::
