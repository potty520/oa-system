package com.oa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oa.system.domain.SysPost;
import com.oa.system.mapper.SysPostMapper;
import com.oa.system.service.ISysPostService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements ISysPostService {
    @Override
    public List<SysPost> selectPostList(SysPost post) {
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(post.getPostCode()), SysPost::getPostCode, post.getPostCode())
                .like(StringUtils.isNotBlank(post.getPostName()), SysPost::getPostName, post.getPostName())
                .eq(StringUtils.isNotBlank(post.getStatus()), SysPost::getStatus, post.getStatus())
                .orderByAsc(SysPost::getPostSort);
        return baseMapper.selectList(wrapper);
    }
}