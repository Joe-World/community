package org.wgd.community.common.exceptions;


import org.wgd.community.common.ResponseStatusEnum;

/**
 * 优雅的处理异常，统一封装
 */
public class GraceException {
    public static void display(ResponseStatusEnum responseStatusEnum) {
        throw new CustomException(responseStatusEnum);
    }

}
