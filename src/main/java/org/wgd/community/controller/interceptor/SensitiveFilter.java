package org.wgd.community.controller.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SensitiveFilter {
    // 替换符
    private static final String REPLACE = "***";

    // 根节点
    private Node rootNode = new Node();


    @PostConstruct
    public void init() {
        try (
                // 获取敏感词字节输入流
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                // 读取字符，所以用缓存字符输入流要效率高
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                this.addWord(line);
            }

        } catch (IOException e) {
            log.error("加载敏感词文件失败：", e.getMessage());
        }
    }

    private void addWord(String word) {
        // 获取根节点
        Node curNode = rootNode;
        // 一个个遍历这个敏感词的字
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            // 是否有这个字
            Node subNode = curNode.getSubMap(c);
            if (subNode == null) {
                subNode = new Node();
                curNode.addSubMap(c, subNode);
            }

            curNode = subNode;
            // 库库循环

            // 打上结束标记
            if (i == word.length() - 1) {
                curNode.setEnd(true);
            }
        }
    }

    public String filter(String text) {
        // 非空判断
        if (StringUtils.isBlank(text)) {
            return null;
        }

        // 指针1-node
        Node curNode = rootNode;
        // 指针2-前
        int begin = 0;
        // 指针3-后
        int end = 0;
        // 返回结果
        StringBuilder sb = new StringBuilder();

        // 当end指针移动到末尾，停止循环
        while (end < text.length()) {
            // 1、拿到字
            char c = text.charAt(end);

            // 2、判断是否为特殊字符
            if (isSymbol(c)) {
                // 在当前节点在初始位置，需要移动指针begin指针
                if (curNode == rootNode) {
                    // 特殊字符，允许通过
                    sb.append(c);
                    ++begin;
                }
                // 无论特殊符号夹在敏感内还是外，都移动
                ++end;
                continue;
            }

            // 3、根据字搜索到的节点返回值，做响应的处理
            curNode = curNode.getSubMap(c);
            if (curNode == null) {
                // 若无响应子节点，则直接处理：通过字符；左指针后移一位，右指针跟着来
                sb.append(text.charAt(begin));
                begin = ++end;
                // 归位
                curNode = rootNode;
            } else if (curNode.isEnd) {
                // 已经到末尾了，找到敏感词，*处理；左指针后移一位，右指针跟着来
                sb.append(REPLACE);
                begin = ++end;
                curNode = rootNode;
            } else {
                // 说明疑似敏感词，需要再次确认，只需要右指针后移一位
                ++end;
            }
        }

        // 最后可能剩下个疑似敏感词，但确实不是敏感词，需要将其返回
        sb.append(text.substring(begin));

        return sb.toString();
    }

    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        // 前一个判断是否是正常字符，后一个再次过滤非东亚文字
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    private class Node {
        // 是否末尾标记
        private boolean isEnd = false;

        // 当前值+下一个子节点
        // 一个节点储存一个map：map里装有n个子节点（key，value），可以通过get(key)找到你想要的node
        private Map<Character, Node> subNode = new HashMap<>();

        public boolean isEnd() {
            return isEnd;
        }

        public void setEnd(boolean end) {
            isEnd = end;
        }

        // 添加子节点
        public void addSubMap(Character c, Node node) {
            subNode.put(c, node);
        }

        // 获取子节点()
        public Node getSubMap(Character c) {
            return subNode.get(c);
        }
    }
}




