$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false,
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function(data) {
            console.log("asd");
            if(data && data.status == 200) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.status == 200) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false;
}