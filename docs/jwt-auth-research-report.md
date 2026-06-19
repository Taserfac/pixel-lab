# Pixel Lab 技术调研报告：JWT（JSON Web Token）认证协议

## 1. 选题背景

结合项目代码可见：
- 后端使用 `jsonwebtoken` 生成与校验令牌；
- 前端通过 Axios 拦截器自动携带 `Authorization: Bearer <token>`；
- 接口通过认证中间件进行白名单放行、普通鉴权与管理员权限控制。

因此，JWT 是本项目当前最核心、最需要“正确使用”的 Web 认证协议之一。

---

## 2. JWT 协议概述

JWT（JSON Web Token）是 IETF 标准（RFC 7519），用于在通信双方之间安全传输声明（claims）。

典型结构（JWS 场景）分为三段：

1. Header：算法与类型（如 `{"alg":"HS256","typ":"JWT"}`）
2. Payload：业务声明（如用户 ID、角色、过期时间）
3. Signature：基于密钥和前两段计算的签名，确保完整性

最终格式：

`base64url(header).base64url(payload).base64url(signature)`

JWT 的核心价值：
- **无状态认证**：服务端不必保存会话（相比传统 Session 更易水平扩展）
- **跨域/多端友好**：Web、移动端、微服务间都容易传递
- **声明自包含**：可携带角色、权限、上下文信息

---

## 3. 项目现状分析（结合 Pixel Lab）

### 3.1 现有实现优点

1. **前后端流程完整**：登录后签发 token，前端统一注入请求头。
2. **鉴权中间件集中化**：避免每个控制器重复写校验逻辑。
3. **角色隔离基础到位**：通过 `adminMiddleware` 做权限分层。
4. **密码与 token 分离**：密码采用哈希，token 不含敏感明文密码。

### 3.2 当前风险与改进空间

1. **密钥管理风险**：若 `jwt.secret` 太弱或泄漏，全部 token 失效安全性。
2. **撤销能力弱**：纯无状态 token 一旦签发，在有效期内难以“主动失效”。
3. **细粒度声明不足**：缺少 `iss`、`aud`、`jti` 等声明，不利于跨服务治理与审计。
4. **刷新机制不明确**：仅依赖单 token 过期可能导致体验和安全折中不佳。
5. **前端存储策略风险**：若 token 存在 `localStorage`，在 XSS 场景会被窃取。
6. **算法与验证策略需显式固化**：应防止“算法混淆”“宽松校验”等历史问题。

---

## 4. 深入调研：JWT 的关键安全实践

### 4.1 必要声明与校验规则

建议在签发和校验阶段完整处理以下声明：

- `exp`（过期时间）：必须验证；
- `iat`（签发时间）：用于审计；
- `nbf`（生效时间，可选）：防止提前使用；
- `iss`（签发者）：防止非授权签发源；
- `aud`（受众）：防止 token 被错误服务接收；
- `jti`（唯一 ID）：用于撤销名单与重放检测。

实践要点：
- 服务端应配置允许的时间偏移（clock skew）如 30~120 秒；
- 严格检查 `iss`/`aud` 的精确匹配；
- 对关键接口引入 `jti` 黑名单或短期缓存以支持主动注销。

### 4.2 算法选择：HS256 vs RS256/ES256

- **HS256（对称密钥）**：实现简单，适合单体应用；但密钥分发与隔离难。
- **RS256/ES256（非对称）**：私钥签发、公钥验签，更适合多服务/网关架构。

对 Pixel Lab 的建议：
- 当前单体后端可继续 HS256，但应提升密钥长度和轮换机制；
- 若未来拆分为网关 + 多服务，优先迁移到 RS256，并配置 JWK/JWKS 发布。

### 4.3 生命周期设计：短访问令牌 + 刷新令牌

推荐模型：
- Access Token：5~15 分钟，放内存（或受控容器）；
- Refresh Token：7~30 天，HttpOnly + Secure + SameSite Cookie；
- 服务端保存 refresh token 哈希与设备信息，支持单设备踢下线。

收益：
- 被窃取 access token 的可利用窗口显著缩短；
- 用户无感续期，兼顾安全与体验；
- 支持异常登录处置与会话治理。

### 4.4 前端存储与传输策略

**不建议**长期将高价值 token 放在 `localStorage`。

更稳妥方案：
- access token：内存态（页面刷新失效可接受）；
- refresh token：HttpOnly Cookie（JS 不可读）；
- 全站 HTTPS、HSTS；
- 配套 CSP、输入输出编码、防止 XSS。

### 4.5 常见攻击与防护清单

1. **XSS 窃取令牌** → CSP、模板转义、依赖审计。
2. **重放攻击** → 短期 token + `jti` + 关键操作二次校验。
3. **密钥泄露** → KMS/环境变量注入、定期轮换、最小暴露面。
4. **算法降级/混淆** → 验签时白名单固定算法，不信任 token header 自声明。
5. **越权（角色滥用）** → 后端强制 RBAC 校验，不仅依赖前端按钮隐藏。
6. **跨服务误用** → 强制 `aud`/`iss` 校验，避免“拿 A 的 token 调 B”。

---

## 5. 面向 Pixel Lab 的落地方案（分阶段）

### Phase 1（1~2 周，可快速落地）

1. JWT 载荷新增：`iss`、`aud`、`jti`、`iat`；
2. 鉴权中间件增强：强制验证 `iss`、`aud`，固定算法白名单；
3. 调整 token 过期策略：将访问 token 降到 15~60 分钟；
4. 增加密钥轮换计划（至少预留 `kid` 设计）。

### Phase 2（2~4 周，安全体验并进）

1. 引入 refresh token 流程与续期接口；
2. 建立注销/封禁时的 token 撤销表（按 `jti`）；
3. 前端重构存储：access token 内存化，refresh token Cookie 化；
4. 登录态异常监控：IP/UA 异常告警。

### Phase 3（中长期，支持平台化扩展）

1. 若微服务化，迁移 RS256 + JWKS；
2. 接入统一认证网关（API Gateway / BFF）；
3. 构建权限中心（RBAC/ABAC）与审计追踪。

---

## 6. 推荐配置基线（可直接作为工程规范）

- Access Token TTL：10 分钟（上限不超过 30 分钟）
- Refresh Token TTL：14 天（高风险场景 7 天）
- 签名算法：
  - 单体：HS256（密钥 >= 256 bit 随机）
  - 分布式：RS256（私钥保管于 KMS）
- 强制校验声明：`exp`、`iss`、`aud`、`sub`、`jti`
- 令牌传输：仅 HTTPS
- Cookie：`HttpOnly` + `Secure` + `SameSite=Lax/Strict`
- 注销：服务端维护 refresh token 版本号/黑名单
- 审计：记录 token 签发、刷新、撤销事件

---

## 7. 结论

JWT 作为 Pixel Lab 当前登录态与权限控制的基础协议，方向是正确的，但“**可用**”与“**可长期安全运行**”之间仍有工程差距。建议优先落实：

1. 声明与验签策略的严格化；
2. token 生命周期拆分（access + refresh）；
3. 前端令牌存储安全升级；
4. 撤销与审计能力建设。

完成上述优化后，项目在安全性、可运维性、扩展性上都会明显提升，也能为后续多端接入与服务拆分打下认证基础。

---

## 8. 参考资料（建议进一步阅读）

1. RFC 7519: JSON Web Token (JWT)
2. RFC 8725: JSON Web Token Best Current Practices
3. OWASP Cheat Sheet: JSON Web Token for Java / Session Management / REST Security
4. IETF JOSE（JWS/JWA/JWK）相关规范
