package com.bruis.learnsb.aware;

import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
public class Flag {

    private boolean canOperate = true;

    public boolean isCanOperate() {
        return canOperate;
    }

    public void setCanOperate(boolean canOperate) {
        this.canOperate = canOperate;
    }
}
