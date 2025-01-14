/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

import java.math.BigInteger;

public class GeneralDivModNode extends RubyNode {

    @Child private FixnumOrBignumNode fixnumOrBignumQuotient;
    @Child private FixnumOrBignumNode fixnumOrBignumRemainder;

    private final BranchProfile bZeroProfile = BranchProfile.create();
    private final BranchProfile bMinusOneProfile = BranchProfile.create();
    private final BranchProfile nanProfile = BranchProfile.create();
    private final BranchProfile bigIntegerFixnumProfile = BranchProfile.create();
    private final BranchProfile useFixnumPairProfile = BranchProfile.create();
    private final BranchProfile useObjectPairProfile = BranchProfile.create();

    public GeneralDivModNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        fixnumOrBignumQuotient = new FixnumOrBignumNode(context, sourceSection);
        fixnumOrBignumRemainder = new FixnumOrBignumNode(context, sourceSection);
    }

    public DynamicObject execute(long a, long b) {
        return divMod(a, b);
    }

    public DynamicObject execute(long a, BigInteger b) {
        return divMod(BigInteger.valueOf(a), b);
    }

    public DynamicObject execute(long a, double b) {
        return divMod(a, b);
    }

    public DynamicObject execute(BigInteger a, long b) {
        return divMod(a, BigInteger.valueOf(b));
    }

    public DynamicObject execute(BigInteger a, BigInteger b) {
        return divMod(a, b);
    }

    public DynamicObject execute(double a, long b) {
        return divMod(a, b);
    }

    public DynamicObject execute(double a, BigInteger b) {
        return divMod(a, b.doubleValue());
    }

    public DynamicObject execute(double a, double b) {
        return divMod(a, b);
    }

    /*
     * div-mod algorithms copied from org.jruby.RubyFixnum, org.jruby.RubyBignum and org.jrubyRubyFloat. See license
     * and contributors there.
     */

    @TruffleBoundary
    private DynamicObject divMod(long a, long b) {
        if (b == 0) {
            bZeroProfile.enter();
            throw new ArithmeticException("divide by zero");
        }

        long mod;
        Object integerDiv;

        if (b == -1) {
            bMinusOneProfile.enter();

            if (a == Long.MIN_VALUE) {
                integerDiv = BigInteger.valueOf(a).negate();
            } else {
                integerDiv = -a;
            }
            mod = 0;
        } else {
            long div = a / b;
            mod = a - b * div;
            if (mod < 0 && b > 0 || mod > 0 && b < 0) {
                div -= 1;
                mod += b;
            }
            integerDiv = div;
        }

        if (integerDiv instanceof Long && ((long) integerDiv) >= Integer.MIN_VALUE && ((long) integerDiv) <= Integer.MAX_VALUE && mod >= Integer.MIN_VALUE && mod <= Integer.MAX_VALUE) {
            useFixnumPairProfile.enter();
            return createArray(new int[]{(int) (long) integerDiv, (int) mod}, 2);
        } else if (integerDiv instanceof Long) {
            useObjectPairProfile.enter();
            return createArray(new Object[]{integerDiv, mod}, 2);
        } else {
            useObjectPairProfile.enter();
            return createArray(new Object[]{
                    fixnumOrBignumQuotient.fixnumOrBignum((BigInteger) integerDiv),
                    mod}, 2);
        }
    }

    @TruffleBoundary
    private DynamicObject divMod(double a, double b) {
        if (b == 0) {
            bZeroProfile.enter();
            throw new ArithmeticException("divide by zero");
        }

        double mod = Math.IEEEremainder(a, b);

        if (Double.isNaN(mod)) {
            nanProfile.enter();
            throw new RaiseException(getContext().getCoreLibrary().floatDomainError("NaN", this));
        }

        final double div = Math.floor(a / b);

        if (b * mod < 0) {
            mod += b;
        }

        return createArray(new Object[]{
                fixnumOrBignumQuotient.fixnumOrBignum(div),
                mod}, 2);
    }

    @TruffleBoundary
    private DynamicObject divMod(BigInteger a, BigInteger b) {
        if (b.signum() == 0) {
            bZeroProfile.enter();
            throw new ArithmeticException("divide by zero");
        }

        final BigInteger[] bigIntegerResults = a.divideAndRemainder(b);

        if ((a.signum() * b.signum()) == -1 && bigIntegerResults[1].signum() != 0) {
            bigIntegerFixnumProfile.enter();
            bigIntegerResults[0] = bigIntegerResults[0].subtract(BigInteger.ONE);
            bigIntegerResults[1] = b.add(bigIntegerResults[1]);
        }

        return createArray(new Object[]{
                fixnumOrBignumQuotient.fixnumOrBignum(bigIntegerResults[0]),
                fixnumOrBignumRemainder.fixnumOrBignum(bigIntegerResults[1])}, 2);
    }

    public DynamicObject create(BigInteger value) {
        return BignumNodes.createRubyBignum(getContext().getCoreLibrary().getBignumClass(), value);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

}
