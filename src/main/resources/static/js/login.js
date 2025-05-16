$(function () {
    $("#loginBtn").click(login_jwt);
});

function login_jwt() {
    // 收集参数
    var username = $("#username").val();
    var password = $("#password").val();
    var code = $("#verifycode").val();
    var rememberme = $("#remember-me").val();

    // 发起请求
    $.post(
        CONTEXT_PATH + "/login",
        {"username": username, "password": password, "code": code, "rememberme": rememberme},
        function (data) {
            data = $.parseJSON(data);
            if (data.status == 200) {
                const token = data.data;

                window.localStorage.setItem("Authorization", token);
                window.location.replace(CONTEXT_PATH + "/index");
            } else {
                if (data.status == 566) {
                    $("#usernameDiv").val(data.msg);
                }else if (data.status == 571) {
                    $("#passwordDiv").val(data.msg);
                }else if (data.status == 506) {
                    $("#codeDiv").val(data.msg);
                } else {
                    $("#usernameDiv").val(data.data.usernameMsg);
                    $("#passwordDiv").val(data.msg.passwordMsg);
                }
            }

        }
    );
}