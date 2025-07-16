package com.kuma.tools.dynamicHotCompute.register;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.Properties;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.register.type", havingValue = "nacos", matchIfMissing = true)
public class NacosRegister implements Register {

    @Value("${spring.cloud.nacos.config.server-addr:127.0.0.1:8848}")
    private String serverAddr;
    @Value("${spring.register.servername:hotkey-netty}")
    private String serverName;
    @Value("${spring.register.namespace:public}")
    private String namespace;
    @Value("${server.port:8080}")
    private int port;
    @Value("${spring.register.ip:}")
    private String ip;

    private NamingService namingService;


    @Override
    @PostConstruct
    public void initChannel() {
        try {
            if (ip == null || ip.isEmpty()) {
                InetAddress inetAddress = InetAddress.getLocalHost();
                ip = inetAddress.getHostAddress();
            }
            register();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void register() {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", namespace);
        try {
            namingService = NamingFactory.createNamingService(properties);
            namingService.registerInstance(serverName, ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            namingService.shutDown();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
