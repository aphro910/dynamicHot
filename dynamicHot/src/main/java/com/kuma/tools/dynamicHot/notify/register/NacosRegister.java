package com.kuma.tools.dynamicHot.notify.register;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.kuma.tools.dynamicHot.notify.netty.NettyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.register.type", havingValue = "nacos", matchIfMissing = true)
public class NacosRegister implements Register {

    @Value("${spring.cloud.nacos.config.server-addr:212.129.223.152:8848}")
    private String serverAddr;
    @Value("${spring.regist.servername:im-netty}")
    private String serverName;
    @Value("${spring.regist.namespace:public}")
    private String namespace;
    @Value("${spring.regist.port:8088}")
    private int port;

    @Autowired
    NettyClient nettyClient;

    private NamingService namingService;
    private String localHost;

    @Override
    @PostConstruct
    public void initChannel() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            localHost = inetAddress.getHostAddress();

            channelInitialize();
            watchService(serverName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void channelInitialize() {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", namespace);
        try {
            namingService = NamingFactory.createNamingService(properties);
            List<Instance> instanceList = namingService.getAllInstances(serverName);
            for (Instance instance : instanceList) {
                nettyClient.connect(instance.getIp(), port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听netty的上下线
     */
    public void watchService(String serviceName) throws Exception {
        namingService.subscribe(serviceName, new EventListener() {
            @Override
            public void onEvent(Event event) {
                if (event instanceof NamingEvent) {
                    NamingEvent namingEvent = (NamingEvent) event;
                    List<Instance> instanceList = namingEvent.getInstances();
                    for (Instance instance : instanceList) {
                        nettyClient.connect(instance.getIp(), port);
                    }
                }
            }
        });
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
