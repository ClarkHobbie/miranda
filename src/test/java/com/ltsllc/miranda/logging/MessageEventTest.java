package com.ltsllc.miranda.logging;

import com.ltsllc.commons.io.ScannerWithUnget;
import com.ltsllc.miranda.TestSuperclass;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageEventTest extends TestSuperclass {

    @Test
    void toStorageString() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setWhere(new Exception());
        messageEvent.setType(MessageEventType.attempted);
        messageEvent.setTime(System.currentTimeMillis());
        UUID uuid = UUID.randomUUID();
        messageEvent.setId(uuid);

        String string = messageEvent.toStorageString();

        assert (string.startsWith("ID: " + uuid));
    }

    @Test
    void readMessageEvent() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setWhere(new Exception());
        messageEvent.setType(MessageEventType.attempted);
        messageEvent.setTime(System.currentTimeMillis());
        UUID uuid = UUID.randomUUID();
        messageEvent.setId(uuid);

        String line = messageEvent.toStorageString();

        MessageEvent readMessage = MessageEvent.readMessageEvent(line);

        assert (uuid.equals(readMessage.getId()));
    }

    @Test
    void readInternals() {
        String line = "ID: 61fd97f7-24e5-405d-b003-32510d5ea1d5 TIME: 1755784468739 TYPE: attempted WHERE: com.ltsllc.miranda.logging.MessageEventTest readMessageEvent MessageEventTest.java 28 jdk.internal.reflect.DirectMethodHandleAccessor invoke DirectMethodHandleAccessor.java 104 java.lang.reflect.Method invoke Method.java 565 org.junit.platform.commons.util.ReflectionUtils invokeMethod ReflectionUtils.java 787 org.junit.platform.commons.support.ReflectionSupport invokeMethod ReflectionSupport.java 479 org.junit.jupiter.engine.execution.MethodInvocation proceed MethodInvocation.java 60 org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation proceed InvocationInterceptorChain.java 131 org.junit.jupiter.engine.extension.TimeoutExtension intercept TimeoutExtension.java 161 org.junit.jupiter.engine.extension.TimeoutExtension interceptTestableMethod TimeoutExtension.java 152 org.junit.jupiter.engine.extension.TimeoutExtension interceptTestMethod TimeoutExtension.java 91 org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall lambda$ofVoidMethod$0 InterceptingExecutableInvoker.java 112 org.junit.jupiter.engine.execution.InterceptingExecutableInvoker lambda$invoke$0 InterceptingExecutableInvoker.java 94 org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation proceed InvocationInterceptorChain.java 106 org.junit.jupiter.engine.execution.InvocationInterceptorChain proceed InvocationInterceptorChain.java 64 org.junit.jupiter.engine.execution.InvocationInterceptorChain chainAndInvoke InvocationInterceptorChain.java 45 org.junit.jupiter.engine.execution.InvocationInterceptorChain invoke InvocationInterceptorChain.java 37 org.junit.jupiter.engine.execution.InterceptingExecutableInvoker invoke InterceptingExecutableInvoker.java 93 org.junit.jupiter.engine.execution.InterceptingExecutableInvoker invoke InterceptingExecutableInvoker.java 87 org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor lambda$invokeTestMethod$4 TestMethodTestDescriptor.java 221 org.junit.platform.engine.support.hierarchical.ThrowableCollector execute ThrowableCollector.java 73 org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor invokeTestMethod TestMethodTestDescriptor.java 217 org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor execute TestMethodTestDescriptor.java 159 org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor execute TestMethodTestDescriptor.java 70 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$6 NodeTestTask.java 157 org.junit.platform.engine.support.hierarchical.ThrowableCollector execute ThrowableCollector.java 73 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$8 NodeTestTask.java 147 org.junit.platform.engine.support.hierarchical.Node around Node.java 137 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$9 NodeTestTask.java 145 org.junit.platform.engine.support.hierarchical.ThrowableCollector execute ThrowableCollector.java 73 org.junit.platform.engine.support.hierarchical.NodeTestTask executeRecursively NodeTestTask.java 144 org.junit.platform.engine.support.hierarchical.NodeTestTask execute NodeTestTask.java 101 java.util.ArrayList forEach ArrayList.java 1604 org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService invokeAll SameThreadHierarchicalTestExecutorService.java 41 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$6 NodeTestTask.java 161 org.junit.platform.engine.support.hierarchical.ThrowableCollector execute ThrowableCollector.java 73 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$8 NodeTestTask.java 147 org.junit.platform.engine.support.hierarchical.Node around Node.java 137 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$9 NodeTestTask.java 145 org.junit.platform.engine.support.hierarchical.ThrowableCollector execute ThrowableCollector.java 73 org.junit.platform.engine.support.hierarchical.NodeTestTask executeRecursively NodeTestTask.java 144 org.junit.platform.engine.support.hierarchical.NodeTestTask execute NodeTestTask.java 101 java.util.ArrayList forEach ArrayList.java 1604 org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService invokeAll SameThreadHierarchicalTestExecutorService.java 41 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$6 NodeTestTask.java 161 org.junit.platform.engine.support.hierarchical.ThrowableCollector execute ThrowableCollector.java 73 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$8 NodeTestTask.java 147 org.junit.platform.engine.support.hierarchical.Node around Node.java 137 org.junit.platform.engine.support.hierarchical.NodeTestTask lambda$executeRecursively$9 NodeTestTask.java 145 org.junit.platform.engine.support.hierarchical.ThrowableCollector execute ThrowableCollector.java 73 org.junit.platform.engine.support.hierarchical.NodeTestTask executeRecursively NodeTestTask.java 144 org.junit.platform.engine.support.hierarchical.NodeTestTask execute NodeTestTask.java 101 org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService submit SameThreadHierarchicalTestExecutorService.java 35 org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor execute HierarchicalTestExecutor.java 57 org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine execute HierarchicalTestEngine.java 54 org.junit.platform.launcher.core.EngineExecutionOrchestrator executeEngine EngineExecutionOrchestrator.java 230 org.junit.platform.launcher.core.EngineExecutionOrchestrator failOrExecuteEngine EngineExecutionOrchestrator.java 204 org.junit.platform.launcher.core.EngineExecutionOrchestrator execute EngineExecutionOrchestrator.java 172 org.junit.platform.launcher.core.EngineExecutionOrchestrator execute EngineExecutionOrchestrator.java 101 org.junit.platform.launcher.core.EngineExecutionOrchestrator lambda$execute$0 EngineExecutionOrchestrator.java 64 org.junit.platform.launcher.core.EngineExecutionOrchestrator withInterceptedStreams EngineExecutionOrchestrator.java 150 org.junit.platform.launcher.core.EngineExecutionOrchestrator execute EngineExecutionOrchestrator.java 63 org.junit.platform.launcher.core.DefaultLauncher execute DefaultLauncher.java 109 org.junit.platform.launcher.core.DefaultLauncher execute DefaultLauncher.java 91 org.junit.platform.launcher.core.DelegatingLauncher execute DelegatingLauncher.java 47 org.junit.platform.launcher.core.InterceptingLauncher lambda$execute$1 InterceptingLauncher.java 39 org.junit.platform.launcher.core.ClasspathAlignmentCheckingLauncherInterceptor intercept ClasspathAlignmentCheckingLauncherInterceptor.java 25 org.junit.platform.launcher.core.InterceptingLauncher execute InterceptingLauncher.java 38 org.junit.platform.launcher.core.DelegatingLauncher execute DelegatingLauncher.java 47 org.junit.platform.launcher.core.SessionPerRequestLauncher execute SessionPerRequestLauncher.java 66 com.intellij.junit5.JUnit5IdeaTestRunner startRunnerWithArgs JUnit5IdeaTestRunner.java 66 com.intellij.rt.junit.IdeaTestRunner$Repeater$1 execute IdeaTestRunner.java 38 com.intellij.rt.execution.junit.TestsRepeater repeat TestsRepeater.java 11 com.intellij.rt.junit.IdeaTestRunner$Repeater startRunnerWithArgs IdeaTestRunner.java 35 com.intellij.rt.junit.JUnitStarter prepareStreamsAndStart JUnitStarter.java 231 com.intellij.rt.junit.JUnitStarter main JUnitStarter.java 55";
        Scanner scanner = new Scanner(line);
        ScannerWithUnget scannerWithUnget = new ScannerWithUnget(scanner);
        UUID uuid = UUID.fromString("61fd97f7-24e5-405d-b003-32510d5ea1d5");
        MessageEvent messageEvent = new MessageEvent();

        MessageEvent.readInternals(scannerWithUnget, messageEvent);

        assert (messageEvent.getId().equals(uuid));
    }
}