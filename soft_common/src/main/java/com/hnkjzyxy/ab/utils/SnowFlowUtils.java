package com.hnkjzyxy.ab.utils;

import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器
 * 生成全局唯一、趋势递增的 64 位长整型 ID
 *
 * @author hnkjzy
 * @version 1.0
 */
@Component
public class SnowFlowUtils {
    /**
     * 机器ID（二进制 5 位，32 位减掉 1 位共 31 个）
     */
    private long workerId;

    /**
     * 机房ID（二进制 5 位，32 位减掉 1 位共 31 个）
     */
    private long datacenterId;

    /**
     * 一毫秒内生成的多个 ID 的最新序号（12 位，4096 - 1 = 4095 个）
     */
    private long sequence;

    /**
     * 时间起始值（2^41 - 1，差不多可以用 69 年）
     */
    private long twepoch = 1585644268888L;

    /**
     * 机器ID所占位数
     */
    private long workerIdBits = 5L;

    /**
     * 机房ID所占位数
     */
    private long datacenterIdBits = 5L;

    /**
     * 每毫秒内产生的 ID 数（2 的 12 次方）
     */
    private long sequenceBits = 12L;

    /**
     * 机器ID最大值（5 bit 最多只能有 31 个数字，即机器ID最多只能是 32 以内）
     */
    private long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 机房ID最大值（5 bit 最多只能有 31 个数字，即机房ID最多只能是 32 以内）
     */
    private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    private long workerIdShift = sequenceBits;
    private long datacenterIdShift = sequenceBits + workerIdBits;
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 记录产生时间毫秒数，用于判断是否是同一毫秒
     */
    private long lastTimestamp = -1L;

    public void SnowFlow(long workerId, long datacenterId, long sequence) {
        // 检查机房id和机器id是否超过31 不能小于0
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("机器id超过31,机器id小于0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("机房id最多只能是32以内,且机房id不能小于0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = sequence;
    }

    /**
     * 核心方法：让当前这台机器上的 snowflake 算法程序生成一个全局唯一的 ID
     *
     * @return 全局唯一 ID
     */
    public synchronized long nextId() {
        // 这儿就是获取当前时间戳，单位是毫秒
        long timestamp = timeGen();
        // 判断是否小于上次时间戳，如果小于的话，就抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("当前时间戳小于上次时间戳", lastTimestamp - timestamp));
        }
        // 下面是说假设在同一个毫秒内，又发送了一个请求生成一个id
        // 这个时候就得把seqence序号给递增1，最多就是4096
        if (timestamp == lastTimestamp) {
            // 这个意思是说一个毫秒内最多只能有4096个数字，无论你传递多少进来，
            //这个位运算保证始终就是在4096这个范围内，避免你自己传递个sequence超过了4096这个范围
            sequence = (sequence + 1) & sequenceMask;
            //当某一毫秒的时间，产生的id数 超过4095，系统会进入等待，直到下一毫秒，系统继续产生ID
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        // 这儿记录一下最近一次生成id的时间戳，单位是毫秒
        lastTimestamp = timestamp;
        // 这儿就是最核心的二进制位运算操作，生成一个64bit的id
        // 先将当前时间戳左移，放到41 bit那儿；将机房id左移放到5 bit那儿；将机器id左移放到5 bit那儿；将序号放最后12 bit
        // 最后拼接起来成一个64 bit的二进制数字，转换成10进制就是个long型
        return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift) | (workerId << workerIdShift) | sequence;
    }

    /**
     * 当某一毫秒的时间，产生的id数 超过4095，系统会进入等待，直到下一毫秒，系统继续产生ID
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 下一毫秒的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前时间的毫秒数
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
}
