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

/** 负责对应业务模板的 Excel 导入，保留原解析和事务规则。 */
@Service
public class UserExcelImportService {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private TransactionTemplate transactionTemplate;

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
