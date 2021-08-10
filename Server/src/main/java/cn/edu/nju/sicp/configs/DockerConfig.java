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

    @Override
    public String toString() {
        return "DockerConfig{" +
                "host='" + host + '\'' +
                ", tlsVerify='" + tlsVerify + '\'' +
                ", tlsCertPath='" + tlsCertPath + '\'' +
                '}';
    }

    public DockerClient getInstance() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(host)
                .withDockerTlsVerify(tlsVerify)
                .withDockerCertPath(tlsCertPath)
                .build();
        DockerHttpClient client = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, client);
    }

}
