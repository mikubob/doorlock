package com.hnkjzyxy.ab.hanlder;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * MyBatis-Plus 自动填充处理器
 * 用于在插入和更新数据时自动填充 createTime 和 updateTime 字段
 *
 * @version 1.1
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-12 9:59
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作自动填充
     * 当执行 insert 操作时，自动为 createTime 和 updateTime 字段设置当前时间
     * 仅当字段值为 null 时才进行填充，防止覆盖手动设置的值
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 自动填充创建时间（仅当为空时填充）
        if (metaObject.hasSetter("createTime") && this.getFieldValByName("createTime", metaObject) == null) {
            fillTimeField(metaObject, "createTime");
        }
        // 自动填充更新时间（仅当为空时填充）
        if (metaObject.hasSetter("updateTime") && this.getFieldValByName("updateTime", metaObject) == null) {
            fillTimeField(metaObject, "updateTime");
        }
    }

    /**
     * 更新操作自动填充
     * 当执行 update 操作时，自动为 updateTime 字段设置当前时间
     * 更新时强制刷新为最新时间
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 自动填充更新时间（强制覆盖）
        if (metaObject.hasSetter("updateTime")) {
            fillTimeField(metaObject, "updateTime");
        }
    }

    /**
     * 根据实体类字段类型自动填充时间
     * 兼容 java.time.LocalDateTime 和 java.util.Date
     *
     * @param metaObject 元对象
     * @param fieldName  字段名称
     */
    private void fillTimeField(MetaObject metaObject, String fieldName) {
        Class<?> fieldType = metaObject.getSetterType(fieldName);
        if (fieldType.equals(LocalDateTime.class)) {
            this.setFieldValByName(fieldName, LocalDateTime.now(), metaObject);
        } else if (fieldType.equals(Date.class)) {
            this.setFieldValByName(fieldName, new Date(), metaObject);
        }
    }
}