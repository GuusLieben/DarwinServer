package org.dockbox.hartshorn.hsl;

import org.dockbox.hartshorn.application.context.ApplicationContext;
import org.dockbox.hartshorn.context.DefaultApplicationAwareContext;
import org.dockbox.hartshorn.hsl.customizer.ScriptContext;
import org.dockbox.hartshorn.hsl.runtime.Phase;
import org.dockbox.hartshorn.hsl.runtime.ScriptRuntime;
import org.dockbox.hartshorn.hsl.runtime.StandardRuntime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HslScript extends DefaultApplicationAwareContext {

    private final String source;
    private ScriptContext context;
    private ScriptRuntime runtime;

    protected HslScript(final ApplicationContext context, final String source) {
        super(context);
        this.source = source;
    }

    public static HslScript of(final ApplicationContext context, final String source) {
        return new HslScript(context, source);
    }

    public static HslScript of(final ApplicationContext context, final Path path) throws IOException {
        return of(context, sourceFromPath(path));
    }

    public static HslScript of(final ApplicationContext context, final File file) throws IOException {
        return of(context, file.toPath());
    }

    public static String sourceFromPath(final Path path) throws IOException {
        final List<String> lines = Files.readAllLines(path);
        return String.join("\n", lines);
    }

    protected ScriptRuntime createRuntime() {
        return this.applicationContext().get(StandardRuntime.class);
    }

    protected ScriptRuntime getOrCreateRuntime() {
        if (this.runtime == null) {
            this.runtime = this.createRuntime();
        }
        return this.runtime;
    }

    public ScriptContext resolve() {
        this.context = this.getOrCreateRuntime().run(this.source, Phase.RESOLVING);
        return this.context;
    }

    public ScriptContext evaluate() {
        if (this.context != null) {
            this.context = this.getOrCreateRuntime().run(this.context, Phase.INTERPRETING);
        }
        else {
            this.context = this.resolve();
            this.context = this.evaluate();
        }
        return this.context;
    }

    public String source() {
        return source;
    }

    public ScriptContext scriptContext() {
        return context;
    }

    public ScriptRuntime runtime() {
        return getOrCreateRuntime();
    }
}
