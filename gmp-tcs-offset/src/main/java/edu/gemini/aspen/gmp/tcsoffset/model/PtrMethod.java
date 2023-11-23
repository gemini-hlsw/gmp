package edu.gemini.aspen.gmp.tcsoffset.model;

import java.util.List;

/*
 *  Interface used to implement the equivalent
 *  of the c++ function pointer. 
 */

public interface PtrMethod<T> {
    void func(List<T> values);
}
