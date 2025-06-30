package com.kuma.tools.dynamicHotCompute.register;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${spring.cloud.nacos.config.server-addr:212.129.223.152:8848}")
    private String serverAddr;
    @Value("${spring.regist.servername:hotkey-netty}")
    private String serverName;
    @Value("${spring.regist.namespace:public}")
    private String namespace;
    @Value("${server.port:8080}")
    private int port;

    private NamingService namingService;
    private String localHost;

    @Override
    @PostConstruct
    public void initChannel() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            localHost = inetAddress.getHostAddress();
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
            namingService.registerInstance(serverName, localHost, port);
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
