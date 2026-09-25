param(
    [string]$BaseUrl = 'http://localhost:18081/api',
    # 仓库不再内置默认口令，admin 初始密码来自启动日志或 OA_INIT_ADMIN_PASSWORD
    [string]$AdminPassword = $env:OA_INIT_ADMIN_PASSWORD
)

$ErrorActionPreference = 'Stop'
if (-not $AdminPassword) { throw '请用 -AdminPassword 或环境变量 OA_INIT_ADMIN_PASSWORD 提供 admin 密码' }
$script:Results = New-Object System.Collections.Generic.List[object]
$suffix = [DateTimeOffset]::Now.ToUnixTimeSeconds()

function Assert-Test {
    param([bool]$Condition, [string]$Name, [string]$Detail = '')
    $result = if ($Condition) { 'PASS' } else { 'FAIL' }
    $script:Results.Add([pscustomobject]@{ Test = $Name; Result = $result; Detail = $Detail })
    if (-not $Condition) { throw "Test failed: $Name $Detail" }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body,
        [string]$Token
    )
    $headers = @{}
    if ($Token) { $headers.Authorization = 'Bearer ' + $Token }
    $params = @{ Uri = $BaseUrl + $Path; Method = $Method; Headers = $headers; TimeoutSec = 10 }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
    }
    return Invoke-RestMethod @params
}

$token = $null
$createdIds = @{}

try {
    $wrong = Invoke-Api 'Post' '/login' @{ username = 'admin'; password = 'wrong-password' }
    Assert-Test ($wrong.code -eq 500) '错误密码拒绝' $wrong.msg

    try {
        Invoke-Api 'Get' '/system/user/list?pageNum=1&pageSize=10' $null $null | Out-Null
        Assert-Test $false '未登录访问拦截' 'anonymous request was accepted'
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Assert-Test ($status -in 401, 403) '未登录访问拦截' "HTTP $status"
    }

    $login = Invoke-Api 'Post' '/login' @{ username = 'admin'; password = $AdminPassword }
    Assert-Test ($login.code -eq 200 -and $login.data.token) '管理员登录' $login.data.userInfo.userName
    $token = $login.data.token

    $info = Invoke-Api 'Get' '/getInfo' $null $token
    Assert-Test ($info.code -eq 200 -and $info.data.user.userName -eq 'admin') '当前用户信息'

    $userName = "testuser$suffix"
    $createUser = Invoke-Api 'Post' '/system/user' @{
        userName = $userName; nickName = '测试用户'; password = 'Test123456'
        phonenumber = '13900000000'; email = 'test@example.com'; status = '0'; deptId = 103; roleIds = @(2)
    } $token
    Assert-Test ($createUser.code -eq 200) '用户新增'

    $userList = Invoke-Api 'Get' ("/system/user/list?pageNum=1&pageSize=20&userName=" + $userName) $null $token
    $user = $userList.rows | Select-Object -First 1
    Assert-Test ($null -ne $user -and $user.userName -eq $userName) '用户筛选与查询' "id=$($user.userId)"
    $createdIds.user = $user.userId

    $disable = Invoke-Api 'Put' '/system/user/changeStatus' @{ userId = $user.userId; status = '1' } $token
    $disabledLogin = Invoke-Api 'Post' '/login' @{ username = $userName; password = 'Test123456' }
    Assert-Test ($disable.code -eq 200 -and $disabledLogin.code -eq 500) '用户停用生效' $disabledLogin.msg

    $enable = Invoke-Api 'Put' '/system/user/changeStatus' @{ userId = $user.userId; status = '0' } $token
    $reset = Invoke-Api 'Put' '/system/user/resetPwd' @{ userId = $user.userId; password = 'NewPass123' } $token
    $newLogin = Invoke-Api 'Post' '/login' @{ username = $userName; password = 'NewPass123' }
    Assert-Test ($enable.code -eq 200 -and $reset.code -eq 200 -and $newLogin.code -eq 200) '用户启用、重置密码与重新登录'

    $roleName = "测试角色$suffix"
    Invoke-Api 'Post' '/system/role' @{ roleName = $roleName; roleKey = "test$suffix"; roleSort = 99; status = '0'; remark = '自动化测试' } $token | Out-Null
    $roleList = Invoke-Api 'Get' ("/system/role/list?pageNum=1&pageSize=20&roleName=" + $roleName) $null $token
    $role = $roleList.rows | Select-Object -First 1
    Assert-Test ($null -ne $role -and $role.roleName -eq $roleName) '角色新增与查询' "id=$($role.roleId)"
    $createdIds.role = $role.roleId
    $roleUpdate = Invoke-Api 'Put' '/system/role' @{ roleId = $role.roleId; roleName = $roleName; roleKey = "test$suffix"; roleSort = 100; status = '0' } $token
    $roleStatus = Invoke-Api 'Put' '/system/role/changeStatus' @{ roleId = $role.roleId; status = '1' } $token
    Assert-Test ($roleUpdate.code -eq 200 -and $roleStatus.code -eq 200) '角色修改与状态变更'

    $deptName = "测试部门$suffix"
    Invoke-Api 'Post' '/system/dept' @{ parentId = 100; ancestors = '0,100'; deptName = $deptName; orderNum = 99; leader = '测试'; status = '0' } $token | Out-Null
    $deptList = Invoke-Api 'Get' ("/system/dept/list?deptName=" + $deptName) $null $token
    $dept = $deptList.data | Select-Object -First 1
    Assert-Test ($null -ne $dept -and $dept.deptName -eq $deptName) '部门新增与查询' "id=$($dept.deptId)"
    $createdIds.dept = $dept.deptId
    $deptUpdate = Invoke-Api 'Put' '/system/dept' @{ deptId = $dept.deptId; parentId = 100; ancestors = '0,100'; deptName = $deptName; orderNum = 100; status = '0' } $token
    $deptStatus = Invoke-Api 'Put' '/system/dept/changeStatus' @{ deptId = $dept.deptId; status = '1' } $token
    Assert-Test ($deptUpdate.code -eq 200 -and $deptStatus.code -eq 200) '部门修改与状态变更'

    $annTitle = "自动化公告$suffix"
    Invoke-Api 'Post' '/oa/announcement' @{
        annTitle = $annTitle; annType = '1'; annLevel = '1'; annContent = '自动化测试内容'
        isTop = '0'; isPublic = '1'; publishStartTime = '2026-09-18 00:00:00'
        publishEndTime = '2027-12-31 23:59:59'; status = '1'
    } $token | Out-Null
    $annList = Invoke-Api 'Get' ("/oa/announcement/list?pageNum=1&pageSize=20&annTitle=" + $annTitle) $null $token
    $ann = $annList.rows | Select-Object -First 1
    Assert-Test ($null -ne $ann -and $ann.annTitle -eq $annTitle) '公告新增与查询' "id=$($ann.annId)"
    $createdIds.announcement = $ann.annId
    $annUpdate = Invoke-Api 'Put' '/oa/announcement' @{ annId = $ann.annId; annTitle = $annTitle; annContent = '修改后的内容'; status = '1' } $token
    $annInfo = Invoke-Api 'Get' ("/oa/announcement/" + $ann.annId) $null $token
    Assert-Test ($annUpdate.code -eq 200 -and $annInfo.data.annContent -eq '修改后的内容') '公告修改'

    $scheduleTitle = "自动化日程$suffix"
    Invoke-Api 'Post' '/oa/schedule' @{
        scheduleTitle = $scheduleTitle; scheduleType = '2'; startTime = '2026-09-20 09:00:00'
        endTime = '2026-09-20 10:00:00'; location = '测试会议室'; scheduleContent = '自动化测试'; status = '0'
    } $token | Out-Null
    $scheduleList = Invoke-Api 'Get' ("/oa/schedule/list?pageNum=1&pageSize=20&scheduleTitle=" + $scheduleTitle) $null $token
    $schedule = $scheduleList.rows | Select-Object -First 1
    Assert-Test ($null -ne $schedule -and $schedule.scheduleTitle -eq $scheduleTitle) '日程新增与查询' "id=$($schedule.scheduleId)"
    $createdIds.schedule = $schedule.scheduleId

    Invoke-Api 'Post' '/oa/leave' @{
        leaveType = '1'; startTime = '2026-09-21 09:00:00'; endTime = '2026-09-21 18:00:00'
        leaveDays = 1; leaveReason = "自动化请假$suffix"
    } $token | Out-Null
    $leaveList = Invoke-Api 'Get' '/oa/leave/list?pageNum=1&pageSize=100' $null $token
    $leave = $leaveList.rows | Where-Object { $_.leaveReason -eq "自动化请假$suffix" } | Select-Object -First 1
    Assert-Test ($null -ne $leave) '请假新增与查询' "id=$($leave.leaveId)"
    $createdIds.leave = $leave.leaveId
    $leaveSubmit = Invoke-Api 'Put' ("/oa/leave/submit/" + $leave.leaveId) $null $token
    $leaveApprove = Invoke-Api 'Put' '/oa/leave/approve' @{ leaveId = $leave.leaveId; approvalStatus = '2'; remark = '测试通过' } $token
    Assert-Test ($leaveSubmit.code -eq 200 -and $leaveApprove.code -eq 200) '请假提交与审批'

    Invoke-Api 'Post' '/oa/reimbursement' @{
        reimbType = '2'; reimbReason = "自动化报销$suffix"; remark = '测试'
        details = @(@{ expenseType = '办公用品'; expenseDate = '2026-09-18'; expenseAmount = 88.50; expenseDesc = '测试明细' })
    } $token | Out-Null
    $reimbList = Invoke-Api 'Get' '/oa/reimbursement/list?pageNum=1&pageSize=100' $null $token
    $reimb = $reimbList.rows | Where-Object { $_.reimbReason -eq "自动化报销$suffix" } | Select-Object -First 1
    Assert-Test ($null -ne $reimb) '报销新增与查询' "id=$($reimb.reimbId)"
    $createdIds.reimbursement = $reimb.reimbId
    $details = Invoke-Api 'Get' ("/oa/reimbursement/details/" + $reimb.reimbId) $null $token
    $reimbSubmit = Invoke-Api 'Put' ("/oa/reimbursement/submit/" + $reimb.reimbId) $null $token
    $reimbApprove = Invoke-Api 'Put' '/oa/reimbursement/approve' @{ reimbId = $reimb.reimbId; approvalStatus = '2'; approveRemark = '测试通过' } $token
    Assert-Test ($details.data.Count -eq 1 -and $reimbSubmit.code -eq 200 -and $reimbApprove.code -eq 200) '报销明细、提交与审批'

    Invoke-Api 'Post' '/oa/incomingDoc' @{
        docNumber = "IN-$suffix"; docTitle = "自动化收文$suffix"; sourceUnit = '测试单位'; receiveDate = '2026-09-18'
        docLevel = '1'; docSecret = '1'; summary = '测试收文'; status = '0'
    } $token | Out-Null
    $incomingList = Invoke-Api 'Get' ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=IN-" + $suffix) $null $token
    $incoming = $incomingList.rows | Select-Object -First 1
    Assert-Test ($null -ne $incoming) '收文新增与查询' "id=$($incoming.docId)"
    $createdIds.incoming = $incoming.docId
    $incomingSign = Invoke-Api 'Put' ("/oa/incomingDoc/sign/" + $incoming.docId) $null $token
    $incomingComplete = Invoke-Api 'Put' ("/oa/incomingDoc/complete/" + $incoming.docId) $null $token
    Assert-Test ($incomingSign.code -eq 200 -and $incomingComplete.code -eq 200) '收文签收与办结'

    Invoke-Api 'Post' '/oa/outgoingDoc' @{
        docNumber = "OUT-$suffix"; docTitle = "自动化发文$suffix"; draftUnit = '测试部门'; draftDate = '2026-09-18'
        docLevel = '1'; docSecret = '1'; mainSend = '测试单位'; summary = '测试发文'; status = '0'
    } $token | Out-Null
    $outgoingList = Invoke-Api 'Get' ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=OUT-" + $suffix) $null $token
    $outgoing = $outgoingList.rows | Select-Object -First 1
    Assert-Test ($null -ne $outgoing) '发文新增与查询' "id=$($outgoing.docId)"
    $createdIds.outgoing = $outgoing.docId
    $outgoingSubmit = Invoke-Api 'Put' ("/oa/outgoingDoc/submit/" + $outgoing.docId) $null $token
    $outgoingSign = Invoke-Api 'Put' ("/oa/outgoingDoc/sign/" + $outgoing.docId) $null $token
    Assert-Test ($outgoingSubmit.code -eq 200 -and $outgoingSign.code -eq 200) '发文提交与签发'

    $flowName = "自动化流程$suffix"
    Invoke-Api 'Post' '/oa/docFlow' @{
        flowName = $flowName; flowType = '1'; description = '测试流程'; status = '0'
        steps = @(@{ stepName = '初审'; stepOrder = 1; roleId = 2; isSign = '1' })
    } $token | Out-Null
    $flowList = Invoke-Api 'Get' ("/oa/docFlow/list?pageNum=1&pageSize=20&flowName=" + $flowName) $null $token
    $flow = $flowList.rows | Select-Object -First 1
    Assert-Test ($null -ne $flow) '流程新增与查询' "id=$($flow.flowId)"
    $createdIds.flow = $flow.flowId
    $flowInfo = Invoke-Api 'Get' ("/oa/docFlow/" + $flow.flowId) $null $token
    $flowUpdate = Invoke-Api 'Put' '/oa/docFlow' @{
        flowId = $flow.flowId; flowName = $flowName; flowType = '1'; description = '修改后的流程'; status = '0'
        steps = @(@{ stepName = '复核'; stepOrder = 1; roleId = 2; isSign = '1' })
    } $token
    Assert-Test ($flowInfo.data.steps.Count -eq 1 -and $flowUpdate.code -eq 200) '流程步骤修改'

    $coreEndpoints = @(
        '/system/user/list?pageNum=1&pageSize=10',
        '/system/role/list?pageNum=1&pageSize=10',
        '/system/dept/list',
        '/oa/announcement/list?pageNum=1&pageSize=10',
        '/oa/schedule/list?pageNum=1&pageSize=10',
        '/oa/leave/list?pageNum=1&pageSize=10',
        '/oa/reimbursement/list?pageNum=1&pageSize=10',
        '/oa/incomingDoc/list?pageNum=1&pageSize=10',
        '/oa/outgoingDoc/list?pageNum=1&pageSize=10',
        '/oa/docFlow/list?pageNum=1&pageSize=10',
        '/oa/todo/list?pageNum=1&pageSize=10'
    )
    foreach ($endpoint in $coreEndpoints) {
        $response = Invoke-Api 'Get' $endpoint $null $token
        Assert-Test ($response.code -eq 200) ('核心列表 ' + $endpoint)
    }
} finally {
    if ($token) {
        try {
            if ($createdIds.flow) { Invoke-Api 'Delete' ("/oa/docFlow/" + $createdIds.flow) $null $token | Out-Null }
            if ($createdIds.outgoing) { Invoke-Api 'Delete' ("/oa/outgoingDoc/" + $createdIds.outgoing) $null $token | Out-Null }
            if ($createdIds.incoming) { Invoke-Api 'Delete' ("/oa/incomingDoc/" + $createdIds.incoming) $null $token | Out-Null }
            if ($createdIds.reimbursement) { Invoke-Api 'Delete' ("/oa/reimbursement/" + $createdIds.reimbursement) $null $token | Out-Null }
            if ($createdIds.leave) { Invoke-Api 'Delete' ("/oa/leave/" + $createdIds.leave) $null $token | Out-Null }
            if ($createdIds.schedule) { Invoke-Api 'Delete' ("/oa/schedule/" + $createdIds.schedule) $null $token | Out-Null }
            if ($createdIds.announcement) { Invoke-Api 'Delete' ("/oa/announcement/" + $createdIds.announcement) $null $token | Out-Null }
            if ($createdIds.dept) { Invoke-Api 'Delete' ("/system/dept/" + $createdIds.dept) $null $token | Out-Null }
            if ($createdIds.role) { Invoke-Api 'Delete' ("/system/role/" + $createdIds.role) $null $token | Out-Null }
            if ($createdIds.user) { Invoke-Api 'Delete' ("/system/user/" + $createdIds.user) $null $token | Out-Null }
            $script:Results.Add([pscustomobject]@{ Test = '测试数据清理'; Result = 'PASS'; Detail = '' })
        } catch {
            $script:Results.Add([pscustomobject]@{ Test = '测试数据清理'; Result = 'FAIL'; Detail = $_.Exception.Message })
        }
    }
}

$script:Results | Format-Table -AutoSize
$pass = @($script:Results | Where-Object Result -eq 'PASS').Count
$fail = @($script:Results | Where-Object Result -eq 'FAIL').Count
"SUMMARY PASS=$pass FAIL=$fail"
if ($fail -gt 0) { exit 1 }