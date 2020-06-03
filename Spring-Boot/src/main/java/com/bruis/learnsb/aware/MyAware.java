package com.bruis.learnsb.aware;

import org.springframework.beans.factory.Aware;

/**
 * @author LuoHaiYang
 */
public interface MyAware extends Aware {

    void setFlag(Flag flag);

}
