package com.hnkjzyxy.ab.service.excel;

import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.hnkjzyxy.ab.dto.excel.UserModel;
import com.hnkjzyxy.ab.service.listener.UserDataListener;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.service.UserRoleService;

/**
 * 用户模板 Excel 导入服务，在编程式事务内保存用户及角色信息
 */
@Service
public class UserExcelImportService {
    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;
    /**
     * 密码编码器
     */
    @Autowired
    private PasswordEncoder bCryptPasswordEncoder;
    /**
     * 用户角色关联业务服务
     */
    @Autowired
    private UserRoleService userRoleService;
    /**
     * 编程式事务模板
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 在编程式事务内导入用户 Excel 模板
     * <p>
     * 通过用户监听器保存用户、编码后的密码及角色关联；事务内异常会触发回滚。
     * </p>
     *
     * @param path 用户 Excel 文件的本地路径
     * @throws RuntimeException 读取或保存用户数据失败时抛出
     */
    public void readUserExcel(String path) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    EasyExcel.read(path, UserModel.class, new UserDataListener(userService, bCryptPasswordEncoder, userRoleService)).doReadAll();
                }
            });
        } catch (Exception e) {
            // Transaction will be automatically rolled back due to exception
            throw e;
        }
    }
}
