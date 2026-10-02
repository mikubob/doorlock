package com.hnkjzyxy.ab.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * MyBatis-Plus 自动填充处理器
 * 用于在插入和更新数据时自动填充 createTime 和 updateTime 字段
 *
 * @version 1.2
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
        if (hasWritableTimeField(metaObject, "createTime")
                && this.getFieldValByName("createTime", metaObject) == null) {
            fillTimeField(metaObject, "createTime");
        }
        // 自动填充更新时间（仅当为空时填充）
        if (hasWritableTimeField(metaObject, "updateTime")
                && this.getFieldValByName("updateTime", metaObject) == null) {
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
        if (hasWritableTimeField(metaObject, "updateTime")) {
            fillTimeField(metaObject, "updateTime");
        }
    }

    /**
     * 判断给定字段是否是可以安全写入的时间字段
     * <p>
     * 这里**不能**直接用 {@link MetaObject#hasSetter(String)} 作为判据：
     * MyBatis-Plus 在 {@code updateById} / {@code update(entity, wrapper)} 时，
     * 会把参数对象包装成 {@code MapperMethod.ParamMap}（本质是 HashMap），
     * 而 {@code MetaObject} 对 Map 走的是 {@code MapWrapper} 分支，其
     * {@code hasSetter} 对任何合法属性名都恒返回 {@code true}，
     * 但 {@code MapWrapper#getSetterType} 取的是 {@code map.get(name).getClass()}，
     * key 不存在时为 {@code null}，随后在 {@code MetaObject.getSetterType} 处抛
     * {@code BindingException: Parameter 'xxx' not found}。
     * <p>
     * 典型现象：实体类没有 {@code updateTime} 字段（如 {@code User}、{@code CheckResult}、
     * {@code Notice} 等），调用 {@code updateById} 时登录、保存等接口直接 500。
     * <p>
     * 因此这里显式排除 Map 类参数（ParamMap 场景），只对真正的实体对象做填充；
     * 同时用 {@link MetaObject#hasGetter}/{@link MetaObject#hasSetter} 双重校验，
     * 保证后续 {@code getSetterType} 一定不会因缺字段而失败。
     *
     * @param metaObject 元对象
     * @param fieldName  字段名称
     * @return 该字段可安全填充时返回 true
     */
    private boolean hasWritableTimeField(MetaObject metaObject, String fieldName) {
        if (metaObject == null) {
            return false;
        }
        Object originalObject = metaObject.getOriginalObject();
        // ParamMap（Map 类型参数）不具备「实体字段」语义，直接跳过，避免误填充与异常
        if (originalObject instanceof Map) {
            return false;
        }
        // 属性必须同时可读可写，后续 getSetterType 才不会抛异常
        return metaObject.hasGetter(fieldName) && metaObject.hasSetter(fieldName);
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
        if (LocalDateTime.class.equals(fieldType)) {
            this.setFieldValByName(fieldName, LocalDateTime.now(), metaObject);
        } else if (Date.class.equals(fieldType)) {
            this.setFieldValByName(fieldName, new Date(), metaObject);
        }
    }
}
