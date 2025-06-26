package com.kuma.tools.dynamicHot.utils;

import java.net.InetAddress;

public class SnowFlakeGenerator {

    // 起始时间戳（2023-01-01）
    private static final long START_STAMP = 1672531200000L;

    // 各部分位数
    private static final long SEQUENCE_BIT = 12;   // 序列号位数
    private static final long MACHINE_BIT = 5;     // 机器标识位数
    private static final long DATACENTER_BIT = 5;  // 数据中心位数

    // 最大值计算
    private static final long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);
    private static final long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    // 各部分左移量
    private static final long MACHINE_LEFT = SEQUENCE_BIT;
    private static final long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private static final long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;


    private static long datacenterId;  // 数据中心ID

    private static long machineId;     // 机器ID

    private static long sequence = 0L; // 序列号
    private static long lastStamp = -1L; // 上次时间戳

    // 校验配置参数
    public SnowFlakeGenerator() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String ipAddr = inetAddress.getHostAddress();
            String[] arr = ipAddr.split("\\.");
            datacenterId = 1000000000000L + Integer.parseInt(arr[0]) * 1000000000 + Integer.parseInt(arr[1]) * 1000000
                    + Integer.parseInt(arr[2])* 100 + Integer.parseInt(arr[3]);
            machineId = 123456789012L + Integer.parseInt(arr[0]) * 1000000000 + Integer.parseInt(arr[1]) * 1000000
                    + Integer.parseInt(arr[2])* 100 + Integer.parseInt(arr[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than " + MAX_DATACENTER_NUM + " or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than " + MAX_MACHINE_NUM + " or less than 0");
        }
    }

    /**
     * 生成下一个ID
     */
    public synchronized static long nextId() {
        long currStamp = getNewStamp();

        if (currStamp < lastStamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (currStamp == lastStamp) {
            // 相同毫秒内序列号递增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 同一毫秒的序列数已达到最大
            if (sequence == 0L) {
                currStamp = getNextMill();
            }
        } else {
            // 不同毫秒序列号重置为0
            sequence = 0L;
        }

        lastStamp = currStamp;

        return (currStamp - START_STAMP) << TIMESTAMP_LEFT // 时间戳部分
                | datacenterId << DATACENTER_LEFT         // 数据中心部分
                | machineId << MACHINE_LEFT               // 机器标识部分
                | sequence;                                // 序列号部分
    }

    private static long getNextMill() {
        long mill = getNewStamp();
        while (mill <= lastStamp) {
            mill = getNewStamp();
        }
        return mill;
    }

    private static long getNewStamp() {
        return System.currentTimeMillis();
    }
}