package org.hyperic.hq.hqapi1.test;

/**
 * A condition to be evaluated by an {@link SpinBarrier}
 * 
 * @author Jennifer Hickey
 * 
 */
public interface SpinBarrierCondition {
    /**
     * Evaluate the condition
     * 
     * @return true if condition evaluates to true, else false
     */
    boolean evaluate();
}
