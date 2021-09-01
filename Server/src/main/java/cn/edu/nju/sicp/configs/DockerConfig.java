package cn.edu.nju.sicp.configs;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "spring.application.docker")
public class DockerConfig {

    private String host;
    private String tlsVerify;
    private String tlsCertPath;
    private String registryUrl;
    private String registryEmail;
    private String registryUsername;
    private String registryPassword;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTlsVerify() {
        return tlsVerify;
    }

    public void setTlsVerify(String tlsVerify) {
        this.tlsVerify = tlsVerify;
    }

    public String getTlsCertPath() {
        return tlsCertPath;
    }

    public void setTlsCertPath(String tlsCertPath) {
        this.tlsCertPath = tlsCertPath;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public String getRegistryEmail() {
        return registryEmail;
    }

    public void setRegistryEmail(String registryEmail) {
        this.registryEmail = registryEmail;
    }

    public String getRegistryUsername() {
        return registryUsername;
    }

    public void setRegistryPassword(String registryPassword) {
        this.registryPassword = registryPassword;
    }

    public String getRegistryPassword() {
        return registryPassword;
    }

    public void setRegistryUsername(String registryUsername) {
        this.registryUsername = registryUsername;
    }

    @Override
    public String toString() {
        return String.format("DockerConfig{host='%s', tls='%s', registry='%s'}", host, tlsVerify,
                registryUrl);
    }

    public DockerClient getInstance() {
        DockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(host)
                .withDockerTlsVerify(tlsVerify)
                .withDockerCertPath(tlsCertPath)
                .withRegistryUrl(registryUrl)
                .build();
        DockerHttpClient client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(60))
                .build();
        return DockerClientImpl.getInstance(config, client);
    }

}
