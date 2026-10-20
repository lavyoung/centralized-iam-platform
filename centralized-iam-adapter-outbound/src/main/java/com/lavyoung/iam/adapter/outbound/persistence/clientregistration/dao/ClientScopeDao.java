package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po.ClientScopePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端 scope 关联表 MyBatis 数据访问接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Mapper
public interface ClientScopeDao extends BaseMapper<ClientScopePO> {
}