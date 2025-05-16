package org.wgd.community.controller;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.wgd.community.annotation.LoginRequired;
import org.wgd.community.common.GraceJSONResult;
import org.wgd.community.common.ResponseStatusEnum;
import org.wgd.community.common.enums.EntityTypeEnum;
import org.wgd.community.model.pojo.DiscussPost;
import org.wgd.community.model.pojo.Page;
import org.wgd.community.model.pojo.ReplyInfo;
import org.wgd.community.model.pojo.User;
import org.wgd.community.service.CommentService;
import org.wgd.community.service.UserService;
import org.wgd.community.service.impl.CommentServiceImpl;
import org.wgd.community.service.impl.DiscussPostServiceImpl;
import org.wgd.community.service.impl.RedisServiceImpl;
import org.wgd.community.util.HostHolder;
import org.wgd.community.util.UUIDUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {
    @Value("${community.path.upload}")
    private String uploadPath;

    /**
     * 协议+ip+端口
     */
    @Value("${community.path}")
    private String path;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private RedisServiceImpl redisService;

    @Autowired
    private DiscussPostServiceImpl discussPostService;

    @Autowired
    private CommentServiceImpl commentService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${quniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 生成头像名
        String fileName = UUIDUtils.simpleUUID();
        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", GraceJSONResult.okJson());
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        // 将上传凭证，生成唯一随机图片名，返回给前端
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }

    /**
     * 修改密码
     * @param oldPassword
     * @param newPassword
     * @param confirmPassword
     * @param model
     * @return
     */
    @RequestMapping(path = "/updatePwd", method = RequestMethod.POST)
    public String updatePwd(String oldPassword, String newPassword, String confirmPassword, Model model){
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword, confirmPassword);

        model.addAttribute("errorPsd", map.get("errorPsd"));
        model.addAttribute("successPsd", map.get("successPsd"));

        return "site/setting";
    }

    /**
     * 更新头像路径
     * @param fileName
     * @return
     */
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        // 判空
        if (StringUtils.isBlank(fileName)) {
            return GraceJSONResult.errorCustomJson(ResponseStatusEnum.FILE_UPLOAD_NULL_ERROR);
        }

        // 更新数据库中user的头像访问路径
        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeaderById(hostHolder.getUser().getId(), url);

        return GraceJSONResult.okJson();
    }

    /**
     * 已废弃
     * 上传头像
     * @param headerImage
     * @param model
     * @return
     */
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting";
        }

        // 原始文件名
        String fileName = headerImage.getOriginalFilename();
        // 后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 后缀为空
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = UUIDUtils.simpleUUID() + suffix;
        // 确定文件存放的路径：D。。。
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 将图片文件写入指定路径
            headerImage.transferTo(dest);
        } catch (IOException e) {
            log.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = path + contextPath + "/user/header/" + fileName;
        userService.updateHeaderById(user.getId(), headerUrl);

        return "redirect:/index";
    }

    /**
     * 已废弃
     * @param fileName
     * @param response
     */
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 本地存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 动态的根据上传图片类型来设置contentType
        response.setContentType("image/" + suffix);
        // 将图片数据读入响应
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            log.error("读取头像失败: " + e.getMessage());
        }
    }

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.getUser(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = (int) redisService.countUserLiked(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        long followCount = redisService.countUserFollow(userId, EntityTypeEnum.USER.getType());
        model.addAttribute("followeeCount", followCount);
        // 粉丝数量
        long fanCount = redisService.countUserFan(EntityTypeEnum.USER.getType(), userId);
        model.addAttribute("followerCount", fanCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = redisService.isFollowed(hostHolder.getUser().getId(), EntityTypeEnum.USER.getType(), userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    @RequestMapping(path = "/myreply/{userId}",method = RequestMethod.GET)
    @LoginRequired
    public String getMyReplyPage(@PathVariable("userId") Integer userId,Page pageInfo, Model model){
        int replyInfoCount = commentService.findReplyInfoCount(userId);
        model.addAttribute("replyInfoCount",replyInfoCount);

        // 设置分页信息
        pageInfo.setPath("/user/myreply");
        pageInfo.setLimit(5);
        pageInfo.setRows(replyInfoCount);

        // 查询帖子回复列表
        List<ReplyInfo> replyInfoList = commentService.findReplyInfoList(userId, pageInfo.getOffset(), pageInfo.getLimit());
        model.addAttribute("replyInfoList",replyInfoList);
        // 用户
        model.addAttribute("user", userService.getUser(userId));

        return "/site/my-reply";
    }

    @RequestMapping(path = "/mypost/{userId}",method = RequestMethod.GET)
    @LoginRequired
    public String getMyPostPage(@PathVariable("userId") Integer userId,Model model, Page pageInfo){
        int discussPostCount = discussPostService.findDiscussPostCount(userId);
        model.addAttribute("discussPostCount",discussPostCount);

        // 设置分页信息
        pageInfo.setPath("/user/mypost");
        pageInfo.setLimit(5);
        pageInfo.setRows(discussPostCount);

        // 查询帖子列表
        List<DiscussPost> discussPosts = discussPostService.findDiscussPostList(userId, pageInfo.getOffset(), pageInfo.getLimit(),0);
        model.addAttribute("discussPosts",discussPosts);

        // 用户
        model.addAttribute("user", userService.getUser(userId));
        return "/site/my-post";
    }
}
