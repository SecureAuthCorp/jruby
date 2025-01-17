/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jcodings.Encoding;
import org.jcodings.EncodingDB;
import org.jcodings.transcode.EConvFlags;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Platform.OS_TYPE;
import org.jruby.runtime.Constants;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.core.array.ArrayNodesFactory;
import org.jruby.truffle.nodes.core.fixnum.FixnumNodesFactory;
import org.jruby.truffle.nodes.core.hash.HashNodesFactory;
import org.jruby.truffle.nodes.ext.*;
import org.jruby.truffle.nodes.objects.FreezeNode;
import org.jruby.truffle.nodes.objects.FreezeNodeGen;
import org.jruby.truffle.nodes.objects.SingletonClassNode;
import org.jruby.truffle.nodes.objects.SingletonClassNodeGen;
import org.jruby.truffle.nodes.rubinius.*;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.TruffleFatalException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.layouts.ThreadBacktraceLocationLayoutImpl;
import org.jruby.truffle.runtime.layouts.ThreadLayoutImpl;
import org.jruby.truffle.runtime.layouts.TimeLayoutImpl;
import org.jruby.truffle.runtime.layouts.ext.DigestLayoutImpl;
import org.jruby.truffle.runtime.layouts.rubinius.ByteArrayLayoutImpl;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.rubinius.RubiniusTypes;
import org.jruby.truffle.runtime.signal.SignalOperations;
import org.jruby.truffle.translator.NodeWrapper;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CoreLibrary {

    private static final String CLI_RECORD_SEPARATOR = Options.CLI_RECORD_SEPARATOR.load();

    private final RubyContext context;

    private final DynamicObject argumentErrorClass;
    private final DynamicObject arrayClass;
    private final DynamicObjectFactory arrayFactory;
    private final DynamicObject basicObjectClass;
    private final DynamicObject bignumClass;
    private final DynamicObjectFactory bignumFactory;
    private final DynamicObject bindingClass;
    private final DynamicObject classClass;
    private final DynamicObject complexClass;
    private final DynamicObject dirClass;
    private final DynamicObject encodingClass;
    private final DynamicObject encodingConverterClass;
    private final DynamicObject encodingErrorClass;
    private final DynamicObject exceptionClass;
    private final DynamicObject falseClass;
    private final DynamicObject fiberClass;
    private final DynamicObject fixnumClass;
    private final DynamicObject floatClass;
    private final DynamicObject floatDomainErrorClass;
    private final DynamicObject hashClass;
    private final DynamicObject integerClass;
    private final DynamicObject indexErrorClass;
    private final DynamicObject ioErrorClass;
    private final DynamicObject loadErrorClass;
    private final DynamicObject localJumpErrorClass;
    private final DynamicObject lookupTableClass;
    private final DynamicObject matchDataClass;
    private final DynamicObject moduleClass;
    private final DynamicObject nameErrorClass;
    private final DynamicObject nilClass;
    private final DynamicObject noMethodErrorClass;
    private final DynamicObject notImplementedErrorClass;
    private final DynamicObject numericClass;
    private final DynamicObject objectClass;
    private final DynamicObject procClass;
    private final DynamicObjectFactory procFactory;
    private final DynamicObject processModule;
    private final DynamicObject rangeClass;
    private final DynamicObject rangeErrorClass;
    private final DynamicObject rationalClass;
    private final DynamicObject regexpClass;
    private final DynamicObject regexpErrorClass;
    private final DynamicObject rubyTruffleErrorClass;
    private final DynamicObject runtimeErrorClass;
    private final DynamicObject securityErrorClass;
    private final DynamicObject standardErrorClass;
    private final DynamicObject stringClass;
    private final DynamicObjectFactory stringFactory;
    private final DynamicObject stringDataClass;
    private final DynamicObject symbolClass;
    private final DynamicObject syntaxErrorClass;
    private final DynamicObject systemCallErrorClass;
    private final DynamicObject threadClass;
    private final DynamicObject threadBacktraceClass;
    private final DynamicObject threadBacktraceLocationClass;
    private final DynamicObject timeClass;
    private final DynamicObject transcodingClass;
    private final DynamicObject trueClass;
    private final DynamicObject tupleClass;
    private final DynamicObject typeErrorClass;
    private final DynamicObject zeroDivisionErrorClass;
    private final DynamicObject enumerableModule;
    private final DynamicObject errnoModule;
    private final DynamicObject kernelModule;
    private final DynamicObject rubiniusModule;
    private final DynamicObject rubiniusChannelClass;
    private final DynamicObject rubiniusFFIModule;
    private final DynamicObject rubiniusFFIPointerClass;
    private final DynamicObject rubiniusMirrorClass;
    private final DynamicObject signalModule;
    private final DynamicObject truffleModule;
    private final DynamicObject bigDecimalClass;
    private final DynamicObject encodingCompatibilityErrorClass;
    private final DynamicObject methodClass;
    private final DynamicObject unboundMethodClass;
    private final DynamicObject byteArrayClass;
    private final DynamicObject fiberErrorClass;
    private final DynamicObject threadErrorClass;
    private final DynamicObject internalBufferClass;

    private final DynamicObject argv;
    private final DynamicObject globalVariablesObject;
    private final DynamicObject mainObject;
    private final DynamicObject nilObject;
    private final DynamicObject rubiniusUndefined;
    private final DynamicObject digestClass;

    private final ArrayNodes.MinBlock arrayMinBlock;
    private final ArrayNodes.MaxBlock arrayMaxBlock;

    private final DynamicObject rubyInternalMethod;
    private final Map<Errno, DynamicObject> errnoClasses = new HashMap<>();

    @CompilationFinal private InternalMethod basicObjectSendMethod;

    private enum State {
        INITIALIZING,
        LOADING_RUBY_CORE,
        LOADED
    }

    private State state = State.INITIALIZING;

    private final DynamicObjectFactory integerFixnumRangeFactory;
    private final DynamicObjectFactory longFixnumRangeFactory;

    private static class CoreLibraryNode extends RubyNode {

        @Child SingletonClassNode singletonClassNode;
        @Child FreezeNode freezeNode;

        public CoreLibraryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, null);
            this.freezeNode = FreezeNodeGen.create(context, sourceSection, null);
            adoptChildren();
        }

        public SingletonClassNode getSingletonClassNode() {
            return singletonClassNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return nil();
        }

    }

    private final CoreLibraryNode node;

    public CoreLibrary(RubyContext context) {
        this.context = context;
        this.node = new CoreLibraryNode(context, new CoreSourceSection("CoreLibrary", "initialize"));

        // Nothing in this constructor can use RubyContext.getCoreLibrary() as we are building it!
        // Therefore, only initialize the core classes and modules here.

        // Create the cyclic classes and modules

        classClass = ClassNodes.createClassClass(context);

        basicObjectClass = ClassNodes.createBootClass(classClass, null, "BasicObject");
        Layouts.CLASS.setInstanceFactoryUnsafe(basicObjectClass, Layouts.BASIC_OBJECT.createBasicObjectShape(basicObjectClass, basicObjectClass));

        objectClass = ClassNodes.createBootClass(classClass, basicObjectClass, "Object");
        Layouts.CLASS.setInstanceFactoryUnsafe(objectClass, Layouts.BASIC_OBJECT.createBasicObjectShape(objectClass, objectClass));

        moduleClass = ClassNodes.createBootClass(classClass, objectClass, "Module");
        Layouts.CLASS.setInstanceFactoryUnsafe(moduleClass, Layouts.MODULE.createModuleShape(moduleClass, moduleClass));

        // Close the cycles
        Layouts.MODULE.getFields(classClass).parentModule = Layouts.MODULE.getFields(moduleClass).start;
        Layouts.MODULE.getFields(moduleClass).addDependent(Layouts.MODULE.getFields(classClass).rubyModuleObject);
        Layouts.MODULE.getFields(classClass).newVersion();

        Layouts.MODULE.getFields(classClass).getAdoptedByLexicalParent(objectClass, "Class", node);
        Layouts.MODULE.getFields(basicObjectClass).getAdoptedByLexicalParent(objectClass, "BasicObject", node);
        Layouts.MODULE.getFields(objectClass).getAdoptedByLexicalParent(objectClass, "Object", node);
        Layouts.MODULE.getFields(moduleClass).getAdoptedByLexicalParent(objectClass, "Module", node);

        // Create Exception classes

        // Exception
        exceptionClass = defineClass("Exception");
        Layouts.CLASS.setInstanceFactoryUnsafe(exceptionClass, Layouts.EXCEPTION.createExceptionShape(exceptionClass, exceptionClass));

        // NoMemoryError
        defineClass(exceptionClass, "NoMemoryError");

        // RubyTruffleError
        rubyTruffleErrorClass = defineClass(exceptionClass, "RubyTruffleError");

        // StandardError
        standardErrorClass = defineClass(exceptionClass, "StandardError");
        argumentErrorClass = defineClass(standardErrorClass, "ArgumentError");
        encodingErrorClass = defineClass(standardErrorClass, "EncodingError");
        fiberErrorClass = defineClass(standardErrorClass, "FiberError");
        ioErrorClass = defineClass(standardErrorClass, "IOError");
        localJumpErrorClass = defineClass(standardErrorClass, "LocalJumpError");
        regexpErrorClass = defineClass(standardErrorClass, "RegexpError");
        runtimeErrorClass = defineClass(standardErrorClass, "RuntimeError");
        threadErrorClass = defineClass(standardErrorClass, "ThreadError");
        typeErrorClass = defineClass(standardErrorClass, "TypeError");
        zeroDivisionErrorClass = defineClass(standardErrorClass, "ZeroDivisionError");

        // StandardError > RangeError
        rangeErrorClass = defineClass(standardErrorClass, "RangeError");
        floatDomainErrorClass = defineClass(rangeErrorClass, "FloatDomainError");

        // StandardError > IndexError
        indexErrorClass = defineClass(standardErrorClass, "IndexError");
        defineClass(indexErrorClass, "KeyError");

        // StandardError > IOError
        defineClass(ioErrorClass, "EOFError");

        // StandardError > NameError
        nameErrorClass = defineClass(standardErrorClass, "NameError");
        noMethodErrorClass = defineClass(nameErrorClass, "NoMethodError");

        // StandardError > SystemCallError
        systemCallErrorClass = defineClass(standardErrorClass, "SystemCallError");

        errnoModule = defineModule("Errno");

        for (Errno errno : Errno.values()) {
            if (errno.name().startsWith("E")) {
                errnoClasses.put(errno, defineClass(errnoModule, systemCallErrorClass, errno.name()));
            }
        }

        // ScriptError
        DynamicObject scriptErrorClass = defineClass(exceptionClass, "ScriptError");
        loadErrorClass = defineClass(scriptErrorClass, "LoadError");
        notImplementedErrorClass = defineClass(scriptErrorClass, "NotImplementedError");
        syntaxErrorClass = defineClass(scriptErrorClass, "SyntaxError");

        // SecurityError
        securityErrorClass = defineClass(exceptionClass, "SecurityError");

        // SignalException
        DynamicObject signalExceptionClass = defineClass(exceptionClass, "SignalException");
        defineClass(signalExceptionClass, "Interrupt");

        // SystemExit
        defineClass(exceptionClass, "SystemExit");

        // SystemStackError
        defineClass(exceptionClass, "SystemStackError");

        // Create core classes and modules

        numericClass = defineClass("Numeric");
        complexClass = defineClass(numericClass, "Complex");
        floatClass = defineClass(numericClass, "Float");
        integerClass = defineClass(numericClass, "Integer");
        fixnumClass = defineClass(integerClass, "Fixnum");
        bignumClass = defineClass(integerClass, "Bignum");
        bignumFactory = Layouts.BIGNUM.createBignumShape(bignumClass, bignumClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(bignumClass, bignumFactory);
        rationalClass = defineClass(numericClass, "Rational");

        // Classes defined in Object

        arrayClass = defineClass("Array");
        arrayFactory = Layouts.ARRAY.createArrayShape(arrayClass, arrayClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(arrayClass, arrayFactory);
        bindingClass = defineClass("Binding");
        Layouts.CLASS.setInstanceFactoryUnsafe(bindingClass, Layouts.BINDING.createBindingShape(bindingClass, bindingClass));
        dirClass = defineClass("Dir");
        Layouts.CLASS.setInstanceFactoryUnsafe(dirClass, Layouts.DIR.createDirShape(dirClass, dirClass));
        encodingClass = defineClass("Encoding");
        Layouts.CLASS.setInstanceFactoryUnsafe(encodingClass, Layouts.ENCODING.createEncodingShape(encodingClass, encodingClass));
        falseClass = defineClass("FalseClass");
        fiberClass = defineClass("Fiber");
        Layouts.CLASS.setInstanceFactoryUnsafe(fiberClass, Layouts.FIBER.createFiberShape(fiberClass, fiberClass));
        defineModule("FileTest");
        hashClass = defineClass("Hash");
        Layouts.CLASS.setInstanceFactoryUnsafe(hashClass, Layouts.HASH.createHashShape(hashClass, hashClass));
        matchDataClass = defineClass("MatchData");
        Layouts.CLASS.setInstanceFactoryUnsafe(matchDataClass, Layouts.MATCH_DATA.createMatchDataShape(matchDataClass, matchDataClass));
        methodClass = defineClass("Method");
        Layouts.CLASS.setInstanceFactoryUnsafe(methodClass, Layouts.METHOD.createMethodShape(methodClass, methodClass));
        final DynamicObject mutexClass = defineClass("Mutex");
        Layouts.CLASS.setInstanceFactoryUnsafe(mutexClass, Layouts.MUTEX.createMutexShape(mutexClass, mutexClass));
        nilClass = defineClass("NilClass");
        procClass = defineClass("Proc");
        procFactory = Layouts.PROC.createProcShape(procClass, procClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(procClass, procFactory);
        processModule = defineModule("Process");
        DynamicObject queueClass = defineClass("Queue");
        Layouts.CLASS.setInstanceFactoryUnsafe(queueClass, Layouts.QUEUE.createQueueShape(queueClass, queueClass));
        DynamicObject sizedQueueClass = defineClass(queueClass, "SizedQueue");
        Layouts.CLASS.setInstanceFactoryUnsafe(sizedQueueClass, Layouts.SIZED_QUEUE.createSizedQueueShape(sizedQueueClass, sizedQueueClass));
        rangeClass = defineClass("Range");
        Layouts.CLASS.setInstanceFactoryUnsafe(rangeClass, Layouts.OBJECT_RANGE.createObjectRangeShape(rangeClass, rangeClass));
        integerFixnumRangeFactory = Layouts.INTEGER_FIXNUM_RANGE.createIntegerFixnumRangeShape(rangeClass, rangeClass);
        longFixnumRangeFactory = Layouts.LONG_FIXNUM_RANGE.createLongFixnumRangeShape(rangeClass, rangeClass);
        regexpClass = defineClass("Regexp");
        Layouts.CLASS.setInstanceFactoryUnsafe(regexpClass, Layouts.REGEXP.createRegexpShape(regexpClass, regexpClass));
        stringClass = defineClass("String");
        stringFactory = Layouts.STRING.createStringShape(stringClass, stringClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(stringClass, stringFactory);
        symbolClass = defineClass("Symbol");
        Layouts.CLASS.setInstanceFactoryUnsafe(symbolClass, Layouts.SYMBOL.createSymbolShape(symbolClass, symbolClass));
        threadClass = defineClass("Thread");
        Layouts.CLASS.setInstanceFactoryUnsafe(threadClass, ThreadLayoutImpl.INSTANCE.createThreadShape(threadClass, threadClass));
        threadBacktraceClass = defineClass(threadClass, objectClass, "Backtrace");
        threadBacktraceLocationClass = defineClass(threadBacktraceClass, objectClass, "Location");
        Layouts.CLASS.setInstanceFactoryUnsafe(threadBacktraceLocationClass, ThreadBacktraceLocationLayoutImpl.INSTANCE.createThreadBacktraceLocationShape(threadBacktraceLocationClass, threadBacktraceLocationClass));
        timeClass = defineClass("Time");
        Layouts.CLASS.setInstanceFactoryUnsafe(timeClass, Layouts.TIME.createTimeShape(timeClass, timeClass));
        trueClass = defineClass("TrueClass");
        unboundMethodClass = defineClass("UnboundMethod");
        Layouts.CLASS.setInstanceFactoryUnsafe(unboundMethodClass, Layouts.UNBOUND_METHOD.createUnboundMethodShape(unboundMethodClass, unboundMethodClass));
        final DynamicObject ioClass = defineClass("IO");
        Layouts.CLASS.setInstanceFactoryUnsafe(ioClass, Layouts.IO.createIOShape(ioClass, ioClass));
        internalBufferClass = defineClass(ioClass, objectClass, "InternalBuffer");
        Layouts.CLASS.setInstanceFactoryUnsafe(internalBufferClass, Layouts.IO_BUFFER.createIOBufferShape(internalBufferClass, internalBufferClass));

        // Modules

        DynamicObject comparableModule = defineModule("Comparable");
        defineModule("Config");
        enumerableModule = defineModule("Enumerable");
        defineModule("GC");
        kernelModule = defineModule("Kernel");
        defineModule("Math");
        defineModule("ObjectSpace");
        signalModule = defineModule("Signal");

        // The rest

        encodingCompatibilityErrorClass = defineClass(encodingClass, encodingErrorClass, "CompatibilityError");

        encodingConverterClass = defineClass(encodingClass, objectClass, "Converter");
        Layouts.CLASS.setInstanceFactoryUnsafe(encodingConverterClass, Layouts.ENCODING_CONVERTER.createEncodingConverterShape(encodingConverterClass, encodingConverterClass));

        truffleModule = defineModule("Truffle");
        defineModule(truffleModule, "Interop");
        defineModule(truffleModule, "Debug");
        defineModule(truffleModule, "Primitive");
        defineModule(truffleModule, "Digest");
        defineModule(truffleModule, "Zlib");
        bigDecimalClass = defineClass(truffleModule, numericClass, "BigDecimal");
        Layouts.CLASS.setInstanceFactoryUnsafe(bigDecimalClass, Layouts.BIG_DECIMAL.createBigDecimalShape(bigDecimalClass, bigDecimalClass));

        // Rubinius

        rubiniusModule = defineModule("Rubinius");

        rubiniusFFIModule = defineModule(rubiniusModule, "FFI");
        defineModule(defineModule(rubiniusFFIModule, "Platform"), "POSIX");
        rubiniusFFIPointerClass = defineClass(rubiniusFFIModule, objectClass, "Pointer");
        Layouts.CLASS.setInstanceFactoryUnsafe(rubiniusFFIPointerClass, Layouts.POINTER.createPointerShape(rubiniusFFIPointerClass, rubiniusFFIPointerClass));

        rubiniusChannelClass = defineClass(rubiniusModule, objectClass, "Channel");
        rubiniusMirrorClass = defineClass(rubiniusModule, objectClass, "Mirror");
        defineModule(rubiniusModule, "Type");

        byteArrayClass = defineClass(rubiniusModule, objectClass, "ByteArray");
        Layouts.CLASS.setInstanceFactoryUnsafe(byteArrayClass, Layouts.BYTE_ARRAY.createByteArrayShape(byteArrayClass, byteArrayClass));
        lookupTableClass = defineClass(rubiniusModule, hashClass, "LookupTable");
        stringDataClass = defineClass(rubiniusModule, objectClass, "StringData");
        transcodingClass = defineClass(encodingClass, objectClass, "Transcoding");
        tupleClass = defineClass(rubiniusModule, arrayClass, "Tuple");

        // Interop

        rubyInternalMethod = null;

        // Include the core modules

        includeModules(comparableModule);

        // Create some key objects

        mainObject = Layouts.CLASS.getInstanceFactory(objectClass).newInstance();
        nilObject = Layouts.CLASS.getInstanceFactory(nilClass).newInstance();
        argv = ArrayNodes.createEmptyArray(arrayClass);
        rubiniusUndefined = Layouts.CLASS.getInstanceFactory(objectClass).newInstance();

        globalVariablesObject = Layouts.CLASS.getInstanceFactory(objectClass).newInstance();

        arrayMinBlock = new ArrayNodes.MinBlock(context);
        arrayMaxBlock = new ArrayNodes.MaxBlock(context);

        digestClass = defineClass(truffleModule, basicObjectClass, "Digest");
        Layouts.CLASS.setInstanceFactoryUnsafe(digestClass, DigestLayoutImpl.INSTANCE.createDigestShape(digestClass, digestClass));
    }

    private void includeModules(DynamicObject comparableModule) {
        assert RubyGuards.isRubyModule(comparableModule);

        Layouts.MODULE.getFields(objectClass).include(node, kernelModule);

        Layouts.MODULE.getFields(numericClass).include(node, comparableModule);
        Layouts.MODULE.getFields(symbolClass).include(node, comparableModule);

        Layouts.MODULE.getFields(arrayClass).include(node, enumerableModule);
        Layouts.MODULE.getFields(dirClass).include(node, enumerableModule);
        Layouts.MODULE.getFields(hashClass).include(node, enumerableModule);
        Layouts.MODULE.getFields(rangeClass).include(node, enumerableModule);
    }

    /**
     * Initializations which may access {@link RubyContext#getCoreLibrary()}.
     */
    public void initialize() {
        addCoreMethods();
        initializeGlobalVariables();
        initializeConstants();
        initializeEncodingConstants();
        initializeSignalConstants();
    }

    private void addCoreMethods() {
        // Bring in core method nodes
        CoreMethodNodeManager coreMethodNodeManager = new CoreMethodNodeManager(objectClass, node.getSingletonClassNode());

        coreMethodNodeManager.addCoreMethodNodes(ArrayNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BasicObjectNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BindingNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BignumNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ClassNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ExceptionNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FalseClassNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FiberNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FixnumNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(FloatNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(HashNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(IntegerNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(KernelNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MainNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MatchDataNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MathNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ModuleNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MutexNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ObjectSpaceNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ProcessNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ProcNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(QueueNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(RangeNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(RegexpNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(SizedQueueNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(StringNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(SymbolNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ThreadNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TrueClassNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TrufflePrimitiveNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(EncodingNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(EncodingConverterNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TruffleInteropNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(MethodNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(UnboundMethodNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ByteArrayNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(TimeNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(PosixNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(RubiniusTypeNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ThreadBacktraceLocationNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(DigestNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(BigDecimalNodesFactory.getFactories());
        coreMethodNodeManager.addCoreMethodNodes(ZlibNodesFactory.getFactories());
        coreMethodNodeManager.allMethodInstalled();

        basicObjectSendMethod = Layouts.MODULE.getFields(basicObjectClass).getMethods().get("__send__");
        assert basicObjectSendMethod != null;
    }

    private void initializeGlobalVariables() {
        DynamicObject globals = globalVariablesObject;

        BasicObjectNodes.setInstanceVariable(globals, "$LOAD_PATH", ArrayNodes.createEmptyArray(arrayClass));
        BasicObjectNodes.setInstanceVariable(globals, "$LOADED_FEATURES", ArrayNodes.createEmptyArray(arrayClass));
        BasicObjectNodes.setInstanceVariable(globals, "$:", BasicObjectNodes.getInstanceVariable(globals, "$LOAD_PATH"));
        BasicObjectNodes.setInstanceVariable(globals, "$\"", BasicObjectNodes.getInstanceVariable(globals, "$LOADED_FEATURES"));
        BasicObjectNodes.setInstanceVariable(globals, "$,", nilObject);
        BasicObjectNodes.setInstanceVariable(globals, "$0", context.toTruffle(context.getRuntime().getGlobalVariables().get("$0")));

        BasicObjectNodes.setInstanceVariable(globals, "$DEBUG", context.getRuntime().isDebug());

        Object value = context.getRuntime().warningsEnabled() ? context.getRuntime().isVerbose() : nilObject;
        BasicObjectNodes.setInstanceVariable(globals, "$VERBOSE", value);

        final DynamicObject defaultRecordSeparator = StringNodes.createString(stringClass, CLI_RECORD_SEPARATOR);
        node.freezeNode.executeFreeze(defaultRecordSeparator);

        // TODO (nirvdrum 05-Feb-15) We need to support the $-0 alias as well.
        BasicObjectNodes.setInstanceVariable(globals, "$/", defaultRecordSeparator);

        BasicObjectNodes.setInstanceVariable(globals, "$SAFE", 0);
    }

    private void initializeConstants() {
        // Set constants

        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_VERSION", StringNodes.createString(stringClass, Constants.RUBY_VERSION));
        Layouts.MODULE.getFields(objectClass).setConstant(node, "JRUBY_VERSION", StringNodes.createString(stringClass, Constants.VERSION));
        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_PATCHLEVEL", 0);
        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_REVISION", Constants.RUBY_REVISION);
        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_ENGINE", StringNodes.createString(stringClass, Constants.ENGINE + "+truffle"));
        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_PLATFORM", StringNodes.createString(stringClass, Constants.PLATFORM));
        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_RELEASE_DATE", StringNodes.createString(stringClass, Constants.COMPILE_DATE));
        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_DESCRIPTION", StringNodes.createString(stringClass, OutputStrings.getVersionString()));
        Layouts.MODULE.getFields(objectClass).setConstant(node, "RUBY_COPYRIGHT", StringNodes.createString(stringClass, OutputStrings.getCopyrightString()));

        // BasicObject knows itself
        Layouts.MODULE.getFields(basicObjectClass).setConstant(node, "BasicObject", basicObjectClass);

        Layouts.MODULE.getFields(objectClass).setConstant(node, "ARGV", argv);

        Layouts.MODULE.getFields(rubiniusModule).setConstant(node, "UNDEFINED", rubiniusUndefined);
        Layouts.MODULE.getFields(rubiniusModule).setConstant(node, "LIBC", Platform.LIBC);

        Layouts.MODULE.getFields(processModule).setConstant(node, "CLOCK_MONOTONIC", ProcessNodes.CLOCK_MONOTONIC);
        Layouts.MODULE.getFields(processModule).setConstant(node, "CLOCK_REALTIME", ProcessNodes.CLOCK_REALTIME);

        if (Platform.getPlatform().getOS() == OS_TYPE.LINUX) {
            Layouts.MODULE.getFields(processModule).setConstant(node, "CLOCK_THREAD_CPUTIME_ID", ProcessNodes.CLOCK_THREAD_CPUTIME_ID);
        }

        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "INVALID_MASK", EConvFlags.INVALID_MASK);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "INVALID_REPLACE", EConvFlags.INVALID_REPLACE);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "UNDEF_MASK", EConvFlags.UNDEF_MASK);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "UNDEF_REPLACE", EConvFlags.UNDEF_REPLACE);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "UNDEF_HEX_CHARREF", EConvFlags.UNDEF_HEX_CHARREF);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "PARTIAL_INPUT", EConvFlags.PARTIAL_INPUT);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "AFTER_OUTPUT", EConvFlags.AFTER_OUTPUT);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "UNIVERSAL_NEWLINE_DECORATOR", EConvFlags.UNIVERSAL_NEWLINE_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "CRLF_NEWLINE_DECORATOR", EConvFlags.CRLF_NEWLINE_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "CR_NEWLINE_DECORATOR", EConvFlags.CR_NEWLINE_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "XML_TEXT_DECORATOR", EConvFlags.XML_TEXT_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "XML_ATTR_CONTENT_DECORATOR", EConvFlags.XML_ATTR_CONTENT_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(node, "XML_ATTR_QUOTE_DECORATOR", EConvFlags.XML_ATTR_QUOTE_DECORATOR);
    }

    private void initializeSignalConstants() {
        Object[] signals = new Object[SignalOperations.SIGNALS_LIST.size()];

        int i = 0;
        for (Map.Entry<String, Integer> signal : SignalOperations.SIGNALS_LIST.entrySet()) {
            DynamicObject signalName = StringNodes.createString(context.getCoreLibrary().getStringClass(), signal.getKey());
            signals[i++] = ArrayNodes.fromObjects(arrayClass, signalName, signal.getValue());
        }

        Layouts.MODULE.getFields(signalModule).setConstant(node, "SIGNAL_LIST", ArrayNodes.createArray(arrayClass, signals, signals.length));
    }

    private DynamicObject defineClass(String name) {
        return defineClass(objectClass, name);
    }

    private DynamicObject defineClass(DynamicObject superclass, String name) {
        assert RubyGuards.isRubyClass(superclass);
        return ClassNodes.createRubyClass(context, objectClass, superclass, name);
    }

    private DynamicObject defineClass(DynamicObject lexicalParent, DynamicObject superclass, String name) {
        assert RubyGuards.isRubyModule(lexicalParent);
        assert RubyGuards.isRubyClass(superclass);
        return ClassNodes.createRubyClass(context, lexicalParent, superclass, name);
    }

    private DynamicObject defineModule(String name) {
        return defineModule(objectClass, name);
    }

    private DynamicObject defineModule(DynamicObject lexicalParent, String name) {
        assert RubyGuards.isRubyModule(lexicalParent);
        return ModuleNodes.createRubyModule(context, moduleClass, lexicalParent, name, node);
    }

    public void initializeAfterMethodsAdded() {
        initializeRubiniusFFI();

        // Load Ruby core

        try {
            state = State.LOADING_RUBY_CORE;
            loadRubyCore("core.rb");
        } catch (RaiseException e) {
            final Object rubyException = e.getRubyException();

            for (String line : Backtrace.DISPLAY_FORMATTER.format(getContext(), (DynamicObject) rubyException, Layouts.EXCEPTION.getBacktrace((DynamicObject) rubyException))) {
                System.err.println(line);
            }

            throw new TruffleFatalException("couldn't load the core library", e);
        } finally {
            state = State.LOADED;
        }
    }

    private void initializeRubiniusFFI() {
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_CHAR", RubiniusTypes.TYPE_CHAR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_UCHAR", RubiniusTypes.TYPE_UCHAR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_BOOL", RubiniusTypes.TYPE_BOOL);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_SHORT", RubiniusTypes.TYPE_SHORT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_USHORT", RubiniusTypes.TYPE_USHORT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_INT", RubiniusTypes.TYPE_INT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_UINT", RubiniusTypes.TYPE_UINT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_LONG", RubiniusTypes.TYPE_LONG);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_ULONG", RubiniusTypes.TYPE_ULONG);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_LL", RubiniusTypes.TYPE_LL);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_ULL", RubiniusTypes.TYPE_ULL);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_FLOAT", RubiniusTypes.TYPE_FLOAT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_DOUBLE", RubiniusTypes.TYPE_DOUBLE);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_PTR", RubiniusTypes.TYPE_PTR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_VOID", RubiniusTypes.TYPE_VOID);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_STRING", RubiniusTypes.TYPE_STRING);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_STRPTR", RubiniusTypes.TYPE_STRPTR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_CHARARR", RubiniusTypes.TYPE_CHARARR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_ENUM", RubiniusTypes.TYPE_ENUM);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(node, "TYPE_VARARGS", RubiniusTypes.TYPE_VARARGS);
    }

    public void loadRubyCore(String fileName) {
        loadRubyCore(fileName, "core:/");
    }

    public void loadRubyCore(String fileName, String prefix) {
        final Source source;

        try {
            // TODO CS 28-Feb-15 need to use SourceManager here so that the debugger knows about the core files
            source = Source.fromReader(new InputStreamReader(getRubyCoreInputStream(fileName), StandardCharsets.UTF_8), prefix + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.load(source, node, NodeWrapper.IDENTITY);
    }

    public InputStream getRubyCoreInputStream(String fileName) {
        final InputStream resource = getClass().getResourceAsStream("/" + fileName);

        if (resource == null) {
            throw new RuntimeException("couldn't load Truffle core library " + fileName);
        }

        return resource;
    }

    public void initializeEncodingConstants() {
        getContext().getRuntime().getEncodingService().defineEncodings(new EncodingService.EncodingDefinitionVisitor() {
            @Override
            public void defineEncoding(EncodingDB.Entry encodingEntry, byte[] name, int p, int end) {
                Encoding e = encodingEntry.getEncoding();

                DynamicObject re = EncodingNodes.newEncoding(encodingClass, e, name, p, end, encodingEntry.isDummy());
                EncodingNodes.storeEncoding(encodingEntry.getIndex(), re);
            }

            @Override
            public void defineConstant(int encodingListIndex, String constName) {
                Layouts.MODULE.getFields(encodingClass).setConstant(node, constName, EncodingNodes.getEncoding(encodingListIndex));
            }
        });

        getContext().getRuntime().getEncodingService().defineAliases(new EncodingService.EncodingAliasVisitor() {
            @Override
            public void defineAlias(int encodingListIndex, String constName) {
                DynamicObject re = EncodingNodes.getEncoding(encodingListIndex);
                EncodingNodes.storeAlias(constName, re);
            }

            @Override
            public void defineConstant(int encodingListIndex, String constName) {
                Layouts.MODULE.getFields(encodingClass).setConstant(node, constName, EncodingNodes.getEncoding(encodingListIndex));
            }
        });
    }

    public DynamicObject getMetaClass(Object object) {
        if (object instanceof DynamicObject) {
            return Layouts.BASIC_OBJECT.getMetaClass(((DynamicObject) object));
        } else if (object instanceof Boolean) {
            if ((boolean) object) {
                return trueClass;
            } else {
                return falseClass;
            }
        } else if (object instanceof Integer) {
            return fixnumClass;
        } else if (object instanceof Long) {
            return fixnumClass;
        } else if (object instanceof Double) {
            return floatClass;
        } else if (object == null) {
            throw new RuntimeException("Can't get metaclass for null");
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException(String.format("Don't know how to get the metaclass for %s", object.getClass()));
        }
    }

    public DynamicObject getLogicalClass(Object object) {
        if (object instanceof DynamicObject) {
            return BasicObjectNodes.getLogicalClass(((DynamicObject) object));
        } else if (object instanceof Boolean) {
            if ((boolean) object) {
                return trueClass;
            } else {
                return falseClass;
            }
        } else if (object instanceof Integer) {
            return fixnumClass;
        } else if (object instanceof Long) {
            return fixnumClass;
        } else if (object instanceof Double) {
            return floatClass;
        } else if (object == null) {
            throw new RuntimeException();
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException(String.format("Don't know how to get the logical class for %s", object.getClass()));
        }
    }

    /**
     * Convert a value to a {@code Float}, without doing any lookup.
     */
    public static double toDouble(Object value, DynamicObject nil) {
        assert value != null;

        if (value == nil) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (long) value;
        }

        if (RubyGuards.isRubyBignum(value)) {
            return Layouts.BIGNUM.getValue((DynamicObject) value).doubleValue();
        }

        if (value instanceof Double) {
            return (double) value;
        }

        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException();
    }

    public static boolean fitsIntoInteger(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    public DynamicObject runtimeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(runtimeErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject frozenError(String className, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return runtimeError(String.format("can't modify frozen %s", className), currentNode);
    }

    public DynamicObject argumentError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(argumentErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject argumentErrorOutOfRange(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError("out of range", currentNode);
    }

    public DynamicObject argumentErrorInvalidRadix(int radix, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("invalid radix %d", radix), currentNode);
    }

    public DynamicObject argumentErrorMissingKeyword(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("missing keyword: %s", name), currentNode);
    }

    public DynamicObject argumentError(int passed, int required, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("wrong number of arguments (%d for %d)", passed, required), currentNode);
    }

    public DynamicObject argumentError(int passed, int required, int optional, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError(String.format("wrong number of arguments (%d for %d..%d)", passed, required, required + optional), currentNode);
    }

    public DynamicObject argumentErrorEmptyVarargs(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return argumentError("wrong number of arguments (0 for 1+)", currentNode);
    }

    public DynamicObject argumentErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String badClassName = Layouts.MODULE.getFields(getLogicalClass(object)).getName();
        return argumentError(String.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
    }

    public DynamicObject errnoError(int errno, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();

        Errno errnoObj = Errno.valueOf(errno);
        if (errnoObj == null) {
            return systemCallError(String.format("Unknown Error (%s)", errno), currentNode);
        }

        return ExceptionNodes.createRubyException(getErrnoClass(errnoObj), StringNodes.createString(context.getCoreLibrary().getStringClass(), errnoObj.description()), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject indexError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(indexErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject indexTooSmallError(String type, int index, int length, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return indexError(String.format("index %d too small for %s; minimum: -%d", index, type, length), currentNode);
    }

    public DynamicObject indexNegativeLength(int length, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return indexError(String.format("negative length (%d)", length), currentNode);
    }

    public DynamicObject localJumpError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(localJumpErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject noBlockGiven(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("no block given", currentNode);
    }

    public DynamicObject unexpectedReturn(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("unexpected return", currentNode);
    }

    public DynamicObject noBlockToYieldTo(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return localJumpError("no block given (yield)", currentNode);
    }

    public DynamicObject typeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(typeErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject typeErrorCantDefineSingleton(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError("can't define singleton", currentNode);
    }

    public DynamicObject typeErrorNoClassToMakeAlias(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError("no class to make alias", currentNode);
    }

    public DynamicObject typeErrorShouldReturn(String object, String method, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s#%s should return %s", object, method, expectedType), currentNode);
    }

    public DynamicObject typeErrorCantConvertTo(Object from, DynamicObject to, String methodUsed, Object result, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyClass(to);
        String fromClass = Layouts.MODULE.getFields(getLogicalClass(from)).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                fromClass, Layouts.MODULE.getFields(to).getName(), fromClass, methodUsed, getLogicalClass(result).toString()), currentNode);
    }

    public DynamicObject typeErrorCantConvertInto(Object from, DynamicObject to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyClass(to);
        return typeError(String.format("can't convert %s into %s", Layouts.MODULE.getFields(getLogicalClass(from)).getName(), Layouts.MODULE.getFields(to).getName()), currentNode);
    }

    public DynamicObject typeErrorIsNotA(String value, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s is not a %s", value, expectedType), currentNode);
    }

    public DynamicObject typeErrorNoImplicitConversion(Object from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("no implicit conversion of %s into %s", Layouts.MODULE.getFields(getLogicalClass(from)).getName(), to), currentNode);
    }

    public DynamicObject typeErrorMustBe(String variable, String type, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("value of %s must be %s", variable, type), currentNode);
    }

    public DynamicObject typeErrorBadCoercion(Object from, String to, String coercionMethod, Object coercedTo, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String badClassName = Layouts.MODULE.getFields(getLogicalClass(from)).getName();
        return typeError(String.format("can't convert %s to %s (%s#%s gives %s)",
                badClassName,
                to,
                badClassName,
                coercionMethod,
                Layouts.MODULE.getFields(getLogicalClass(coercedTo)).getName()), currentNode);
    }

    public DynamicObject typeErrorCantCoerce(Object from, String to, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return typeError(String.format("%s can't be coerced into %s", from, to), currentNode);
    }

    public DynamicObject typeErrorCantDump(Object object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String logicalClass = Layouts.MODULE.getFields(getLogicalClass(object)).getName();
        return typeError(String.format("can't dump %s", logicalClass), currentNode);
    }

    public DynamicObject typeErrorWrongArgumentType(Object object, String expectedType, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String badClassName = Layouts.MODULE.getFields(getLogicalClass(object)).getName();
        return typeError(String.format("wrong argument type %s (expected %s)", badClassName, expectedType), currentNode);
    }

    public DynamicObject nameError(String message, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        DynamicObject nameError = ExceptionNodes.createRubyException(nameErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
        BasicObjectNodes.setInstanceVariable(nameError, "@name", context.getSymbolTable().getSymbol(name));
        return nameError;
    }

    public DynamicObject nameErrorConstantNotDefined(DynamicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("constant %s::%s not defined", Layouts.MODULE.getFields(module).getName(), name), name, currentNode);
    }

    public DynamicObject nameErrorUninitializedConstant(DynamicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        final String message;
        if (module == objectClass) {
            message = String.format("uninitialized constant %s", name);
        } else {
            message = String.format("uninitialized constant %s::%s", Layouts.MODULE.getFields(module).getName(), name);
        }
        return nameError(message, name, currentNode);
    }

    public DynamicObject nameErrorUninitializedClassVariable(DynamicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("uninitialized class variable %s in %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    public DynamicObject nameErrorPrivateConstant(DynamicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("private constant %s::%s referenced", Layouts.MODULE.getFields(module).getName(), name), name, currentNode);
    }

    public DynamicObject nameErrorInstanceNameNotAllowable(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("`%s' is not allowable as an instance variable name", name), name, currentNode);
    }

    public DynamicObject nameErrorReadOnly(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("%s is a read-only variable", name), name, currentNode);
    }

    public DynamicObject nameErrorUndefinedLocalVariableOrMethod(String name, String object, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("undefined local variable or method `%s' for %s", name, object), name, currentNode);
    }

    public DynamicObject nameErrorUndefinedMethod(String name, DynamicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("undefined method `%s' for %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    public DynamicObject nameErrorMethodNotDefinedIn(DynamicObject module, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("method `%s' not defined in %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    public DynamicObject nameErrorPrivateMethod(String name, DynamicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return nameError(String.format("method `%s' for %s is private", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    public DynamicObject nameErrorLocalVariableNotDefined(String name, DynamicObject binding, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyBinding(binding);
        return nameError(String.format("local variable `%s' not defined for %s", name, binding.toString()), name, currentNode);
    }

    public DynamicObject nameErrorClassVariableNotDefined(String name, DynamicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return nameError(String.format("class variable `%s' not defined for %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    public DynamicObject noMethodError(String message, String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        DynamicObject noMethodError = ExceptionNodes.createRubyException(context.getCoreLibrary().getNoMethodErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
        BasicObjectNodes.setInstanceVariable(noMethodError, "@name", context.getSymbolTable().getSymbol(name));
        return noMethodError;
    }

    public DynamicObject noSuperMethodError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        String message = "super called outside of method";
        DynamicObject noMethodError = ExceptionNodes.createRubyException(context.getCoreLibrary().getNoMethodErrorClass(),
                StringNodes.createString(context.getCoreLibrary().getStringClass(), message),
                RubyCallStack.getBacktrace(currentNode));
        BasicObjectNodes.setInstanceVariable(noMethodError, "@name", nilObject);
        return noMethodError;
    }

    public DynamicObject noMethodErrorOnModule(String name, DynamicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return noMethodError(String.format("undefined method `%s' for %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    public DynamicObject noMethodErrorOnReceiver(String name, Object receiver, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        DynamicObject logicalClass = getLogicalClass(receiver);
        String repr = Layouts.MODULE.getFields(logicalClass).getName();
        if (RubyGuards.isRubyModule(receiver)) {
            repr = Layouts.MODULE.getFields(((DynamicObject) receiver)).getName() + ":" + repr;
        }
        return noMethodError(String.format("undefined method `%s' for %s", name, repr), name, currentNode);
    }

    public DynamicObject privateMethodError(String name, DynamicObject module, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyModule(module);
        return noMethodError(String.format("private method `%s' called for %s", name, Layouts.MODULE.getFields(module).getName()), name, currentNode);
    }

    public DynamicObject loadError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(context.getCoreLibrary().getLoadErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject loadErrorCannotLoad(String name, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return loadError(String.format("cannot load such file -- %s", name), currentNode);
    }

    public DynamicObject zeroDivisionError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(context.getCoreLibrary().getZeroDivisionErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), "divided by 0"), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject notImplementedError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(notImplementedErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Method %s not implemented", message)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject syntaxError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(syntaxErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject floatDomainError(String value, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(floatDomainErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), value), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject mathDomainError(String method, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EDOM), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Numerical argument is out of domain - \"%s\"", method)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject invalidArgumentError(String value, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EINVAL), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Invalid argument -  %s", value)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject ioError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(ioErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Error reading file -  %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject badAddressError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EFAULT), StringNodes.createString(context.getCoreLibrary().getStringClass(), "Bad address"), RubyCallStack.getBacktrace(currentNode));
    }


    public DynamicObject badFileDescriptor(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EBADF), StringNodes.createString(context.getCoreLibrary().getStringClass(), "Bad file descriptor"), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject fileExistsError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EEXIST), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("File exists - %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject fileNotFoundError(String fileName, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.ENOENT), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("No such file or directory -  %s", fileName)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject dirNotEmptyError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.ENOTEMPTY), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Directory not empty - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject operationNotPermittedError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EPERM), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Operation not permitted - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject permissionDeniedError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.EACCES), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Permission denied - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject notDirectoryError(String path, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(getErrnoClass(Errno.ENOTDIR), StringNodes.createString(context.getCoreLibrary().getStringClass(), String.format("Not a directory - %s", path)), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject rangeError(int code, DynamicObject encoding, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isRubyEncoding(encoding);
        return rangeError(String.format("invalid codepoint %x in %s", code, Layouts.ENCODING.getEncoding(encoding)), currentNode);
    }

    public DynamicObject rangeError(String type, String value, String range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return rangeError(String.format("%s %s out of range of %s", type, value, range), currentNode);
    }

    public DynamicObject rangeError(DynamicObject range, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        assert RubyGuards.isIntegerFixnumRange(range);
        return rangeError(String.format("%d..%s%d out of range",
                Layouts.INTEGER_FIXNUM_RANGE.getBegin(range),
                Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? "." : "",
                Layouts.INTEGER_FIXNUM_RANGE.getEnd(range)), currentNode);
    }

    public DynamicObject rangeError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(rangeErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject internalError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(context.getCoreLibrary().getRubyTruffleErrorClass(), StringNodes.createString(context.getCoreLibrary().getStringClass(), "internal implementation error - " + message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject regexpError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(regexpErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject encodingCompatibilityErrorIncompatible(String a, String b, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return encodingCompatibilityError(String.format("incompatible character encodings: %s and %s", a, b), currentNode);
    }

    public DynamicObject encodingCompatibilityError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(encodingCompatibilityErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject fiberError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(fiberErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject deadFiberCalledError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return fiberError("dead fiber called", currentNode);
    }

    public DynamicObject yieldFromRootFiberError(Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return fiberError("can't yield from root fiber", currentNode);
    }

    public DynamicObject threadError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(threadErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject securityError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(securityErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public DynamicObject systemCallError(String message, Node currentNode) {
        CompilerAsserts.neverPartOfCompilation();
        return ExceptionNodes.createRubyException(systemCallErrorClass, StringNodes.createString(context.getCoreLibrary().getStringClass(), message), RubyCallStack.getBacktrace(currentNode));
    }

    public RubyContext getContext() {
        return context;
    }

    public DynamicObject getArrayClass() {
        return arrayClass;
    }

    public DynamicObject getBasicObjectClass() {
        return basicObjectClass;
    }

    public DynamicObject getBignumClass() {
        return bignumClass;
    }

    public DynamicObject getBigDecimalClass() {
        return bigDecimalClass;
    }

    public DynamicObject getBindingClass() {
        return bindingClass;
    }

    public DynamicObject getClassClass() {
        return classClass;
    }

    public DynamicObject getFalseClass() {
        return falseClass;
    }

    public DynamicObject getFiberClass() {
        return fiberClass;
    }

    public DynamicObject getFixnumClass() {
        return fixnumClass;
    }

    public DynamicObject getFloatClass() {
        return floatClass;
    }

    public DynamicObject getHashClass() {
        return hashClass;
    }

    public DynamicObject getStandardErrorClass() { return standardErrorClass; }

    public DynamicObject getLoadErrorClass() {
        return loadErrorClass;
    }

    public DynamicObject getMatchDataClass() {
        return matchDataClass;
    }

    public DynamicObject getModuleClass() {
        return moduleClass;
    }

    public DynamicObject getNameErrorClass() {
        return nameErrorClass;
    }

    public DynamicObject getNilClass() {
        return nilClass;
    }

    public DynamicObject getRubyInternalMethod() {
        return rubyInternalMethod;
    }

    public DynamicObject getNoMethodErrorClass() {
        return noMethodErrorClass;
    }

    public DynamicObject getObjectClass() {
        return objectClass;
    }

    public DynamicObject getProcClass() {
        return procClass;
    }

    public DynamicObject getRangeClass() {
        return rangeClass;
    }

    public DynamicObject getRationalClass() {
        return rationalClass;
    }

    public DynamicObject getRegexpClass() {
        return regexpClass;
    }

    public DynamicObject getRubyTruffleErrorClass() {
        return rubyTruffleErrorClass;
    }

    public DynamicObject getRuntimeErrorClass() {
        return runtimeErrorClass;
    }

    public DynamicObject getStringClass() {
        return stringClass;
    }

    public DynamicObject getThreadClass() {
        return threadClass;
    }

    public DynamicObject getTimeClass() {
        return timeClass;
    }

    public DynamicObject getTypeErrorClass() { return typeErrorClass; }

    public DynamicObject getTrueClass() {
        return trueClass;
    }

    public DynamicObject getZeroDivisionErrorClass() {
        return zeroDivisionErrorClass;
    }

    public DynamicObject getKernelModule() {
        return kernelModule;
    }

    public DynamicObject getArgv() {
        return argv;
    }

    public DynamicObject getGlobalVariablesObject() {
        return globalVariablesObject;
    }

    public DynamicObject getLoadPath() {
        return (DynamicObject) BasicObjectNodes.getInstanceVariable(globalVariablesObject, "$LOAD_PATH");
    }

    public DynamicObject getLoadedFeatures() {
        return (DynamicObject) BasicObjectNodes.getInstanceVariable(globalVariablesObject, "$LOADED_FEATURES");
    }

    public DynamicObject getMainObject() {
        return mainObject;
    }

    public DynamicObject getNilObject() {
        return nilObject;
    }

    public DynamicObject getENV() {
        return (DynamicObject) Layouts.MODULE.getFields(objectClass).getConstants().get("ENV").getValue();
    }

    public ArrayNodes.MinBlock getArrayMinBlock() {
        return arrayMinBlock;
    }

    public ArrayNodes.MaxBlock getArrayMaxBlock() {
        return arrayMaxBlock;
    }

    public DynamicObject getNumericClass() {
        return numericClass;
    }

    public DynamicObject getIntegerClass() {
        return integerClass;
    }

    public DynamicObject getEncodingConverterClass() {
        return encodingConverterClass;
    }

    public DynamicObject getUnboundMethodClass() {
        return unboundMethodClass;
    }

    public DynamicObject getMethodClass() {
        return methodClass;
    }

    public DynamicObject getComplexClass() {
        return complexClass;
    }

    public DynamicObject getByteArrayClass() {
        return byteArrayClass;
    }

    public DynamicObject getLookupTableClass() {
        return lookupTableClass;
    }

    public DynamicObject getStringDataClass() {
        return stringDataClass;
    }

    public DynamicObject getTranscodingClass() {
        return transcodingClass;
    }

    public DynamicObject getTupleClass() {
        return tupleClass;
    }

    public DynamicObject getRubiniusChannelClass() {
        return rubiniusChannelClass;
    }

    public DynamicObject getRubiniusFFIPointerClass() {
        return rubiniusFFIPointerClass;
    }

    public DynamicObject getRubiniusMirrorClass() {
        return rubiniusMirrorClass;
    }

    public DynamicObject getRubiniusUndefined() {
        return rubiniusUndefined;
    }

    public DynamicObject getErrnoClass(Errno errno) {
        return errnoClasses.get(errno);
    }

    public DynamicObject getSymbolClass() {
        return symbolClass;
    }

    public DynamicObject getThreadBacktraceLocationClass() {
        return threadBacktraceLocationClass;
    }

    public DynamicObject getInternalBufferClass() {
        return internalBufferClass;
    }

    public boolean isLoadingRubyCore() {
        return state == State.LOADING_RUBY_CORE;
    }

    public boolean isLoaded() {
        return state == State.LOADED;
    }

    public boolean isSend(InternalMethod method) {
        return method.getCallTarget() == basicObjectSendMethod.getCallTarget();
    }

    public DynamicObjectFactory getIntegerFixnumRangeFactory() {
        return integerFixnumRangeFactory;
    }

    public DynamicObjectFactory getLongFixnumRangeFactory() {
        return longFixnumRangeFactory;
    }

    public DynamicObject getDigestClass() {
        return digestClass;
    }

    public DynamicObjectFactory getArrayFactory() {
        return arrayFactory;
    }

    public DynamicObjectFactory getBignumFactory() {
        return bignumFactory;
    }

    public DynamicObjectFactory getProcFactory() {
        return procFactory;
    }

    public DynamicObjectFactory getStringFactory() {
        return stringFactory;
    }

}
