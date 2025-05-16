$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");

	// 获取标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();
	// 发送异步请求(POST)
	$.post(
	    CONTEXT_PATH + "/discuss/add",
	    {"title":title,"content":content},
	    function(data) {
	    	// data = jQuery.parseJSON(data);
			data = $.parseJSON(data);
	        // 在提示框中显示返回消息
			// console.log(data.msg)

			if(data.status == 200) {
				setTimeout(function(){
					// $("#hintModal").modal("hide");
					location.reload();
				}, 100);
				// $("#hintBody").text("发送成功!");
			} else {
				$("#hintBody").text(data.msg);
				$("#hintModal").modal("show");
				setTimeout(function(){
					// $("#hintModal").modal("hide");
					location.reload();
				}, 700);
			}

	        // $("#hintBody").text(data.msg);
	        // // 显示提示框
            // $("#hintModal").modal("show");
            // 2秒后,自动隐藏提示框
            // setTimeout(function(){
            //     // $("#hintModal").modal("hide");
            //     // 刷新页面
            //     if(data.status == 200) {
            //         window.location.reload();
            //     }
            // }, 700);
	    }
	);

}