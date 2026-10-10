param(
  [string]$BaseUrl='http://localhost:18081/api',
  # 仓库不再内置默认口令，初始密码由启动时生成并写入 app.init-password-file（默认 ./oa-init-passwords.txt），或由 OA_INIT_<用户名大写>_PASSWORD 提供
  [string]$AdminPassword=$env:OA_INIT_ADMIN_PASSWORD,
  [string]$CommonPassword=$env:OA_INIT_ZHANGSAN_PASSWORD
)
$ErrorActionPreference='Stop'
if(-not $AdminPassword){throw '请用 -AdminPassword 或环境变量 OA_INIT_ADMIN_PASSWORD 提供 admin 密码'}
if(-not $CommonPassword){throw '请用 -CommonPassword 或环境变量 OA_INIT_ZHANGSAN_PASSWORD 提供 zhangsan 密码'}
$suffix=[DateTimeOffset]::Now.ToUnixTimeSeconds()
$results=New-Object System.Collections.Generic.List[object]
function Check($name,$condition,$detail=''){ $state=$(if($condition){'PASS'}else{'FAIL'}); Write-Host "$state $name $detail"; $results.Add([pscustomobject]@{Test=$name;Result=$state;Detail=$detail})|Out-Null }
function Api($method,$path,$body,$token){
  $headers=@{}; if($token){$headers.Authorization='Bearer '+$token}
  $p=@{Uri=$BaseUrl+$path;Method=$method;Headers=$headers;TimeoutSec=20;UseBasicParsing=$true}
  if($null -ne $body){
    $json=$body|ConvertTo-Json -Depth 12 -Compress
    # 显式声明 charset 并以 UTF-8 字节发送：否则 PS 5.1 按 ISO-8859-1 编码，中文会被服务端存成乱码
    $p.ContentType='application/json; charset=utf-8'
    $p.Body=[System.Text.Encoding]::UTF8.GetBytes($json)
  }
  try {
    $r=Invoke-WebRequest @p
    $text=[System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
    if([string]::IsNullOrWhiteSpace($text)){ return $null }
    return $text|ConvertFrom-Json
  } catch {
    $status=[int]$_.Exception.Response.StatusCode
    $msg=$_.Exception.Message
    try {
      $sr=New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream(),[System.Text.Encoding]::UTF8)
      $body2=$sr.ReadToEnd(); if(-not [string]::IsNullOrWhiteSpace($body2)){ $msg=$body2 }
    } catch {}
    [pscustomobject]@{code=$status;msg=$msg}
  }
}
# 控制台按 UTF-8 输出，否则中文提示在日志里是乱码（断言只比较 code，不影响判定）
try { [Console]::OutputEncoding=[System.Text.Encoding]::UTF8 } catch {}
# PS 5.1 的 Invoke-RestMethod 没有 -Form 参数（仅 PS 7 有），这里手工拼 multipart/form-data
function UploadFile($url,$filePath,$fields,$token){
  $boundary=[System.Guid]::NewGuid().ToString('N'); $LF="`r`n"; $enc=[System.Text.Encoding]::UTF8
  $ms=New-Object System.IO.MemoryStream
  $fileName=[System.IO.Path]::GetFileName($filePath)
  $pre="--$boundary$LF"+"Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$LF"+"Content-Type: application/octet-stream$LF$LF"
  $b=$enc.GetBytes($pre); $ms.Write($b,0,$b.Length)
  $fb=[System.IO.File]::ReadAllBytes($filePath); $ms.Write($fb,0,$fb.Length)
  foreach($k in $fields.Keys){
    $part="$LF--$boundary$LF"+"Content-Disposition: form-data; name=`"$k`"$LF$LF"+"$($fields[$k])"
    $b=$enc.GetBytes($part); $ms.Write($b,0,$b.Length)
  }
  $b=$enc.GetBytes("$LF--$boundary--$LF"); $ms.Write($b,0,$b.Length)
  $body=$ms.ToArray(); $ms.Dispose()
  try {
    $r=Invoke-WebRequest -Uri $url -Method Post -Headers @{Authorization='Bearer '+$token} -ContentType "multipart/form-data; boundary=$boundary" -Body $body -UseBasicParsing -TimeoutSec 30
    return ([System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()))|ConvertFrom-Json
  }
  catch { $s=[int]$_.Exception.Response.StatusCode; [pscustomobject]@{code=$s;msg=$_.Exception.Message} }
}
$admin=Api Post '/login' @{username='admin';password=$AdminPassword} $null; $token=$admin.data.token
# 第 11 轮起 /register 不再匿名开放（要求 system:user:add），且已被移出 SecurityConfig 白名单，
# 因此匿名调用在鉴权过滤器阶段即返回 401（此前是 403）。两者都表示"匿名不可注册"。
$register=Api Post '/register' @{userName="anonymous$suffix";nickName='匿名';password='Test123456'} $null; Check '匿名注册拦截' ($register.code -eq 401 -or $register.code -eq 403) $register.msg
$probeUser="missing-user-$suffix"; $probeCode=0; foreach($attempt in 1..5){$probe=Api Post '/login' @{username=$probeUser;password='wrong-password'} $null; $probeCode=$probe.code}; Check '登录失败计数' ($probeCode -eq 400) "code=$probeCode"
$locked=Api Post '/login' @{username=$probeUser;password='wrong-password'} $null; Check '登录失败锁定' ($locked.code -eq 429) $locked.msg
# Duplicate and empty data validation
$dupUser=Api Post '/system/user' @{userName='admin';nickName='Duplicate';password='Test123456'} $token; Check '重复账号拦截' ($dupUser.code  -eq  409) $dupUser.msg
$emptyUser=Api Post '/system/user' @{userName='';password=''} $token; Check '空用户数据拦截' ($emptyUser.code  -eq  400) $emptyUser.msg
$emptyDoc=Api Post '/oa/incomingDoc' @{docNumber='';docTitle=''} $token; Check '空公文数据拦截' ($emptyDoc.code  -eq  400) $emptyDoc.msg
# Incoming workflow
$inNum="P2-IN-$suffix"; $a=Api Post '/oa/incomingDoc' @{docNumber=$inNum;docTitle="P2收文$suffix";sourceUnit='测试单位';receiveDate='2026-09-20';docLevel='1';docSecret='1';summary='测试';status='0'} $token; Check '收文创建' ($a.code -eq  200)
$inList=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=$inNum") $null $token; $inDoc=$inList.rows|Select-Object -First 1; $inId=$inDoc.docId
$badArchive=Api Post ("/oa/doc/action/1/$inId") @{action='archive'} $token; Check '非法状态归档拦截' ($badArchive.code  -eq  400) $badArchive.msg
Api Post ("/oa/doc/action/1/$inId") @{action='receive'} $token|Out-Null
$ret=Api Post ("/oa/doc/action/1/$inId") @{action='return';opinion='退回修改'} $token; Check '收文退回' ($ret.code -eq  200)
$trans=Api Post ("/oa/doc/action/1/$inId") @{action='transfer';targetUserId=2;targetUserName='张三';opinion='转办'} $token; Check '收文转办' ($trans.code -eq  200)
$add=Api Post ("/oa/doc/action/1/$inId") @{action='add_sign';targetUserIds=@(2,3);opinion='加签'} $token; Check '多用户加签' ($add.code -eq  200)
# 撤签撤的是「流程派发出来的待办」记录（approve_result='0'）；加签/会签写入的是已办结的办理意见（'5'/'7'），
# 按设计不可撤。而 $inId 这条收文没有绑定流程、从来没有待办可撤，所以下面单独造一条绑定流程的收文来验证撤签。
$csFlowName="CANCELSIGN-FLOW-$suffix"; Api Post '/oa/docFlow' @{flowName=$csFlowName;flowType='1';description='撤签回归';status='0';steps=@(@{stepName='承办';stepOrder=1;roleId=2;isSign='1'})} $token|Out-Null
$csFlowList=Api Get ("/oa/docFlow/list?pageNum=1&pageSize=20&flowName=$csFlowName") $null $token; $csFlowId=($csFlowList.rows|Select-Object -First 1).flowId
$csDocNum="CANCELSIGN-DOC-$suffix"; Api Post '/oa/incomingDoc' @{docNumber=$csDocNum;docTitle='撤签验证收文';sourceUnit='测试单位';receiveDate='2026-09-20';docLevel='1';docSecret='1';summary='撤签';status='0';flowId=$csFlowId} $token|Out-Null
$csDocList=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=$csDocNum") $null $token; $csDocId=($csDocList.rows|Select-Object -First 1).docId
Api Post ("/oa/doc/action/1/$csDocId") @{action='receive'} $token|Out-Null
$csTl=Api Get ("/oa/doc/timeline/1/$csDocId") $null $token; $csPending=@($csTl.data.approvals|Where-Object {$_.approveResult -eq '0'})|Select-Object -First 1
Check '流程派发产生待办签批' ($null -ne $csPending.approvalId) "approvalId=$($csPending.approvalId)"
$cancel=Api Post ("/oa/doc/action/1/$csDocId") @{action='cancel_sign';approvalId=$csPending.approvalId;opinion='撤签'} $token; Check '撤签待办签批' ($cancel.code -eq  200) $cancel.msg
$cancelAgain=Api Post ("/oa/doc/action/1/$csDocId") @{action='cancel_sign';approvalId=$csPending.approvalId;opinion='重复撤签'} $token; Check '重复撤签被拒' ($cancelAgain.code -eq  400) $cancelAgain.msg
$tl=Api Get ("/oa/doc/timeline/1/$inId") $null $token; $sign=@($tl.data.approvals|Where-Object {$_.approveResult -eq '7'})|Select-Object -First 1
Check '加签产生已办结签批记录' ($null -ne $sign.approvalId) "approvalId=$($sign.approvalId)"
$cancelDone=Api Post ("/oa/doc/action/1/$inId") @{action='cancel_sign';approvalId=$sign.approvalId;opinion='撤已办理'} $token; Check '已办结签批不可撤' ($cancelDone.code -eq  400) $cancelDone.msg
$counter=Api Post ("/oa/doc/action/1/$inId") @{action='countersign';targetUserIds=@(2,3);opinion='多人会签'} $token; Check '多用户会签' ($counter.code -eq  200)
$delegate=Api Post ("/oa/doc/action/1/$inId") @{action='delegate';targetUserId=2;targetUserName='张三';opinion='委托'} $token; Check '委托' ($delegate.code -eq  200)
$agent=Api Post ("/oa/doc/action/1/$inId") @{action='agent';targetUserId=3;targetUserName='李四';opinion='代理'} $token; Check '代理' ($agent.code -eq  200)
$withdraw=Api Post ("/oa/doc/action/1/$inId") @{action='withdraw';opinion='撤回'} $token; Check '收文撤回' ($withdraw.code -eq  200)
Api Post ("/oa/doc/action/1/$inId") @{action='receive'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='complete'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='archive';archiveCode="P2-ARCH-IN-$suffix";archiveLocation='档案室'} $token|Out-Null
$tl2=Api Get ("/oa/doc/timeline/1/$inId") $null $token; Check '收文流程记录完整' (@($tl2.data.approvals).Count  -ge  5 -and @($tl2.data.delegates).Count  -eq  2) "approvals=$(@($tl2.data.approvals).Count) delegates=$(@($tl2.data.delegates).Count)"
# Outgoing workflow
# 发文提交要求先指定签发人（requireSignerAssigned），且签发人不能是起草人本人，因此这里指定 userId=2
$outNum="P2-OUT-$suffix"; Api Post '/oa/outgoingDoc' @{docNumber=$outNum;docTitle="P2发文$suffix";draftUnit='测试部门';draftDate='2026-09-20';docLevel='1';docSecret='1';mainSend='测试单位';summary='测试';signUserId=2;status='0'} $token|Out-Null
$outList=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$outNum") $null $token; $outDoc=$outList.rows|Select-Object -First 1; $outId=$outDoc.docId
$badSign=Api Post ("/oa/doc/action/2/$outId") @{action='sign'} $token; Check '非法状态签发拦截' ($badSign.code -eq  400) $badSign.msg
Api Post ("/oa/doc/action/2/$outId") @{action='submit'} $token|Out-Null
$ow=Api Post ("/oa/doc/action/2/$outId") @{action='withdraw'} $token; Check '发文撤回' ($ow.code -eq  200)
Api Post ("/oa/doc/action/2/$outId") @{action='submit'} $token|Out-Null
$or=Api Post ("/oa/doc/action/2/$outId") @{action='return'} $token; Check '发文退回' ($or.code -eq  200)
Api Post ("/oa/doc/action/2/$outId") @{action='submit'} $token|Out-Null
Api Post ("/oa/doc/action/2/$outId") @{action='approve';opinion='审核通过'} $token|Out-Null
$outSign=Api Post ("/oa/doc/action/2/$outId") @{action='sign'} $token; Check '发文签发' ($outSign.code -eq 200) $outSign.msg
Api Post ("/oa/doc/action/2/$outId") @{action='archive';archiveCode="P2-ARCH-OUT-$suffix";archiveLocation='档案室'} $token|Out-Null
$otl=Api Get ("/oa/doc/timeline/2/$outId") $null $token; Check '发文全流程完成' (@($otl.data.approvals).Count  -ge  3 -and @($otl.data.archives).Count  -eq  1) "approvals=$(@($otl.data.approvals).Count)"
# Duplicate document number
$dupDoc=Api Post '/oa/incomingDoc' @{docNumber=$inNum;docTitle='重复编号';sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';status='0'} $token; Check '重复公文编号拦截' ($dupDoc.code  -eq  409) $dupDoc.msg
# Illegal IDs
$badId=Api Post '/oa/doc/action/1/99999999' @{action='receive'} $token; Check '不存在公文ID拦截' ($badId.code -eq  400) $badId.msg
# Data scopes: /system/user/list 会套用数据范围；/system/user/options 是跨部门选人用的，故意不套范围
$usersToCreate=@(
 @{userName="scope103$suffix";nickName='范围103';password='Test123456';deptId=103;roleIds=@(2)},
 @{userName="scope101$suffix";nickName='范围101';password='Test123456';deptId=101;roleIds=@(2)}
); $createdUsers=@(); foreach($u in $usersToCreate){ $r=Api Post '/system/user' $u $token; $ul=Api Get ("/system/user/list?pageNum=1&pageSize=20&userName="+$u.userName) $null $token; $createdUsers+=($ul.rows|Select-Object -First 1) }
$commonLogin=Api Post '/login' @{username='zhangsan';password=$CommonPassword} $null; $zh=$commonLogin.data.token
$unauthorizedAdd=Api Post '/oa/incomingDoc' @{docNumber="DENIED-IN-$suffix";docTitle='越权收文';sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';status='0'} $zh; Check '普通用户收文新增权限拦截' ($unauthorizedAdd.code -eq 403) $unauthorizedAdd.msg
$unauthorizedDelete=Api Delete ("/oa/incomingDoc/"+$inId) $null $zh; Check '普通用户收文删除权限拦截' ($unauthorizedDelete.code -eq 403) $unauthorizedDelete.msg
$unauthNum="DENIED-ACTION-$suffix"; Api Post '/oa/incomingDoc' @{docNumber=$unauthNum;docTitle='越权审批测试';sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';docTypeId=1000;status='0'} $token|Out-Null
$unauthList=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=$unauthNum") $null $token; $unauthDocId=($unauthList.rows|Select-Object -First 1).docId
$unauthorizedAction=Api Post ("/oa/doc/action/1/$unauthDocId") @{action='receive';opinion='越权签收'} $zh; Check '普通用户公文处理人校验' ($unauthorizedAction.code -eq 403) $unauthorizedAction.msg
$flowName="AUTO-FLOW-$suffix"; Api Post '/oa/docFlow' @{flowName=$flowName;flowType='2';description='自动流转回归';status='0';steps=@(@{stepName='领导签发';stepOrder=1;roleId=2;isSign='1'})} $token|Out-Null
$flowList=Api Get ("/oa/docFlow/list?pageNum=1&pageSize=20&flowName=$flowName") $null $token; $deniedFlowId=($flowList.rows|Select-Object -First 1).flowId
$flowInfo=Api Get ("/oa/docFlow/$deniedFlowId") $null $token; $deniedStepId=($flowInfo.data.steps|Select-Object -First 1).stepId
# 流程类发文同样要先指定签发人（且不能是起草人），否则 submit 会失败、流程不会分派
$flowDocNum="AUTO-FLOW-DOC-$suffix"; Api Post '/oa/outgoingDoc' @{docNumber=$flowDocNum;docTitle='自动流转测试';draftUnit='测试部门';draftDate='2026-09-21';docLevel='1';docSecret='1';docTypeId=1000;flowId=$deniedFlowId;mainSend='测试单位';signUserId=2;status='0'} $token|Out-Null
$flowDocList=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$flowDocNum") $null $token; $flowDocId=($flowDocList.rows|Select-Object -First 1).docId
Api Post ("/oa/doc/action/2/$flowDocId") @{action='submit'} $token|Out-Null
$todoList=Api Get '/oa/todo/list?pageNum=1&pageSize=100' $null $zh
Check '提交后自动流转到对应角色' (@($todoList.rows|Where-Object {$_.relatedId -eq $flowDocId}).Count -ge 1) "count=$(@($todoList.rows).Count)"
$flowApprove=Api Post ("/oa/doc/action/2/$flowDocId") @{action='approve';opinion='同意'} $zh
Check '角色待办审批处理' ($flowApprove.code -eq 200) $flowApprove.msg
$flowDocAfter=Api Get ("/oa/outgoingDoc/$flowDocId") $null $token
Check '流程末步骤自动完成' ($flowDocAfter.data.status -eq '2' -and $flowDocAfter.data.signUserId -eq 2) "status=$($flowDocAfter.data.status) signUser=$($flowDocAfter.data.signUserId)"

$adminFlowName="BINDING-FLOW-$suffix"; Api Post '/oa/docFlow' @{flowName=$adminFlowName;flowType='2';description='绑定回归';status='0';steps=@(@{stepName='管理员步骤';stepOrder=1;roleId=1;isSign='1'})} $token|Out-Null
$adminFlowList=Api Get ("/oa/docFlow/list?pageNum=1&pageSize=20&flowName=$adminFlowName") $null $token; $adminFlowId=($adminFlowList.rows|Select-Object -First 1).flowId
$bindingDocNum="BINDING-FLOW-DOC-$suffix"; Api Post '/oa/outgoingDoc' @{docNumber=$bindingDocNum;docTitle='流程绑定越权测试';draftUnit='测试部门';draftDate='2026-09-21';docLevel='1';docSecret='1';docTypeId=1000;flowId=$adminFlowId;mainSend='测试单位';signUserId=2;status='0'} $token|Out-Null
$bindingList=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$bindingDocNum") $null $token; $bindingDocId=($bindingList.rows|Select-Object -First 1).docId
Api Post ("/oa/doc/action/2/$bindingDocId") @{action='submit'} $token|Out-Null
$flowBypass=Api Post ("/oa/doc/action/2/$bindingDocId") @{action='sign';flowId=$deniedFlowId;stepId=$deniedStepId;opinion='流程角色越权'} $zh
Check '流程模板角色不能绕过公文分派' ($flowBypass.code -eq 403) $flowBypass.msg
# 普通用户角色默认没有 system:user:list，临时补上该菜单（本段结束后还原），否则只会拿到 403
$commonRole=Api Get '/system/role/2' $null $token; $commonMenuIds=@($commonRole.data.menuIds)
Check '读取普通用户角色菜单' ($null -ne $commonRole.data -and $commonMenuIds.Count -gt 0) "count=$($commonMenuIds.Count)"
# self scope (5)
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='5';deptIds=@();menuIds=($commonMenuIds+103);status='0'} $token|Out-Null
$selfScope=Api Get '/system/user/list?pageNum=1&pageSize=100' $null $zh; Check '仅本人数据范围' (@($selfScope.rows).Count  -eq  1) "count=$(@($selfScope.rows).Count)"
# dept scope (3): zhangsan + scope103
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='3';deptIds=@();status='0'} $token|Out-Null
$deptScope=Api Get '/system/user/list?pageNum=1&pageSize=100' $null $zh; Check '本部门数据范围' (@($deptScope.rows|Where-Object {$_.deptId -eq  103}).Count  -ge  2) "count=$(@($deptScope.rows).Count)"
# dept and below (4)
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='4';deptIds=@();status='0'} $token|Out-Null
$belowScope=Api Get '/system/user/list?pageNum=1&pageSize=100' $null $zh; Check '本部门及以下数据范围' (@($belowScope.rows|Where-Object {$_.deptId -eq  103}).Count  -ge  2) "count=$(@($belowScope.rows).Count)"
# custom dept (2): self + dept101
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='2';deptIds=@(101);status='0'} $token|Out-Null
$customScope=Api Get '/system/user/list?pageNum=1&pageSize=100' $null $zh; Check '自定义部门数据范围' (@($customScope.rows|Where-Object {$_.deptId -eq  101}).Count  -ge  1 -and @($customScope.rows|Where-Object {$_.userId -eq  2}).Count  -eq  1) "count=$(@($customScope.rows).Count)"
# multi-role union: self scope + current department scope
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='5';deptIds=@();status='0'} $token|Out-Null
Api Put '/system/role' @{roleId=3;roleName='部门经理';roleKey='manager';roleSort=3;dataScope='3';deptIds=@();status='0'} $token|Out-Null
Api Put '/system/user' @{userId=2;userName='zhangsan';nickName='张三';deptId=103;roleIds=@(2,3);status='0'} $token|Out-Null
$combinedLogin=Api Post '/login' @{username='zhangsan';password=$CommonPassword} $null
$combinedScope=Api Get '/system/user/list?pageNum=1&pageSize=100' $null $combinedLogin.data.token
Check '多角色数据范围并集' (@($combinedScope.rows|Where-Object {$_.deptId -eq 103}).Count -ge 2) "count=$(@($combinedScope.rows).Count)"
# 去掉临时补上的用户列表权限
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='5';deptIds=@();menuIds=$commonMenuIds;status='0'} $token|Out-Null
# restore role scopes and user role
Api Put '/system/role' @{roleId=3;roleName='部门经理';roleKey='manager';roleSort=3;dataScope='4';deptIds=@();status='0'} $token|Out-Null
Api Put '/system/user' @{userId=2;userName='zhangsan';nickName='张三';deptId=103;roleIds=@(2);status='0'} $token|Out-Null
# State transition rejection matrix
$matrixDocs=@()
foreach($status in 0..3){
  $num="P2-MATRIX-IN-$suffix-$status"
  $created=Api Post '/oa/incomingDoc' @{docNumber=$num;docTitle="矩阵收文$status";sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';status='0'} $token
  $list=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=$num") $null $token
  $id=($list.rows|Select-Object -First 1).docId
  if($status -ge 1){Api Post ("/oa/doc/action/1/$id") @{action='receive'} $token|Out-Null}
  if($status -ge 2){Api Post ("/oa/doc/action/1/$id") @{action='complete'} $token|Out-Null}
  if($status -ge 3){Api Post ("/oa/doc/action/1/$id") @{action='archive';archiveCode="P2-M-ARCH-IN-$suffix-$status"} $token|Out-Null}
  $matrixDocs += [pscustomobject]@{Type='1';Id=$id;Status="$status"}
}
foreach($status in 0..3){
  $num="P2-MATRIX-OUT-$suffix-$status"
  Api Post '/oa/outgoingDoc' @{docNumber=$num;docTitle="矩阵发文$status";draftUnit='测试部门';draftDate='2026-09-20';docLevel='1';docSecret='1';mainSend='测试单位';status='0'} $token|Out-Null
  $list=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$num") $null $token
  $id=($list.rows|Select-Object -First 1).docId
  if($status -ge 1){Api Post ("/oa/doc/action/2/$id") @{action='submit'} $token|Out-Null}
  if($status -ge 2){Api Post ("/oa/doc/action/2/$id") @{action='sign'} $token|Out-Null}
  if($status -ge 3){Api Post ("/oa/doc/action/2/$id") @{action='archive';archiveCode="P2-M-ARCH-OUT-$suffix-$status"} $token|Out-Null}
  $matrixDocs += [pscustomobject]@{Type='2';Id=$id;Status="$status"}
}
$allActions=@('receive','submit','approve','countersign','add_sign','cancel_sign','return','withdraw','transfer','delegate','agent','circulate','urge','complete','sign','archive')
$legalActions=@{
  '1'=@{
    '0'=@('receive')
    '1'=@('approve','countersign','add_sign','cancel_sign','return','withdraw','transfer','delegate','agent','circulate','urge','complete')
    '2'=@('archive','circulate','urge')
    '3'=@()
  }
  '2'=@{
    '0'=@('submit')
    '1'=@('approve','countersign','add_sign','cancel_sign','return','withdraw','transfer','delegate','agent','circulate','urge','sign')
    '2'=@('archive','circulate','urge')
    '3'=@()
  }
}
$illegalChecked=0
foreach($entry in $matrixDocs){
  foreach($action in $allActions){
    if($action -in @($legalActions[$entry.Type][$entry.Status])){continue}
    $illegal=Api Post ("/oa/doc/action/$($entry.Type)/$($entry.Id)") @{action=$action;opinion='矩阵测试';targetUserId=2;targetUserIds=@(2,3);approvalId=1} $token
    $illegalChecked++
    Check "状态$($entry.Status)非法操作$action" ($illegal.code -eq 400) "type=$($entry.Type) code=$($illegal.code) msg=$($illegal.msg)"
  }
}
Check '非法状态矩阵完整执行' ($illegalChecked -gt 90) "checked=$illegalChecked"
# 管理员会短路 requireActionPermission，上面所有动作都走管理员，这里用普通用户验证公文授权仍然生效
$matrixIncoming=($matrixDocs|Where-Object {$_.Type -eq '1' -and $_.Status -eq '0'})|Select-Object -First 1
$nonAdminAction=Api Post ("/oa/doc/action/1/"+$matrixIncoming.Id) @{action='receive';opinion='越权办理'} $zh
Check '普通用户越权办理公文拦截' ($nonAdminAction.code -eq 403) $nonAdminAction.msg
$blankAction=Api Post ("/oa/doc/action/1/$inId") @{action=''} $token; Check '空动作拦截' ($blankAction.code -eq 400) $blankAction.msg
$badDocType=Api Post ("/oa/doc/action/9/$inId") @{action='receive'} $token; Check '非法公文类型拦截' ($badDocType.code -eq 400) $badDocType.msg

# File upload, download and ownership checks
$tempFile=Join-Path (Resolve-Path 'ruoyi-admin\target').Path ("phase2-upload-$suffix.txt")
[System.IO.File]::WriteAllText($tempFile,'phase2 upload permission test')
$fileName=[System.IO.Path]::GetFileName($tempFile)
$upload=UploadFile ($BaseUrl+'/oa/document/upload') $tempFile @{documentType='phase2'} $token
Check '文件上传成功' ($upload.code -eq 200) $upload.msg
$blockedFile=Join-Path (Resolve-Path 'ruoyi-admin\target').Path ("phase2-blocked-$suffix.html")
[System.IO.File]::WriteAllText($blockedFile,'<script>alert(1)</script>')
$blockedUpload=UploadFile ($BaseUrl+'/oa/document/upload') $blockedFile @{documentType='phase2'} $token
Check '危险文件类型拦截' ($blockedUpload.code -eq 400) $blockedUpload.msg
[System.IO.File]::Delete($blockedFile)
$adminFiles=Api Get ("/oa/document/list?pageNum=1&pageSize=100&documentName=$fileName") $null $token
$fileDoc=$adminFiles.rows|Select-Object -First 1
Check '管理员可见上传文件' ($null -ne $fileDoc) "count=$($adminFiles.total)"
$userFiles=Api Get ("/oa/document/list?pageNum=1&pageSize=100&documentName=$fileName") $null $zh
Check '普通用户不可见他人文件' (@($userFiles.rows).Count -eq 0) "count=$($userFiles.total)"
$adminDownload=Invoke-WebRequest -Uri ($BaseUrl+'/oa/document/download/'+$fileDoc.documentId) -Headers @{Authorization='Bearer '+$token} -UseBasicParsing -TimeoutSec 20
Check '管理员下载文件成功' ($adminDownload.StatusCode -eq 200 -and $adminDownload.RawContentLength -gt 0) "length=$($adminDownload.RawContentLength)"
$userDownloadStatus=0
try{Invoke-WebRequest -Uri ($BaseUrl+'/oa/document/download/'+$fileDoc.documentId) -Headers @{Authorization='Bearer '+$zh} -UseBasicParsing -TimeoutSec 20|Out-Null}catch{$userDownloadStatus=[int]$_.Exception.Response.StatusCode}
Check '普通用户下载他人文件被拦截' ($userDownloadStatus -eq 404) "status=$userDownloadStatus"
$userDelete=Api Delete ("/oa/document/"+$fileDoc.documentId) $null $zh
Check '普通用户删除他人文件被拦截' ($userDelete.code -eq 403) $userDelete.msg
$adminDeleteFile=Api Delete ("/oa/document/"+$fileDoc.documentId) $null $token
Check '管理员删除文件成功' ($adminDeleteFile.code -eq 200) $adminDeleteFile.msg
[System.IO.File]::Delete($tempFile)

# audit logs
$logs=Api Get '/system/log/list?pageNum=1&pageSize=100' $null $token; Check '审计日志生成' (@($logs.rows).Count  -gt  0) "count=$($logs.total)"
$loginLogs=Api Get '/system/log/list?pageNum=1&pageSize=100&title=SysLoginController' $null $token
$loginLog=$loginLogs.rows|Where-Object {$_.operParam -match 'password'}|Select-Object -First 1; Check '登录日志存在' ($null -ne $loginLog) "count=$($loginLogs.total)"
Check '登录参数密码脱敏' ($loginLog.operParam -match '\*\*\*' -and $loginLog.operParam -notmatch [regex]::Escape($AdminPassword)) $loginLog.operParam
Check '登录响应token脱敏' ($loginLog.jsonResult -match '\*\*\*' -and $loginLog.jsonResult -notmatch 'eyJ') $loginLog.jsonResult
$badActionLogs=Api Get '/system/log/list?pageNum=1&pageSize=500&title=OaDocProcessController' $null $token
$badActionLog=$badActionLogs.rows|Where-Object {$_.operUrl -eq '/oa/doc/action/1/99999999'}|Select-Object -First 1
Check '失败操作审计准确' ($null -ne $badActionLog -and $badActionLog.status -eq '1' -and $badActionLog.operatorName -eq 'admin' -and $badActionLog.errorMsg -match '公文不存在') "log=$($badActionLog|ConvertTo-Json -Compress)"
$export=Invoke-WebRequest -Uri ($BaseUrl+'/system/log/export') -Headers @{Authorization='Bearer '+$token} -UseBasicParsing -TimeoutSec 20; Check '审计日志导出' ($export.StatusCode -eq  200 -and $export.RawContentLength -gt  0) "length=$($export.RawContentLength)"
Check '审计归档不含明文凭证' ($export.Content -notmatch [regex]::Escape($AdminPassword) -and $export.Content -notmatch 'eyJ') "length=$($export.RawContentLength)"
$clean=Api Delete '/system/log/clean?days=3650' $null $token; Check '审计日志清理接口' ($clean.code -eq  200) $clean.msg
# cleanup documents/users
foreach($matrix in $matrixDocs){ if($matrix.Id){Api Delete ("/oa/"+($(if($matrix.Type -eq '1'){'incomingDoc'}else{'outgoingDoc'}))+"/"+$matrix.Id) $null $token|Out-Null} }
foreach($u in $createdUsers){ if($u){Api Delete ("/system/user/"+$u.userId) $null $token|Out-Null} }
if($unauthDocId){Api Delete ("/oa/incomingDoc/"+$unauthDocId) $null $token|Out-Null}
if($flowDocId){Api Delete ("/oa/outgoingDoc/"+$flowDocId) $null $token|Out-Null}
if($bindingDocId){Api Delete ("/oa/outgoingDoc/"+$bindingDocId) $null $token|Out-Null}
if($deniedFlowId){Api Delete ("/oa/docFlow/"+$deniedFlowId) $null $token|Out-Null}
if($adminFlowId){Api Delete ("/oa/docFlow/"+$adminFlowId) $null $token|Out-Null}
if($csDocId){Api Delete ("/oa/incomingDoc/"+$csDocId) $null $token|Out-Null}
if($csFlowId){Api Delete ("/oa/docFlow/"+$csFlowId) $null $token|Out-Null}
Api Delete ("/oa/incomingDoc/"+$inId) $null $token|Out-Null; Api Delete ("/oa/outgoingDoc/"+$outId) $null $token|Out-Null
# 还原角色2（普通用户）的数据范围与菜单：前面把它依次改成过 scope 5/3/4/2 并临时加了菜单103，不还原会污染环境
if($commonMenuIds){ Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='5';deptIds=@();menuIds=@($commonMenuIds);status='0'} $token|Out-Null }
$results|Format-Table -AutoSize; $pass=@($results|Where-Object Result -eq 'PASS').Count; $fail=@($results|Where-Object Result -eq 'FAIL').Count; "SUMMARY PASS=$pass FAIL=$fail"
