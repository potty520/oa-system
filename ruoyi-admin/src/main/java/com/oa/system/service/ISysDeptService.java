package com.oa.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.system.domain.SysDept;

import java.util.List;

public interface ISysDeptService extends IService<SysDept> {
    List<SysDept> selectDeptList(SysDept dept);
    int changeStatus(Long deptId, String status);
}