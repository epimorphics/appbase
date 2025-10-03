package com.epimorphics.appbase.task;

import com.epimorphics.appbase.tasks.RunShell;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestRunShell {

    @Test
    public void testSuccessScript() throws InterruptedException, ExecutionException {
        boolean status = new RunShell("src/test/actionApp/scripts/helloEnv.sh").run("arg").get();
        assertTrue(status);
    }

    @Test
    public void testFailedScript() throws InterruptedException, ExecutionException {
        boolean status = new RunShell("src/test/actionApp/scripts/fail.sh").run().get();
        assertFalse(status);
    }
}
