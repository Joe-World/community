package org.wgd.community.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;
import org.wgd.community.common.GraceJSONResult;
import org.wgd.community.common.ResponseStatusEnum;
import org.wgd.community.common.base.BaseInfoProperties;
import org.wgd.community.model.pojo.Message;
import org.wgd.community.model.pojo.Page;
import org.wgd.community.model.pojo.User;
import org.wgd.community.service.MessageService;
import org.wgd.community.service.UserService;
import org.wgd.community.util.HostHolder;
import org.wgd.community.util.JacksonUtil;

import java.util.*;

@Controller
public class MessageController extends BaseInfoProperties {
    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    // 私信列表
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getConversationList(Model model, Page page) {
        User user = hostHolder.getUser();

        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.countConversations(user.getId()));

        List<Message> messageList = messageService.pageConversationsByUserId(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> messsages = new ArrayList<>();
        if (messageList != null) {
            for (Message message : messageList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount", messageService.countLettersByConversationId(message.getConversationId()));
                map.put("unreadCount", messageService.countUnreadInfo(user.getId(), message.getConversationId()));
                User targetUser = message.getFromId().equals(user.getId()) ? userService.getUser(message.getToId()) : userService.getUser(message.getFromId());

                map.put("target", targetUser);

                messsages.add(map);
            }
        }

        model.addAttribute("conversations", messsages);
        model.addAttribute("letterUnreadCount", messageService.countUnreadInfo(user.getId(), null));
        // 系统消息总共未读数量
        int noticeUnreadCount = messageService.countUnReadSystemInfo(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }


    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.countLettersByConversationId(conversationId));

        List<Message> messageList = messageService.pageLettersByConversationId(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (messageList != null) {
            for (Message letter : messageList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", letter);
                map.put("fromUser", userService.getUser(letter.getFromId()));
                letters.add(map);
            }
        }

        model.addAttribute("letters", letters);
        model.addAttribute("target", getLetterTarget(conversationId));

        // 设置已读
        List<Integer> ids = getLetterIds(messageList);
        if (!ids.isEmpty()) {
            messageService.updateMessagesByIds(ids);
        }

        return "/site/letter-detail";
    }

    private User getLetterTarget(String conversationId) {
        String[] s = conversationId.split("_");

        String targetId = hostHolder.getUser().getId().equals(s[0]) ? s[1] : s[0];

        return userService.getUser(Integer.valueOf(targetId));
    }

    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId().equals(message.getToId()) && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }


    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        User target = userService.getUserByName(toName);
        if (target == null) {
            return GraceJSONResult.errorCustomJson(ResponseStatusEnum.USER_NOT_EXIST_ERROR);
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.add(message);

        return GraceJSONResult.okJson();
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();

        // 拿到评论主题对应最新消息
        Message message = messageService.getLatestMessage(user.getId(), TOPIC_COMMENT);
        Map<String, Object> messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            // 反注意html
            String content = HtmlUtils.htmlUnescape(message.getContent());
            // json - > obj
            Map<String, Object> data = JacksonUtil.getJsonToBean(content, HashMap.class);

            // 消息触发者
            messageVO.put("user", userService.getUser((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.countMessage(user.getId(), TOPIC_COMMENT);
            messageVO.put("count", count);

            int unread = messageService.countUnReadSystemInfo(user.getId(), TOPIC_COMMENT);
            messageVO.put("unread", unread);

            model.addAttribute("commentNotice", messageVO);
        }

        // 查询点赞类通知
        message = messageService.getLatestMessage(user.getId(), TOPIC_LIKE);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JacksonUtil.getJsonToBean(content, HashMap.class);

            messageVO.put("user", userService.getUser((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.countMessage(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);

            int unread = messageService.countUnReadSystemInfo(user.getId(), TOPIC_LIKE);
            messageVO.put("unread", unread);

            model.addAttribute("likeNotice", messageVO);
        }

        // 查询关注类通知
        message = messageService.getLatestMessage(user.getId(), TOPIC_FOLLOW);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JacksonUtil.getJsonToBean(content, HashMap.class);

            messageVO.put("user", userService.getUser((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));

            int count = messageService.countMessage(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unread = messageService.countUnReadSystemInfo(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread", unread);

            model.addAttribute("followNotice", messageVO);
        }

        // 查询未读消息数量
        int letterUnreadCount = messageService.countUnreadInfo(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.countUnReadSystemInfo(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        User user = hostHolder.getUser();

        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.countMessage(user.getId(), topic));

        List<Message> noticeList = messageService.listMessageByTopic(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JacksonUtil.getJsonToBean(content, HashMap.class);
                map.put("user", userService.getUser((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 通知作者
                map.put("fromUser", userService.getUser(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.updateMessagesByIds(ids);
        }

        return "/site/notice-detail";
    }

}
