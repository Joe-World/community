package org.wgd.community.service;

import java.util.Date;

public interface DataService {
    /**
     * 保存独立访客-ip入redis
     *
     * @param ip
     */
    public void saveUV(String ip);

    public long getUV(Date start, Date end);

    public void saveDAU(int userId);

    public long getDAU(Date start, Date end);
}

