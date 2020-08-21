package com.bruis.learnsb.condi;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author LuoHaiYang
 */
@Component
// 当存在com.bruis.condition这个配置时才注入ConditionTest这个bean
@ConditionalOnProperty("com.bruis.condition")
public class ConditionTest {
}
