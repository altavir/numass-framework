package hep.dataforge.grind.helpers

import groovy.transform.CompileStatic
import hep.dataforge.context.Context
import hep.dataforge.io.output.Output
import hep.dataforge.io.output.SelfRendered
import hep.dataforge.io.output.TextOutput
import hep.dataforge.meta.Meta
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.*
import java.lang.reflect.Method

@CompileStatic
abstract class AbstractHelper implements GrindHelper, SelfRendered {
    private final Context context;

    AbstractHelper(Context context) {
        this.context = context
    }

    @Override
    Context getContext() {
        return context
    }

    /**
     * get the list of all methods that need describing
     * @return
     */
    protected Collection<Method> listDescribedMethods() {
        return getClass().getDeclaredMethods()
                .findAll { it.isAnnotationPresent(MethodDescription) }
    }

    protected abstract void renderDescription(@NotNull TextOutput output, @NotNull Meta meta)

    @Override
    void render(@NotNull Output output, @NotNull Meta meta) {
        if (output instanceof TextOutput) {
            TextOutput textOutput = output
            renderDescription(textOutput, meta)
            listDescribedMethods().each {
                textOutput.renderText(it.name, Color.MAGENTA)

                if (it.parameters) {
                    textOutput.renderText(" (")
                    for (int i = 0; i < it.parameters.length; i++) {
                        def par = it.parameters[i]
                        textOutput.renderText(par.type.simpleName, Color.BLUE)
                        if (i != it.parameters.length - 1) {
                            textOutput.renderText(", ")
                        }
                    }
                    textOutput.renderText(")")

                }

                textOutput.renderText(": ")
                textOutput.renderText(it.getAnnotation(MethodDescription).value())
                textOutput.newLine(meta)
            }
        }
    }

    Logger getLogger() {
        return LoggerFactory.getLogger(getClass())
    }
}
