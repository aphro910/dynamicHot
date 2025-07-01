# DynamicHot

 

### What


DynamicHot是一个基于Netty的轻量化hot-key监测系统，用户可通过注解的方式对需要监测的接口进行包装，动态地将当前接口hot-key的内容加入到本地缓存（caffeine），提升接口访问性能，并节省宝贵的JVM内存资源  
压力测试：  
在单机单channel条件下测试，netty server每秒可写入7w条来自client的汇报数据(每条汇报数据包含单个key的聚合次数)  
更详细的压测数据需要在分布式条件下测试，目前只有一台电脑，没有条件进行。


### How

本项目设计思路参考了京东的hotkey项目，得到了不少启发。项目地址如下：
> https://gitee.com/jd-platform-opensource/hotkey

 **Client:** 
通过注解@DynamicHot对接口进行增强，包含两个参数tableName、key,tableName表示需要查询的数据表名称(仅做区分，不一定为真实表名),key表示该请求中需要查询数据的unique key(支持spel表达式)  

e.g:

```
@DynamicHot(tableName="user",key="#userId")
public User getUser(Long userId){
    //数据查询逻辑
}
```


被注解包装的方法会在请求前获取到用户请求的tablename和key，并生成对应的request key,缓存到本地JVM内存中进行统计，通过定时任务定期通过Netty的长连接发送给对应的server进行汇总并计算。  
client发送数据时会根据每个request key进行hashcode取模，选择Netty server集群中对应的server进行发送，即每一个request key都有一个相应的server，保证该key在全局视图的一致性


 **Server:** 
Server收到Client汇报的数据，保存至本地JVM内存进行统计，定期对统计数据进行计算，计算逻辑为：汇报时间在设定的窗口时间内，request key被统计的总次数大于等于设定的hot-key阈值。
同样举个例子  

e.g：
client的汇报数据为{key:"item_123456",count:80,timestamp:123456789},{key:"item_123456",count:56,timestamp:123456987},......  
server计算时，判断 {当前时间戳}-{设定的时间窗口} <= timestamp , true=> totalcount += count;若totalcount >= {hot-key阈值},则通知Client客户端进行更新  


提供了一些配置参数以适配各方面的需求:  
spring.dynamic.hotkey.report.maxSize//Client的本地request key缓存keyMap的大小，默认为-1(不限)，大于0时，当keymap的size大于该值，则会通过LRU算法进行淘汰  
spring.dynamic.hotkey.collect.initial-delay//Client定时任务启动后等待时间，单位ms  
spring.dynamic.hotkey.collect.fixed-rate//Client定时任务执行频率，单位ms  

spring.dynamic.hotkey.compute.initial-delay//Server定时任务启动后等待时间，单位ms  
spring.dynamic.hotkey.compute.fixed-rate//Server定时任务执行频率，单位ms  
spring.dynamic.hotkey.compute.maxKeySize//Server的本地request key缓存keyMap的大小，默认为-1(不限)，大于0时，当keymap的size大于该值，则会通过LRU算法进行淘汰  
spring.dynamic.hotkey.compute.timeRange//HotKey的时间窗口,单位ms,默认5000  
spring.dynamic.hotkey.compute.hotCount//HotKey阈值，默认5000  

spring.server.netty.port//Netty长连接端口

 **Others:** 
京东选择的注册中心是etcd，而我上家公司则是采用的Nacos，其他公司例如携程采用的Apollo，甚至一些自研的，不太可能为了一个组件特地引入新的注册中心，因此考虑对其做一个可定制化，自行选择注册中心。目前的话只实现了Nacos的方案，后续可能会添加别的。

仅个人开发，经验有限，难免考虑不周，有好想法欢迎交流！