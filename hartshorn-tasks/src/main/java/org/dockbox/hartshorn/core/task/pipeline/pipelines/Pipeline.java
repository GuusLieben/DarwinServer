/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dockbox.hartshorn.core.task.pipeline.pipelines;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.dockbox.hartshorn.core.domain.Exceptional;
import org.dockbox.hartshorn.core.task.pipeline.pipes.IPipe;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @deprecated Moved to https://github.com/GuusLieben/JPipelines
 */
@Deprecated(forRemoval = true, since = "22.2")
public class Pipeline<I> extends AbstractPipeline<I, I> {

    /**
     * Processes an input by first wrapping it in an {@link Exceptional} and calling {@link
     * Pipeline#process(Exceptional)} on it.
     *
     * @param input The non-null {@code I} input value
     * @param throwable The nullable input {@link Throwable}
     *
     * @return An {@link Exceptional} containing the output. If the output is not present it will contain a throwable describing why
     */
    @Override
    public Exceptional<I> process(@NonNull final I input, @Nullable final Throwable throwable) {
        final Exceptional<I> exceptionalInput = Exceptional.of(input, throwable);

        return this.process(exceptionalInput);
    }

    /**
     * Processes an {@link Exceptional input} by calling {@link AbstractPipeline#processPipe(IPipe,
     * Exceptional)} on each {@link IPipe} in the pipeline and then returns the output wrapped in an
     * {@link Exceptional}.
     *
     * @param exceptionalInput A non-null {@link Exceptional} which contains the input value and throwable
     *
     * @return An {@link Exceptional} containing the output after it has been processed by the pipeline
     */
    @Override
    protected Exceptional<I> process(@NonNull Exceptional<I> exceptionalInput) {
        for (final IPipe<I, I> pipe : this.pipes()) {
            exceptionalInput = super.processPipe(pipe, exceptionalInput);

            // If the pipelines been cancelled, stop processing any further pipes.
            if (this.cancelled()) {
                // Reset it straight after it's been detected for next time the pipeline's used.
                this.permit();
                return Exceptional.of(
                        (I) super.cancelBehaviour().act(exceptionalInput.orNull()),
                        exceptionalInput.unsafeError()
                );
            }
        }

        return exceptionalInput;
    }
}
