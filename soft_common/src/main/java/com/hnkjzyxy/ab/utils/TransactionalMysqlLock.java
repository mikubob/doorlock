package com.hnkjzyxy.ab.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 事务连接上的 MySQL 会话锁
 * <p>
 * 不依赖 Redis TTL，锁保持到事务提交或回滚之后；连接断开时数据库自动释放。
 * 用于兜住进程暂停、Redis 重启或租约过期后旧持有者与新持有者并发写入的窗口。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-02
 */
@Slf4j
@Component
public class TransactionalMysqlLock {
    /**
     * 课表写入事务使用的数据源
     */
    private final DataSource dataSource;

    /**
     * 创建数据库事务锁工具
     *
     * @param dataSource 课表写入事务使用的数据源
     */
    public TransactionalMysqlLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取当前事务连接上的会话锁
     * <p>
     * 不等待其他持有者，未获取到锁时放弃本次替换；事务完成后释放会话锁。
     * </p>
     *
     * @param name 锁名称
     * @throws IllegalStateException 当前连接不在事务中、锁被其他实例持有或获取锁失败
     */
    public void acquire(String name) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("数据库同步锁必须在事务内获取");
        }
        Connection connection = DataSourceUtils.getConnection(dataSource);
        if (!DataSourceUtils.isConnectionTransactional(connection, dataSource)) {
            DataSourceUtils.releaseConnection(connection, dataSource);
            throw new IllegalStateException("同步锁与课表写入必须使用同一事务连接");
        }
        try {
            if (!execute(connection, "SELECT GET_LOCK(?, 0)", name)) {
                throw new IllegalStateException("其他实例正在替换课表，本次同步放弃");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("无法获取数据库同步锁，本次同步放弃", e);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /**
             * 事务完成后释放锁；释放失败时终止连接，避免复用仍持有锁的会话。
             *
             * @param status 事务完成状态
             */
            @Override
            public void afterCompletion(int status) {
                try {
                    if (!execute(connection, "SELECT RELEASE_LOCK(?)", name)) {
                        throw new SQLException("数据库同步锁释放失败");
                    }
                } catch (SQLException e) {
                    log.error("数据库同步锁释放失败，终止当前连接：{}", name, e);
                    try {
                        connection.abort(Runnable::run);
                    } catch (SQLException | RuntimeException abortError) {
                        log.error("终止数据库连接失败", abortError);
                    }
                }
            }
        });
    }

    /**
     * 执行获取或释放会话锁的 SQL
     *
     * @param connection 当前事务连接
     * @param sql        会话锁 SQL
     * @param name       锁名称
     * @return SQL 返回非空的 1 时返回 true
     * @throws SQLException SQL 执行失败
     */
    private boolean execute(Connection connection, String sql, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) == 1 && !result.wasNull();
            }
        }
    }
}
