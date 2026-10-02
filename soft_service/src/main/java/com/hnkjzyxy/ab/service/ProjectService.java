package com.hnkjzyxy.ab.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Project;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.params.ProjectParam;
import com.hnkjzyxy.ab.params.ProjectQueryParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.params.ProjectItemSaveParam;
import com.hnkjzyxy.ab.vo.ResultVo;

import java.util.List;
import java.util.Map;

/**
 * @author 16702
 */
public interface ProjectService extends IService<Project> {
    Map<String, Object> getProjectList(ProjectParam param);

    Map<String, Object> getUserProjectList(ProjectParam param);

    List<Integer> getRoles(Integer userId);

    Project getEndProject(Integer userId);

    void addProject(Project project, User user);

    Project getProjectById(String id, User user);

    void publishProject(String id, User user);

    void resultProject(ResultVo result, User user);

    void projectStaging(ResultVo result, User user);

    ResultVo getProjectStaging(User user, Integer pId);

    List<String> getProjectYears(ProjectParam param);

    List<String> getUserProjectYears(ProjectParam param);

    ApiResult getProjectAssessList(ProjectQueryParam param, User user);

    List<Project> getProjectByYear(String year, User user);

    List<String> getYearByProject(User user);

    /**
     * 新增或修改项目分类、子项
     *
     * @param projectItemVo 分类或子项维护信息
     * @param operator 当前认证操作人
     */
    void addOrUpdateProjectItem(ProjectItemSaveParam projectItemVo, User operator);
    List<Project> getProjectAndCollegeByYear(String year, String college,User user);
}
