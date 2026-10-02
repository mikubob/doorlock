package com.hnkjzyxy.ab.params;

import lombok.Data;

/** 选中子项的导入请求；评分字段始终从数据库重读。 */
@Data
public class ProjectItemImportParam {
    /** 来源子项ID。 */
    private Integer id;
    /** 所属项目ID。 */
    private Integer projectId;
}
