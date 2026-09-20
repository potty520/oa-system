package com.oa.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oa.system.domain.SysPost;

import java.util.List;

public interface ISysPostService extends IService<SysPost> {
    List<SysPost> selectPostList(SysPost post);
}