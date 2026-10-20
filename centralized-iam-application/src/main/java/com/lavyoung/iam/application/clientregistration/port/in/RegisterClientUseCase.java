package com.lavyoung.iam.application.clientregistration.port.in;

import com.lavyoung.iam.application.clientregistration.command.RegisterClientCommand;
import com.lavyoung.iam.application.clientregistration.result.RegisteredClientResult;
import com.lavyoung.iam.shared.error.IamException;

/**
 * 定义注册公共客户端和机密客户端的应用层输入端口。
 *
 * <p>端口只表达用例能力，不负责 HTTP 参数解析、事务实现或数据库访问。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public interface RegisterClientUseCase {

    /**
     * 注册不持有客户端密钥的公共客户端。
     *
     * @param command 客户端注册命令
     * @return 已创建并持久化的客户端注册结果
     * @throws IamException 当客户端标识重复或配置不适用于公共客户端时抛出
     */
    RegisteredClientResult registerPublic(RegisterClientCommand command);

    /**
     * 注册使用密钥认证的机密客户端。
     *
     * @param command   客户端注册命令
     * @param rawSecret 客户端明文密钥，仅允许在本次调用链中短暂存在
     * @return 已创建并持久化的客户端注册结果
     * @throws IamException             当客户端标识重复或配置不适用于机密客户端时抛出
     * @throws IllegalArgumentException 当明文密钥为空时抛出
     */
    RegisteredClientResult registerConfidential(RegisterClientCommand command, CharSequence rawSecret);
}