package com.oa.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接口授权覆盖率的回归闸门。
 *
 * <p>项目里没有 @SpringBootTest/MockMvc，新增一个写接口忘加 @PreAuthorize、
 * 或者顺手删掉已有的 @PreAuthorize，mvn verify 都不会变红。这里不启动 Spring 上下文，
 * 只把 src/main/java 下的 *Controller.java 当纯文本扫描，定位方式与 PreAuthorizeSeedTest
 * 一致（Surefire 以模块目录为工作目录，同时兼容从仓库根目录运行）。</p>
 *
 * <p>规则：POST/PUT/DELETE/PATCH 映射必须带 @PreAuthorize，除非登记在 {@link #ALLOW_LIST}。
 * 只读映射（GET）不做强制要求，但仍受表达式风格校验的约束。</p>
 */
class ControllerAuthorizationContractTest {

    /**
     * 只认这 5 个具体的方法级映射注解。类级 @RequestMapping 不在匹配集合内，
     * 因此不可能被误判成方法映射（类级注解位于类声明之前，扫描也只从类声明之后开始）。
     */
    private static final Pattern MAPPING = Pattern.compile("^@(Get|Post|Put|Delete|Patch)Mapping\\b");

    /**
     * 行首必须就是 @PreAuthorize 注解本身：注释或 Javadoc 里提到 “@PreAuthorize” 不算保护。
     * 例如 OaDocTypeController#options 的文档里就写着“不加 @PreAuthorize”，
     * 若按 contains 判断会把这个无注解的接口误判成已保护。
     */
    private static final Pattern PRE_AUTHORIZE = Pattern.compile("^@PreAuthorize\\s*\\(");

    /** 从 @PreAuthorize("...") 里取出表达式字面量。 */
    private static final Pattern PRE_AUTHORIZE_EXPRESSION =
            Pattern.compile("@PreAuthorize\\s*\\(\\s*\"([^\"]*)\"\\s*\\)");

    /** 本项目认的表达式风格：@ss.hasPermi('x') 或 @ss.hasAnyPermi('x,y')。 */
    private static final Pattern PERMISSION_STYLE =
            Pattern.compile("^@ss\\.has(?:Permi|AnyPermi)\\(\\s*'[^']+'(?:\\s*,\\s*'[^']+')*\\s*\\)$");

    /** 出现这些关键词说明接口被放宽成了“登录即可”，不再校验具体权限。 */
    private static final Pattern WEAKENED = Pattern.compile("permitAll\\(|isAuthenticated\\(");

    /** 类级 @RequestMapping：锚定行首不 trim，缩进的方法级 @RequestMapping 不会被算进来。 */
    private static final Pattern CLASS_LEVEL_REQUEST_MAPPING = Pattern.compile("^@RequestMapping\\s*\\(");

    /** 已知的 HTTP 动词，用来确认扫描器没有把别的东西当成映射。 */
    private static final List<String> KNOWN_VERBS = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");

    /**
     * 有意不加 @PreAuthorize 的接口豁免清单，键为 控制器简单类名#方法名。
     * 每条都要写明理由；写接口只要不在清单里，就必须带 @PreAuthorize。
     *
     * <p>另有只读接口 GET /oa/docType/options 同样有意不加注解，它属 GET，
     * 不参与写接口校验，因此不列在这里。</p>
     */
    private static final Map<String, String> ALLOW_LIST = new LinkedHashMap<String, String>();

    static {
        // 登录入口：必须匿名可达（SecurityConfig 放行 /login），加权限校验等于谁也登不进来
        ALLOW_LIST.put("SysLoginController#login", "登录入口必须匿名可达");
        // 退出登录：只回一句“退出成功”，不落库不改状态，仅需已认证，无需细粒度权限
        ALLOW_LIST.put("SysLoginController#logout", "退出登录不写库，仅需已认证");
        // 选人下拉：只返回最小字段，普通用户转办/传阅公文时也要能查到同事
        ALLOW_LIST.put("SysUserController#options", "只读下拉，普通用户也需要");
        // 自助改密：被强制改密的初始账号只有基础菜单，要求 system:user:* 会把自己锁在系统外
        ALLOW_LIST.put("SysProfileController#updatePwd", "自助改密，强制改密账号只有基础菜单");
    }

    /** 最少应该扫到多少个 *Controller.java（当前 22 个）。 */
    private static final int MIN_CONTROLLERS = 15;

    /** 方法级映射注解下限（当前 115 个）：扫描器一旦失效，这里先炸，避免空跑即通过。 */
    private static final int MIN_MAPPINGS = 40;

    /** 写接口下限（当前 66 个）：写接口规则尤其不能空跑。 */
    private static final int MIN_MUTATING_MAPPINGS = 30;

    /** @PreAuthorize 下限（当前 106 个）。 */
    private static final int MIN_PRE_AUTHORIZE_ANNOTATIONS = 40;

    /** 类级 @RequestMapping 下限（当前 20 个）：证明它们确实存在且被排除了。 */
    private static final int MIN_CLASS_LEVEL_MAPPINGS = 15;

    /** 映射注解与签名之间允许隔几行。 */
    private static final int SIGNATURE_WINDOW = 6;

    /** 映射注解之前向前找 @PreAuthorize 的窗口；碰到上一段代码即停。 */
    private static final int BACKWARD_WINDOW = 8;

    @Test
    void everyMutatingMappingShouldCarryPreAuthorizeUnlessAllowed() throws Exception {
        List<String> offenders = new ArrayList<String>();
        int mutating = 0;
        for (Endpoint endpoint : endpoints()) {
            if (!endpoint.isMutating()) {
                continue;
            }
            mutating++;
            if (endpoint.expression == null && !ALLOW_LIST.containsKey(endpoint.key())) {
                offenders.add(endpoint.toString());
            }
        }
        assertTrue(mutating >= MIN_MUTATING_MAPPINGS,
                "只解析到 " + mutating + " 个写接口（POST/PUT/DELETE/PATCH），源码扫描范围或扫描器可能不对");
        assertTrue(offenders.isEmpty(),
                "以下写接口没有 @PreAuthorize，也不在 ALLOW_LIST 豁免清单里：\n  "
                        + String.join("\n  ", offenders)
                        + "\n需要鉴权的请补 @PreAuthorize(\"@ss.hasPermi('模块:资源:动作')\")；"
                        + "确实要匿名或仅登录可用的写接口，请登记进 ALLOW_LIST 并写明一行理由。");
    }

    @Test
    void everyPreAuthorizeShouldUseThePermissionServiceStyle() throws Exception {
        List<String> offenders = new ArrayList<String>();
        int annotations = 0;
        for (Path source : controllerSources()) {
            String controller = controllerName(source);
            for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
                if (!PRE_AUTHORIZE.matcher(line.trim()).find()) {
                    continue;
                }
                annotations++;
                String value = expressionOf(line);
                if (!PERMISSION_STYLE.matcher(value).matches()) {
                    offenders.add(controller + " -> " + value);
                }
            }
        }
        assertTrue(annotations >= MIN_PRE_AUTHORIZE_ANNOTATIONS,
                "只解析到 " + annotations + " 个 @PreAuthorize，源码扫描范围或扫描器可能不对");
        assertTrue(offenders.isEmpty(),
                "以下 @PreAuthorize 没有使用本项目统一的 @ss.hasPermi/@ss.hasAnyPermi 风格：\n  "
                        + String.join("\n  ", offenders));
    }

    @Test
    void noEndpointShouldBeWeakenedToAuthenticatedOnly() throws Exception {
        List<String> offenders = new ArrayList<String>();
        for (Endpoint endpoint : endpoints()) {
            if (endpoint.expression != null && WEAKENED.matcher(endpoint.expression).find()) {
                offenders.add(endpoint + " -> " + endpoint.expression);
            }
        }
        assertTrue(offenders.isEmpty(),
                "以下接口把授权放宽成了 permitAll()/isAuthenticated()，写接口必须落到具体权限上：\n  "
                        + String.join("\n  ", offenders));
    }

    /**
     * 扫描器的自检：解析数量低于下限就直接失败，保证上面几条规则不会因为“什么都没扫到”而空跑通过；
     * 顺带确认类级 @RequestMapping 没有被算成接口。
     */
    @Test
    void scannerShouldCoverEveryControllerAndSkipClassLevelMappings() throws Exception {
        List<Path> sources = controllerSources();
        List<Endpoint> parsed = endpoints();

        assertTrue(sources.size() >= MIN_CONTROLLERS,
                "只找到 " + sources.size() + " 个 *Controller.java，源码扫描范围可能不对");

        long mutating = 0;
        long protectedEndpoints = 0;
        for (Endpoint endpoint : parsed) {
            if (endpoint.isMutating()) {
                mutating++;
            }
            if (endpoint.expression != null) {
                protectedEndpoints++;
            }
        }
        assertTrue(parsed.size() >= MIN_MAPPINGS,
                "只解析到 " + parsed.size() + " 个方法级映射注解，扫描器可能已经失效");
        assertTrue(mutating >= MIN_MUTATING_MAPPINGS,
                "只解析到 " + mutating + " 个写接口，扫描器可能已经失效");

        int annotations = 0;
        int classLevelMappings = 0;
        for (Path source : sources) {
            for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
                if (PRE_AUTHORIZE.matcher(line.trim()).find()) {
                    annotations++;
                }
                // 这里刻意不 trim：类级 @RequestMapping 顶格写，缩进的是方法级，两者要区分开
                if (CLASS_LEVEL_REQUEST_MAPPING.matcher(line).find()) {
                    classLevelMappings++;
                }
            }
        }
        assertTrue(annotations >= MIN_PRE_AUTHORIZE_ANNOTATIONS,
                "只解析到 " + annotations + " 个 @PreAuthorize，扫描器可能已经失效");

        // 每条被判为“已保护”的映射都得有一条自己的 @PreAuthorize 撑着。窗口一旦串到隔壁方法，
        // 同一个注解会被算到两个接口头上，受保护数就会超过注解总数，这里立刻暴露。
        assertTrue(protectedEndpoints <= annotations,
                "受保护的映射数 " + protectedEndpoints + " 超过 @PreAuthorize 总数 " + annotations
                        + "，说明扫描窗口串到了相邻方法，会出现漏报");

        // 类级 @RequestMapping 必须存在（否则本项无意义），但一个都不能进 endpoints：
        // 若扫描器把它们也当成接口，动词就会是“RequestMapping”而不是 5 个已知动词之一。
        assertTrue(classLevelMappings >= MIN_CLASS_LEVEL_MAPPINGS,
                "只扫到 " + classLevelMappings + " 个类级 @RequestMapping，扫描范围可疑");
        List<String> bogus = new ArrayList<String>();
        for (Endpoint endpoint : parsed) {
            if (!KNOWN_VERBS.contains(endpoint.httpMethod)) {
                bogus.add(endpoint.toString());
            }
        }
        assertTrue(bogus.isEmpty(), "扫描器把非方法级映射当成了接口: " + bogus);
    }

    /** 扫描所有 *Controller.java，解析出每一条方法级映射。 */
    private List<Endpoint> endpoints() throws Exception {
        List<Endpoint> found = new ArrayList<Endpoint>();
        for (Path source : controllerSources()) {
            String controller = controllerName(source);
            List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
            for (int i = classDeclarationLine(lines, controller) + 1; i < lines.size(); i++) {
                Matcher mapping = MAPPING.matcher(lines.get(i).trim());
                if (!mapping.find()) {
                    continue;
                }
                int signature = signatureLine(lines, i);
                found.add(new Endpoint(controller, methodName(lines, signature),
                        mapping.group(1).toUpperCase(Locale.ROOT), preAuthorize(lines, i, signature)));
            }
        }
        return found;
    }

    /** 类声明所在行：类级 @RequestMapping 在它之前，只有它之后的行才可能是方法级映射。 */
    private int classDeclarationLine(List<String> lines, String controller) {
        Pattern declaration = Pattern.compile("class\\s+" + Pattern.quote(controller) + "\\b");
        for (int i = 0; i < lines.size(); i++) {
            if (declaration.matcher(lines.get(i)).find()) {
                return i;
            }
        }
        return -1;
    }

    /** 映射注解之后的第一个方法签名行（public 且带左括号）。 */
    private int signatureLine(List<String> lines, int mappingLine) {
        int limit = Math.min(lines.size() - 1, mappingLine + SIGNATURE_WINDOW);
        for (int i = mappingLine + 1; i <= limit; i++) {
            String text = lines.get(i).trim();
            if (text.startsWith("public ") && text.indexOf('(') >= 0) {
                return i;
            }
        }
        return -1;
    }

    /** 从签名行取出方法名；取不到就返回 “?”，让它以未登记的方式暴露出来而不是被静默跳过。 */
    private String methodName(List<String> lines, int signature) {
        if (signature < 0) {
            return "?";
        }
        String line = lines.get(signature);
        String head = line.substring(0, line.indexOf('(')).trim();
        int space = head.lastIndexOf(' ');
        return space < 0 ? head : head.substring(space + 1).trim();
    }

    /**
     * 这条映射身上的 @PreAuthorize 表达式，没有则返回 null。只看两个位置：
     * 映射注解与签名之间（注解写在 @XxxMapping 之后也认），以及映射注解之前的一段窗口。
     *
     * <p>向前找时，空行、其他注解和注释都跳过，一旦碰到上一段代码就停下，
     * 避免把上一个方法的 @PreAuthorize 借过来，把真正的漏加掩盖掉。</p>
     */
    private String preAuthorize(List<String> lines, int mappingLine, int signature) {
        int forwardEnd = signature >= 0
                ? signature
                : Math.min(lines.size() - 1, mappingLine + SIGNATURE_WINDOW);
        for (int i = mappingLine + 1; i <= forwardEnd; i++) {
            if (PRE_AUTHORIZE.matcher(lines.get(i).trim()).find()) {
                return expressionOf(lines.get(i));
            }
        }
        int backwardStart = Math.max(0, mappingLine - BACKWARD_WINDOW);
        for (int i = mappingLine - 1; i >= backwardStart; i--) {
            String text = lines.get(i).trim();
            if (PRE_AUTHORIZE.matcher(text).find()) {
                return expressionOf(lines.get(i));
            }
            if (isSkippable(text)) {
                continue;
            }
            break;
        }
        return null;
    }

    /** 空行、注解行、Javadoc/行注释可以继续向前找 @PreAuthorize。 */
    private boolean isSkippable(String trimmed) {
        return trimmed.isEmpty()
                || trimmed.startsWith("@")
                || trimmed.startsWith("*")
                || trimmed.startsWith("/*")
                || trimmed.startsWith("//");
    }

    /** 取出 @PreAuthorize 的表达式字面量；解析不出来就原样返回，交给风格校验报错。 */
    private String expressionOf(String line) {
        Matcher matcher = PRE_AUTHORIZE_EXPRESSION.matcher(line);
        return matcher.find() ? matcher.group(1) : line.trim();
    }

    /** 源码目录：Surefire 以模块目录为工作目录，同时也兼容从仓库根目录运行。 */
    private List<Path> controllerSources() throws Exception {
        Path root = null;
        for (String candidate : new String[]{"src/main/java", "../src/main/java", "ruoyi-admin/src/main/java"}) {
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) {
                root = path;
                break;
            }
        }
        assertNotNull(root, "找不到 src/main/java 源码目录");
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(path -> path.toString().endsWith("Controller.java"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private String controllerName(Path source) {
        return source.getFileName().toString().replace(".java", "");
    }

    /** 一条方法级映射：控制器、方法名、HTTP 动词，以及 @PreAuthorize 表达式（没有则为 null）。 */
    private static final class Endpoint {

        private final String controller;
        private final String method;
        private final String httpMethod;
        private final String expression;

        private Endpoint(String controller, String method, String httpMethod, String expression) {
            this.controller = controller;
            this.method = method;
            this.httpMethod = httpMethod;
            this.expression = expression;
        }

        /** 会改数据的动词；GET/HEAD 之类的只读映射不在此列。 */
        private boolean isMutating() {
            return "POST".equals(httpMethod) || "PUT".equals(httpMethod)
                    || "DELETE".equals(httpMethod) || "PATCH".equals(httpMethod);
        }

        /** 豁免清单的键：控制器简单类名#方法名。 */
        private String key() {
            return controller + "#" + method;
        }

        @Override
        public String toString() {
            return key() + " (" + httpMethod + ")";
        }
    }
}
